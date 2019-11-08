package eu.dissco.digitisers.clients.digitalObjectRepository;

import com.google.gson.JsonObject;
import eu.dissco.digitisers.utils.FileUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@Ignore
public class DigitalObjectRepositoryClientTest {

    private final static Logger logger = LoggerFactory.getLogger(DigitalObjectRepositoryClientTest.class);

    private static DigitalObjectRepositoryClient digitalObjectRepositoryClient;

    @BeforeClass
    public static void init() throws Exception {
        Configuration config = FileUtils.loadConfigurationFromResourceFile("config.properties");
        DigitalObjectRepositoryInfo digitalObjectRepositoryInfo =  DigitalObjectRepositoryInfo.getDigitalObjectRepositoryInfoFromConfig(config);
        digitalObjectRepositoryClient = DigitalObjectRepositoryClient.getInstance(digitalObjectRepositoryInfo);
    }

    @AfterClass
    public static void setup() {
        digitalObjectRepositoryClient.close();
    }

    @Test
    public void testDoipServerIsUp() throws DigitalObjectRepositoryException {
        DigitalObject helloResponse = digitalObjectRepositoryClient.hello();
        assertNotNull("Hello response shouldn't be null", helloResponse);

        Double protocolVersion = helloResponse.attributes.get("protocolVersion").getAsDouble();
        assertTrue("Protocol should be >=2.0",protocolVersion>=2.0);
    }


    @Test
    public void testDoipServerListOperations() throws DigitalObjectRepositoryException {
        List<String> operations = digitalObjectRepositoryClient.listOperations();
        operations.forEach(System.out::println);
    }
    

    @Test
    public void testGetAllSchemas() throws DigitalObjectRepositoryException{
        List<DigitalObject> schemas = digitalObjectRepositoryClient.getAllSchemas();
        assertTrue("Number of schemas should be more than 1",schemas.size()>=1);
    }

    @Test
    public void testGetSchemaByName() throws DigitalObjectRepositoryException{
        String schemaName = "DigitalSpecimen";
        DigitalObject dsSchema = digitalObjectRepositoryClient.getSchemaByName("DigitalSpecimen");
        assertNotNull("Schema " + schemaName + " should exist", dsSchema);
    }

    @Test
    public void testGetAllUsers() throws DigitalObjectRepositoryException{
        List<DigitalObject> users = digitalObjectRepositoryClient.getAllUsers();
        //Note: the admin user is special user and it is not returned by the function getAllUsers as it is not a digital object
        assertTrue("Number of users should be more than 1",users.size()>=1);
    }

    @Test
    public void testGetUserByUsername() throws DigitalObjectRepositoryException{
        String username = "francisco";
        DigitalObject user = digitalObjectRepositoryClient.getUserByUsername("francisco");
        assertNotNull("User " + username +"  should exist", user);
    }

    @Test
    public void testGetAllDigitalSpecimens() throws DigitalObjectRepositoryException{
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getAllDigitalSpecimens();
        assertTrue("Number of digital specimens should be more than 1",listDs.size()>=1);
    }

    @Test
    public void testGetAllDigitalObjects() throws DigitalObjectRepositoryException{
        List<DigitalObject> listDO = digitalObjectRepositoryClient.getAllDigitalObjects();
        assertTrue("Number of digital objects should be more than 1",listDO.size()>=1);
    }

    @Test
    public void testRetrieveDigitalSpecimenByDsId() throws DigitalObjectRepositoryException {
        String dsid="20.5000.1025/c4942d87a9f89d8929c1";
        DigitalObject ds = digitalObjectRepositoryClient.retrieve(dsid);

        assertNotNull("Digital specimen shouldn't be null", ds);

        String dsScientificName = ds.attributes.get("content").getAsJsonObject().get("scientificName").getAsString();
        logger.debug("DS ID: " + dsid + " has the scientific name '" + dsScientificName + "'");
        assertEquals("The scientific doesn't match","Achillea pannonica Scheele",dsScientificName);
    }

    @Test
    public void testRetrieveDigitalSpecimenByInstitutionCodeAndPhysicalSpecimenId() throws DigitalObjectRepositoryException {
        String scientificName = "Achillea pannonica Scheele";
        String institutionCode="B";
        String physicalSpecimenId="B 10 0586893";
        DigitalObject ds = digitalObjectRepositoryClient.getDigitalSpecimen(scientificName,institutionCode,physicalSpecimenId);

        assertNotNull("Digital specimen shouldn't be null", ds);

        String dsScientificName = ds.attributes.get("content").getAsJsonObject().get("scientificName").getAsString();
        logger.debug("DS has the scientific name '" + dsScientificName + "'");
        assertEquals("The scientific doesn't match","Achillea pannonica Scheele",dsScientificName);
    }

    @Test
    public void testGetDigitalSpecimensByGbifId() throws DigitalObjectRepositoryException {
        String gbifId="https://www.gbif.org/occurrence/1838967874";
        List<DigitalObject> listDS = digitalObjectRepositoryClient.getDigitalSpecimensByGbifId(gbifId);
        assertTrue("Number of digital specimens found for gbigID " + gbifId + " should be more than 1",listDS.size()>=1);
    }


