package eu.dissco.digitisers;

import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryException;
import eu.dissco.digitisers.clients.gbif.*;
import eu.dissco.digitisers.tasks.DigitalObjectVisitor;
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
    public GbifDigitiser(String configFilePath) throws ConfigurationException {
        super(configFilePath);
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    public void digitiseDigitalSpecimensByCanonicalNameAndKindgdom(String canonicalName, String kingdom) throws DigitalObjectRepositoryException {
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-n", canonicalName, "-k", kingdom));
        this.digitise(commandLineArgs);
    }

    public int readDigitalSpecimensData(List<String> args, DigitalObjectVisitor digitalObjectVisitor) {
        Options options = new Options();
        Option scientificNameParameter = new Option("n", "name", true, "canonical scientific name");
        scientificNameParameter.setRequired(true);
        options.addOption(scientificNameParameter);

        Option kingdomNameParameter = new Option("k", "kingdom", true, "kingdom");
        kingdomNameParameter.setRequired(true);
        options.addOption(kingdomNameParameter);

        int numDsRead=0;
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args.toArray(new String[args.size()]));

            String canonicalName = commandLine.getOptionValue("name");
            String kingdom = commandLine.getOptionValue("kingdom");

            numDsRead = readDigitalSpecimensFromGbifOccurrenceDownloadRequest(canonicalName,kingdom,digitalObjectVisitor);
        } catch (ParseException e) {
            this.getLogger().error(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("discoDigitiser", options);
        } catch (Exception e) {
            this.getLogger().error(e.getMessage());
        }
        return numDsRead;
    }

    /*********************/
    /* PROTECTED METHODS */
    /*********************/

    protected int readDigitalSpecimensFromGbifOccurrenceDownloadRequest(String canonicalName, String kingdom, DigitalObjectVisitor digitalObjectVisitor) throws Exception {
        GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
        GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
        File dwcaFile = gbifClient.downloadOccurrencesByCanonicalNameAndKingdom(canonicalName,kingdom);
        return this.readDigitalSpecimensFromDwcaFile(dwcaFile,digitalObjectVisitor);
    }

}
