package eu.dissco.digitisers.processors.enrichers;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import eu.dissco.digitisers.clients.ebi.EbiClient;
import eu.dissco.digitisers.utils.DigitalSpecimenUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.terms.DwcTerm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EbiEnricher extends Enricher {

    public EbiEnricher(DigitalObject ds, Configuration config){
        super(ds,config);
    }

    @Override
    protected Map<String, Object> enrichData() {
        Map<String,Object> data=null;
        try{
            JsonElement ebiSearchResults = DigitalSpecimenUtils.getPropertyFromDS(this.getDs(),"ebiSearchResults");
            if (ebiSearchResults==null){
                EbiClient ebiClient = EbiClient.getInstance();
                JsonArray ebiResults = new JsonArray();

                JsonElement bh2020MappingsObj = DigitalSpecimenUtils.getPropertyFromDS(this.getDs(),"bh2020Mappings");
                if (bh2020MappingsObj!=null) {
                    //Mappings with ENA samples are provided in DS object, so we query EBI to obtain directly information of those accession Ids
                    //This can be the case when we use the tool built during the BioHackathon 2020 in the project 33
                    JsonArray enaIdsObj = bh2020MappingsObj.getAsJsonObject().get("enaIds").getAsJsonArray();
                    for (JsonElement enaIdObj : enaIdsObj) {
                        String domain = "nucleotideSequences";
                        String query = "acc:" + enaIdObj.getAsString();
                        this.getLogger().info("Enriching ds physicalSpecimenId=" + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(), "physicalSpecimenId") + " with EBI data " + query);
                        ebiResults.addAll(ebiClient.domainSearchAsJson(domain,query));
                    }
                    data = new HashMap<String, Object>();
                    data.put("ebiSearchResults", ebiResults);
                    DigitalSpecimenUtils.removePropertyFromDS(this.getDs(),"bh2020Mappings");
                } else{
                    //Mappings are not provided, so we will try to perform a search for entries related to the given specimen using
                    //the triplet institutionCode:collectionCode:catalogNumber
                    String institutionCode = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(), DwcTerm.institutionCode);
                    String collectionCode = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(),DwcTerm.collectionCode);
                    String catalogNumber = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(), DwcTerm.catalogNumber);
                    if (StringUtils.isNotBlank(institutionCode) && StringUtils.isNotBlank(collectionCode) && StringUtils.isNotBlank(catalogNumber)) {
                        String searchTermWithSpaces = String.join(" ", Arrays.asList(institutionCode, collectionCode, catalogNumber));
                        String searchTermWithColons = String.join(":", Arrays.asList(institutionCode, collectionCode, catalogNumber));
                        ebiResults.addAll(ebiClient.rootSearchAsJson(searchTermWithSpaces, true));
                        ebiResults.addAll(ebiClient.rootSearchAsJson(searchTermWithColons, true));
                        data = new HashMap<String, Object>();
                        data.put("ebiSearchResults", ebiResults);
                    } else {
                        this.getLogger().info("Not enough information for enriching ds with EBI data " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(), "physicalSpecimenId"));
                    }
                }
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching EBI data for ds " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId") + " . Reason: " + e.getMessage());
        }
        return data;
    }
}
