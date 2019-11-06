package eu.dissco.digitisers.dwca;

import com.google.common.io.Resources;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryInfo;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryClient;
import eu.dissco.digitisers.utils.FileUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class DwcaReaderTest {

    private final static Logger logger = LoggerFactory.getLogger(DwcaReaderTest.class);

    @Test
    public void testParseContentDwcaToDigitalSpecimens_small() throws Exception {
        String dwcaFilePath = "GBIF_DwC-a/small/0022116-190918142434337_Profundiconus_profundorum.zip";
        File dwcaFile = new File(Resources.getResource(dwcaFilePath).toURI());

        DwcaReader dwcaReader = new DwcaReader();
        List<DigitalObject> listDs = dwcaReader.parseContentDwcaToDigitalSpecimens(dwcaFile);
        assertNotNull("List of digital specimens shouldn't be null", listDs);
        assertTrue("The list of digital specimens  should contains 39 elements",listDs.size()==39);
    }

    @Test
    public void testParseContentDwcaToDigitalSpecimens_big() throws Exception {
        String dwcaFilePath = "GBIF_DwC-a/big/0029199-190918142434337_Canis_lupus.zip";
        File dwcaFile = new File(Resources.getResource(dwcaFilePath).toURI());

        DwcaReader dwcaReader = new DwcaReader();
        List<DigitalObject> listDs = dwcaReader.parseContentDwcaToDigitalSpecimens(dwcaFile);
        assertNotNull("List of digital specimens shouldn't be null", listDs);
        assertTrue("The list of digital specimens  should contains 39 elements",listDs.size()==39);
    }

    @Test
    public void testValidateDigitalSpecimensFromDwcaAgainstSchema() throws Exception{
        String dwcaFilePath = "GBIF_DwC-a/small/0022116-190918142434337_Profundiconus_profundorum.zip";
        File dwcaFile = new File(Resources.getResource(dwcaFilePath).toURI());

        //Parse data from dwca-file
        DwcaReader dwcaReader = new DwcaReader();
        List<DigitalObject> listDs = dwcaReader.parseContentDwcaToDigitalSpecimens(dwcaFile);

        //Validate the digital specimens against current schema
        Configuration config = FileUtils.loadConfigurationFromResourceFile("config.properties");
        DigitalObjectRepositoryInfo digitalObjectRepositoryInfo =  DigitalObjectRepositoryInfo.getDigitalObjectRepositoryInfoFromConfig(config);
        try (DigitalObjectRepositoryClient digitalObjectRepositoryClient = DigitalObjectRepositoryClient.getInstance(digitalObjectRepositoryInfo)){
            for (DigitalObject ds: listDs) {
                logger.info("Validating jsonDS gbifId:" +  ds.attributes.getAsJsonObject("content").get("gbifId").getAsString());
                boolean isValid = digitalObjectRepositoryClient.validateDigitalSpecimenAgainstSchema(ds,false);
                assertTrue("The jsonDS gbifId " + ds.attributes.getAsJsonObject("content").get("gbifId").getAsString() + " should be valid according to the schema" ,isValid);
            }
        }
    }
}