package eu.dissco.digitisers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryException;
import eu.dissco.digitisers.clients.gbif.GbifClient;
import eu.dissco.digitisers.clients.gbif.GbifInfo;
import eu.dissco.digitisers.processors.DigitalObjectProcessor;
import eu.dissco.digitisers.processors.DigitalObjectVisitor;
import eu.dissco.digitisers.readers.Bh2020MappingReader;
import eu.dissco.digitisers.utils.FileUtils;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class Bh2020Digitiser extends Digitiser {

    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Create a new DwcaDigitiser
     * @param configFilePath path of the configuration file to be used in this digitiser
     * @throws ConfigurationException
     */
    public Bh2020Digitiser(String configFilePath) throws ConfigurationException {
        super(configFilePath);
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Digitise specimens using output from the project 33 of the Biohackathon 2020.
     * The input file is in json format and contains a list of specimens found in gbif with the validated list the accession ID in ENA
     * according to the mapping perform by tool created in project 33.
     * @param bh2020mappingFile path of the dwca file to be digitised
     * @throws DigitalObjectRepositoryException
     */
    public void digitiseDigitalSpecimensFromBh2020MappingFile(String bh2020mappingFile) throws DigitalObjectRepositoryException {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-f", bh2020mappingFile));
        this.digitise(commandLineArgs);
    }



    /*********************/
    /* PROTECTED METHODS */
    /*********************/

    /**
     * Function that reads data from a json file indicated in the command line parameter -f bh2020mappingFile.
     * @param args
     *  -f bh2020mappingFile input file path to process
     */
    @Override
    protected void digitiseDigitalSpecimensData(List<String> args) {
        Options options = new Options();
        Option fileParameter = new Option("f", "file", true, "bh2020 mapping file path to process");
        fileParameter.setRequired(false);
        options.addOption(fileParameter);

        List<File> bh2020MappingFiles = new ArrayList<File>();
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args.toArray(new String[args.size()]));
            if (!commandLine.hasOption("f")){
                throw new ParseException("Please specified the path of the bh2020 mapping file (-f) to process ");
            }

            if (commandLine.hasOption("f")) {
                File bh2020MappingFile = new File(commandLine.getOptionValue("file"));
                if (!bh2020MappingFile.exists()) {
                    throw new ParseException("File " + commandLine.getOptionValue("file") + " doesn't exits");
                }
                bh2020MappingFiles.add(bh2020MappingFile);
            }
        } catch (ParseException e) {
            this.getLogger().error(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("discoDigitiser", options);
        }

        //Digitise digital specimens from dwca files
        this.digitiseDigitalSpecimensFromBh2020MappingFiles(bh2020MappingFiles);
    }

    /**
     * Digitise digital specimen from the list of the bh2020's project 30 mapping files
     * @param bh2020MappingFiles List of dwca files to read its digital specimen
     */
    protected void digitiseDigitalSpecimensFromBh2020MappingFiles(List<File> bh2020MappingFiles){
        try{
            //Process (digitise) data on tohse
            this.digitiseBh2020MappingFiles(bh2020MappingFiles);
        } catch (Exception e) {
            this.getLogger().error("Unexpected error reading bh2020MappingFiles  " + e.getMessage());
        }
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /**
     * Function that digitise the list of bh2020 mapping files
     * Note: By using the visitor design pattern, as soon as a digital specimen is read from a bh2020 mapping file,
     * it uses the visitor to process it.
     * @param bh2020MappingFiles
     * @throws InterruptedException
     */
    private void digitiseBh2020MappingFiles(List<File> bh2020MappingFiles) throws InterruptedException {
        GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
        GbifClient gbifClient = GbifClient.getInstance(gbifInfo);

        //We process each dwc-a as a task that can run in parallel with other
        List<Callable<Void>> taskList = new ArrayList<>();
        for (File bh2020MappingFile:bh2020MappingFiles) {
            Callable<Void> task = () -> {
                this.getLogger().info("Parsing bh2020 mapping file " + bh2020MappingFile.toURI() + " into digital specimens ");

                List<String> occurenceIds = new ArrayList<>();
                Map<String, List<String>> mapGbifIdsEnaIds = new HashMap<>();
                JsonArray bh2020GbifEnaMappingsObj = (JsonArray) FileUtils.loadJsonElementFromFilePath(bh2020MappingFile.getAbsolutePath());
                for (JsonElement bh2020GbifEnaMappingObj : bh2020GbifEnaMappingsObj) {
                    String gbifId = bh2020GbifEnaMappingObj.getAsJsonObject().get("gbifID").getAsString();
                    String occurrenceId = gbifClient.getOccurrenceIdByGbifId(gbifId);
                    if (StringUtils.isNotBlank(occurrenceId)){
                        occurenceIds.add(occurrenceId);
                    }
                    JsonArray enaIdsObj =  bh2020GbifEnaMappingObj.getAsJsonObject().get("enaIDs").getAsJsonArray();
                    List<String> enaIds = new ArrayList<>();
                    for (JsonElement enaIdObj : enaIdsObj) {
                        enaIds.add(enaIdObj.getAsString());
                    }
                    mapGbifIdsEnaIds.put(gbifId,enaIds);
                }
                File dwcaFile = gbifClient.downloadOccurrencesByListOccurrenceIds(occurenceIds);
                this.getLogger().info("bh2020 mapping file " + bh2020MappingFile.toURI() + " associated to dwc-a file " + bh2020MappingFile.toURI());

                // Create visitor to be used to process the digital specimens as soon as they are read
                DigitalObjectVisitor digitalObjectVisitor = new DigitalObjectProcessor(this.getConfig());
                Bh2020MappingReader bh2020MappingReader = new Bh2020MappingReader();
                //Read data inside the bh2020 mapping file
                bh2020MappingReader.readDigitalSpecimensFromBh2020MappingFile(mapGbifIdsEnaIds,dwcaFile,digitalObjectVisitor);
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
            this.getLogger().warn("Some of the readDigitalSpecimensFromBh2020MappingFile tasks didn't finished on time");
        }
    }

}
