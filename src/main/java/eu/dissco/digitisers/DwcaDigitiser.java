package eu.dissco.digitisers;

import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryException;
import eu.dissco.digitisers.readers.DwcaReader;
import eu.dissco.digitisers.tasks.DigitalObjectVisitor;
import eu.dissco.digitisers.utils.*;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DwcaDigitiser extends Digitiser {

    /****************/
    /* CONSTRUCTORS */
    /****************/

    public DwcaDigitiser(String configFilePath) throws ConfigurationException {
        super(configFilePath);
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    public void digitiseDigitalSpecimensFromDwcaFile(String dwcaFilePath) throws DigitalObjectRepositoryException {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-f", dwcaFilePath));
        this.digitise(commandLineArgs);
    }

    public void digitiseDigitalSpecimensFromFolder(String folderPath) throws DigitalObjectRepositoryException {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-d", folderPath));
        this.digitise(commandLineArgs);
    }

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
     * @return Number of digital specimens parsed after reading the dwca file(s)
     */
    @Override
    protected int readDigitalSpecimensData(List<String> args, DigitalObjectVisitor digitalObjectVisitor) {
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
                File[] files = this.getDwcaFilesFromDirectory(commandLine.getOptionValue("directory"));
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

        //Read dwca files
        return this.readDigitalSpecimensFromDwcaFiles(dwcaFiles,digitalObjectVisitor);
    }

    protected int readDigitalSpecimensFromDwcaFiles(List<File> dwcaFiles, DigitalObjectVisitor digitalObjectVisitor){
        int totalNumberDsRead = 0;
        for (File dwcaFile:dwcaFiles) {
            totalNumberDsRead = totalNumberDsRead + this.readDigitalSpecimensFromDwcaFile(dwcaFile,digitalObjectVisitor);
        }
        return totalNumberDsRead;
    }

    protected int readDigitalSpecimensFromDwcaFile(File dwcaFile, DigitalObjectVisitor digitalObjectVisitor){
        DwcaReader dwcaReader = new DwcaReader(digitalObjectVisitor);
        return dwcaReader.readDigitalSpecimensFromDwcaFile(dwcaFile);
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    private File getDwcaFileFromFilePath(String filePath) throws ParseException {
        File dwcaFile = new File(filePath);
        if (!dwcaFile.exists()) {
            throw new ParseException("File " + dwcaFile + " doesn't exits");
        }
        return dwcaFile;
    }

    private File getDwcaFileFromUrl(String sDwcaURL) throws ParseException {
        try {
            File dwcaFile = NetUtils.downloadFile(sDwcaURL);
            return dwcaFile;
        } catch (Exception e){
            throw new ParseException("Fail to download dwca file from URL " + sDwcaURL + " " + e.getMessage());
        }
    }

    private File[] getDwcaFilesFromDirectory(String directoryPath) throws ParseException {
        Path folderPath = Paths.get(directoryPath);
        if (!java.nio.file.Files.exists(folderPath)) {
            throw new ParseException("Directory " + folderPath + " doesn't exits");
        }
        File[] files = folderPath.toFile().listFiles((d, name) -> name.endsWith(".zip"));
        return files;
    }
}
