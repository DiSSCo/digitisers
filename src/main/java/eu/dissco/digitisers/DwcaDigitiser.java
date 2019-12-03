package eu.dissco.digitisers;

import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryException;
import eu.dissco.digitisers.processors.DigitalObjectProcessor;
import eu.dissco.digitisers.readers.DwcaReader;
import eu.dissco.digitisers.processors.DigitalObjectVisitor;
import eu.dissco.digitisers.utils.*;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

import java.util.*;
import java.util.concurrent.*;

public class DwcaDigitiser extends Digitiser {

    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Create a new DwcaDigitiser
     * @param configFilePath path of the configuration file to be used in this digitiser
     * @throws ConfigurationException
     */
    public DwcaDigitiser(String configFilePath) throws ConfigurationException {
        super(configFilePath);
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Digitise the dwca file passed a parameter
     * @param dwcaFilePath path of the dwca file to be digitised
     * @throws DigitalObjectRepositoryException
     */
    public void digitiseDigitalSpecimensFromDwcaFile(String dwcaFilePath) throws DigitalObjectRepositoryException {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-f", dwcaFilePath));
        this.digitise(commandLineArgs);
    }

    /**
     * Digitise all the dwca files found in the folder passed a parameter
     * @param folderPath path of folder form which we want to digitise all its dwca files
     * @throws DigitalObjectRepositoryException
     */
    public void digitiseDigitalSpecimensFromFolder(String folderPath) throws DigitalObjectRepositoryException {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-d", folderPath));
        this.digitise(commandLineArgs);
    }

    /**
     * Digitise the dwca file passed as url
     * @param sUrl url of the dwca file to be digitised
     * @throws DigitalObjectRepositoryException
     */
    public void digitiseDigitalSpecimensFromUrl(String sUrl) throws DigitalObjectRepositoryException {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-u", sUrl));
        this.digitise(commandLineArgs);
    }


    /*********************/
    /* PROTECTED METHODS */
    /*********************/

    /**
     * Function that reads data from a dwca-file indicated in the command line parameter -f dwcaFilePath or in parameter -u dwcaURL
     * or all the dwca files found in the directory indicated in the command line paramer -d directoryPath.
     * Note: When processing a dwca file, if there is any entry that fails to be parsed as digital specimen,
     * the system will report that error in the log file, but it will continue to process the following entries.
     * In the same way, when processing a directory, the system won't stop if an entry in a file failed, it carry on
     * evaluating that file and the entries in successive files
     * @param args
     *  -f dwca input file path to process
     *  -d directory path of dwca files to process
     *  -u url of dwca file to process
     */
    @Override
    protected void digitiseDigitalSpecimensData(List<String> args) {
        Options options = new Options();
        Option fileParameter = new Option("f", "file", true, "dwca input file path to process");
        fileParameter.setRequired(false);
        options.addOption(fileParameter);

        Option folderParameter = new Option("d", "directory", true, "directory path of dwca files to process");
        folderParameter.setRequired(false);
        options.addOption(folderParameter);

        Option urlParameter = new Option("u", "url", true, "url of dwca file to process");
        urlParameter.setRequired(false);
        options.addOption(urlParameter);

        List<File> dwcaFiles = new ArrayList<File>();
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args.toArray(new String[args.size()]));
            if (!commandLine.hasOption("f") &&  !commandLine.hasOption("d") &&  !commandLine.hasOption("u")){
                throw new ParseException("Please specified the path of the dwca file (-f) or its http(s) url, or the directory with dwca files (-d) to process ");
            } else if (commandLine.getOptions().length!=1){
                throw new ParseException("Please specified ONLY one option at a time: file, url or directory");
            }

            if (commandLine.hasOption("f")) {
                File dwcaFile = this.getDwcaFileFromFilePath(commandLine.getOptionValue("file"));
                dwcaFiles.add(dwcaFile);
            } else if (commandLine.hasOption("d")){
                List<File> files = this.getDwcaFilesFromDirectory(commandLine.getOptionValue("directory"));
                for (File dwcaFile:files) {
                    dwcaFiles.add(dwcaFile);
                }
            } else if (commandLine.hasOption("u")){
                File dwcaFile = this.getDwcaFileFromUrl(commandLine.getOptionValue("url"));
                dwcaFiles.add(dwcaFile);
            }
        } catch (ParseException e) {
            this.getLogger().error(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("discoDigitiser", options);
        }

        //Digitise digital specimens from dwca files
        this.digitiseDigitalSpecimensFromDwcaFiles(dwcaFiles);
    }

