package eu.dissco.digitisers.clients.digitalObjectRepository;

import net.cnri.cordra.api.CordraException;
import net.dona.doip.client.DoipException;

public class DigitalObjectRepositoryException extends Exception {
    private final String statusCode;

    public DigitalObjectRepositoryException(String message) {
        super(message);
        this.statusCode = null;
    }

    public DigitalObjectRepositoryException(String statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public DigitalObjectRepositoryException(Throwable cause) {
        super(cause);
        this.statusCode = null;
    }

    public DigitalObjectRepositoryException(String statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public String getStatusCode() {
        return this.statusCode;
    }

    public static DigitalObjectRepositoryException convertDoipException(DoipException doipException){
        return new DigitalObjectRepositoryException(doipException.getStatusCode(), doipException.getMessage(),doipException.getCause());
    }

    public static DigitalObjectRepositoryException convertCordraException(CordraException codraException){
        return new DigitalObjectRepositoryException(null, codraException.getMessage(),codraException.getCause());
    }
}
