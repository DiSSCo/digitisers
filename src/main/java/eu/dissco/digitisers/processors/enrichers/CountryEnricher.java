package eu.dissco.digitisers.processors.enrichers;

import eu.dissco.digitisers.clients.misc.CountryClient;
import eu.dissco.digitisers.utils.DigitalSpecimenUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class CountryEnricher extends Enricher {

    public CountryEnricher(DigitalObject ds, Configuration config){
        super(ds,config);
    }

    @Override
    protected Map<String, Object> enrichData() {
        Map<String,Object> data=null;
        try{
            String country = DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"country");
            if (StringUtils.isBlank(country)){
                String countryCode = DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"countryCode");
                if (StringUtils.isNotBlank(countryCode)){
                    CountryClient countryClient = CountryClient.getInstance();
                    country = countryClient.getCountryNameByCountryCode(countryCode);
                    if (country!=null){
                        data = new HashMap<String, Object>();
                        data.put("country",country);
                    } else{
                        this.getLogger().info("Country could't be resolved " + countryCode);
                    }
                } else{
                    this.getLogger().info("Not enough information for enriching ds with country data " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId"));
                }
            }
        } catch (Exception e){
            this.getLogger().error("Error enriching country data for ds " + DigitalSpecimenUtils.getStringPropertyFromDS(this.getDs(),"physicalSpecimenId") + " . Reason: " + e.getMessage());
        }
        return data;
    }
}
