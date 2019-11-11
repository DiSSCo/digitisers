package eu.dissco.digitisers;

import org.apache.commons.cli.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DigitiserFactory {

    private final static Logger logger = LoggerFactory.getLogger(DigitiserFactory.class);

    /**
     * Get the specific DiSSCo digitiser according to the one requested in command line parameter. Using the factory design pattern
     * @param args
     * @return Specific digitiser
     * @throws ParseException
     * @throws ConfigurationException
     */
    public static Digitiser getDigitiser(List<String> args) throws ParseException, ConfigurationException {
        Digitiser digitiser = null;
        Options options = new Options();
        Option fileParameter = new Option("m", "method", true, "digitiser method (dwca,gbif,...)");
        fileParameter.setRequired(true);
        options.addOption(fileParameter);
        Option configFileParameter = new Option("c", "config", true, "config config file path");
        configFileParameter.setRequired(true);
        options.addOption(configFileParameter);

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args.toArray(new String[args.size()]),true);

            String digitiserMethod = commandLine.getOptionValue("method");
            String configPropertiesFilePath = commandLine.getOptionValue("config");
            switch (digitiserMethod) {
                case "dwca":
                    digitiser = new DwcaDigitiser(configPropertiesFilePath);
                    break;
                case "gbif":
                    digitiser = new GbifDigitiser(configPropertiesFilePath);
                    break;
                default:
                    throw new ParseException("Digitisation method not supported");
            }

            //Remove "method" and "config" arguments, as not longer required
            List<String> commandLineArgs = Arrays.asList(commandLine.getArgs());
            for (Iterator<String> iter = args.iterator(); iter.hasNext(); ) {
                String arg = iter.next();
                if (!commandLineArgs.contains(arg)){
                    iter.remove();
                }
            }

        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Digitiser", options);
            throw e;
        }
        return digitiser;
    }
}
