package eu.dissco.digitisers;

import com.google.common.io.Resources;
import eu.dissco.digitisers.utils.EmailUtils;
import eu.dissco.digitisers.utils.FileUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public abstract class Digitiser {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Configuration config;


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/
    protected Logger getLogger() {
        return logger;
    }

    protected Configuration getConfig() {
        return config;
    }

    protected void setConfig(Configuration config) {
        this.config = config;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Create a Digitiser with properties from the config file
     * @param configFilePath path of the configuration file to be used in this digitiser
     * @throws ConfigurationException
     */
    public Digitiser(String configFilePath) throws ConfigurationException {
        this.config = FileUtils.loadConfigurationFromFilePath(configFilePath);
    }


    /********************/
    /* ABSTRACT METHODS */
    /********************/

    /***
     * Abstract method that needs to be implemented in the specific digitiser classes and depending on the parameters
     * will start certain digitisation technique in the given class (eg: dwca file importation, dwca folder importation, etc.)
     * @param args Command line arguments that indicate what digitiser should be used as well as the parameters it requires
     */
    protected abstract void digitiseDigitalSpecimensData(List<String> args);


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Digitise digital specimens from the datasource passed as argument, sending the result of the operation by
     * email to the list of addresses defined in the configuration file.
     * Note: The process, will not stop if a digital specimen fails to be read or processed, it will noted downed in
     * the log file, but it will try to carry on processing the next digital specimen
     * @param args
     * @return List of digital specimen processed as result of digitisation
     */
    public void digitise(List<String> args) {
        //Digitise digital specimens from  a data source (it could be a dwc-a file, a gbif download request, etc)
        LocalDateTime digitisationStartDateTime = LocalDateTime.now();
        this.digitiseDigitalSpecimensData(args);
        this.getLogger().info("Digitisation completed.");
        LocalDateTime digitisationEndDateTime= LocalDateTime.now();

        List<String> emailAddresses = this.getConfig().getList(String.class,"digitiser.sendDigitisationResultsByEmailTo");
        if (emailAddresses.size()>0){
            EmailUtils emailUtils = new EmailUtils(this.getConfig());
            emailUtils.sendResultDigitiserExecution(digitisationStartDateTime,digitisationEndDateTime,emailAddresses);
        }
    }


    /*****************************************************/
    /* Static methods to execute class from command line */
    /*****************************************************/
    /**
     * Initial point of entry for the application. According to the command line arguments passed as parameters it will
     * instanciate a specific digitisier and tell it to digitise the data
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        Digitiser digitiser = null;
        try{
            List<String> listArgs = new ArrayList<String>(Arrays.asList(args));
            digitiser = DigitiserFactory.getDigitiser(listArgs);
            digitiser.digitise(listArgs);
        } catch (Exception e){
            LoggerFactory.getLogger(Digitiser.class).error("There has been an unexpected error in the system" + e.getMessage());
        }
    }

}
