package eu.dissco.digitisers;

import eu.dissco.digitisers.clients.gbif.*;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GbifDigitiser extends DwcaDigitiser{

    public GbifDigitiser(String configFilePath) throws IOException, URISyntaxException {
        super(configFilePath);
    }

    public List<DigitalObject> digitiseDigitalSpecimensByCanonicalNameAndKindgdom(String canonicalName, String kingdom){
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList("-n", canonicalName, "-k", kingdom));
        return this.startDigitisation(commandLineArgs);
    }

    public List<DigitalObject> readDigitalSpecimensData(List<String> args) {
        List<DigitalObject> listDs=new ArrayList<DigitalObject>();

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

            listDs = readDigitalSpecimensFromGbifOccurrenceDownloadRequest(canonicalName,kingdom);
        } catch (ParseException e) {
            this.getLogger().error(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("discoDigitiser", options);
        } catch (Exception e) {
            this.getLogger().error(e.getMessage());
        }
        return listDs;
    }

    protected List<DigitalObject> readDigitalSpecimensFromGbifOccurrenceDownloadRequest(String canonicalName, String kingdom) throws Exception {
        GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
        GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
        File dwcaFile = gbifClient.downloadOccurrencesByCanonicalNameAndKingdom(canonicalName,kingdom);
        return this.readDigitalSpecimensFromDwcaFile(dwcaFile);
    }

}