    @Test
    public void testGetDigitalSpecimensWithProperty() throws DigitalObjectRepositoryException {
        String property = "catOfLifeReference";
        List<DigitalObject> listDS = digitalObjectRepositoryClient.getDigitalSpecimensWithProperty(property);
        assertTrue("Number of digital specimens found with property " + property + " should be more than 1",listDS.size()>=1);
    }


    @Test
    public void testValidateDsAgainstSchema() throws Exception{
        String dsid="20.5000.1025/c4942d87a9f89d8929c1";
        DigitalObject ds = digitalObjectRepositoryClient.retrieve(dsid);

        boolean isValid = digitalObjectRepositoryClient.validateDigitalSpecimenAgainstSchema(ds,true);
        assertTrue("The ds " + ds.id + " should be valid according to the schema" ,isValid);
    }

    @Test
    public void testCreateDigitalSpecimen() throws DigitalObjectRepositoryException{
        DigitalObject dummyDs = createDummyDS();
        DigitalObject creationResult = digitalObjectRepositoryClient.createDigitalSpecimen(dummyDs);
        if (creationResult==null){
            logger.error("Fail to create ds",dummyDs);
        } else{
            logger.info("DS created with id: " + creationResult.attributes.getAsJsonObject("content").get("id").getAsString());
        }

        assertNotNull("Digital specimen should have been created", creationResult);
    }

