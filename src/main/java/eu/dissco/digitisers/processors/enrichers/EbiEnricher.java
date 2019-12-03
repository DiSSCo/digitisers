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
            String institutionCode = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(), DwcTerm.institutionCode);
            String collectionCode = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(),DwcTerm.collectionCode);
            String catalogNumber = DigitalSpecimenUtils.getTermFromDsDwcaJson(this.getDs(), DwcTerm.catalogNumber);
            JsonElement ebiSearchResults =  DigitalSpecimenUtils.getPropertyFromDS(this.getDs(),"ebiSearchResults");
            if (ebiSearchResults==null){
                if (StringUtils.isNotBlank(institutionCode) && StringUtils.isNotBlank(collectionCode) && StringUtils.isNotBlank(catalogNumber)){
                    EbiClient ebiClient = EbiClient.getInstance();
                    String searchTermWithSpaces = String.join(" ", Arrays.asList(institutionCode,collectionCode,catalogNumber));
                    String searchTermWithColons = String.join(" ", Arrays.asList(institutionCode,collectionCode,catalogNumber));
                    JsonArray ebiResults = ebiClient.rootSearchAsJson(searchTermWithSpaces,true);
                    ebiResults.addAll(ebiClient.rootSearchAsJson(searchTermWithColons,true));
                    data = new HashMap<String, Object>();
                    data.put("ebiSearchResults",ebiResults);
                } else{
                    this.getLogger().info("Not enough information for enriching ds with EBI data " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId"));
                }
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching EBI data for ds " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId") + " . Reason: " + e.getMessage());
        }
        return data;
    }
}
