package eu.dissco.digitisers.utils;

import com.google.common.io.Files;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.DwcFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DwcaUtils {

    /******************/
    /* STATIC METHODS */
    /******************/

    /**
     * Function that get the list of manageable files from the original list of dwca files.
     * In case of a dwc-a file to big, it split it in several smaller files.
     * Note: It tries to obtain the list of manageable files for each original dwca-file pass concurrently
     * @param dwcaFiles List of dwca files to get a list of manageable files
     * @param maxNumRecordsPerFile Maximum numbers of record that the core file of each manageable new dwca-file should have
     * @return List of manageable dwca files
     * @throws Exception
     */
    public static List<File> getManageableDwcaFiles(List<File> dwcaFiles, int maxNumRecordsPerFile) throws Exception {
        //We process each dwc-a file as a task that can run in parallel with other
        List<Callable<List<File>>> taskList = new ArrayList<>();
        for (File dwcaFile:dwcaFiles) {
            Callable<List<File>> task = () -> {
                DwcaUtils dwcaUtils = new DwcaUtils();
                return dwcaUtils.getManageableDwcaFiles(dwcaFile,maxNumRecordsPerFile);
            };
            taskList.add(task);
        }

        //Create executor service
        ExecutorService executorService = Executors.newCachedThreadPool();

        //Submit all tasks to executorService
        List<Future<List<File>>> futures = executorService.invokeAll(taskList);

        //Get the results from the tasks (list of manageable files for each original dwca file)
        List<File> manageableDwcaFiles = new ArrayList<File>();
        for(Future<List<File>> future: futures) {
            try{
                manageableDwcaFiles.addAll(future.get());
            } catch (Exception e) {
                // interrupts if there is any possible error
                future.cancel(true);
            }
        }

        //Wait until all threads finishe
        executorService.shutdown();
        boolean finished = executorService.awaitTermination(6, TimeUnit.HOURS);
        if (!finished){
            LoggerFactory.getLogger(DwcaUtils.class).warn("Some of the readDigitalSpecimensFromDwcaFile tasks didn't finished on time");
        }

        return manageableDwcaFiles;
    }


    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String dbFile;


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    protected String getDbFile() {
        return dbFile;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Create a new DwcaUtils
     * @throws IOException
     */
    private DwcaUtils() throws IOException {
        File tempDbFile = new File(com.google.common.io.Files.createTempDir(), "test.db");
        this.dbFile = tempDbFile.getAbsolutePath();
    }

    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /**
     * Function that split a dwc-a file into manageable parts. It does that by loading its content into
     * a sqlite file database and then by using pagination exporting the result of each "page" into new dwca-file
     * @param dwcaFile Dwca file to get a list of manageable files
     * @param maxNumRecordsPerFile Maximum numbers of record that the core file of each manageable new dwca-file should have
     * @return
     * @throws Exception
     */
    private List<File> getManageableDwcaFiles(File dwcaFile, int maxNumRecordsPerFile) throws Exception {
        List<File> manageableDwcaFiles = new ArrayList<File>();

        File tempDir = Files.createTempDir();
        Path extractToFolder = Paths.get(tempDir.getPath());
        Archive dwcArchive = DwcFiles.fromCompressed(Paths.get(dwcaFile.toURI()), extractToFolder);

        //Load data from the dwc-a file into the database
        List<DwcaUtils.FileTableSchema> schemas = this.loadArchiveToDb(dwcArchive);

        //Count number of records in core (reading them from data load in db)
        int numRecordsInCore = this.getNumberOfRecordsInTable(schemas.get(0).getName());

        //Check if we need to split the file into manageable chunks
        if (numRecordsInCore<=maxNumRecordsPerFile){
            manageableDwcaFiles.add(dwcaFile);
        } else{
            //Split big dwc-a file into smaller manageable files
            File metaFile = new File(dwcArchive.getLocation() + "/" + "meta.xml");
            //Get number of parts we need to create
            int numberOfParts = (numRecordsInCore+maxNumRecordsPerFile-1)/maxNumRecordsPerFile;
            //For each part create new dwc-a file, by exporting partially data store in db
            for (int i=1;i<=numberOfParts;i++){
                //Export data store in db that correspond to this part (limit,offset). We export each table into an individual file
                List<File> files = this.exportData(schemas,maxNumRecordsPerFile,maxNumRecordsPerFile*(i-1));

                //Copy all the files into a directory so we can zip it to produce the new dwc-a file for this part
                String newDwcaFileName =  FilenameUtils.removeExtension(dwcaFile.getName()) + "_part_" + i + ".zip";
                File newDwcaFile = new File(com.google.common.io.Files.createTempDir(), newDwcaFileName);
                File newDwcaDir = Files.createTempDir();
                for (File file:files) {
                    org.apache.commons.io.FileUtils.copyFileToDirectory(file, newDwcaDir);
                }
                org.apache.commons.io.FileUtils.copyFileToDirectory(dwcArchive.getMetadataLocationFile(), newDwcaDir);
                org.apache.commons.io.FileUtils.copyFileToDirectory(metaFile, newDwcaDir);

                //Zip the folder
                FileUtils.zipFolder(Paths.get(newDwcaDir.getAbsolutePath()), Paths.get(newDwcaFile.getAbsolutePath()));

                //Add the new dwc-a file (zip file) to the list of manageable files
                manageableDwcaFiles.add(newDwcaFile);
            }
        }
        return manageableDwcaFiles;
    }

    /**
     * Export the subset of data (limit,offset) from each table in the database (table=files in the original dwca file)
     * into several into a file (one file per table)
     * @param schemas Information about the tables (files) from the original dwca file
     * @param limit Number of records from the core table (file) to be exported
     * @param offset Offest applied to the core table
     * @return List of files (one per table) with the subset of data requested (limit,offset)
     * @throws IOException
     */
    private List<File> exportData(List<DwcaUtils.FileTableSchema> schemas, int limit, int offset) throws IOException {
        List<File> files = new ArrayList<File>();
        DwcaUtils.FileTableSchema primaryTableSchema = schemas.get(0);
        String primaryTableName = primaryTableSchema.getName();
        String primaryColumnName = primaryTableSchema.getColumns().get(0);

        try (Connection conn = this.getConnection()){
            try (Statement stmt = conn.createStatement()) {
                for (DwcaUtils.FileTableSchema schema:schemas){
                    //Query to get subset of records in this table to be included in the dwc-a part
                    String sql = "select t1.* from " + schema.getName() + " t1 " +
                            "inner join (" +
                            "       SELECT ["+primaryColumnName+"] FROM (SELECT ROW_NUMBER () OVER (ORDER BY 1) sqlite3DbRowNum, ["+primaryColumnName+"] FROM "+primaryTableName+") t " +
                            "       WHERE sqlite3DbRowNum > " + offset + " AND sqlite3DbRowNum <=" + (offset+limit)  +
                            ") t2 on t1.["+primaryColumnName+"]=t2.["+primaryColumnName+"]";

                    //Run sql query
                    ResultSet rs = stmt.executeQuery(sql);

                    //Save results of sql query into a file
                    File tableFile = new File(com.google.common.io.Files.createTempDir(), schema.getName() +".txt");
                    try (CSVPrinter printer = new CSVPrinter(new FileWriter(tableFile),
                            CSVFormat.newFormat(schema.getSeparator())
                                    .withHeader(rs)
                                    .withRecordSeparator(System.getProperty("line.separator")))) {
                        printer.printRecords(rs);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    //Add file to the list of files to return
                    files.add(tableFile);
                }
            }
        } catch (SQLException e) {
            this.getLogger().error(e.getMessage());
        }
        return files;
    }


    /**
     * Load the dwca file into the database
     * @param archive
     * @return
     * @throws IOException
     */
    private List<DwcaUtils.FileTableSchema> loadArchiveToDb(Archive archive) throws IOException {
        List<DwcaUtils.FileTableSchema> schemas = new ArrayList<DwcaUtils.FileTableSchema>();

        //Load data from core
        DwcaUtils.FileTableSchema coreSchema = this.loadArchiveFileToDb(archive.getCore());
        schemas.add(coreSchema);

        //Load data from extensions
        for (ArchiveFile extension:archive.getExtensions()) {
            DwcaUtils.FileTableSchema extensionSchema = this.loadArchiveFileToDb(extension);
            schemas.add(extensionSchema);
        }
        return schemas;
    }

    /**
     * Load a file from the original dwca file into the database
     * @param archiveFile
     * @return Information about the table saved in the database
     * @throws IOException
     */
    private DwcaUtils.FileTableSchema loadArchiveFileToDb(ArchiveFile archiveFile) throws IOException {
        //Load data from core
        String csvFile = archiveFile.getLocationFile().toString();
        String tableName = FilenameUtils.removeExtension(FilenameUtils.getName(csvFile));
        Character separator = archiveFile.getFieldsTerminatedBy().charAt(0);
        DwcaUtils.FileTableSchema schema = this.loadCsvToDb(csvFile,separator,tableName);
        return schema;
    }

    /**
     * Load a csv file to the database
     * @param csvFile File to be loaded
     * @param separator Separator between fields
     * @param tableName Name of the table to be created
     * @return Information about the table created
     * @throws IOException
     */
    private DwcaUtils.FileTableSchema loadCsvToDb(String csvFile, Character separator, String tableName) throws IOException {
        InputStreamReader input = new InputStreamReader(new FileInputStream(csvFile));
        List<String> headers = new ArrayList<String>();
        try(CSVParser csvParser = CSVFormat.newFormat(separator)
                .withFirstRecordAsHeader()
                .withRecordSeparator(System.getProperty("line.separator"))
                .parse(input)) {

            headers = csvParser.getHeaderNames();

            String dropTableIfExists = "DROP TABLE IF EXISTS " + tableName + ";";

            StringBuilder createTableSb = new StringBuilder();
            createTableSb.append("CREATE TABLE " + tableName + " (");
            for (String header : headers) {
                createTableSb.append("'" + header + "' text NOT NULL,");
            }
            createTableSb.deleteCharAt(Math.max(createTableSb.length() - 1, 0));
            createTableSb.append(");");
            String createTableSql = createTableSb.toString();

            String insertDataSql = "INSERT INTO " + tableName + "('" + String.join("','", headers) + "') VALUES (" + StringUtils.repeat("?,", headers.size() - 1) + "?)";

            try (Connection conn = this.getConnection()) {
                //Create table
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(dropTableIfExists);
                    stmt.execute(createTableSql);
                }

                //Insert data
                try (PreparedStatement pstmt = conn.prepareStatement(insertDataSql)) {
                    for (CSVRecord record : csvParser) {
                        for (int i = 0; i < headers.size(); i++) {
                            pstmt.setString(i + 1, record.get(i));
                        }
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            } catch (SQLException e) {
                this.getLogger().error(e.getMessage());
            }
        }
        return new DwcaUtils.FileTableSchema(tableName,headers,separator);
    }

    /**
     * Get the connection to the sqlite3 file database
     * @return Connection to the database
     */
    private Connection getConnection() {
        String url = "jdbc:sqlite:" + this.getDbFile();
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            this.getLogger().error(e.getMessage());
        }
        return conn;
    }

    /**
     * Get the number of records in a table
     * @param tableName Table for which we want to obtain its number of records
     * @return Number of records (rows) in the table
     */
    private int getNumberOfRecordsInTable(String tableName){
        int numRecords=0;
        try (Connection conn = this.getConnection()){
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("select count(*) from " + tableName);
                while (rs.next()) {
                    numRecords = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            this.getLogger().error(e.getMessage());
        }
        return numRecords;
    }


    /**
     * Private class to pass information about the table information in the database
     */
    class FileTableSchema{
        private String name;
        private List<String> columns;
        private Character separator;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }

        public Character getSeparator() {
            return separator;
        }

        public void setSeparator(Character separator) {
            this.separator = separator;
        }

        public FileTableSchema(String name, List<String> columns, Character separator) {
            this.name = name;
            this.columns = columns;
            this.separator = separator;
        }
    }    
}
