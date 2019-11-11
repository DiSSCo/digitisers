package eu.dissco.digitisers.clients.ebi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import eu.dissco.digitisers.clients.ebi.openapi.ApiException;
import eu.dissco.digitisers.clients.ebi.openapi.model.WSEntry;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

public class EbiClientTest {

    private static final Logger logger = LoggerFactory.getLogger(EbiClientTest.class);

    private static EbiClient ebiClient;

    @BeforeClass
    public static void setup() throws Exception {
        ebiClient = EbiClient.getInstance();
    }

    @AfterClass
    public static void tearDown() {

    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void rootSearchSpecimen() throws ApiException {
        String specimenKey="MNHN IM 2013-7767";
        JsonArray ebiResults = ebiClient.rootSearchAsJson(specimenKey,true);

        assertTrue("The number of results should be greater or equals than 1 ", ebiResults.size()>=1);

        Gson gson = new Gson();
        logger.debug(gson.toJson(ebiResults));
    }

    @Test
    public void intermediateDomainSearchSpecimen() throws ApiException {
        String domain="nucleotideSequences";
        String specimenKey="MNHN IM 2013-7767";
        JsonArray ebiResults = ebiClient.domainSearchAsJson(domain,specimenKey,true);

        assertTrue("The number of results should be greater or equals than 1 ", ebiResults.size()>=1);

        Gson gson = new Gson();
        logger.info(gson.toJson(ebiResults));
    }

    @Test
    public void fakeDomainSearchSpecimen() throws ApiException {
        exceptionRule.expect(ApiException.class);
        exceptionRule.expectMessage("Invalid domain parameter");

        String domain="fake";
        String specimenKey="MNHN IM 2013-7767";
        JsonArray ebiResults = ebiClient.domainSearchAsJson(domain,specimenKey,true);
    }

    @Test
    public void intermediateDomainSearchFakeSpecimen() throws ApiException {
        String domain="nucleotideSequences";
        String specimenKey="non-exist-search-term-123";
        JsonArray ebiResults = ebiClient.domainSearchAsJson(domain,specimenKey,true);

        assertEquals("The number of results should be 0 ", 0,ebiResults.size());

        Gson gson = new Gson();
        logger.debug(gson.toJson(ebiResults));
    }

    @Test
    public void leafDomainSearchSpecimen_freeText() throws ApiException {
        String domain="emblrelease_standard";
        String specimenKey="MNHN IM 2013-7767";
        JsonArray ebiResults = ebiClient.domainSearchAsJson(domain,specimenKey,true);

        assertTrue("The number of results should be greater or equals than 1 ", ebiResults.size()>=1);

        Gson gson = new Gson();
        logger.debug(gson.toJson(ebiResults));
    }

    @Test
    public void leafDomainSearchSpecimen_specificField_found() throws ApiException {
        String domain="emblrelease_standard";
        String specimenKey="description:\"MNHN IM 2013-7767\" AND TAXON:1504874 AND INTERPRO:IPR036927 AND INTERPRO:IPR023616";
        JsonArray ebiResults = ebiClient.domainSearchAsJson(domain,specimenKey);

        assertTrue("The number of results should be greater or equals than 1 ", ebiResults.size()>=1);

        Gson gson = new Gson();
        logger.debug(gson.toJson(ebiResults));
    }

    @Test
    public void leafDomainSearchSpecimen_specificField_notFound() throws ApiException {
        String domain="emblrelease_standard";
        String specimenKey="BIOSAMPLE:\"MNHN IM 2013-7767\"";
        JsonArray ebiResults = ebiClient.domainSearchAsJson(domain,specimenKey);

        assertEquals("The number of results should be 0 ", 0,ebiResults.size());

        Gson gson = new Gson();
        logger.debug(gson.toJson(ebiResults));
    }

}