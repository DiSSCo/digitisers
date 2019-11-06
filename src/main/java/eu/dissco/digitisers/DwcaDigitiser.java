package eu.dissco.digitisers;

import eu.dissco.digitisers.dwca.DwcaReader;
import eu.dissco.digitisers.utils.*;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DwcaDigitiser extends DiscoDigitiser {

    public DwcaDigitiser(String configFilePath) throws IOException, URISyntaxException {
        super(configFilePath);
    }


    public List<DigitalObject> digitiseDigitalSpecimensFromDwcaFile(String dwcaFilePath){
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-f", dwcaFilePath));
        return this.startDigitisation(commandLineArgs);
    }

    public List<DigitalObject> digitiseDigitalSpecimensFromFolder(String folderPath){
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-d", folderPath));
        return this.startDigitisation(commandLineArgs);
    }

    public List<DigitalObject> digitiseDigitalSpecimensFromUrl(String sUrl){
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-u", sUrl));
        return this.startDigitisation(commandLineArgs);
    }

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
     * @return List of digital specimens parsed after processing the dwca file(s)
     */
    @Override
    protected List<DigitalObject> readDigitalSpecimensData(List<String> args) {
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

        //Process dwca files
        List<DigitalObject> listDs = this.readDigitalSpecimensFromDwcaFiles(dwcaFiles);
        return listDs;
    }


    protected List<DigitalObject> readDigitalSpecimensFromDwcaFiles(List<File> dwcaFiles){
        List<DigitalObject> listDs = new ArrayList<DigitalObject>();
        for (File dwcaFile:dwcaFiles) {
            List<DigitalObject> listDsInDwcaFile = this.readDigitalSpecimensFromDwcaFile(dwcaFile);
            listDs.addAll(listDsInDwcaFile);
        }
        return listDs;
    }

    protected List<DigitalObject> readDigitalSpecimensFromDwcaFile(File dwcaFile){
        DwcaReader dwcaReader = new DwcaReader();
        return dwcaReader.parseContentDwcaToDigitalSpecimens(dwcaFile);
    }

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
