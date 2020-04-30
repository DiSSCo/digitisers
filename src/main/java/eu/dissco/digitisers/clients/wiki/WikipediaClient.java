package eu.dissco.digitisers.clients.wiki;

import com.google.gson.JsonObject;
import eu.dissco.digitisers.utils.NetUtils;

import java.net.URLEncoder;
import java.util.Optional;

public class WikipediaClient extends WikiClient {


    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Private constructor to avoid client applications to use constructor as we use the singleton design pattern
     */
    protected WikipediaClient(){
        super("https://en.wikipedia.org/w/api.php");
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Function that returns the type of wiki we are using
     * @return wikipedia
     */
    @Override
    public String getWikiType() {
        return "wikipedia";
    }

    /**
     * Function to get information about the taxon concept hold in the wiki
     * @param canonicalName canonical name of the taxon concept to search
     * @param kingdom name of the kingdom the taxon concept belongs to
     * @return Json object with information of the page for the taxon concept, or null if not found
     * @throws Exception
     */
    @Override
    public JsonObject getWikiInformation(String canonicalName, String kingdom) throws Exception {
        JsonObject wikiInfoObj = null;

        if (this.getMapPageInfoByCanonicalNameAndKingdom().containsKey(canonicalName+"#"+kingdom)) {
            wikiInfoObj=this.getMapPageInfoByCanonicalNameAndKingdom().get(canonicalName+"#"+kingdom).orElse(null);
        } else {
            String canonicalNameEncoded = URLEncoder.encode("\""+canonicalName+"\"", "UTF-8");
            JsonObject wikiResponse =(JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+
                    "?action=query&list=search&srsearch=intitle:"+canonicalNameEncoded+"&format=json");

            if (wikiResponse!=null && wikiResponse.getAsJsonObject("query").getAsJsonObject("searchinfo").get("totalhits").getAsInt()==1){
                int pageId = wikiResponse.getAsJsonObject("query").getAsJsonArray("search").get(0).getAsJsonObject().get("pageid").getAsInt();
                wikiResponse =(JsonObject) NetUtils.doGetRequestJson(this.getApiUrl()+
                        "?action=query&prop=info&inprop=url&pageids="+pageId+"&format=json");
                if (wikiResponse!=null){
                    wikiInfoObj = wikiResponse.getAsJsonObject("query").getAsJsonObject("pages").getAsJsonObject(String.valueOf(pageId));
                }
            }
            this.getMapPageInfoByCanonicalNameAndKingdom().put(canonicalName+"#"+kingdom, Optional.ofNullable(wikiInfoObj));
        }
        return wikiInfoObj;
    }

    /**
     * Function that get the page url of the given wiki info object
     * @param wikiInfo wiki info object from where to obtain the wiki page url
     * @return wiki page url, or null if not found
     */
    @Override
    public String getPageURL(JsonObject wikiInfo){
        String pageUrl = null;
        if (wikiInfo!=null && wikiInfo.has("fullurl")){
            pageUrl = wikiInfo.get("fullurl").getAsString();
        }
        return pageUrl;
    }

}