    @Test
    public void testGetDigitalSpecimensCreatedBy() throws DigitalObjectRepositoryException {
        String username="admin";
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensCreatedBy(username);
        assertTrue("List of ds created by "+ username + " should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensModifiedBy() throws DigitalObjectRepositoryException {
        String username="francisco";
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensModifiedBy(username);
        assertTrue("List of ds modified by "+ username + " should have at least 1 element", listDs.size()>=1);
    }


    @Test
    public void testGetDigitalSpecimensCreatedBetweenDateRange() throws DigitalObjectRepositoryException {
        //LocalDateTime startDatetime=LocalDateTime.parse("2019-10-01T00:00:00.000");
        //LocalDateTime endDatetime=LocalDateTime.parse("2019-10-31T23:59:59.999");
        LocalDateTime startDatetime = LocalDateTime.now().minusDays(7);
        LocalDateTime endDatetime = LocalDateTime.now();

        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");

        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensCreatedBetweenDateRange(startDatetime,endDatetime,zoneId);
        assertTrue("List of ds created in the time period "+ startDatetime + " to "+ endDatetime + " should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensCreatedRecently() throws DigitalObjectRepositoryException {
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensCreatedRecently(7);
        assertTrue("List of ds created in the last 7 days should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensCreatedSince() throws DigitalObjectRepositoryException {
        LocalDateTime datetime=LocalDateTime.parse("2019-10-01T00:00:00.000");
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensCreatedSince(datetime,zoneId);
        assertTrue("List of ds created since "+ datetime + " should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensCreatedUntil() throws DigitalObjectRepositoryException {
        LocalDateTime datetime=LocalDateTime.parse("2019-10-01T00:00:00.000");
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensCreatedUntil(datetime,zoneId);
        assertTrue("List of ds created until "+ datetime + " should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensModifiedBetweenDateRange() throws DigitalObjectRepositoryException {
        //LocalDateTime startDatetime=LocalDateTime.parse("2019-10-01T00:00:00.000");
        //LocalDateTime endDatetime=LocalDateTime.parse("2019-10-31T23:59:59.999");
        LocalDateTime startDatetime = LocalDateTime.now().minusDays(7);
        LocalDateTime endDatetime = LocalDateTime.now();

        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");

        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensModifiedBetweenDateRange(startDatetime,endDatetime,zoneId);
        assertTrue("List of ds modified in the time period "+ startDatetime + " to "+ endDatetime + " should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensModifiedSince() throws DigitalObjectRepositoryException {
        LocalDateTime datetime=LocalDateTime.parse("2019-10-01T00:00:00.000");
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensModifiedSince(datetime,zoneId);
        assertTrue("List of ds modified since "+ datetime + " should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensModifiedUntil() throws DigitalObjectRepositoryException {
        LocalDateTime datetime=LocalDateTime.parse("2019-10-01T00:00:00.000");
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensModifiedUntil(datetime,zoneId);
        assertTrue("List of ds modified until "+ datetime + " should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testGetDigitalSpecimensModifiedRecently() throws DigitalObjectRepositoryException {
        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensModifiedRecently(7);
        assertTrue("List of ds modified in the last 7 days should have at least 1 element", listDs.size()>=1);
    }

    @Test
    public void testDeleteDigitalSpecimensByInstitutionCodeAndScientificName() throws DigitalObjectRepositoryException{
        String institutionCode="test";
        String scientificName="test fran";

        //Delete all digital specimens that institution code is "test" and scientificName is "test fran"
        String query = "+type:DigitalSpecimen +/institutionCode:" + QueryParserBase.escape(institutionCode) + " +/scientificName:" + QueryParserBase.escape(scientificName) ;
        List<DigitalObject> listDs = digitalObjectRepositoryClient.searchAll(query);

        for (DigitalObject ds:listDs) {
            //If deletion fails, it will raise a DigitalObjectRepositoryException
            digitalObjectRepositoryClient.delete(ds.id);
            logger.info("Deleted ds " + ds.id);
        }

        List<DigitalObject> listDsAfterDeletion = digitalObjectRepositoryClient.searchAll(query);
        assertTrue("All dummy digital specimens should have been deleted", listDsAfterDeletion.size()==0);
    }

    @Test
    public void testDeleteDigitalSpecimensByUser() throws DigitalObjectRepositoryException{
        String username="francisco";

        List<DigitalObject> listDs = digitalObjectRepositoryClient.getDigitalSpecimensCreatedBy(username);

        long startTime = System.nanoTime();
        for (DigitalObject ds:listDs) {
            digitalObjectRepositoryClient.delete(ds.id);
            logger.info("Deleted ds " + ds.id);
        }
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        logger.info("Average deletion time per ds " + (timeElapsed/1000000)/listDs.size() + " miliseconds ");

        List<DigitalObject> listDsAfterDeletion = digitalObjectRepositoryClient.getDigitalSpecimensCreatedBy(username);
        assertTrue("All dummy digital specimens should have been deleted", listDsAfterDeletion.size()==0);
    }

    @Test
    public void getVersionsOfObject() throws DigitalObjectRepositoryException{
        String objectID="prov.994/86f7e437faa5a7fce15d";
        List<DigitalObject> listVersions = digitalObjectRepositoryClient.getVersionsOfObject(objectID);
        assertTrue("List of version should be more than 1 ", listVersions.size()>1);
    }

    @Test
    public void getVersionOfObjectAtGivenTime_pastVersion() throws DigitalObjectRepositoryException{
        String objectID="prov.994/86f7e437faa5a7fce15d";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse("2019-11-06 17:01:02",formatter);
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");

        DigitalObject digitalObject = digitalObjectRepositoryClient.getVersionOfObjectAtGivenTime(objectID,dateTime,zoneId);
        assertEquals("The id of the version should be","prov.994/845dbdafcc184cd0ddea",digitalObject.id);
        assertEquals("Content.name should be","c",digitalObject.attributes.getAsJsonObject("content").get("name").getAsString());
    }

    @Test
    public void getVersionOfObjectAtGivenTime_currentVersion() throws DigitalObjectRepositoryException{
        String objectID="prov.994/86f7e437faa5a7fce15d";
        LocalDateTime dateTime = LocalDateTime.now();
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");

        DigitalObject digitalObject = digitalObjectRepositoryClient.getVersionOfObjectAtGivenTime(objectID,dateTime,zoneId);
        assertEquals("The id of the version should be","prov.994/86f7e437faa5a7fce15d",digitalObject.id);
        assertEquals("Content.name should be","d",digitalObject.attributes.getAsJsonObject("content").get("name").getAsString());
    }

    @Test
    public void getVersionObjectAtGivenTime_dateBeforeObjectWasCreated() throws DigitalObjectRepositoryException{
        String objectID="prov.994/86f7e437faa5a7fce15d";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse("2019-11-01 00:00:00",formatter);
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");

        DigitalObject digitalObject = digitalObjectRepositoryClient.getVersionOfObjectAtGivenTime(objectID,dateTime,zoneId);
        assertNull("Object shouldn't exist at that time",digitalObject);
    }

    @Test
    public void getVersionOfObjectAtGivenTime_dateBeforeFirstVersionWasMadeButAfterObjectWasCreated() throws DigitalObjectRepositoryException{
        //A version of the object is created just before modifying the object, so it is still possible to try to
        //get the status of the object after it was created but before is was modified for the first time
        String objectID="prov.994/86f7e437faa5a7fce15d";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse("2019-11-06 16:08:35",formatter);
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/London");

        DigitalObject digitalObject = digitalObjectRepositoryClient.getVersionOfObjectAtGivenTime(objectID,dateTime,zoneId);
        assertEquals("The id of the version should be","prov.994/586023191257543c91d6",digitalObject.id);
        assertEquals("Content.name should be","a",digitalObject.attributes.getAsJsonObject("content").get("name").getAsString());
    }

    @Test
    public void publishVersion() throws DigitalObjectRepositoryException{
        String objectID="prov.994/86f7e437faa5a7fce15d";
        DigitalObject version = digitalObjectRepositoryClient.publishVersion(objectID);
        assertEquals("The version should have been created correctly",objectID,version.attributes.getAsJsonObject("metadata").get("versionOf").getAsString());
    }

    private DigitalObject createDummyDS(){
        JsonObject dsContent = new JsonObject();
        dsContent.addProperty("midslevel",1);
        dsContent.addProperty("scientificName","test fran");
        dsContent.addProperty("institutionCode","test");
        dsContent.addProperty("physicalSpecimenId", UUID.randomUUID().toString());

        DigitalObject ds = new DigitalObject();
        ds.type = "DigitalSpecimen";
        ds.setAttribute("content", dsContent);

        return ds;
    }

}