package eu.dissco.digitisers.clients.wiki;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.TreeMap;

public abstract class WikiClient {

    private String apiUrl;

    //Hashmap to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results
    private Map<String,JsonObject> mapPageInfoByCanonicalNameAndKingdom;

    public String getApiUrl() {
        return apiUrl;
    }

    protected void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    protected Map<String, JsonObject> getMapPageInfoByCanonicalNameAndKingdom() {
        return mapPageInfoByCanonicalNameAndKingdom;
    }

    protected void setMapPageInfoByCanonicalNameAndKingdom(Map<String, JsonObject> mapPageInfoByCanonicalNameAndKingdom) {
        this.mapPageInfoByCanonicalNameAndKingdom = mapPageInfoByCanonicalNameAndKingdom;
    }

    protected WikiClient(String apiUrl){
        this.apiUrl = apiUrl;
        this.mapPageInfoByCanonicalNameAndKingdom = new TreeMap<String,JsonObject>();
    }

    public abstract JsonObject getWikiInformation(String canonicalName, String kingdom) throws Exception;

    public abstract String getPageURL(JsonObject wikiInfo);

    public abstract String getClientName();
}
