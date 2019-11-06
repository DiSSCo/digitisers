package eu.dissco.digitisers;

import com.google.common.io.Resources;
import net.dona.doip.client.DigitalObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

import static org.junit.Assert.*;

@Ignore
public class DwcaDigitiserTest {

    private final static Logger logger = LoggerFactory.getLogger(DwcaDigitiserTest.class);

    private static DwcaDigitiser digitiser;

    @BeforeClass
    public static void init() throws Exception {
        String configPropertiesFilePath = Resources.getResource("config.properties").getPath();
        digitiser = new DwcaDigitiser(configPropertiesFilePath);
    }

    @AfterClass
    public static void setup() {
        digitiser.getDigitalObjectRepositoryClient().close();
    }


    @Test
    public void testImportDwcaFileToDigitalSpecimens_file_small() throws Exception{
        String dwcaFilePath = Resources.getResource("GBIF_DwC-a/small/0022116-190918142434337_Profundiconus_profundorum.zip").getPath();

        long startTime = System.nanoTime();
        List<DigitalObject> listDsSaved = digitiser.digitiseDigitalSpecimensFromDwcaFile(dwcaFilePath);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        logger.info("Average importation time per ds " + (timeElapsed/1000000)/listDsSaved.size() + " miliseconds ");

        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }

    @Test
    public void testImportDwcaFileToDigitalSpecimens_file_big() throws Exception{
        String dwcaFilePath = Resources.getResource("GBIF_DwC-a/big/0029199-190918142434337_Canis_lupus.zip").getPath();

        long startTime = System.nanoTime();
        List<DigitalObject> listDsSaved = digitiser.digitiseDigitalSpecimensFromDwcaFile(dwcaFilePath);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        logger.info("Average importation time per ds " + (timeElapsed/1000000)/listDsSaved.size() + " miliseconds ");

        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }

    @Test
    public void testImportDwcaFileToDigitalSpecimens_folder() throws Exception{
        String folderPath = Resources.getResource("GBIF_DwC-a/small").getPath();

        long startTime = System.nanoTime();
        List<DigitalObject> listDsSaved = digitiser.digitiseDigitalSpecimensFromFolder(folderPath);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        logger.info("Average importation time per ds " + (timeElapsed/1000000)/listDsSaved.size() + " miliseconds ");

        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }

    @Test
    public void testImportDwcaFileToDigitalSpecimens_url() throws Exception{
        String sDwcaURL = "https://www.gbif.org/occurrence/download/0032443-190918142434337";

        long startTime = System.nanoTime();
        List<DigitalObject> listDsSaved = digitiser.digitiseDigitalSpecimensFromUrl(sDwcaURL);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        logger.info("Average importation time per ds " + (timeElapsed/1000000)/listDsSaved.size() + " miliseconds ");

        assertNotNull("The importation should completed without raising any unhandled exception",listDsSaved);
    }

}
