package eu.dissco.digitisers.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maxmind.geoip2.model.CountryResponse;
import eu.dissco.digitisers.clients.col.CoLClient;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryClient;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryException;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryInfo;
import eu.dissco.digitisers.clients.ebi.EbiClient;
import eu.dissco.digitisers.clients.gbif.GbifClient;
import eu.dissco.digitisers.clients.gbif.GbifInfo;
import eu.dissco.digitisers.clients.misc.CountryClient;
import eu.dissco.digitisers.clients.wiki.WikiClient;
import eu.dissco.digitisers.utils.NetUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

public class DigitalObjectProcessor implements DigitalObjectVisitor {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Configuration config;
    private DigitalObjectRepositoryClient digitalObjectRepositoryClient;


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    protected Configuration getConfig() {
        return config;
    }

    protected DigitalObjectRepositoryClient getDigitalObjectRepositoryClient() {
        return digitalObjectRepositoryClient;
    }

    protected void setDigitalObjectRepositoryClient(DigitalObjectRepositoryClient digitalObjectRepositoryClient) {
        this.digitalObjectRepositoryClient = digitalObjectRepositoryClient;
    }

    /****************/
    /* CONSTRUCTORS */
    /****************/
    public DigitalObjectProcessor(Configuration config) throws DigitalObjectRepositoryException {
        this.config = config;
        DigitalObjectRepositoryInfo digitalObjectRepositoryInfo =  DigitalObjectRepositoryInfo.getDigitalObjectRepositoryInfoFromConfig(this.config);
        this.digitalObjectRepositoryClient = DigitalObjectRepositoryClient.getInstance(digitalObjectRepositoryInfo);
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    @Override
    public DigitalObject visitDigitalSpecimen(DigitalObject ds) {
        //Enrich data in digital specimen
        this.enrichDigitalSpecimenData(ds);

        //Calculate digital specimen MIDS level
        this.calculateDigitalSpecimenMidsLevel(ds);

        //Save (insert, update) digital specimen in repository
        DigitalObject dsSaved = this.saveDigitalSpecimen(ds);

        if (dsSaved!=null && dsSaved.attributes.has("operation")){
            this.getLogger().info("DS "+ dsSaved.attributes.get("operation").getAsString() +" with id: " + dsSaved.id);
        }
        return  dsSaved;
    }

    @Override
    public void close(){
        //release the connection to the digital object repository  back to the pool of connections managed by this client library
        if (this.getDigitalObjectRepositoryClient()==null){
            this.getDigitalObjectRepositoryClient().close();
        }
    }


    /*********************/
    /* PROTECTED METHODS */
    /*********************/

    protected void enrichDigitalSpecimenData(DigitalObject ds){
        this.enrichDigitalSpecimenWithCountryInfo(ds);
        this.enrichDigitalSpecimenWithCatalogueOfLifeInfo(ds);
        this.enrichDigitalSpecimenWithEbiInfo(ds);
        this.enrichDigitalSpecimenWithWikiInfo(ds);
    }

    protected void calculateDigitalSpecimenMidsLevel(DigitalObject ds){
        //TODO: How to calculate midslevel?
        int midsLevel=0;
        if (StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"scientificName")) && StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"catalogNumber"))) {
            midsLevel=1;
            if (StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"locality")) && StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"country"))
                    && StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"decimalLatLon"))) {
                midsLevel=2;
                if (StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"commonName")) || StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"availableImages"))
                        || StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"annotations")) || StringUtils.isNotBlank(this.getStringPropetyInDigitalObject(ds,"interpretations"))
                        || StringUtils.isNotBlank("literatureReference")) {
                    midsLevel=3;
                }
            }
        }
        ds.attributes.getAsJsonObject("content").addProperty("midslevel",midsLevel);
    }

    protected DigitalObject saveDigitalSpecimen(DigitalObject ds) {
        DigitalObject dsSaved = null;
        try{
            //Check if digital object can be saved
            if (this.canDigitalSpecimenBeSaved(ds)){
                dsSaved = this.getDigitalObjectRepositoryClient().saveDigitalSpecimen(ds);
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

    @Override
    protected void finalize() throws Throwable{
        try{
            this.close();
        } catch(Throwable t){
            throw t;
        } finally{
            super.finalize();
        }
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    private boolean canDigitalSpecimenBeSaved(DigitalObject ds){
        boolean canBeSaved =true;

        if (this.getConfig().getBoolean("digitiser.onlyUseRecordsFromEuropeanInstitutions")){
            //Only save digital objects that belongs to European institutions
            String institutionRegion = this.getDigitalSpecimenInstitutionRegion(ds);
            if (!institutionRegion.equalsIgnoreCase("Europe")){
                canBeSaved=false;
            }
        }
        return canBeSaved;
    }

    private String getDigitalSpecimenInstitutionRegion(DigitalObject ds){
        String institutionRegion = null;
        String institutionId = this.getTermFromDsDwcaJson(ds, DwcTerm.basisOfRecord);
        String institutionCode = this.getStringPropetyInDigitalObject(ds,"institutionCode");
        String collectionCode = this.getStringPropetyInDigitalObject(ds,"collectionCode");
        String collectionId =  this.getTermFromDsDwcaJson(ds, DwcTerm.collectionID);
        String identifier =  this.getTermFromDsDwcaJson(ds, DcTerm.identifier);
        try{
            GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
            GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
            JsonObject institutionInfo = null;
            if (StringUtils.isNotBlank(institutionId) && isStringAValidUuid(institutionId)){
                institutionInfo = gbifClient.getInstitutionInfoByInstitutionId(institutionId);
            } else if (StringUtils.isNotBlank(institutionCode)){
                JsonArray posibleInstitutions = gbifClient.getInstitutionsInfoByInstitutionCode(institutionCode);
                if (posibleInstitutions.size()==1){
                    institutionInfo = posibleInstitutions.get(0).getAsJsonObject();
                } else if (posibleInstitutions.size()>1){
                    //There are several institution with that institution code
                    //Check if all of them are from the same region
                    String possibleRegion = posibleInstitutions.get(0).getAsJsonObject().getAsJsonObject("country").get("region").getAsString();
                    boolean sameRegion=true;
                    for (JsonElement posibleInstitution:posibleInstitutions) {
                        if (!possibleRegion.equalsIgnoreCase(posibleInstitution.getAsJsonObject().getAsJsonObject("country").get("region").getAsString())){
                            sameRegion=false;
                            break;
                        }
                    }
                    if (sameRegion){
                        institutionInfo=posibleInstitutions.get(0).getAsJsonObject();
                    } else if (StringUtils.isNotBlank(collectionCode)){
                        //Try to see if we can find what institution this digital specimen belongs to by looking at the collection code
                        for (Iterator<JsonElement> iter = posibleInstitutions.iterator(); iter.hasNext(); ) {
                            JsonObject posibleInstitution = (JsonObject) iter.next();
                            JsonObject collectionInfo = gbifClient.getCollectionInfoByInstitutionIdAndCollectionCode(posibleInstitution.get("key").getAsString(),collectionCode);
                            if (collectionInfo==null){
                                iter.remove();
                            }
                        }
                        if (posibleInstitutions.size()==1){
                            institutionInfo=posibleInstitutions.get(0).getAsJsonObject();
                        }
                    }
                }
            }

            if (institutionInfo!=null && institutionInfo.has("country")){
                institutionRegion = institutionInfo.getAsJsonObject("country").get("region").getAsString();
            } else{
                //As last attempt, try to get the country from the record institutionId, collectionsId or identifier if they are valid url
                UrlValidator urlValidator = new UrlValidator();
                CountryResponse countryResponse = null;
                if (StringUtils.isNotBlank(institutionId) && urlValidator.isValid(institutionId)){
                    logger.debug("Obtaining institution code from ");
                    countryResponse = NetUtils.getCountryInfoFromUrl(institutionId);
                } else if (StringUtils.isNotBlank(collectionId) && urlValidator.isValid(collectionId)){
                    countryResponse = NetUtils.getCountryInfoFromUrl(collectionId);
                } else if (StringUtils.isNotBlank(identifier) && urlValidator.isValid(identifier)){
                    countryResponse = NetUtils.getCountryInfoFromUrl(identifier);
                }
                if (countryResponse!=null){
                    institutionRegion = countryResponse.getContinent().getName();
                }
            }

            if (StringUtils.isBlank(institutionRegion)){
                logger.error("It was not possible to obtain institution's regions for ds" ,ds);
            }
        } catch (Exception e){
            logger.error("Unexpected error trying to obtain institution's region for ds "  + e.getMessage(),ds);
        }
        return institutionRegion;
    }

    private void enrichDigitalSpecimenWithCountryInfo(DigitalObject ds){
        try{
            String country = this.getStringPropetyInDigitalObject(ds,"country");
            String countryCode = this.getStringPropetyInDigitalObject(ds,"countryCode");
            if (StringUtils.isBlank(country) && StringUtils.isNotBlank(countryCode)){
                CountryClient countryClient = CountryClient.getInstance();
                country = countryClient.getCountryNameByCountryCode(countryCode);
                this.addStringPropertyToDigitalObject(ds,"country",country);
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching country data for ds. " + e.getMessage(),ds);
        }
    }

    private void enrichDigitalSpecimenWithCatalogueOfLifeInfo(DigitalObject ds){
        try{
            GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
            GbifClient gbifClient = GbifClient.getInstance(gbifInfo);

            //Get CoL Data
            String catOfLifeReference = this.getStringPropetyInDigitalObject(ds,"catOfLifeReference");
            String acceptedScientificName = this.getStringPropetyInDigitalObject(ds,"scientificName");

            String basisOfRecord = this.getTermFromDsDwcaJson(ds, DwcTerm.basisOfRecord);
            String taxonRank = this.getTermFromDsDwcaJson(ds,DwcTerm.taxonRank);
            String gbifKindgdomTaxonId = this.getTermFromDsDwcaJson(ds, GbifTerm.kingdomKey);

            if (StringUtils.isBlank(catOfLifeReference) && StringUtils.isNotBlank(acceptedScientificName)
                    && StringUtils.isNotBlank(basisOfRecord) && StringUtils.isNotBlank(taxonRank) && StringUtils.isNotBlank(gbifKindgdomTaxonId) ){

                JsonObject parsedName = gbifClient.parseName(acceptedScientificName);
                JsonObject kindgdomInfo = gbifClient.getTaxonInfoById(gbifKindgdomTaxonId);
                if (parsedName!=null && kindgdomInfo!=null) {
                    String canonicalName = parsedName.get("canonicalName").getAsString();
                    String kingdomName = kindgdomInfo.get("scientificName").getAsString();

                    CoLClient colClient = CoLClient.getInstance();
                    JsonObject colTaxonInfo = colClient.getTaxonInformation(canonicalName,taxonRank,kingdomName);
                    if (colTaxonInfo!=null){
                        String colURL = colTaxonInfo.get("url").getAsString();
                        this.addStringPropertyToDigitalObject(ds,"catOfLifeReference",colURL);
                        ds.attributes.getAsJsonObject("content").add("colContent",colTaxonInfo);
                    }
                }
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching catalogue of life data for ds " + ds.attributes.get("physicalSpecimenId") + " . Reason: " + e.getMessage());
        }
    }

    private void enrichDigitalSpecimenWithEbiInfo(DigitalObject ds){
        try{
            String institutionCode = this.getTermFromDsDwcaJson(ds, DwcTerm.institutionCode);
            String collectionCode = this.getTermFromDsDwcaJson(ds,DwcTerm.collectionCode);
            String catalogNumber = this.getTermFromDsDwcaJson(ds, DwcTerm.catalogNumber);

            if (StringUtils.isNotBlank(institutionCode) && StringUtils.isNotBlank(collectionCode) && StringUtils.isNotBlank(catalogNumber)){
                EbiClient ebiClient = EbiClient.getInstance();
                String searchTerm = String.join(" ", Arrays.asList(institutionCode,collectionCode,catalogNumber));
                JsonArray ebiResults = ebiClient.rootSearchAsJson(searchTerm,true);
                ds.attributes.getAsJsonObject("content").add("ebiSearchResults",ebiResults);
            } else{
                this.getLogger().info("Not enough information for enriching ds with EBI data" + ds.attributes.get("physicalSpecimenId"));
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching EBI data for ds " + ds.attributes.get("physicalSpecimenId") + " . Reason: " + e.getMessage());
        }
    }

    private void enrichDigitalSpecimenWithWikiInfo(DigitalObject ds) {
        try{

            String wikipedia = this.getStringPropetyInDigitalObject(ds,"wikipedia");
            String acceptedScientificName = this.getStringPropetyInDigitalObject(ds,"scientificName");
            String gbifKindgdomTaxonId = this.getTermFromDsDwcaJson(ds, GbifTerm.kingdomKey);

            if (StringUtils.isBlank(wikipedia) && StringUtils.isNotBlank(acceptedScientificName)
                    && StringUtils.isNotBlank(gbifKindgdomTaxonId) ) {
                GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
                GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
                JsonObject parsedName = gbifClient.parseName(acceptedScientificName);
                JsonObject kindgdomInfo = gbifClient.getTaxonInfoById(gbifKindgdomTaxonId);

                if (parsedName != null && kindgdomInfo != null) {
                    String canonicalName = parsedName.get("canonicalName").getAsString();
                    String kingdomName = kindgdomInfo.get("scientificName").getAsString();
                    WikiClient wikiClient = WikiClient.getInstance("wikidata");
                    JsonObject wikiInfo = wikiClient.getWikiInformation(canonicalName,kingdomName);
                    if (wikiInfo!=null){
                        this.addStringPropertyToDigitalObject(ds,wikiClient.getWikiType(),wikiClient.getPageURL(wikiInfo));
                        ds.attributes.getAsJsonObject("content").add(wikiClient.getWikiType()+"_info",wikiInfo);
                    }
                }
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching wiki data for ds. " + e.getMessage(),ds);
        }
    }

    private String getTermFromDsDwcaJson(DigitalObject ds , Term term){
        String value=null;
        JsonObject dwcaContent = ds.attributes.getAsJsonObject("content").getAsJsonObject("dwcaContent");
        if (dwcaContent.getAsJsonObject("core").getAsJsonObject("content").has(term.prefixedName())){
            value = dwcaContent.getAsJsonObject("core").getAsJsonObject("content").get(term.prefixedName()).getAsString();
        }

        if (StringUtils.isBlank(value) && dwcaContent.has("extensions")){
            //Look in the extension files
            JsonArray extensions = dwcaContent.getAsJsonArray("extensions");
            for (JsonElement extension: extensions) {
                if (extension.getAsJsonObject().has("content")){
                    JsonArray extensionRecords = extension.getAsJsonObject().getAsJsonArray("content");
                    for (JsonElement extensionRecord:extensionRecords) {
                        if (extensionRecord.getAsJsonObject().has(term.prefixedName())){
                            value = extensionRecord.getAsJsonObject().get(term.prefixedName()).getAsString();
                            if (StringUtils.isNotBlank(value)) break;
                        }
                    }
                    if (StringUtils.isNotBlank(value)) break;
                }
            }
        }
        return value;
    }

    private void addStringPropertyToDigitalObject(DigitalObject digitalObject, String property, String value){
        if(StringUtils.isNotBlank(value)){
            digitalObject.attributes.getAsJsonObject("content").addProperty(property,value);
        }
    }

    private String getStringPropetyInDigitalObject(DigitalObject digitalObject, String property){
        String value = null;
        JsonObject jsonContent = digitalObject.attributes.getAsJsonObject("content");
        if (jsonContent.has(property)) {
            try {
                value = jsonContent.get(property).getAsString();
            }catch(Exception e){
                value = jsonContent.get(property).toString();
            }
        }
        return value;
    }

    private boolean isStringAValidUuid(String value){
        boolean isValidUuid;
        try{
            UUID uuid = UUID.fromString(value);
            isValidUuid = true;
        } catch (IllegalArgumentException exception){
            isValidUuid = false;
        }
        return isValidUuid;
    }
}