package eu.dissco.digitisers.clients.gbif;

import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class GbifInfo {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private String username;
    private String password;


    /***********************/
    /* GETTERS AND SETTERS */
    /***********************/

    protected Logger getLogger() {
        return logger;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    /****************/
    /* CONSTRUCTORS */
    /****************/

    public GbifInfo(String username, String password) {
        this.username = username;
        this.password = password;
    }


    /******************/
    /* PUBLIC METHODS */
    /******************/

    public static GbifInfo getGbifInfoFromConfig(Configuration config){
        GbifInfo gbifInfo = new GbifInfo(config.getString("gbif.username"), config.getString("gbif.password"));
        return gbifInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GbifInfo gbifInfo = (GbifInfo) o;
        return Objects.equals(username, gbifInfo.username) &&
                Objects.equals(password, gbifInfo.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        return "GbifInfo{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
