package eu.dissco.digitisers.clients.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.dissco.digitisers.utils.NetUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

public class CountryClient {

    private final static Logger logger = LoggerFactory.getLogger(CountryClient.class);

    private static CountryClient instance=null;

    //We could also use http://api.gbif.org/v1/enumeration/country as backup service in case restcountries.eu disappears
    private String apiUrl ="https://restcountries.eu/rest/v2";

    //Hashmap to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results
    private Map<String, JsonObject> mapCountryInfoNameByCode;

    protected String getApiUrl() {
        return apiUrl;
    }

    protected void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    protected Map<String, JsonObject> getMapCountryInfoNameByCode() {
        return mapCountryInfoNameByCode;
    }

    protected void setMapCountryInfoNameByCode(Map<String, JsonObject> mapCountryInfoNameByCode) {
        this.mapCountryInfoNameByCode = mapCountryInfoNameByCode;
    }

    //private constructor to avoid client applications to use constructor
    //as we use the singleton pattern
    private CountryClient(){
        this.mapCountryInfoNameByCode = new TreeMap<String,JsonObject>();
    }

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
                    logger.error("Error getting the country by countryCode " + countryCode + " using restcountries.eu API");
                }
                this.getMapCountryInfoNameByCode().put(countryCode,countryInfo);
            }
            countryInfo=this.getMapCountryInfoNameByCode().get(countryCode);
        }
        return countryInfo;
    }
}
