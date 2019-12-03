package eu.dissco.digitisers.clients.ebi;

import com.google.gson.JsonArray;
import eu.dissco.digitisers.clients.ebi.openapi.ApiException;
import eu.dissco.digitisers.clients.ebi.openapi.model.*;
import eu.dissco.digitisers.clients.ebi.openapi.rest.SearchApi;
import eu.dissco.digitisers.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EbiClient {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static EbiClient instance=null;
    private final String apiUrl ="https://www.ebi.ac.uk/ebisearch/ws/rest/";
    private Map<String, Optional<List<WSEntry>>> mapSearchResultBySearchTermAndDomain; //Map to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results
    private Map<String, Optional<List<String>>> mapRetrievablesFieldsByDomain; //Map to improve efficiency of this class


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    protected String getApiUrl() {
        return apiUrl;
    }

    protected Map<String, Optional<List<WSEntry>>> getMapSearchResultBySearchTermAndDomain() {
        return mapSearchResultBySearchTermAndDomain;
    }

    protected void setMapSearchResultBySearchTermAndDomain(Map<String, Optional<List<WSEntry>>> mapSearchResultBySearchTermAndDomain) {
        this.mapSearchResultBySearchTermAndDomain = mapSearchResultBySearchTermAndDomain;
    }

    protected Map<String, Optional<List<String>>> getMapRetrievablesFieldsByDomain() {
        return mapRetrievablesFieldsByDomain;
    }

    protected void setMapRetrievablesFieldsByDomain(Map<String, Optional<List<String>>> mapRetrievablesFieldsByDomain) {
        this.mapRetrievablesFieldsByDomain = mapRetrievablesFieldsByDomain;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Private constructor to avoid client applications to use constructor as we use the singleton design pattern
     */
    private EbiClient(){
        this.mapSearchResultBySearchTermAndDomain = new ConcurrentHashMap<String,Optional<List<WSEntry>>>();
        this.mapRetrievablesFieldsByDomain = new ConcurrentHashMap<String,Optional<List<String>>>();
    }


    /*******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Method to get an instance of EbiClient as we use the singleton design pattern
     * @return
     */
    public static EbiClient getInstance(){
        if (instance==null){
            instance = new EbiClient();
        }
        return instance;
    }

    /**
     * Function that does a free text of the search term passed as parameter in all the domains of EBI
     * @param searchTerm term to be searched. The term has to be a string or group of strings
     * @return JsonArray with the result of the search in the different database domain levels
     * @throws ApiException
     */
    public JsonArray rootSearchAsJson(String searchTerm) throws ApiException {
        List<WSEntry> ebiResults = this.rootSearch(searchTerm,false);
        return (JsonArray)JsonUtils.convertObjectToJsonElement(ebiResults);
    }

    /**
     * Function that does a free text, using exact match, of the search term passed as parameter in all the domains of EBI
     * @param searchTerm term to be searched. The term has to be a string or group of strings
     * @param exactMatch flag to indicate if we should do a exact match (when we search for a group of strings, they have
     *                   to appear in that order)
     * @return JsonArray with the result of the search in the different database domain levels
     * @throws ApiException
     */
    public JsonArray rootSearchAsJson(String searchTerm, boolean exactMatch) throws ApiException {
        List<WSEntry> ebiResults = this.rootSearch(searchTerm,exactMatch);
        return (JsonArray)JsonUtils.convertObjectToJsonElement(ebiResults);
    }

    /**
     * Function that does a search of the term passed as parameter in a specif EBI domain
     * If the domain is a database level, then the term to search could be field specific (eg: for "emblrelease_standard",
     * the search term can be "description:\"MNHN IM 2013-7767\" AND TAXON:1504874 AND INTERPRO:IPR036927 AND INTERPRO:IPR023616"
     * @param domainId domain where to do the search
     * @param searchTerm term to be searched
     * @return JsonArray with the result of the search in the different database domain levels from that domainId
     * @throws ApiException
     */
    public JsonArray domainSearchAsJson(String domainId, String searchTerm) throws ApiException {
        List<WSEntry> ebiResults = this.domainSearch(domainId,searchTerm,false);
        return (JsonArray)JsonUtils.convertObjectToJsonElement(ebiResults);
    }

    /**
     * Function that does a search, using a exact match, of the term passed as parameter in a specif EBI domain
     * Note: The exact match only apply if we are only searching free text
     * @param domainId domain where to do the search
     * @param searchTerm term to be searched
     * @param exactMatch flag to indicate if we should do a exact match (when we search for a group of strings, they have
     *                   to appear in that order)
     * @return JsonArray with the result of the search in the different database domain levels from that domainId
     * @throws ApiException
     */
    public JsonArray domainSearchAsJson(String domainId, String searchTerm, boolean exactMatch) throws ApiException {
        List<WSEntry> ebiResults = this.domainSearch(domainId,searchTerm,exactMatch);
        return (JsonArray)JsonUtils.convertObjectToJsonElement(ebiResults);
    }



    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /***
     * Function that searches for the searchTerm in the specific ebi domain
     * @param domainId domain where to do the search
     * @param searchTerm term to be searched
     * @param exactMatch flag to indicate if we should do a exact match (when we search for a group of strings, they have
     *                   to appear in that order)
     * @return list with information of all the entries found
     * @throws ApiException
     */
    private List<WSEntry> domainSearch(String domainId, String searchTerm, boolean exactMatch) throws ApiException {
        if (exactMatch && !searchTerm.startsWith("\"")){
            searchTerm = "\""+searchTerm+"\"";
        }

        if (!this.getMapSearchResultBySearchTermAndDomain().containsKey(searchTerm+"#"+domainId)){
            SearchApi api = new SearchApi();
            List<String> retrievableFields = this.getRetrievableFieldsByDomain(domainId);
            String fields = String.join(",", retrievableFields);

            WSSearchResult searchResult = api.search(domainId, null, searchTerm, null, null, null, null,
                    null, fields, null, null, null, null, null, null,
                    null, null, null, null, null, null, null);

            List<WSEntry> resultsEntries = this.getEntriesDetailsInSearchResult(searchResult,searchTerm,exactMatch);
            this.getMapSearchResultBySearchTermAndDomain().put(searchTerm+"#"+domainId,Optional.ofNullable(resultsEntries));
        }

        return this.getMapSearchResultBySearchTermAndDomain().get(searchTerm+"#"+domainId).orElse(null);
    }

    /***
     * Function that searches for the searchTerm as free text in all ebi domains, trying to expanding the result
     * of the search for all the domains where it had a hit, with information of the fields in the databases it was found
     * @param searchTerm term to be searched
     * @param exactMatch flag to indicate if we should do a exact match (when we search for a group of strings, they have
     *                   to appear in that order)
     * @return list with information of all the entries found
     * @throws ApiException
     */
    private List<WSEntry> rootSearch(String searchTerm, boolean exactMatch) throws ApiException {
        if (exactMatch && !searchTerm.startsWith("\"")){
            searchTerm = "\""+searchTerm+"\"";
        }

        //Note: if we search by specific domain, we can restrict the search by looking at searchable field but not for root search
        //Eg: emblrelease_standard?query=%2522MNHN-IM-2013-7767%2522%2520AND%2520TAXON:1504874&fields=TAXON'
        SearchApi api = new SearchApi();
        WSSearchResult searchResult = api.rootsearch(null,searchTerm,null);
        return this.getEntriesDetailsInSearchResult(searchResult,searchTerm,exactMatch);
    }

    /**
     * Function that expands the result of the search getting information of the fields in the databases where the search term was found
     * @param searchResult result of the search
     * @param searchTerm search term used in the search
     * @param exactMatch flag to indicate if we should do a exact match (when we search for a group of strings, they have
     *                   to appear in that order)
     * @return List of the entries in EBI where the search term was found
     * @throws ApiException
     */
    private List<WSEntry> getEntriesDetailsInSearchResult(WSSearchResult searchResult, String searchTerm, boolean exactMatch) throws ApiException {
        List<WSEntry> resultsEntries=new ArrayList<WSEntry>();
        if (searchResult.getDomains()!=null){
            List<WSDomain> leavesDomains = new ArrayList<WSDomain>();
            for (WSDomain domain : searchResult.getDomains()) {
                leavesDomains.addAll(this.getLeavesDomains(domain));
            }
            for (WSDomain leafDomain : leavesDomains) {
                if (leafDomain.getHitCount()>0){
                    resultsEntries.addAll(this.domainSearch(leafDomain.getId(),searchTerm,exactMatch));
                }
            }
        } else{
            for (WSEntry entry: searchResult.getEntries()) {
                if (entry.getFields()!=null && entry.getFields().size()>0){
                    //We have already a fully detailed entry
                    resultsEntries.add(entry);
                } else{
                    //When we search for an intermediate domain, the results has entries but they don't have fields
                    //so we need to do another search for the specific leaf domain of that entry
                    resultsEntries.addAll(this.domainSearch(entry.getSource(),searchTerm,exactMatch));
                }
            }
        }
        return resultsEntries;
    }

    /**
     * Function that get the leave subdomains (databases) of a given EBI domain
     * @param domain domain from which to obtain its databases
     * @return List of leave subdomains (database) for the given EBI domain
     */
    private List<WSDomain> getLeavesDomains(WSDomain domain){
        List<WSDomain> leavesDomains = new ArrayList<WSDomain>();
        if (domain.getSubdomains()==null){
            leavesDomains.add(domain);
        } else{
            for (WSDomain subdomain: domain.getSubdomains()) {
                leavesDomains.addAll(this.getLeavesDomains(subdomain));
            }
        }
        return leavesDomains;
    }

    /**
     * Function to get the list of retrievable fields of a given EBI database domain
     * @param domainId EBI database domain to obtain its list of retrievable fields
     * @return ist of retrievable fields of the given EBI database domain
     * @throws ApiException
     */
    private List<String> getRetrievableFieldsByDomain(String domainId) throws ApiException {
        if (!this.getMapRetrievablesFieldsByDomain().containsKey(domainId)){
            SearchApi api = new SearchApi();
            WSSearchResult response = api.search(domainId, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null);
            WSDomain domain = response.getDomains().get(0);
            List<String> retrievableFields = this.getRetrievableFields(domain.getFieldInfos());
            this.getMapRetrievablesFieldsByDomain().put(domain.getId(),Optional.ofNullable(retrievableFields));
        }
        return this.getMapRetrievablesFieldsByDomain().get(domainId).orElse(null);
    }

    /**
     * Function that get the list of names of retrievable fields form the list of WSFieldInfo
     * @param fields List of fields from which to obtain the name of the retrievable fields
     * @return list with the names of retrievable fields
     */
    private List<String> getRetrievableFields(List<WSFieldInfo> fields){
        List<String> retrievableFields = new ArrayList<String>();
        if (fields!=null){
            for (WSFieldInfo field: fields) {
                if (this.isFieldRetrievable(field)){
                    retrievableFields.add(field.getId());
                }
            }
        }
        return retrievableFields;
    }

    /**
     * Function that checks if a field is a retrievable field
     * @param field Field to check if it is retrievable
     * @return true if field is retrievable, false otherwise
     */
    private boolean isFieldRetrievable(WSFieldInfo field){
        boolean isRetrievable = false;
        for (WSOption option: field.getOptions()) {
            if (option.getName().equalsIgnoreCase("retrievable") && option.getValue().equalsIgnoreCase("true")){
                isRetrievable=true;
                break;
            }
        }
        return isRetrievable;
    }

}
