package eu.dissco.digitisers.clients.wiki;

import com.google.gson.JsonObject;
import eu.dissco.digitisers.utils.NetUtils;

import java.net.URLEncoder;

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

    @Override
    public String getWikiType() {
        return "wikipedia";
    }

    @Override
    public JsonObject getWikiInformation(String canonicalName, String kingdom) throws Exception {
        JsonObject wikiInfoObj = null;

        if (this.getMapPageInfoByCanonicalNameAndKingdom().containsKey(canonicalName+"#"+kingdom)) {
            wikiInfoObj=this.getMapPageInfoByCanonicalNameAndKingdom().get(canonicalName+"#"+kingdom);
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
            this.getMapPageInfoByCanonicalNameAndKingdom().put(canonicalName+"#"+kingdom,wikiInfoObj);
        }
        return wikiInfoObj;
    }

    @Override
    public String getPageURL(JsonObject wikiInfoObj){
        String pageUrl = null;
        if (wikiInfoObj!=null){
            pageUrl = wikiInfoObj.get("fullurl").getAsString();
        }
        return pageUrl;
    }

}
