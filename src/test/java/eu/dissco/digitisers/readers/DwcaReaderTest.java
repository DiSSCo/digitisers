package eu.dissco.digitisers.readers;

import com.google.common.io.Resources;
import eu.dissco.digitisers.clients.col.CoLClient;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryInfo;
import eu.dissco.digitisers.clients.digitalObjectRepository.DigitalObjectRepositoryClient;
import eu.dissco.digitisers.tasks.DigitalObjectProcessor;
import eu.dissco.digitisers.tasks.DigitalObjectVisitor;
import eu.dissco.digitisers.utils.FileUtils;
import net.dona.doip.client.DigitalObject;
import org.apache.commons.configuration2.Configuration;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class DwcaReaderTest {

    private final static Logger logger = LoggerFactory.getLogger(DwcaReaderTest.class);

    private static DigitalObjectVisitor digitalObjectVisitor;

    private long testStartTime,testEndTime;
    private int numRecords;

    @BeforeClass
    public static void setup() throws Exception {
        digitalObjectVisitor = new DigitalObjectVisitor() {
            private final Logger logger = LoggerFactory.getLogger(this.getClass());
            @Override
            public DigitalObject visitDigitalSpecimen(DigitalObject ds) {
                logger.debug("Digital Specimen read: " +  ds);
                return ds;
            }

            @Override
            public void close() {

            }
        };
    }

    @AfterClass
    public static void tearDown() {
        digitalObjectVisitor.close();
    }


    @Before
    public void init() {
        this.testStartTime = System.nanoTime();
    }

    @After
    public void finalize() {
        this.testEndTime = System.nanoTime();
        long timeElapsed = this.testEndTime - this.testStartTime;
        logger.info("Average time per record " + (timeElapsed/1000000)/this.numRecords + " miliseconds ");
    }

    @Test
    public void testParseContentDwcaToDigitalSpecimens_small() throws Exception {
        String dwcaFilePath = "GBIF_DwC-a/small/0022116-190918142434337_Profundiconus_profundorum.zip";
        File dwcaFile = new File(Resources.getResource(dwcaFilePath).toURI());

        DwcaReader dwcaReader = new DwcaReader(digitalObjectVisitor);
        this.numRecords = dwcaReader.readDigitalSpecimensFromDwcaFile(dwcaFile);
        assertEquals("The digital specimens parsed should be",39,this.numRecords);
    }

    @Test
    public void testParseContentDwcaToDigitalSpecimens_big() throws Exception {
        String dwcaFilePath = "GBIF_DwC-a/big/0029199-190918142434337_Canis_lupus.zip";
        File dwcaFile = new File(Resources.getResource(dwcaFilePath).toURI());

        DwcaReader dwcaReader = new DwcaReader(digitalObjectVisitor);
        this.numRecords = dwcaReader.readDigitalSpecimensFromDwcaFile(dwcaFile);
        assertEquals("The digital specimens parsed should be",23254,this.numRecords);
    }
}