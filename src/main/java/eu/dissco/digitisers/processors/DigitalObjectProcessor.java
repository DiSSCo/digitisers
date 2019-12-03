package eu.dissco.digitisers.processors;

import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryClient;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryException;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryInfo;
import eu.dissco.digitisers.clients.gbif.GbifClient;
import eu.dissco.digitisers.clients.gbif.GbifInfo;
import eu.dissco.digitisers.processors.enrichers.CatalogueOfLifeEnricher;
import eu.dissco.digitisers.processors.enrichers.CountryEnricher;
import eu.dissco.digitisers.processors.enrichers.EbiEnricher;
import eu.dissco.digitisers.processors.enrichers.WikiEnricher;
import eu.dissco.digitisers.utils.DigitalSpecimenUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DigitalObjectProcessor implements DigitalObjectVisitor {

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

    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Create a DigitalObjectProcessor with properties from the config file
     * @param config path of the configuration file to be used in this digitiser
     * @throws DigitalObjectRepositoryException
     */
    public DigitalObjectProcessor(Configuration config) throws DigitalObjectRepositoryException {
        this.config = config;
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    @Override
    /**
     * Method in charge of processing a digital specimen when is visited after reading it from the source data.
     * It does the following operations:
     * 1. Enrich the data hold in the digital specimen with information of external services (eg: CoL, EBI, etc)
     * 2. Calculate the MIDs level according with the data it has
     * 3. Save the object in the repository (create or update)
     * @param ds Digital specimen to be processed
     * @return Digital object with the result of saving the digital specimen in the repository. If the digital specimen
     * failed to be saved or it couldn't be saved it return null. If the digital specimen was saved, the Digital object
     * returned will indicate in dsSaved.attributes.operation if the operation was "insert" or "update"
     */
    public DigitalObject visitDigitalSpecimen(DigitalObject ds) {
        //Enrich data in digital specimen
        this.enrichDigitalSpecimenData(ds);

        //Calculate digital specimen MIDS level
        this.calculateDigitalSpecimenMidsLevel(ds);

        //Save (insert, update) digital specimen in repository
        DigitalObject dsSaved = this.saveDigitalSpecimen(ds);

        if (dsSaved!=null && dsSaved.attributes.has("operation")){
            String institutionCode= DigitalSpecimenUtils.getStringPropertyFromDS(ds,"institutionCode");
            String physicalSpecimenId=DigitalSpecimenUtils.getStringPropertyFromDS(ds,"physicalSpecimenId");
            String scientificName=DigitalSpecimenUtils.getStringPropertyFromDS(ds,"scientificName");

            this.getLogger().info("DS "+ dsSaved.attributes.get("operation").getAsString() +" with id: " + dsSaved.id + " for [" + scientificName
                    + " || " + institutionCode + " || "+ physicalSpecimenId + "]");
        }

        return dsSaved;
    }



    /*********************/
    /* PROTECTED METHODS */
    /*********************/

    /**
     * Enrich the data hold in the digital specimen with information of external services (eg: CoL, EBI, etc).
     * The enrichment of the data in the digital specimen for those external services is done in parallel
     * @param ds Digital specimen to be enriched
     */
    protected void enrichDigitalSpecimenData(DigitalObject ds){
        try{
            //Create list of tasks to be executed potentially concurrently
            List<Callable<Map<String, Object>>> taskList = new ArrayList<>();
            taskList.add(new CountryEnricher(ds,this.getConfig()));
            taskList.add(new CatalogueOfLifeEnricher(ds,this.getConfig()));
            taskList.add(new EbiEnricher(ds,this.getConfig()));
            taskList.add(new WikiEnricher(ds,this.getConfig()));

            //Create executor service
            //ExecutorService executorService = Executors.newCachedThreadPool();
            ExecutorService executorService = Executors.newFixedThreadPool(taskList.size());

            //Submit all tasks
            List<Future<Map<String,Object>>> futures = executorService.invokeAll(taskList);

            //Get results of each of the enrichment tasks
            for(Future<Map<String,Object>> future: futures) {
                try{
                    Map<String,Object> enrichData = future.get();
                    if (enrichData!=null){
                        for (Map.Entry<String,Object> mapElement : enrichData.entrySet()) {
                            DigitalSpecimenUtils.addPropertyToDS(ds,mapElement.getKey(),mapElement.getValue());
                        }
                    }
                } catch (Exception e) {
                    this.getLogger().error("Unexpected error getting data from enrichment task for digital specimen "  + e.getMessage(),ds);
                    future.cancel(true);
                }
            }

            //Wait until all threads finished or executor timeout is reached
            executorService.shutdown();
            boolean finished = executorService.awaitTermination(5, TimeUnit.MINUTES);
            if (!finished){
                this.getLogger().warn("Some of the enrichment tasks didn't finished on time",ds);
            }
        } catch (Exception e){
            this.getLogger().error("Unexpected error enriching digital specimen data  "  + e.getMessage(),ds);
        }
    }

    /**
     * Function that calculates the MIDS level of the digital specimen according to the data it has
     * @param ds Digital specimen to calculate its MIDS level
     */
    protected void calculateDigitalSpecimenMidsLevel(DigitalObject ds){
        int midsLevel=0;
        if (StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"scientificName"))
                && StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"catalogNumber"))) {
            midsLevel=1;
            if (StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"locality"))
                    && StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"country"))
                    && DigitalSpecimenUtils.getPropertyFromDS(ds,"decimalLatLon")!=null) {
                midsLevel=2;
                if (StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"commonName"))
                        || DigitalSpecimenUtils.getPropertyFromDS(ds,"availableImages")!=null
                        || StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"annotations"))
                        || StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"interpretations"))
                        || StringUtils.isNotBlank(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"literatureReference"))) {
                    midsLevel=3;
                }
            }
        }
        DigitalSpecimenUtils.addPropertyToDS(ds,"midslevel",midsLevel);
    }

    /**
     * Function that try to save a digital specimen in the repository.
     * Before saving it it check if it satisfies the requirements defined in the config file (like instituion's region
     * and minimun MIDS level)
     * @param ds digital specimen to be saved in the repository
     * @return Digital object with the result of saving the digital specimen in the repository or null if it couldn't be
     * saved or it failed to be saved.
     */
    protected DigitalObject saveDigitalSpecimen(DigitalObject ds) {
        DigitalObject dsSaved = null;
        try{
            //Check if digital object can be saved
            if (this.canDigitalSpecimenBeSaved(ds)){
                DigitalObjectRepositoryInfo digitalObjectRepositoryInfo =  DigitalObjectRepositoryInfo.getDigitalObjectRepositoryInfoFromConfig(this.getConfig());
                try(DigitalObjectRepositoryClient digitalObjectRepositoryClient = new DigitalObjectRepositoryClient(digitalObjectRepositoryInfo)){
                    dsSaved = digitalObjectRepositoryClient.saveDigitalSpecimen(ds);
                }
            }
        } catch (DigitalObjectRepositoryException e){
            if (e.getStatusCode().equals("Warn")){
                this.getLogger().warn(e.getMessage());
            } else{
                this.getLogger().error("Error saving ds  "  + e.getMessage(),ds);
            }
        }
        return dsSaved;
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /**
     * Function that checks if the digital specimen can be saved in the repository
     * @param ds Digital specimen to check if it can be saved
     * @return True if it can be saved, false otherwise
     */
    private boolean canDigitalSpecimenBeSaved(DigitalObject ds){
        return this.institutionRegionSatisfied(ds) && this.midLevelSatisfied(ds);
    }

    /**
     * Function that checks if the digital specimen's institution's region satisfied the requirement defined in the
     * property file
     * @param ds Digital specimen to check if its institution's region satisfied the requirement defined in the property file
     * @return true if the requirement is satisfied, false otherwise
     */
    private boolean institutionRegionSatisfied(DigitalObject ds){
        boolean isInstitutionRegionSatisfied = true;
        String requiredInstitutionInRegion = this.getConfig().getString("digitiser.recordsFromInstitutionInRegion");
        if (!requiredInstitutionInRegion.equalsIgnoreCase("World")){
            //Only save digital objects that belongs to institutions in the given region
            GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
            GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
            String institutionRegion = DigitalSpecimenUtils.getDsInstitutionRegion(ds,gbifClient);
            if (StringUtils.isBlank(institutionRegion) || !StringUtils.containsIgnoreCase(institutionRegion,requiredInstitutionInRegion)){
                isInstitutionRegionSatisfied=false;
                String institutionCode= DigitalSpecimenUtils.getStringPropertyFromDS(ds,"institutionCode");
                String physicalSpecimenId= DigitalSpecimenUtils.getStringPropertyFromDS(ds,"physicalSpecimenId");
                String scientificName= DigitalSpecimenUtils.getStringPropertyFromDS(ds,"scientificName");
                this.getLogger().info("Digital speciment with [" + scientificName + " || " + institutionCode + " || "
                        + physicalSpecimenId + "] can't be saved because it is not from an institution in " + requiredInstitutionInRegion);
            }
        }
        return isInstitutionRegionSatisfied;
    }

    /**
     * Function that checks if the digital specimen's MIDS level satisfied the requirement defined in the
     * property file
     * @param ds Digital specimen to check if its MIDS level satisfied the requirement defined in the property file
     * @return true if the requirement is satisfied, false otherwise
     */
    private boolean midLevelSatisfied(DigitalObject ds){
        int dsMidsLevel = Integer.valueOf(DigitalSpecimenUtils.getStringPropertyFromDS(ds,"midslevel"));
        int minimumMidsLevel = this.getConfig().getInt("digitiser.minimumMidsLevel");
        return dsMidsLevel>=minimumMidsLevel;
    }
}
