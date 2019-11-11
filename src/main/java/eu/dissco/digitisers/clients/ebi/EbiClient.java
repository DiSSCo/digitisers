package eu.dissco.digitisers.clients.ebi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import eu.dissco.digitisers.clients.ebi.openapi.ApiException;
import eu.dissco.digitisers.clients.ebi.openapi.model.*;
import eu.dissco.digitisers.clients.ebi.openapi.rest.SearchApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EbiClient {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static EbiClient instance=null;
    private final String apiUrl ="https://www.ebi.ac.uk/ebisearch/ws/rest/";
    private Map<String, List<WSEntry>> mapSearchResultBySearchTermAndDomain; //Map to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results
    private Map<String, List<String>> mapRetrievablesFieldsByDomain; //Map to improve efficiency of this class


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    protected String getApiUrl() {
        return apiUrl;
    }

    protected Map<String, List<WSEntry>> getMapSearchResultBySearchTermAndDomain() {
        return mapSearchResultBySearchTermAndDomain;
    }

    protected void setMapSearchResultBySearchTermAndDomain(Map<String, List<WSEntry>> mapSearchResultBySearchTermAndDomain) {
        this.mapSearchResultBySearchTermAndDomain = mapSearchResultBySearchTermAndDomain;
    }

    protected Map<String, List<String>> getMapRetrievablesFieldsByDomain() {
        return mapRetrievablesFieldsByDomain;
    }

    protected void setMapRetrievablesFieldsByDomain(Map<String, List<String>> mapRetrievablesFieldsByDomain) {
        this.mapRetrievablesFieldsByDomain = mapRetrievablesFieldsByDomain;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Private constructor to avoid client applications to use constructor as we use the singleton design pattern
     */
    private EbiClient(){
        this.mapSearchResultBySearchTermAndDomain = new TreeMap<String,List<WSEntry>>();
        this.mapRetrievablesFieldsByDomain = new TreeMap<String,List<String>>();
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

    public JsonArray rootSearchAsJson(String searchTerm) throws ApiException {
        Gson gson = new Gson();
        List<WSEntry> ebiResults = this.rootSearch(searchTerm,false);
        return (JsonArray)gson.toJsonTree(ebiResults);
    }

    public JsonArray rootSearchAsJson(String searchTerm, boolean exactMatch) throws ApiException {
        Gson gson = new Gson();
        List<WSEntry> ebiResults = this.rootSearch(searchTerm,exactMatch);
        return (JsonArray)gson.toJsonTree(ebiResults);
    }

    public JsonArray domainSearchAsJson(String domainId, String searchTerm) throws ApiException {
        Gson gson = new Gson();
        List<WSEntry> ebiResults = this.domainSearch(domainId,searchTerm,false);
        return (JsonArray)gson.toJsonTree(ebiResults);
    }

    public JsonArray domainSearchAsJson(String domainId, String searchTerm, boolean exactMatch) throws ApiException {
        Gson gson = new Gson();
        List<WSEntry> ebiResults = this.domainSearch(domainId,searchTerm,exactMatch);
        return (JsonArray)gson.toJsonTree(ebiResults);
    }

    /***
     * Function that searches for the searchTerm as free text in the specific ebi domain
     * @param domainId
     * @param searchTerm
     * @param exactMatch
     * @return list with information of all the entries found
     * @throws ApiException
     */
    public List<WSEntry> domainSearch(String domainId, String searchTerm, boolean exactMatch) throws ApiException {
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
            this.getMapSearchResultBySearchTermAndDomain().put(searchTerm+"#"+domainId,resultsEntries);
        }

        return this.getMapSearchResultBySearchTermAndDomain().get(searchTerm+"#"+domainId);
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /***
     * Function that searches for the searchTerm as free text in all ebi domains
     * @param searchTerm
     * @param exactMatch
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

    private List<String> getRetrievableFieldsByDomain(String domainId) throws ApiException {
        if (!this.getMapRetrievablesFieldsByDomain().containsKey(domainId)){
            SearchApi api = new SearchApi();
            WSSearchResult response = api.search(domainId, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null);
            WSDomain domain = response.getDomains().get(0);
            List<String> retrievableFields = this.getRetrievableFields(domain.getFieldInfos());
            this.getMapRetrievablesFieldsByDomain().put(domain.getId(),retrievableFields);
        }
        return this.getMapRetrievablesFieldsByDomain().get(domainId);
    }

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
