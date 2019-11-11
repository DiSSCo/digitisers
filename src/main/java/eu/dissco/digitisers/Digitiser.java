package eu.dissco.digitisers;

import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryException;
import eu.dissco.digitisers.tasks.DigitalObjectProcessor;
import eu.dissco.digitisers.tasks.DigitalObjectVisitor;
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
     * Create a Digitiser from a config file
     * @param configFilePath
     * @throws ConfigurationException
     */
    public Digitiser(String configFilePath) throws ConfigurationException {
        this.config = FileUtils.loadConfigurationFromFilePath(configFilePath);
    }


    /********************/
    /* ABSTRACT METHODS */
    /********************/

    protected abstract int readDigitalSpecimensData(List<String> args, DigitalObjectVisitor digitalObjectVisitor);


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /***
     * Abstract method that needs to be implemented in the specific digitiser classes and depending on the parameters
     * will start certain digitisation technique in the given class (eg: dwca file importation, dwca folder importation, etc.)
     * @param args
     */
    public void digitise(List<String> args) throws DigitalObjectRepositoryException {
        DigitalObjectVisitor digitalObjectVisitor=null;
        try{
            LocalDateTime startDateTime = LocalDateTime.now();

            //Read data from source (it could be a dwc-a file, a gbif download request, etc)
            //processing each Digital Specimen as soon as it is read using the Digital Object Processor
            digitalObjectVisitor = new DigitalObjectProcessor(this.getConfig());
            int numDsRead = this.readDigitalSpecimensData(args,digitalObjectVisitor);

            this.getLogger().info("Digitisation completed.");

            LocalDateTime endDateTime= LocalDateTime.now();
            List<String> emailAddresses = this.getConfig().getList(String.class,"digitiser.sendDigitisationResultsByEmailTo");
            if (emailAddresses.size()>0){
                boolean emailSent = EmailUtils.sendResultDigitiserExecution(startDateTime,endDateTime,emailAddresses);
            }
        } finally {
            if (digitalObjectVisitor!=null){
                digitalObjectVisitor.close();
            }
        }
    }


    /*****************************************************/
    /* Static methods to execute class from command line */
    /*****************************************************/

    public static void main(String[] args) throws Exception {
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
