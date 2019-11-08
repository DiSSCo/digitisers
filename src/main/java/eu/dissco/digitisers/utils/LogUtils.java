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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class LogUtils {

    private final static Logger logger = LoggerFactory.getLogger(LogUtils.class);

    public static List<String> getLogEntriesBetweenDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) throws IOException {
        List<String> logs = org.apache.commons.io.FileUtils.readLines(getApplicationLogFile(), "utf-8");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        List<String> filteredLogEntries = logs.stream()
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

        return filteredLogEntries;
    }

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
