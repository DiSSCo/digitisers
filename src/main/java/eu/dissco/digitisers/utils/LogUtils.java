package eu.dissco.digitisers.utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class LogUtils {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final static Logger logger = LoggerFactory.getLogger(LogUtils.class);


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Function that returns the list of log entries between the datetime range requested
     * @param startDateTime start date time from which to filter the log entries
     * @param endDateTime end date time from which to filter the log entries
     * @return list of log entries between the datetime range requested
     * @throws IOException
     */
    public static List<String> getLogEntriesBetweenDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) throws IOException {
        List<String> filteredLogEntries = new ArrayList<String>();
        File appLogFile = LogUtils.getApplicationLogFile();
        long logFileSizeInMb = appLogFile.length() / (1024 * 1024);
        if (logFileSizeInMb>100){
            filteredLogEntries.add("Application log file to big to filter. Please ask the DiSCCo Digitiser Admin to filter it for you");
        } else{
            List<String> logs = org.apache.commons.io.FileUtils.readLines(LogUtils.getApplicationLogFile(), "utf-8");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            filteredLogEntries = logs.stream()
                    .filter( logEntry -> {
                        boolean includeLogEntry=false;
                        try{
                            String[] strLogDate = logEntry.split(" .[a-zA-Z]");
                            LocalDateTime logEntryDateTime = LocalDateTime.parse(strLogDate[0],formatter);
                            if (logEntryDateTime.compareTo(startDateTime)>=0 && logEntryDateTime.compareTo(endDateTime)<=0) {
                                includeLogEntry = true;
                            }
                        }catch (Exception e){
                            //Nothing
                        }
                        return includeLogEntry;
                    })
                    .collect(Collectors.toList());
        }

        return filteredLogEntries;
    }


    /*******************/
    /* PRIVATE METHODS */
    /*******************/

    /**
     * Function that returns the actual log file uses by the application
     * @return Log file used by the application
     */
    private static File getApplicationLogFile(){
        File clientLogFile = null;
        FileAppender<?> fileAppender = null;
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()){
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext();){
                Object enumElement = index.next();
                if (enumElement instanceof FileAppender) {
                    fileAppender=(FileAppender<?>)enumElement;
                }
            }
        }
        if (fileAppender != null) {
            clientLogFile=new File(fileAppender.getFile());
        }

        return clientLogFile;
    }
}
