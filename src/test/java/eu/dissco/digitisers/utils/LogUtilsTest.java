package eu.dissco.digitisers.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LogUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(JsonUtilsTest.class);

    @Test
    public void getLogEntriesBetweenDateRange() throws IOException {
        logger.info("message 1 shouldn't be returned in the filter");
        LocalDateTime startDateTime = LocalDateTime.now();
        logger.info("message 2 should be returned in the filter");
        logger.info("message 3 should be returned in the filter");
        LocalDateTime endDateTime= LocalDateTime.now();
        logger.info("message 4 shouldn't be returned in the filter");

        List<String> logEntries = LogUtils.getLogEntriesBetweenDateRange(startDateTime,endDateTime);
        assertEquals("Number of log entries should be 3", 3, logEntries.size());
    }
}