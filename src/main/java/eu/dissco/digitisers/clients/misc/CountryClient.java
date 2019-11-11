package eu.dissco.digitisers.clients.misc;

import com.google.gson.JsonObject;
import eu.dissco.digitisers.utils.NetUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

public class CountryClient {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static CountryClient instance=null;
    private final String apiUrl ="https://restcountries.eu/rest/v2"; //We could also use http://api.gbif.org/v1/enumeration/country as backup service in case restcountries.eu disappears
    private Map<String, JsonObject> mapCountryInfoNameByCode; //Map to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    protected String getApiUrl() {
        return apiUrl;
    }

    protected Map<String, JsonObject> getMapCountryInfoNameByCode() {
        return mapCountryInfoNameByCode;
    }

    protected void setMapCountryInfoNameByCode(Map<String, JsonObject> mapCountryInfoNameByCode) {
        this.mapCountryInfoNameByCode = mapCountryInfoNameByCode;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Private constructor to avoid client applications to use constructor as we use the singleton design pattern
     */
    private CountryClient(){
        this.mapCountryInfoNameByCode = new TreeMap<String,JsonObject>();
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     *  Method to get an instance of CountryClient as we use the singleton design pattern
     * @return
     */
    public static CountryClient getInstance(){
        if (instance==null){
            instance = new CountryClient();
        }
        return instance;
    }

    //Get the country name by its ISO 3166-1 2-letter or 3-letter country code
    public String getCountryNameByCountryCode(String countryCode) {
        String countryName = null;
        JsonObject countryInfo = this.getCountryInfoByCountryCode(countryCode);
        if(countryInfo!=null) {
            countryName = countryInfo.getAsJsonObject().get("name").getAsString();
        }
        return countryName;
    }

    //Get the country info by its ISO 3166-1 2-letter or 3-letter country code
    public JsonObject getCountryInfoByCountryCode(String countryCode) {
        JsonObject countryInfo = null;
        if(StringUtils.isNotBlank(countryCode)) {
            if (!this.getMapCountryInfoNameByCode().containsKey(countryCode)){
                try{
                    countryInfo = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/alpha/" + StringUtils.trim(countryCode));
                } catch (Exception e){
                    //There was an error obtaining the country from its country code
                    this.getLogger().error("Error getting the country by countryCode " + countryCode + " using restcountries.eu API");
                }
                this.getMapCountryInfoNameByCode().put(countryCode,countryInfo);
            }
            countryInfo=this.getMapCountryInfoNameByCode().get(countryCode);
        }
        return countryInfo;
    }
}
