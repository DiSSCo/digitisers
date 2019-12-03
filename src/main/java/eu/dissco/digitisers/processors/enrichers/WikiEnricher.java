package eu.dissco.digitisers.processors.enrichers;

import com.google.gson.JsonObject;
import eu.dissco.digitisers.clients.gbif.GbifClient;
import eu.dissco.digitisers.clients.gbif.GbifInfo;
import eu.dissco.digitisers.clients.wiki.WikiClient;
import eu.dissco.digitisers.utils.DigitalSpecimenUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.terms.GbifTerm;

import java.util.HashMap;
import java.util.Map;

public class WikiEnricher extends Enricher {

    public WikiEnricher(DigitalObject ds, Configuration config){
        super(ds,config);
    }

    @Override
    protected Map<String, Object> enrichData() {
        Map<String, Object> data=null;
        try{
            String wikipedia = DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"wikipedia");
            if (StringUtils.isBlank(wikipedia)){
                String acceptedScientificName = DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"scientificName");
                String gbifKingdomTaxonId = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(), GbifTerm.kingdomKey);
                if (StringUtils.isNotBlank(acceptedScientificName) && StringUtils.isNotBlank(gbifKingdomTaxonId) ) {
                    GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
                    GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
                    JsonObject parsedName = gbifClient.parseName(acceptedScientificName);
                    JsonObject kingdomInfo = gbifClient.getTaxonInfoById(gbifKingdomTaxonId);

                    if (parsedName != null && kingdomInfo != null) {
                        String canonicalName = parsedName.get("canonicalName").getAsString();
                        String kingdomName = kingdomInfo.get("scientificName").getAsString();
                        WikiClient wikiClient = WikiClient.getInstance("wikidata");
                        JsonObject wikiInfo = wikiClient.getWikiInformation(canonicalName,kingdomName);
                        if (wikiInfo!=null){
                            data = new HashMap<String, Object>();
                            data.put(wikiClient.getWikiType(),wikiClient.getPageURL(wikiInfo));
                            data.put(wikiClient.getWikiType()+"_info",wikiInfo);
                        }
                    }
                } else{
                    this.getLogger().info("Not enough information for enriching ds with wiki data " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId"));
                }
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching wiki data for ds " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId") + " . Reason: " + e.getMessage(),this.getDs());
        }
        return data;
    }
}
