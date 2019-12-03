package eu.dissco.digitisers.processors.enrichers;

import com.google.gson.JsonObject;
import eu.dissco.digitisers.clients.col.CoLClient;
import eu.dissco.digitisers.clients.gbif.GbifClient;
import eu.dissco.digitisers.clients.gbif.GbifInfo;
import eu.dissco.digitisers.utils.DigitalSpecimenUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;

import java.util.HashMap;
import java.util.Map;

public class CatalogueOfLifeEnricher extends Enricher {

    public CatalogueOfLifeEnricher(DigitalObject ds, Configuration config){
        super(ds,config);
    }

    @Override
    protected Map<String, Object> enrichData() {
        Map<String,Object> data=null;
        try{
            String catOfLifeReference = DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"catOfLifeReference");
            if (StringUtils.isBlank(catOfLifeReference)){
                String acceptedScientificName = DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"scientificName");
                String taxonRank = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(),DwcTerm.taxonRank);
                String gbifKingdomTaxonId = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(), GbifTerm.kingdomKey);

                if (StringUtils.isNotBlank(acceptedScientificName) && StringUtils.isNotBlank(taxonRank) && StringUtils.isNotBlank(gbifKingdomTaxonId) ){
                    GbifInfo gbifInfo = GbifInfo.getGbifInfoFromConfig(this.getConfig());
                    GbifClient gbifClient = GbifClient.getInstance(gbifInfo);
                    JsonObject parsedName = gbifClient.parseName(acceptedScientificName);
                    JsonObject kingdomInfo = gbifClient.getTaxonInfoById(gbifKingdomTaxonId);
                    if (parsedName!=null && kingdomInfo!=null) {
                        String canonicalName = parsedName.get("canonicalName").getAsString();
                        String kingdomName = kingdomInfo.get("scientificName").getAsString();

                        CoLClient colClient = CoLClient.getInstance();
                        JsonObject colTaxonInfo = colClient.getTaxonInformation(canonicalName,taxonRank,kingdomName);
                        if (colTaxonInfo!=null){
                            String colURL = colTaxonInfo.get("url").getAsString();
                            data = new HashMap<String, Object>();
                            data.put("catOfLifeReference",colURL);
                            data.put("colContent",colTaxonInfo);
                        } else{
                            this.getLogger().warn("Scientific name not found in CoL " + canonicalName + " rank " + taxonRank + "kingdom" + kingdomName);
                        }
                    } else{
                        this.getLogger().warn("Scientific name couldn't be parsed " + acceptedScientificName + " or kingdom info couldn't be obtained " + gbifKingdomTaxonId);
                    }
                } else{
                    this.getLogger().warn("Not enough information for enriching ds with CoL data " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId"));
                }
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching catalogue of life data for ds " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId") + " . Reason: " + e.getMessage());
        }
        return data;
    }
}
