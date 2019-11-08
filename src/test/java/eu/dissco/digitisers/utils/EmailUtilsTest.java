package eu.dissco.digitisers.utils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class EmailUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(EmailUtilsTest.class);

    @Test
    public void sendResultDigitiserExecution() {
        logger.info("message 1 shouldn't be returned in the filter");
        LocalDateTime startDateTime = LocalDateTime.now();
        logger.info("message 2 should be returned in the filter");
        logger.info("message 3 should be returned in the filter");
        LocalDateTime endDateTime= LocalDateTime.now();
        logger.info("message 4 shouldn't be returned in the filter");

        List<String> emailAddresses = Arrays.asList("quevedofernandezf@cardiff.ac.uk");
        boolean emailSent = EmailUtils.sendResultDigitiserExecution(startDateTime,endDateTime,emailAddresses);
        assertTrue("The email should have been sent correctly",emailSent);
    }

    @Test
    public void sendEmail() {
        List<String> emailAddresses = Arrays.asList("quevedofernandezf@cardiff.ac.uk");
        String subject = "This is a test";
        String body = "Hi, this is a test of sending emails through DiSSCo Digitiser";
        boolean emailSent = EmailUtils.sendEmail(emailAddresses,subject,body,null);
        assertTrue("The email should have been sent correctly",emailSent);
    }
}