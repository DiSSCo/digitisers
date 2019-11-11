package eu.dissco.digitisers.clients.wiki;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

public abstract class WikiClient {

    /**************/
    /* ATTRIBUTES */
    /**************/
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static WikiClient instance=null;
    private String apiUrl;
    private Map<String,JsonObject> mapPageInfoByCanonicalNameAndKingdom; //Map to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    protected String getApiUrl() {
        return apiUrl;
    }

    protected Map<String, JsonObject> getMapPageInfoByCanonicalNameAndKingdom() {
        return mapPageInfoByCanonicalNameAndKingdom;
    }

    protected void setMapPageInfoByCanonicalNameAndKingdom(Map<String, JsonObject> mapPageInfoByCanonicalNameAndKingdom) {
        this.mapPageInfoByCanonicalNameAndKingdom = mapPageInfoByCanonicalNameAndKingdom;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/
    protected WikiClient(String apiUrl){
        this.apiUrl = apiUrl;
        this.mapPageInfoByCanonicalNameAndKingdom = new TreeMap<String,JsonObject>();
    }


    /********************/
    /* ABSTRACT METHODS */
    /********************/

    public abstract JsonObject getWikiInformation(String canonicalName, String kingdom) throws Exception;

    public abstract String getPageURL(JsonObject wikiInfo);

    public abstract String getWikiType();


    /******************/
    /* PUBLIC METHODS */
    /******************/
    /**
     * Method to get an instance of WikipediaClient as we use the singleton design pattern
     * @return
     */
    public static WikiClient getInstance(String type) throws Exception {
        if (instance==null){
            switch (type){
                case "wikipedia":
                    instance = new WikipediaClient();
                    break;
                case "wikidata":
                    instance = new WikiDataClient();
                    break;
                default:
                    throw new Exception("Type of wiki not suported");
            }
        }
        return instance;
    }
}
