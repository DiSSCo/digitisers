package eu.dissco.digitisers;

import eu.dissco.digitisers.clients.gbif.*;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GbifDigitiser extends DwcaDigitiser{

    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Create a new GbifDigitiser
     * @param configFilePath path of the configuration file to be used in the this digitiser
     * @throws ConfigurationException
     */
    public GbifDigitiser(String configFilePath) throws ConfigurationException {
        super(configFilePath);
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Digitise specimens for the canonical name and kingdom passed as parameter.
     * This function will download Specimens (preserved, living and fossil) from GBIF occurrance service as dwca file
     * and then processes it
     * @param canonicalName canonical name of the species to obtain its specimens
     * @param kingdom name of the kingdom to unequivocally identify the taxon concept for which to obtain its specimens
     */
    public void digitiseDigitalSpecimensByCanonicalNameAndKingdom(String canonicalName, String kingdom) {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-n", canonicalName, "-k", kingdom));
        this.digitise(commandLineArgs);
    }


    /*********************/
    /* PROTECTED METHODS */
    /*********************/

    /**
     * Function download from gbif all specimens (preserved, living and fossil) as dwca file for the canonical name and
     * kingdom name passed as parameter. Once the dwca file is download it process it
     * @param args
     *  -n name canonical name of the species to obtain its specimens
     *  -k kingdom name of the kingdom to unequivocally identify the taxon concept for which to obtain its specimens
     */
    @Override
    protected void digitiseDigitalSpecimensData(List<String> args) {
        Options options = new Options();
        Option scientificNameParameter = new Option("n", "name", true, "canonical scientific name");
        scientificNameParameter.setRequired(true);
        options.addOption(scientificNameParameter);

        Option kingdomNameParameter = new Option("k", "kingdom", true, "kingdom");
        kingdomNameParameter.setRequired(true);
        options.addOption(kingdomNameParameter);

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args.toArray(new String[args.size()]));

            String canonicalName = commandLine.getOptionValue("name");
            String kingdom = commandLine.getOptionValue("kingdom");

            this.readDigitalSpecimensFromGbifOccurrenceDownloadRequest(canonicalName,kingdom);
        } catch (ParseException e) {
            this.getLogger().error(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("discoDigitiser", options);
        } catch (Exception e) {
            this.getLogger().error(e.getMessage());
        }
    }

    /**
     * Function download from gbif all specimens (preserved, living and fossil) as dwca file for the canonical name and
     * kingdom name passed as parameter. Once the dwca file is download it process it
     * @param canonicalName canonical name of the species to obtain its specimens
     * @param kingdom name of the kingdom to unequivocally identify the taxon concept for which to obtain its specimens
     * @throws Exception
     */
    protected void readDigitalSpecimensFromGbifOccurrenceDownloadRequest(String canonicalName, String kingdom) throws Exception {
        GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
        GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
        File dwcaFile = gbifClient.downloadOccurrencesByCanonicalNameAndKingdom(canonicalName,kingdom);
        List<File> dwcaFiles = new ArrayList<File>();
        dwcaFiles.add(dwcaFile);
        this.digitiseDigitalSpecimensFromDwcaFiles(dwcaFiles);
    }

}
