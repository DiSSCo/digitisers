package eu.dissco.digitisers.utils;

import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.email.EmailPopulatingBuilder;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.util.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.FileDataSource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmailUtils {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final static Logger logger = LoggerFactory.getLogger(LogUtils.class);


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Send an email with the result of the digitisation, by attaching the entries in the log file between startDateTime
     * and endDateTime, to the email address passed as parameter
     * @param startDateTime start date time from which to obtain the log entries
     * @param endDateTime end date time from which to obtain the log entries
     * @param emailAddresses List of email addresses to send the email
     * @return true if the email was sent correctly, false otherwise
     */
    public static boolean sendResultDigitiserExecution(LocalDateTime startDateTime, LocalDateTime endDateTime, List<String> emailAddresses) {
        boolean emailSent = false;
        try {
            if (emailAddresses!=null && emailAddresses.size()>0) {
                //Get log entries
                List<String> filteredLogEntries = LogUtils.getLogEntriesBetweenDateRange(startDateTime, endDateTime);

                //Save filtered log entries into a temp file
                File tempFilterLogFile = new File(com.google.common.io.Files.createTempDir(), "log.txt");
                org.apache.commons.io.FileUtils.writeLines(tempFilterLogFile, "UTF-8", filteredLogEntries);

                File tempZipFile = new File(com.google.common.io.Files.createTempDir(), "log.zip");
                FileUtils.zipFile(tempFilterLogFile,tempZipFile);

                String message;
                List<File> attachments = null;
                long zipFileSizeInMb = tempZipFile.length() / (1024 * 1024);
                if (zipFileSizeInMb>200){
                    message = "The results of the digitisation process is to big to be sent by email. Please ask the DiSCCo Digitiser Admin to send it for you.";
                } else {
                    message= "Please see attached the log entries of its execution.";
                    attachments = Arrays.asList(tempZipFile);
                }

                //Prepare email
                String emailSubject = "Result of digitisation";
                String emailBody = "Dear DiSSCo user,\n\n " +
                        "The digitisation has finished. "+ message + "\n\n" +
                        "Kind regards,\n"+
                        "DiSSCo.eu";

                //Send email
                emailSent = EmailUtils.sendEmail(emailAddresses, emailSubject, emailBody, attachments);
                if (!emailSent) logger.warn("Email was not sent");
            }
        } catch (Exception e) {
            logger.error("Error sending result digitiser execution by email" + e.getMessage(),e);
        }
        return emailSent;
    }

    /**
     * Function that sends an email to the list of email addresses received as parameter
     * @param emailAddresses list of email addresses to send the email to
     * @param subject subject of the email
     * @param body body of the email
     * @param attachedFiles attached files
     * @return true if the email was sent correctly, false otherwise
     */
    public static boolean sendEmail(List<String> emailAddresses, String subject, String body, List<File> attachedFiles){
        boolean emailSent = false;
        try{
            if (emailAddresses!=null && emailAddresses.size()>0){
                //Load mail config from properties files
                ConfigLoader.loadProperties("simplejavamail.properties",true);

                //Get mailer (currently using properties loaded by ConfigLoader)
                Mailer mailer = MailerBuilder
                        .buildMailer();

                //Prepare email
                EmailPopulatingBuilder currentEmailBuilder = EmailBuilder.startingBlank();
                for (String emailAddress:emailAddresses) {
                    currentEmailBuilder.to( emailAddress);
                }
                currentEmailBuilder.withSubject(subject);
                currentEmailBuilder.withPlainText(body);

                //Add attachment if any
                if (attachedFiles!=null && attachedFiles.size()>0){
                    for (File attachedFile:attachedFiles) {
                        currentEmailBuilder.withAttachment(attachedFile.getName(),new FileDataSource(attachedFile.getAbsolutePath()));
                    }
                }

                //Create email
                Email email = currentEmailBuilder.buildEmail();

                //Send email. Note: mailer fails to send to non CU email addresses when not authenticate
                mailer.sendMail(email);

                emailSent = true;
            }
        } catch (Exception e){
            logger.error("Error sending email " + e.getMessage(),e);
        }
        return emailSent;
    }
}