    /**
     * Digitise digital specimen from the list of the dwca files
     * @param dwcaFiles List of dwca files to read its digital specimen
     */
    protected void digitiseDigitalSpecimensFromDwcaFiles(List<File> dwcaFiles){
        try{
            //Get list of manageable files.
            List<File> manageableDwcaFiles = this.getListOfManageableFiles(dwcaFiles);
            //Process (digitasie) data on tohse
            this.digitiseManageableFiles(manageableDwcaFiles);
        } catch (Exception e) {
            this.getLogger().error("Unexpected error reading dwcaFiles  " + e.getMessage());
        }
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /**
     * Function that get the list of manageable files from the original list of dwca files.
     * In case of a dwc-a file to big, it split it in several smaller files.
     * @param dwcaFiles
     * @return List of manageable files
     * @throws Exception
     */
    private List<File> getListOfManageableFiles(List<File> dwcaFiles) throws Exception {
        List<File> manageableDwcaFiles;
        int dwcaMaxNumRecordsPerFile = this.getConfig().getInt("digitiser.dwcaMaxNumRecordsPerFile");
        if (dwcaMaxNumRecordsPerFile>0){
            //Get list of manageable files. Splitting big files in several parts
            manageableDwcaFiles = DwcaUtils.getManageableDwcaFiles(dwcaFiles,dwcaMaxNumRecordsPerFile);
        } else{
            //Don't try to split the files into manageable files. Accept them as they are
            manageableDwcaFiles=dwcaFiles;
        }
        return manageableDwcaFiles;
    }

    /**
     * Function that digitise the list of manageable dwca files.
     * Note: By using the visitor design pattern, as soon as a digital specimen is read from a dwca file,
     * it uses the visitor to process it.
     * @param manageableDwcaFiles
     * @throws InterruptedException
     */
    private void digitiseManageableFiles(List<File> manageableDwcaFiles) throws InterruptedException {
        //We process each dwc-a as a task that can run in parallel with other
        List<Callable<Void>> taskList = new ArrayList<>();
        for (File dwcaFile:manageableDwcaFiles) {
            Callable<Void> task = () -> {
                // Create visitor to be used to process the digital specimens as soon as they are read
                DigitalObjectVisitor digitalObjectVisitor = new DigitalObjectProcessor(this.getConfig());
                DwcaReader dwcaReader = new DwcaReader();
                //Read data inside the dwca file
                dwcaReader.readDigitalSpecimensFromDwcaFile(dwcaFile,digitalObjectVisitor);
                return null;
            };
            taskList.add(task);
        }

        //Create executor service
        //ExecutorService executorService = Executors.newCachedThreadPool();
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(this.getConfig().getInt("digitiser.maxNumberOfThreads"),taskList.size()));

        //Submit all tasks to executorService
        List<Future<Void>> futures = executorService.invokeAll(taskList);

        //Get the results from the tasks (number of digital specimens read in each file)
        for(Future<Void> future: futures) {
            try{
                future.get();
            } catch (Exception e) {
                // interrupts if there is any possible error
                future.cancel(true);
            }
        }

        //Wait until all threads finished
        executorService.shutdown();
        boolean finished = executorService.awaitTermination(6,TimeUnit.HOURS);
        if (!finished){
            this.getLogger().warn("Some of the readDigitalSpecimensFromDwcaFile tasks didn't finished on time");
        }
    }

    /**
     * Function that gets the dwca file from its file path
     * @param filePath file path of the dwca file
     * @return Dwca File
     * @throws ParseException
     */
    private File getDwcaFileFromFilePath(String filePath) throws ParseException {
        File dwcaFile = new File(filePath);
        if (!dwcaFile.exists()) {
            throw new ParseException("File " + dwcaFile + " doesn't exits");
        }
        return dwcaFile;
    }

    /**
     * Function that gets (download) the dwca file from its url
     * @param sDwcaURL string of the url of the dwca file
     * @return Dwca File
     * @throws ParseException
     */
    private File getDwcaFileFromUrl(String sDwcaURL) throws ParseException {
        try {
            File dwcaFile = NetUtils.downloadFile(sDwcaURL);
            return dwcaFile;
        } catch (Exception e){
            throw new ParseException("Fail to download dwca file from URL " + sDwcaURL + " " + e.getMessage());
        }
    }

    /**
     * Function that return the list of dwca files (zip files) in the given directory
     * @param directoryPath directory from which to get the list of its dwca files
     * @return List with the dwca files in the directory
     * @throws ParseException
     */
    private List<File> getDwcaFilesFromDirectory(String directoryPath) throws ParseException {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            throw new ParseException("Directory " + directoryPath + " doesn't exits");
        }
        File[] files = directory.listFiles((d, name) -> name.endsWith(".zip"));
        return new ArrayList<>(Arrays.asList(files));
    }
}
