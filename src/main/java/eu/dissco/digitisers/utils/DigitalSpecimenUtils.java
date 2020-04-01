package eu.dissco.digitisers.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maxmind.geoip2.model.CountryResponse;
import eu.dissco.digitisers.clients.gbif.GbifClient;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.UUID;

public class DigitalSpecimenUtils {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final static Logger logger = LoggerFactory.getLogger(DigitalSpecimenUtils.class);


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Function that returns the value of the term searched from the json object ds.content.dwcaContent
     * @param ds Digital specimen
     * @param term Term to get its value
     * @return String with the value of the term in ds.content.dwcaContent
     */
    public static String getTermFromDsDwcaJson(DigitalObject ds , Term term){
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

    /**
     * Function that tries to obtain the digital specimen's institution's region
     * It tries to find the region by getting information about the institution from GRSciColl,
     * looking for the field institutionId when provided, and if not by the institution code, but as this field
     * is not unique, in case of conflict, using the collection code.
     * If the digital specimen's institution's region couldn't uniquivocally be obtained using the method above,
     * it tries to get it from the country where the machine that hosts the URL for institutionId, collectionId, or
     * identifier is, in case any of those fields (in that order) is a valid URL
     * @param ds Digital specimen obtain its instituion's region
     * @param gbifClient Gbif client obtain data from GRSciColl
     * @return Digital specimen's institution's region when found, or null if not found
     */
    public static String getDsInstitutionRegion(DigitalObject ds, GbifClient gbifClient){
        String institutionRegion = null;
        String institutionId = DigitalSpecimenUtils.getTermFromDsDwcaJson(ds, DwcTerm.institutionID);
        String institutionCode = DigitalSpecimenUtils.getStringPropertyFromDS(ds,"institutionCode");
        String collectionCode = DigitalSpecimenUtils.getStringPropertyFromDS(ds,"collectionCode");
        String collectionId =  DigitalSpecimenUtils.getTermFromDsDwcaJson(ds, DwcTerm.collectionID);
        String identifier =  DigitalSpecimenUtils.getTermFromDsDwcaJson(ds, DcTerm.identifier);
        try{
            JsonObject institutionInfo = null;
            if (StringUtils.isNotBlank(institutionId) && DigitalSpecimenUtils.isStringAValidUuid(institutionId)){
                institutionInfo = gbifClient.getInstitutionInfoByInstitutionId(institutionId);
            } else if (StringUtils.isNotBlank(institutionCode)){
                JsonArray posibleInstitutions = gbifClient.getInstitutionsInfoByInstitutionCode(institutionCode);
                if (posibleInstitutions.size()==1){
                    institutionInfo = posibleInstitutions.get(0).getAsJsonObject();
                } else if (posibleInstitutions.size()>1){
                    //There are several institution with that institution code
                    //Check if all of them are from the same region
                    String possibleRegion = "UNKNOWN";
                    if (posibleInstitutions.get(0).getAsJsonObject().has("country")){
                        possibleRegion = posibleInstitutions.get(0).getAsJsonObject().getAsJsonObject("country").get("region").getAsString();
                    }
                    boolean sameRegion=true;
                    for (JsonElement posibleInstitution:posibleInstitutions) {
                        if (!posibleInstitution.getAsJsonObject().has("country") || !possibleRegion.equalsIgnoreCase(posibleInstitution.getAsJsonObject().getAsJsonObject("country").get("region").getAsString())){
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
                String physicalSpecimenId=DigitalSpecimenUtils.getStringPropertyFromDS(ds,"physicalSpecimenId");
                String scientificName=DigitalSpecimenUtils.getStringPropertyFromDS(ds,"scientificName");
                logger.warn("It was not possible to obtain institution's regions for ds [" + scientificName
                        + " || " + institutionCode + " || "+ physicalSpecimenId + "]");
            }
        } catch (Exception e){
            logger.error("Unexpected error trying to obtain institution's region for ds "  + e.getMessage(),ds);
        }
        return institutionRegion;
    }

    /**
     * Method that adds a property to the content of the Digital specimen
     * @param ds Digital specimen on which we want to add the property in its content
     * @param property Name of the property to be added
     * @param value Vale of the property to be added
     */
    public static void addPropertyToDS(DigitalObject ds, String property, Object value){
        if(value!=null){
            JsonUtils.addPropertyToJsonObj(ds.attributes.getAsJsonObject("content"),property,value);
        }
    }

    /**
     * Funtcion that get the value of the property received as parameter from the content of the digital specimen
     * @param ds Digital specimen
     * @param property Property to obtain its value
     * @return JsonElement with the value of the property or null if property is not found
     */
    public static JsonElement getPropertyFromDS(DigitalObject ds, String property){
        JsonElement jsonElement=null;
        JsonObject jsonContent = ds.attributes.getAsJsonObject("content");
        if (jsonContent.has(property)) {
            jsonElement = jsonContent.get(property);
        }
        return jsonElement;
    }

    /**
     * Function that get the value as string of the property received as parameter from the content of the digital specimen
     * @param ds Digital specimen
     * @param property Property to obtain its value
     * @return String with the value of the property or null if property is not found
     */
    public static String getStringPropertyFromDS(DigitalObject ds, String property){
        String value = null;
        JsonElement jsonElement = DigitalSpecimenUtils.getPropertyFromDS(ds,property);
        if (jsonElement!=null){
            value = jsonElement.getAsString();
        }
        return value;
    }

    /**
     * Function that check if a string is a valid UUID
     * @param value String to check if it is a valid UUID
     * @return true if parameter is a valid UUID or false otherwise
     */
    public static boolean isStringAValidUuid(String value){
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
