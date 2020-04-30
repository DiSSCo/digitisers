package eu.dissco.digitisers.clients.wiki;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WikiClient {

    /**************/
    /* ATTRIBUTES */
    /**************/
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static WikiClient instance=null;
    private String apiUrl;
    private Map<String, Optional<JsonObject>> mapPageInfoByCanonicalNameAndKingdom; //Map to improve efficiency of this class, so it doesn't need to call the external APIs when we already got results


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    protected String getApiUrl() {
        return apiUrl;
    }

    protected Map<String, Optional<JsonObject>> getMapPageInfoByCanonicalNameAndKingdom() {
        return mapPageInfoByCanonicalNameAndKingdom;
    }

    protected void setMapPageInfoByCanonicalNameAndKingdom(Map<String, Optional<JsonObject>> mapPageInfoByCanonicalNameAndKingdom) {
        this.mapPageInfoByCanonicalNameAndKingdom = mapPageInfoByCanonicalNameAndKingdom;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/
    protected WikiClient(String apiUrl){
        this.apiUrl = apiUrl;
        this.mapPageInfoByCanonicalNameAndKingdom = new ConcurrentHashMap<String,Optional<JsonObject>>();
    }


    /********************/
    /* ABSTRACT METHODS */
    /********************/

    /**
     * Function to get information about the taxon concept hold in the wiki
     * @param canonicalName canonical name of the taxon concept to search
     * @param kingdom name of the kingdom the taxon concept belongs to
     * @return Json object with information of the page for the taxon concept, or null if not found
     * @throws Exception
     */
    public abstract JsonObject getWikiInformation(String canonicalName, String kingdom) throws Exception;

    /**
     * Function that get the page url of the given wiki info object
     * @param wikiInfo wiki info object from where to obtain the wiki page url
     * @return wiki page url, or null if not found
     */
    public abstract String getPageURL(JsonObject wikiInfo);

    /**
     * Function that returns the type of wiki we are using
     * @return type of the wiki (wikipedia or wikidata)
     */
    public abstract String getWikiType();


    /******************/
    /* PUBLIC METHODS */
    /******************/
    /**
     * Method to get an instance of WikipediaClient as we use the singleton design pattern
     * @return
     */
    public static WikiClient getInstance(String type) throws Exception {
        switch (type){
            case "wikipedia":
                if (instance==null || !StringUtils.equals(instance.getClass().getSimpleName(),"WikipediaClient")) instance = new WikipediaClient();
                break;
            case "wikidata":
                if (instance==null || !StringUtils.equals(instance.getClass().getSimpleName(),"WikiDataClient")) instance = new WikiDataClient();
                break;
            default:
                throw new Exception("Type of wiki not suported");
        }

        return instance;
    }
}
