package eu.dissco.digitisers.clients.gbif;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.dissco.digitisers.utils.FileUtils;
import org.apache.commons.configuration2.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.*;

public class GbifClientTest {

    private static final Logger logger = LoggerFactory.getLogger(GbifClientTest.class);

    private static GbifClient gbifClient;

    @BeforeClass
    public static void init() throws Exception {
        Configuration config = FileUtils.loadConfigurationFromResourceFile("config.properties");
        GbifInfo gbifInfo =  GbifInfo.getGbifInfoFromConfig(config);
        gbifClient = GbifClient.getInstance(gbifInfo);
    }

    @AfterClass
    public static void setup() {

    }

    @Test
    public void getTaxonInfoById_valid() throws Exception {
        String taxonId = "2685009";
        JsonObject taxonInfo = gbifClient.getTaxonInfoById(taxonId);
        assertEquals("The species name should be ", "Agathis montana de Laub.",taxonInfo.get("scientificName").getAsString());
    }

    @Test
    public void getTaxonInfoById_invalid() throws Exception {
        String taxonId = "-1";
        JsonObject taxonInfo = gbifClient.getTaxonInfoById(taxonId);
        assertNull("The taxon shouldn't exist",taxonInfo);
    }

    @Test
    public void parseName() throws Exception {
        String scientificName = "Agathis montana de Laub.";
        JsonObject parsedName = gbifClient.parseName(scientificName);
        assertEquals("The species canonical name should be ", "Agathis montana",parsedName.get("canonicalName").getAsString());
    }


    @Test
    public void getTaxonIdByCanonicalNameAndKingdom_valid() throws Exception {
        String canonicalName = "Agathis montana";
        String kingdom = "Plantae";
        String taxonId = gbifClient.getTaxonIdByCanonicalNameAndKingdom(canonicalName,kingdom);
        assertEquals("The gbif taxonId should be ", "2685009",taxonId);
    }

    @Test
    public void getTaxonIdByCanonicalNameAndKingdom_invalid() throws Exception {
        String canonicalName = "Fran test";
        String kingdom = "Plantae";
        String taxonId = gbifClient.getTaxonIdByCanonicalNameAndKingdom(canonicalName,kingdom);
        assertNull("The taxon shouldn't exist ", taxonId);
    }

    @Test
    public void downloadOccurrencesByCanonicalNameAndKingdom() throws Exception {
        String canonicalName = "Agathis montana";
        String kingdom = "Plantae";
        File dwcaFile = gbifClient.downloadOccurrencesByCanonicalNameAndKingdom(canonicalName,kingdom);
        assertNotNull("The file should be downloaded correctly ", dwcaFile);
    }

    @Test
    public void downloadOccurrencesByTaxonId() throws Exception {
        String taxonId = "2685009";
        File dwcaFile = gbifClient.downloadOccurrencesByTaxonId(taxonId);
        assertNotNull("The file should be downloaded correctly ", dwcaFile);
    }

    @Test
    public void getInstitutionInfoByInstitutionCode_found() throws Exception {
        String institutionCode="NMWC";
        JsonArray institutions = gbifClient.getInstitutionsInfoByInstitutionCode(institutionCode);
        assertEquals("Only 1 institutions should be found with code " + institutionCode, 1,institutions.size());
        assertEquals("The institution id should be ", "4ee97b8b-e686-4f3e-8d8d-642e6fcc3b6f",institutions.get(0).getAsJsonObject().get("key").getAsString());
    }


    @Test
    public void getInstitutionInfoByInstitutionCode_2_found() throws Exception {
        String institutionCode="MNHN";
        JsonArray institutions = gbifClient.getInstitutionsInfoByInstitutionCode(institutionCode);
        assertEquals("2 institutions should be found with code " + institutionCode, 2,institutions.size());

        Gson gson = new Gson();
        logger.debug(gson.toJson(institutions));
    }

    @Test
    public void getInstitutionInfoByInstitutionCode_notFound() throws Exception {
        String institutionCode="TEST_INSTITUTION_NOT_FOUND";
        JsonArray institutions = gbifClient.getInstitutionsInfoByInstitutionCode(institutionCode);
        assertEquals("No institution should be found " + institutionCode, 0,institutions.size());
    }


    @Test
    public void getInstitutionsInfoByInstitutionId() throws Exception {
        String institutionId="4ee97b8b-e686-4f3e-8d8d-642e6fcc3b6f";
        JsonObject institutionInfo = gbifClient.getInstitutionInfoByInstitutionId(institutionId);
        assertEquals("The institution code should be ", "NMWC",institutionInfo.get("code").getAsString());
        assertEquals("The institution country region should be ", "Europe",institutionInfo.getAsJsonObject("country").get("region").getAsString());
    }
}