package eu.dissco.digitisers.utils;

import com.google.common.io.Resources;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Ignore("Tests are ignored as they need setting up the configuration file")
public class EmailUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(EmailUtilsTest.class);

    @Test
    public void sendResultDigitiserExecution() throws ConfigurationException {
        logger.info("message 1 shouldn't be returned in the filter");
        LocalDateTime startDateTime = LocalDateTime.now();
        logger.info("message 2 should be returned in the filter");
        logger.info("message 3 should be returned in the filter");
        LocalDateTime endDateTime= LocalDateTime.now();
        logger.info("message 4 shouldn't be returned in the filter");

        Configuration config = FileUtils.loadConfigurationFromFilePath(Resources.getResource("config.properties").getPath());
        List<String> emailAddresses = config.getList(String.class,"digitiser.sendDigitisationResultsByEmailTo");
        EmailUtils emailUtils = new EmailUtils(config);
        boolean emailSent = emailUtils.sendResultDigitiserExecution(startDateTime,endDateTime,emailAddresses);
        assertTrue("The email should have been sent correctly",emailSent);
    }

    @Test
    public void sendEmail() throws ConfigurationException {
        Configuration config = FileUtils.loadConfigurationFromFilePath(Resources.getResource("config.properties").getPath());
        List<String> emailAddresses = config.getList(String.class,"digitiser.sendDigitisationResultsByEmailTo");
        String subject = "This is a test";
        String body = "Hi, this is a test of sending emails through DiSSCo Digitiser";
        EmailUtils emailUtils = new EmailUtils(config);
        boolean emailSent = emailUtils.sendEmail(emailAddresses,subject,body,null);
        assertTrue("The email should have been sent correctly",emailSent);
    }
}