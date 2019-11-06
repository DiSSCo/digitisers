package eu.dissco.digitisers;

import com.google.common.io.Resources;
import net.dona.doip.client.DigitalObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Ignore
public class DiscoDigitiserTest {

    private final static Logger logger = LoggerFactory.getLogger(DwcaDigitiserTest.class);

    private static DiscoDigitiser digitiser;

    @BeforeClass
    public static void init() throws Exception {
        digitiser=null;
    }

    @AfterClass
    public static void setup() {
        if (digitiser!=null){
            digitiser.getDigitalObjectRepositoryClient().close();
        }
    }

    @Test
    public void testStartDigitisationDwcaByFile_with_CoL_EBI_Wikidata_Info() throws Exception{
        String digitiserMethod="dwca";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String dwcaFilePath = "GBIF_DwC-a/small/0034622-190918142434337_Pygmaepterys_pointieri.zip";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-f", Resources.getResource(dwcaFilePath).getPath()
        ));
        digitiser = DiscoDigitiser.getDigitiser(commandLineArgs);
        List<DigitalObject> listDsSaved = digitiser.startDigitisation(commandLineArgs);
        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }

    @Test
    public void testStartDigitisationDwcaByFolder() throws Exception{
        String digitiserMethod="dwca";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String folderPath = "GBIF_DwC-a/";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-d", Resources.getResource(folderPath).getPath()
        ));
        digitiser = DiscoDigitiser.getDigitiser(commandLineArgs);
        List<DigitalObject> listDsSaved = digitiser.startDigitisation(commandLineArgs);
        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }


    @Test
    public void testStartDigitisationDwcaByUrl() throws Exception{
        String digitiserMethod="dwca";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String dwcaUrl = "http://api.gbif.org/v1/occurrence/download/request/0031773-190918142434337.zip";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-u", dwcaUrl
        ));
        digitiser = DiscoDigitiser.getDigitiser(commandLineArgs);
        List<DigitalObject> listDsSaved = digitiser.startDigitisation(commandLineArgs);
        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }

    @Test
    public void testStartDigitisationGbifBySpeciesName() throws Exception{
        String digitiserMethod="gbif";
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        String scientificName = "Agathis montana";
        String kindom = "Plantae";
        List<String> commandLineArgs = new ArrayList<String>(Arrays.asList(
                "-m", digitiserMethod,
                "-c", configPropertiesFilePath,
                "-n", scientificName,
                "-k", kindom
        ));
        digitiser = DiscoDigitiser.getDigitiser(commandLineArgs);
        List<DigitalObject> listDsSaved = digitiser.startDigitisation(commandLineArgs);
        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }


}