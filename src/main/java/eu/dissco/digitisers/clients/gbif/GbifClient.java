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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GbifClient {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static GbifClient instance=null;
    private final String apiUrl = "http://api.gbif.org/v1";
    private final GbifInfo gbifInfo;
    private Map<String, Optional<JsonObject>> mapTaxonById; //Map to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results
    private Map<String,Optional<JsonObject>> mapParsedNameByScientificName; //Map to improve efficiency of this class
    private Map<String,Optional<String>> mapTaxonIdByCanonicalNameAndKingdom; //Map to improve efficiency of this class
    private Map<String,Optional<JsonArray>> mapInstitutionsInfoByCode; //Map to improve efficiency of this class
    private Map<String,Optional<JsonObject>> mapInstitutionInfoById; //Map to improve efficiency of this class
    private Map<String,Optional<JsonObject>> mapCollectionInfoByInstitutionIdAndCollectionName; //Map to improve efficiency of this class
    private Map<String,Optional<String>> mapOccurrenceIdByGbifId; //Map to improve efficiency of this class


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

    protected Map<String, Optional<JsonObject>> getMapParsedNameByScientificName() {
        return mapParsedNameByScientificName;
    }

    protected void setMapParsedNameByScientificName(Map<String, Optional<JsonObject>> mapParsedNameByScientificName) {
        this.mapParsedNameByScientificName = mapParsedNameByScientificName;
    }

    protected Map<String, Optional<JsonObject>> getMapTaxonById() {
        return mapTaxonById;
    }

    protected void setMapTaxonById(Map<String, Optional<JsonObject>> mapTaxonById) {
        this.mapTaxonById = mapTaxonById;
    }

    protected Map<String, Optional<String>> getMapTaxonIdByCanonicalNameAndKingdom() {
        return mapTaxonIdByCanonicalNameAndKingdom;
    }

    protected void setMapTaxonIdByCanonicalNameAndKingdom(Map<String, Optional<String>> mapTaxonIdByCanonicalNameAndKingdom) {
        this.mapTaxonIdByCanonicalNameAndKingdom = mapTaxonIdByCanonicalNameAndKingdom;
    }

    public Map<String, Optional<JsonArray>> getMapInstitutionsInfoByCode() {
        return mapInstitutionsInfoByCode;
    }

    public void setMapInstitutionsInfoByCode(Map<String, Optional<JsonArray>> mapInstitutionsInfoByCode) {
        this.mapInstitutionsInfoByCode = mapInstitutionsInfoByCode;
    }

    public Map<String, Optional<JsonObject>> getMapInstitutionInfoById() {
        return mapInstitutionInfoById;
    }

    public void setMapInstitutionInfoById(Map<String, Optional<JsonObject>> mapInstitutionInfoById) {
        this.mapInstitutionInfoById = mapInstitutionInfoById;
    }

    public Map<String, Optional<JsonObject>> getMapCollectionInfoByInstitutionIdAndCollectionName() {
        return mapCollectionInfoByInstitutionIdAndCollectionName;
    }

    public void setMapCollectionInfoByInstitutionIdAndCollectionName(Map<String, Optional<JsonObject>> mapCollectionInfoByInstitutionIdAndCollectionName) {
        this.mapCollectionInfoByInstitutionIdAndCollectionName = mapCollectionInfoByInstitutionIdAndCollectionName;
    }

    public Map<String, Optional<String>> getMapOccurrenceIdByGbifId() {
        return mapOccurrenceIdByGbifId;
    }

    public void setMapOccurrenceIdByGbifIde(Map<String, Optional<String>> mapOccurrenceIdByGbifId) {
        this.mapOccurrenceIdByGbifId = mapOccurrenceIdByGbifId;
    }

    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Private constructor to avoid client applications to use constructor as we use the singleton design pattern
     */
    private GbifClient(GbifInfo gbifInfo){
        this.gbifInfo=gbifInfo;
        this.mapParsedNameByScientificName = new ConcurrentHashMap<String,Optional<JsonObject>>();
        this.mapTaxonById = new ConcurrentHashMap<String, Optional<JsonObject>>();
        this.mapTaxonIdByCanonicalNameAndKingdom = new ConcurrentHashMap<String, Optional<String>>();
        this.mapInstitutionsInfoByCode = new ConcurrentHashMap<String, Optional<JsonArray>>();
        this.mapInstitutionInfoById = new ConcurrentHashMap<String, Optional<JsonObject>>();
        this.mapCollectionInfoByInstitutionIdAndCollectionName = new ConcurrentHashMap<String, Optional<JsonObject>>();
        this.mapOccurrenceIdByGbifId = new ConcurrentHashMap<String, Optional<String>>();
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

    /**
     * Function to get the taxon concept information in GBIF by its taxonID
     * @param taxonId taxonId of the taxon concept we want to obtain its information
     * @return json object with the taxon concept information for the given taxonID if its found in GBIF, or null otherwise
     * @throws Exception
     */
    public JsonObject getTaxonInfoById(String taxonId) throws Exception {
        JsonObject gbifTaxonInfo=null;
        if (this.getMapTaxonById().containsKey(taxonId)){
            gbifTaxonInfo = this.getMapTaxonById().get(taxonId).orElse(null);
        } else{
            try{
                gbifTaxonInfo = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/species/"+taxonId);
            } catch (Exception e){
                this.getLogger().error("Error getting GBIF taxon info for taxonId="+taxonId);
            }
            this.getMapTaxonById().put(taxonId,Optional.ofNullable(gbifTaxonInfo));
        }
        return gbifTaxonInfo;
    }

    /**
     * Function that parsed a scientific names.
     * @param scientificName scientific name to be parsed
     * @return Json object with the information of parsing the scientific name or null if the parser fails
     * @throws Exception
     */
    public JsonObject parseName(String scientificName) throws Exception {
        JsonObject parsedName = null;
        if (this.getMapParsedNameByScientificName().containsKey(scientificName)){
            parsedName = this.getMapParsedNameByScientificName().get(scientificName).orElse(null);
        } else{
            String scientificNameEncoded = URLEncoder.encode(scientificName, "UTF-8");
            JsonArray parsedNames =(JsonArray) NetUtils.doGetRequestJson(this.getApiUrl()+"/parser/name?name="+scientificNameEncoded);
            if (parsedNames.size()==1){
                parsedName = parsedNames.get(0).getAsJsonObject();
            }
            this.getMapParsedNameByScientificName().put(scientificName,Optional.ofNullable(parsedName));
        }
        return parsedName;
    }

    /**
     * Function to get the taxon id holds in GBIF by canonicalName and kingdom
     * @param canonicalName canonical name of the taxon concept we want to obtain its info
     * @param kingdom name of the kingdom the taxon concept belongs to
     * @return taxonId if its found in GBIF, or null otherwise
     * @throws Exception
     */
    public String getTaxonIdByCanonicalNameAndKingdom(String canonicalName, String kingdom) throws Exception {
        String taxonId = null;
        if (this.getMapTaxonIdByCanonicalNameAndKingdom().containsKey(canonicalName+"#"+kingdom)){
            taxonId = this.getMapTaxonIdByCanonicalNameAndKingdom().get(canonicalName+"#"+kingdom).orElse(null);
        } else{
            String scientificNameEncoded = URLEncoder.encode(canonicalName, "UTF-8");
            String kingdomEncoded = URLEncoder.encode(kingdom, "UTF-8");
                JsonObject searchResult = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/species/match?name="+scientificNameEncoded+"&kingdom="+kingdomEncoded);
            if (searchResult!=null && searchResult.has("usageKey") && canonicalName.equalsIgnoreCase(searchResult.get("canonicalName").getAsString())){
                taxonId=searchResult.get("usageKey").getAsString();
            }
            this.getMapTaxonIdByCanonicalNameAndKingdom().put(canonicalName+"#"+kingdom,Optional.ofNullable(taxonId));
        }
        return taxonId;
    }

    /**
     * Function to get the occurrenceId holds in GBIF by a gbifId code
     * @param gbifId gbifId
     * @return json object with the taxon concept information if its found in GBIF, or null otherwise
     * @throws Exception
     */
    public String getOccurrenceIdByGbifId(String gbifId) throws Exception {
        String occurrenceId = null;
        if (this.getMapOccurrenceIdByGbifId().containsKey(gbifId)){
            occurrenceId = this.getMapOccurrenceIdByGbifId().get(gbifId).orElse(null);
        } else{
            JsonObject searchResult = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/occurrence/"+gbifId);
            if (searchResult!=null && searchResult.has("occurrenceID")){
                occurrenceId=searchResult.get("occurrenceID").getAsString();
            }
            this.getMapOccurrenceIdByGbifId().put(gbifId,Optional.ofNullable(occurrenceId));
        }
        return occurrenceId;
    }

    /**
     * Function that download all the specimens (preserved, living and fossils) for a given taxon concept
     * @param canonicalName canonical name of the taxon concept we want to obtain its specimens
     * @param kingdom name of the kingdom the taxon concept belongs to
     * @return Dwca file with the specimen data obtained from GBIF
     * @throws Exception
     */
    public File downloadOccurrencesByCanonicalNameAndKingdom(String canonicalName, String kingdom) throws Exception {
        String taxonId = this.getTaxonIdByCanonicalNameAndKingdom(canonicalName,kingdom);
        return downloadOccurrencesByTaxonId(taxonId);
    }

    /**
     * Function that download all the specimens (preserved, living and fossils) for a given taxon concept
     * @param taxonId taxonID of the taxon concept we want to obtain its specimens
     * @return Dwca file with the specimen data obtained from GBIF
     * @throws Exception
     */
    public File downloadOccurrencesByTaxonId(String taxonId) throws Exception {
        File dwcaFile = null;
        String auth = "Basic " + Base64.getEncoder().encodeToString((this.getGbifInfo().getUsername()+":"+this.getGbifInfo().getPassword()).getBytes());

        JsonObject newTaxonPredicate = new JsonObject();
        newTaxonPredicate.addProperty("type","equals");
        newTaxonPredicate.addProperty("key","TAXON_KEY");
        newTaxonPredicate.addProperty("value",taxonId);

        //Request download occurrences
        JsonObject downloadQuery = (JsonObject) FileUtils.loadJsonElementFromResourceFile("gbifFilterSpecimenOccurrenceDownloadPredicate.json");
        JsonArray predicates = downloadQuery.getAsJsonObject("predicate").getAsJsonArray("predicates");
        predicates.add(newTaxonPredicate);

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

    /**
     * Function that download all the specimens (preserved, living and fossils) for a given taxon concept
     * @param occurrenceIds list with the occurence ids of the specimens to download from GBIF
     * @return Dwca file with the specimen data obtained from GBIF
     * @throws Exception
     */
    public File downloadOccurrencesByListOccurrenceIds(List<String> occurrenceIds) throws Exception {
        File dwcaFile = null;
        String auth = "Basic " + Base64.getEncoder().encodeToString((this.getGbifInfo().getUsername()+":"+this.getGbifInfo().getPassword()).getBytes());

        JsonArray occurrencePredicates = new JsonArray();
        for (String occurrenceId:occurrenceIds) {
            JsonObject newOccurrencePredicate = new JsonObject();
            newOccurrencePredicate.addProperty("type","equals");
            newOccurrencePredicate.addProperty("key","OCCURRENCE_ID");
            newOccurrencePredicate.addProperty("value",occurrenceId);
            occurrencePredicates.add(newOccurrencePredicate);
        }

        //Request download occurrences
        JsonObject downloadQuery = (JsonObject) FileUtils.loadJsonElementFromResourceFile("gbifFilterSpecimenOccurrenceDownloadPredicate.json");
        JsonArray predicates = downloadQuery.getAsJsonObject("predicate").getAsJsonArray("predicates");
        JsonObject newPredicate = new JsonObject();
        newPredicate.addProperty("type","or");
        newPredicate.add("predicates",occurrencePredicates);
        predicates.add(newPredicate);

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

    /**
     * Function that gets the institution information stored in GRSciColl, by searching for its institution code.
     * Note: As the institution code is not unique, this function will return an json array with the information of all
     * the institutions that has this code
     * @param institutionCode code of the institution to get its information
     * @return json array with the information of all the institutions that has this code
     * @throws Exception
     */
    public JsonArray getInstitutionsInfoByInstitutionCode(String institutionCode) throws Exception {
        JsonArray institutionsInfo = null;
        if (this.getMapInstitutionsInfoByCode().containsKey(institutionCode)){
            institutionsInfo = this.getMapInstitutionsInfoByCode().get(institutionCode).orElse(null);
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
                    this.getMapInstitutionInfoById().put(institutionKey,Optional.ofNullable(institutionInfo.getAsJsonObject()));
                }
            }
            this.getMapInstitutionsInfoByCode().put(institutionCode,Optional.ofNullable(institutionsInfo));
        }
        return institutionsInfo;
    }

    /**
     * Function that gets the institution information stored in GRSciColl, by searching for its institution id.
     * @param institutionId id of the institution to get its information
     * @return json object with the information of all the institutions if it was found, or null otherwise
     * @throws Exception
     */
    public JsonObject getInstitutionInfoByInstitutionId(String institutionId) throws Exception {
        JsonObject institutionInfo = null;
        if (this.getMapInstitutionInfoById().containsKey(institutionId)){
            institutionInfo = this.getMapInstitutionInfoById().get(institutionId).orElse(null);
        } else{
            institutionInfo = (JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+"/grscicoll/institution/"+institutionId);

            //Get country information of institution
            JsonObject countryInfo = this.getInstitutionCountryInfo(institutionInfo);
            if (countryInfo!=null){
                institutionInfo.getAsJsonObject().add("country",countryInfo);
            }

            this.getMapInstitutionInfoById().put(institutionId,Optional.ofNullable(institutionInfo.getAsJsonObject()));
        }
        return institutionInfo;
    }

    /**
     * Function that gets collection information from its insitution id and its collection code
     * @param institutionId institution id
     * @param collectionCode collection code
     * @return json object with the information of the collection if its found in GBIF or null otherwise
     * @throws Exception
     */
    public JsonObject getCollectionInfoByInstitutionIdAndCollectionCode(String institutionId, String collectionCode) throws Exception {
        JsonObject collectionInfo = null;
        if (this.getMapCollectionInfoByInstitutionIdAndCollectionName().containsKey(institutionId+"#"+collectionCode)){
            collectionInfo = this.getMapCollectionInfoByInstitutionIdAndCollectionName().get(institutionId+"#"+collectionCode).orElse(null);
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

            this.getMapCollectionInfoByInstitutionIdAndCollectionName().put(institutionId+"#"+collectionCode,Optional.ofNullable(collectionInfo));
        }
        return collectionInfo;
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /**
     * Function to get recursively get all the data, when we call an endpoint that returns the result using pagination
     * @param endPoint endpoint to get the data from
     * @param limit number of the element to get for page
     * @param offset offset
     * @return Json object with the response containing all the object from the endpoint
     * @throws Exception
     */
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

    /**
     * Function that get the country information from an institution object.
     * Note: If the object has an "address" attribute, it tries to get it from there, if not it tries to use the "mailingAdress"
     * attribute and if not it tries to get it from the country where the homepage of the institution is hosted
     * @param institutionInfo institution object from which we want to obtain its country information
     * @return Country information of the institution when it can be found, or null otherwise
     */
    private JsonObject getInstitutionCountryInfo(JsonObject institutionInfo){
        JsonObject countryInfo = null;
        String institutionCountry = null;
        if (institutionInfo.getAsJsonObject().has("address") && institutionInfo.getAsJsonObject().getAsJsonObject("address").has("country")){
            institutionCountry = institutionInfo.getAsJsonObject().getAsJsonObject("address").get("country").getAsString();
        } else if (institutionInfo.getAsJsonObject().has("mailingAddress") && institutionInfo.getAsJsonObject().getAsJsonObject("mailingAddress").has("country")){
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
