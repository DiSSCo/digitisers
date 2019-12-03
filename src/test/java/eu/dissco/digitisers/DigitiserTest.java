package eu.dissco.digitisers;

import com.google.common.io.Resources;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Ignore
public class DigitiserTest {

    private final static Logger logger = LoggerFactory.getLogger(DwcaDigitiserTest.class);

    @BeforeClass
    public static void setup() throws Exception {
    }

    @AfterClass
    public static void tearDown() {
    }

    @Before
    public void init() {
    }

    @After
    public void finalize() {
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testStartDigitisationDwcaByFile_with_CoL_EBI_Wikidata_Info() throws Exception{
        String digitiserMethod="dwca";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String dwcaFilePath = "GBIF_DwC-a/small/0034622-190918142434337_Pygmaepterys_pointieri.zip";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-f", Resources.getResource(dwcaFilePath).getPath()
        ));
        Digitiser digitiser = DigitiserFactory.getDigitiser(commandLineArgs);
        digitiser.digitise(commandLineArgs);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testStartDigitisationDwcaByFile_big() throws Exception{
        String digitiserMethod="dwca";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String dwcaFilePath = "GBIF_DwC-a/big/0029199-190918142434337_Canis_lupus.zip";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-f", Resources.getResource(dwcaFilePath).getPath()
        ));
        Digitiser digitiser = DigitiserFactory.getDigitiser(commandLineArgs);
        digitiser.digitise(commandLineArgs);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testStartDigitisationDwcaByFolder() throws Exception{
        String digitiserMethod="dwca";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String folderPath = "GBIF_DwC-a/small";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-d", Resources.getResource(folderPath).getPath()
        ));
        Digitiser digitiser = DigitiserFactory.getDigitiser(commandLineArgs);
        digitiser.digitise(commandLineArgs);
    }


    @Test(expected = Test.None.class /* no exception expected */)
    public void testStartDigitisationDwcaByUrl() throws Exception{
        String digitiserMethod="dwca";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String dwcaUrl = "http://api.gbif.org/v1/occurrence/download/request/0031773-190918142434337.zip";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-u", dwcaUrl
        ));
        Digitiser digitiser = DigitiserFactory.getDigitiser(commandLineArgs);
        digitiser.digitise(commandLineArgs);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testStartDigitisationGbifBySpeciesName() throws Exception{
        String digitiserMethod="gbif";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String scientificName = "Agathis montana";
        String kingdom = "Plantae";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-n", scientificName,
                "-k", kingdom
        ));
        Digitiser digitiser = DigitiserFactory.getDigitiser(commandLineArgs);
        digitiser.digitise(commandLineArgs);
    }


}