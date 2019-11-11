package eu.dissco.digitisers.clients.gbif;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maxmind.geoip2.model.CountryResponse;
import eu.dissco.digitisers.clients.misc.CountryClient;
import eu.dissco.digitisers.utils.FileUtils;
import eu.dissco.digitisers.utils.JsonUtils;
import eu.dissco.digitisers.utils.NetUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class GbifClient {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static GbifClient instance=null;
    private final String apiUrl = "http://api.gbif.org/v1";
    private final GbifInfo gbifInfo;
    private Map<String,JsonObject> mapTaxonById; //Map to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results
    private Map<String,JsonObject> mapParsedNameByScientificName; //Map to improve efficiency of this class
    private Map<String,String> mapTaxonIdByCanonicalNameAndKingdom; //Map to improve efficiency of this class
    private Map<String,JsonArray> mapInstitutionsInfoByCode; //Map to improve efficiency of this class
    private Map<String,JsonObject> mapInstitutionInfoById; //Map to improve efficiency of this class
    private Map<String,JsonObject> mapCollectionInfoByInstitutionIdAndCollectionName; //Map to improve efficiency of this class


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    protected GbifInfo getGbifInfo() {
        return gbifInfo;
    }

    protected Map<String, JsonObject> getMapParsedNameByScientificName() {
        return mapParsedNameByScientificName;
    }

    protected void setMapParsedNameByScientificName(Map<String, JsonObject> mapParsedNameByScientificName) {
        this.mapParsedNameByScientificName = mapParsedNameByScientificName;
    }

    protected Map<String, JsonObject> getMapTaxonById() {
        return mapTaxonById;
    }

    protected void setMapTaxonById(Map<String, JsonObject> mapTaxonById) {
        this.mapTaxonById = mapTaxonById;
    }

    protected Map<String, String> getMapTaxonIdByCanonicalNameAndKingdom() {
        return mapTaxonIdByCanonicalNameAndKingdom;
    }

    protected void setMapTaxonIdByCanonicalNameAndKingdom(Map<String, String> mapTaxonIdByCanonicalNameAndKingdom) {
        this.mapTaxonIdByCanonicalNameAndKingdom = mapTaxonIdByCanonicalNameAndKingdom;
    }

    public Map<String, JsonArray> getMapInstitutionsInfoByCode() {
        return mapInstitutionsInfoByCode;
    }

    public void setMapInstitutionsInfoByCode(Map<String, JsonArray> mapInstitutionsInfoByCode) {
        this.mapInstitutionsInfoByCode = mapInstitutionsInfoByCode;
    }

    public Map<String, JsonObject> getMapInstitutionInfoById() {
        return mapInstitutionInfoById;
    }

    public void setMapInstitutionInfoById(Map<String, JsonObject> mapInstitutionInfoById) {
        this.mapInstitutionInfoById = mapInstitutionInfoById;
    }

    public Map<String, JsonObject> getMapCollectionInfoByInstitutionIdAndCollectionName() {
        return mapCollectionInfoByInstitutionIdAndCollectionName;
    }

    public void setMapCollectionInfoByInstitutionIdAndCollectionName(Map<String, JsonObject> mapCollectionInfoByInstitutionIdAndCollectionName) {
        this.mapCollectionInfoByInstitutionIdAndCollectionName = mapCollectionInfoByInstitutionIdAndCollectionName;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Private constructor to avoid client applications to use constructor as we use the singleton design pattern
     */
    private GbifClient(GbifInfo gbifInfo){
        this.gbifInfo=gbifInfo;
        this.mapParsedNameByScientificName = new TreeMap<String,JsonObject>();
        this.mapTaxonById = new TreeMap<String, JsonObject>();
        this.mapTaxonIdByCanonicalNameAndKingdom = new TreeMap<String, String>();
        this.mapInstitutionsInfoByCode = new TreeMap<String, JsonArray>();
        this.mapInstitutionInfoById = new TreeMap<String, JsonObject>();
        this.mapCollectionInfoByInstitutionIdAndCollectionName = new TreeMap<String, JsonObject>();
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Method to get an instance of GbifClient as we use the singleton design pattern
     * @return
     */
    public static GbifClient getInstance(GbifInfo gbifInfo){
        if (instance==null){
            instance = new GbifClient(gbifInfo);
        }
        return instance;
    }

    public JsonObject getTaxonInfoById(String taxonId) throws Exception {
        JsonObject gbifTaxonInfo=null;
        if (this.getMapTaxonById().containsKey(taxonId)){
            gbifTaxonInfo = this.getMapTaxonById().get(taxonId);
        } else{
            try{
                gbifTaxonInfo = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/species/"+taxonId);
            } catch (Exception e){
                this.getLogger().error("Error getting GBIF taxon info for taxonId="+taxonId);
            }
            this.getMapTaxonById().put(taxonId,gbifTaxonInfo);
        }
        return gbifTaxonInfo;
    }

    public JsonObject parseName(String scientificName) throws Exception {
        JsonObject parsedName = null;
        if (this.getMapParsedNameByScientificName().containsKey(scientificName)){
            parsedName = this.getMapParsedNameByScientificName().get(scientificName);
        } else{
            String scientificNameEncoded = URLEncoder.encode(scientificName, "UTF-8");
            JsonArray parsedNames =(JsonArray) NetUtils.doGetRequestJson(this.getApiUrl()+"/parser/name?name="+scientificNameEncoded);
            if (parsedNames.size()==1){
                parsedName = parsedNames.get(0).getAsJsonObject();
            }
            this.getMapParsedNameByScientificName().put(scientificName,parsedName);
        }
        return parsedName;
    }

    public String getTaxonIdByCanonicalNameAndKingdom(String canonicalName, String kingdom) throws Exception {
        String taxonId = null;
        if (this.getMapTaxonIdByCanonicalNameAndKingdom().containsKey(canonicalName+"#"+kingdom)){
            this.getMapTaxonIdByCanonicalNameAndKingdom().get(canonicalName+"#"+kingdom);
        } else{
            String scientificNameEncoded = URLEncoder.encode(canonicalName, "UTF-8");
            String kingdomEncoded = URLEncoder.encode(kingdom, "UTF-8");
            JsonObject searchResult = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/species/match?name="+scientificNameEncoded+"&kingdom="+kingdomEncoded);
            if (searchResult!=null && searchResult.has("usageKey") && canonicalName.equalsIgnoreCase(searchResult.get("canonicalName").getAsString())){
                taxonId=searchResult.get("usageKey").getAsString();
            }
            this.getMapTaxonIdByCanonicalNameAndKingdom().put(canonicalName+"#"+kingdom,taxonId);
        }
        return taxonId;
    }

    public File downloadOccurrencesByCanonicalNameAndKingdom(String canonicalName, String kingdom) throws Exception {
        String taxonId = this.getTaxonIdByCanonicalNameAndKingdom(canonicalName,kingdom);
        return downloadOccurrencesByTaxonId(taxonId);
    }

    public File downloadOccurrencesByTaxonId(String taxonId) throws Exception {
        File dwcaFile = null;
        String auth = "Basic " + Base64.getEncoder().encodeToString((this.getGbifInfo().getUsername()+":"+this.getGbifInfo().getPassword()).getBytes());

        //Request download occurrences
        JsonObject downloadQuery = (JsonObject) FileUtils.loadJsonElementFromResourceFile("gbifFilterSpecimenOccurrenceDownloadPredicate.json");
        JsonObject taxonKeyPredicate = downloadQuery.getAsJsonObject("predicate").getAsJsonArray("predicates").get(0).getAsJsonObject();
        taxonKeyPredicate.addProperty("value",taxonId);
        JsonElement result = NetUtils.doPostRequestJson(this.getApiUrl()+"/occurrence/download/request",auth,downloadQuery);

        String downloadKey=result.getAsString();
        boolean downloadBeingProcessed = true;
        String downloadLink=null;
        int i = 0;
        int sleepSeconds = 30;
        while (downloadBeingProcessed){
            JsonObject downloadInfo = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/occurrence/download/"+downloadKey,auth);
            String status = downloadInfo.get("status").getAsString();
            if (i==60 || status.equalsIgnoreCase("SUCCEEDED")){
                downloadBeingProcessed=false;
                downloadLink=downloadInfo.get("downloadLink").getAsString();
                dwcaFile = NetUtils.downloadFile(downloadLink);
            } else{
                Thread.sleep(sleepSeconds * 1000);
            }
            i++;
        }

        return dwcaFile;
    }

    public JsonArray getInstitutionsInfoByInstitutionCode(String institutionCode) throws Exception {
        JsonArray institutionsInfo = null;
        if (this.getMapInstitutionsInfoByCode().containsKey(institutionCode)){
            institutionsInfo = this.getMapInstitutionsInfoByCode().get(institutionCode);
        } else{
            String institutionCodeEncoded = URLEncoder.encode("\""+institutionCode+"\"", "UTF-8");
            JsonObject data = (JsonObject) this.getDataPaginated(this.getApiUrl()+"/grscicoll/institution?q="+institutionCodeEncoded,50,0);
            JsonArray potentialResults = data.getAsJsonArray("results");

            String jsonPath = "$[?(@.code=~/^" + institutionCode + "$/i)]";
            net.minidev.json.JSONArray filterInsitutions = (net.minidev.json.JSONArray) JsonUtils.filterJson(potentialResults, jsonPath);
            /* The institution code is not unique so it could be several institution with same code */
            Gson gson = new Gson();
            institutionsInfo = gson.fromJson(filterInsitutions.toJSONString(), JsonArray.class);

            for (JsonElement institutionInfo:institutionsInfo) {
                JsonObject countryInfo = this.getInstitutionCountryInfo(institutionInfo.getAsJsonObject());
                if (countryInfo!=null){
                    institutionInfo.getAsJsonObject().add("country",countryInfo);
                }
                String institutionKey = institutionInfo.getAsJsonObject().get("key").getAsString();
                if (!this.getMapInstitutionInfoById().containsKey(institutionKey)){
                    this.getMapInstitutionInfoById().put(institutionKey,institutionInfo.getAsJsonObject());
                }
            }
            this.getMapInstitutionsInfoByCode().put(institutionCode,institutionsInfo);
        }
        return institutionsInfo;
    }

    public JsonObject getInstitutionInfoByInstitutionId(String institutionId) throws Exception {
        JsonObject institutionInfo = null;
        if (this.getMapInstitutionInfoById().containsKey(institutionId)){
            institutionInfo = this.getMapInstitutionInfoById().get(institutionId);
        } else{
            institutionInfo = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/grscicoll/institution/"+institutionId);

            //Get country information of institution
            JsonObject countryInfo = this.getInstitutionCountryInfo(institutionInfo);
            if (countryInfo!=null){
                institutionInfo.getAsJsonObject().add("country",countryInfo);
            }

            this.getMapInstitutionInfoById().put(institutionId,institutionInfo.getAsJsonObject());
        }
        return institutionInfo;
    }


    public JsonObject getCollectionInfoByInstitutionIdAndCollectionCode(String institutionId, String collectionCode) throws Exception {
        JsonObject collectionInfo = null;
        if (this.getMapCollectionInfoByInstitutionIdAndCollectionName().containsKey(institutionId+"#"+collectionCode)){
            collectionInfo = this.getMapCollectionInfoByInstitutionIdAndCollectionName().get(institutionId+"#"+collectionCode);
        } else{
            String collectionCodeEncoded = URLEncoder.encode("\""+collectionCode+"\"", "UTF-8");
            JsonObject data = (JsonObject)  NetUtils.doGetRequestJson(this.getApiUrl()+"/grscicoll/collection?institution="+institutionId+"&?q="+collectionCodeEncoded);

            JsonArray potentialResults = data.getAsJsonArray("results");

            String jsonPath = "$[?(@.code=~/^" + collectionCode + "$/i)]";
            net.minidev.json.JSONArray filterCollections = (net.minidev.json.JSONArray) JsonUtils.filterJson(potentialResults, jsonPath);
            if (filterCollections.size()==1){
                Gson gson = new Gson();
                JsonArray jsonArray = gson.fromJson(filterCollections.toJSONString(), JsonArray.class);
                collectionInfo = jsonArray.get(0).getAsJsonObject();
            }

            this.getMapCollectionInfoByInstitutionIdAndCollectionName().put(institutionId+"#"+collectionCode,collectionInfo);
        }
        return collectionInfo;
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    private JsonObject getDataPaginated(String endPoint, int limit, int offset) throws Exception {
        JsonObject response =(JsonObject) NetUtils.doGetRequestJson(endPoint + "&limit=" + limit + "&offset=" + offset);
        int responseLimit = response.get("limit").getAsInt();
        if (response.getAsJsonArray("results").size()==responseLimit){
            //There is more data to fetch
            JsonObject nextResponse = this.getDataPaginated(endPoint,responseLimit,offset+responseLimit);
            response.addProperty("limit",response.get("count").getAsInt());
            response.getAsJsonArray("results").addAll(nextResponse.getAsJsonArray("results"));
        }
        return response;
    }

    private JsonObject getInstitutionCountryInfo(JsonObject institutionInfo){
        JsonObject countryInfo = null;
        String institutionCountry = null;
        if (institutionInfo.getAsJsonObject().has("address")){
            institutionCountry = institutionInfo.getAsJsonObject().getAsJsonObject("address").get("country").getAsString();
        } else if (institutionInfo.getAsJsonObject().has("mailingAddress")){
            institutionCountry = institutionInfo.getAsJsonObject().getAsJsonObject("mailingAddress").get("country").getAsString();
        } else if (institutionInfo.getAsJsonObject().has("homepage")){
            try{
                CountryResponse countryResponse = NetUtils.getCountryInfoFromUrl(institutionInfo.getAsJsonObject().get("homepage").getAsString());
                institutionCountry = countryResponse.getCountry().getIsoCode();
            } catch (Exception e){
                this.getLogger().error("Error getting country from url address");
            }
        }
        if (StringUtils.isNotBlank(institutionCountry)){
            CountryClient countryClient = CountryClient.getInstance();
            countryInfo = countryClient.getCountryInfoByCountryCode(institutionCountry);
        }
        return countryInfo;
    }
}
