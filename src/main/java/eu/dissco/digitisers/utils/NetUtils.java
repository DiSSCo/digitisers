package eu.dissco.digitisers.utils;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;

public class NetUtils {

    /**************/
    /* ATTRIBUTES */
    /**************/

    private final static Logger logger = LoggerFactory.getLogger(NetUtils.class);


    /******************/
    /* PUBLIC METHODS */
    /******************/

    /**
     * Function that do a get request to the url passed as parameter and return the result as string
     * @param sUrl string of the url to do the request
     * @return String with the result of the GET request
     * @throws Exception
     */
    public static String doGetRequest(String sUrl) throws Exception{
        return doGetRequest(sUrl,null);
    }

    /**
     * Function that do a get request to the url passed as parameter with the authentication received as second parameter
     * and return the result as string
     * @param sUrl string of the url to do the request
     * @param auth authentication info to be used in the request
     * @return String with the result of the GET request
     * @throws Exception
     */
    public static String doGetRequest(String sUrl, String auth) throws Exception{
        String result=null;
        try (CloseableHttpClient httpClient = NetUtils.buildUnsafeSslHttpClient()){
            HttpGet request = new HttpGet(sUrl);
            if (StringUtils.isNotBlank(auth)) request.setHeader(HttpHeaders.AUTHORIZATION, auth);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode()/ 100 != 2) throw new RuntimeException("Failed : HTTP error code : "+ response.getStatusLine().getStatusCode());
                HttpEntity entity = response.getEntity();
                if (entity != null) result = EntityUtils.toString(entity);
            }
        }
        return result;
    }

    /**
     * Function that download the file indicated as parameter
     * @param sFileURL string of the url of the file to be downloaded
     * @return File downloaded
     * @throws Exception
     */
    public static File downloadFile(String sFileURL) throws Exception {
        return downloadFile(sFileURL,null);
    }

    /**
     * Function that download the file indicated as parameter, connecting to server with the authentication info passed
     * as second parameter
     * @param sFileURL string of the url of the file to be downloaded
     * @param auth authentication info to be used in the request
     * @return File downloaded
     * @throws Exception
     */
    public static File downloadFile(String sFileURL, String auth) throws Exception {
        logger.debug("Downloading file " + sFileURL);
        URL url = new URL(sFileURL);
        File tempFile = File.createTempFile(FilenameUtils.getBaseName(url.getPath()), FilenameUtils.getExtension(url.getPath()));

        try (CloseableHttpClient httpClient = NetUtils.buildUnsafeSslHttpClient()){
            HttpGet request = new HttpGet(sFileURL);
            if (StringUtils.isNotBlank(auth)) request.setHeader(HttpHeaders.AUTHORIZATION, auth);
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                if (response.getStatusLine().getStatusCode()/ 100 != 2) throw new RuntimeException("Failed : HTTP error code : "+ response.getStatusLine().getStatusCode());
                HttpEntity entity = response.getEntity();
                try (FileOutputStream outstream = new FileOutputStream(tempFile)) {
                    entity.writeTo(outstream);
                }
            }
        }

        logger.debug("File downloaded correctly " + sFileURL);
        return tempFile;
    }

    /**
     * Function that does a get request to the url passed as parameter and return the JsonElement returned by the server
     * @param sUrl string of the url to do the request
     * @return JsonElement with the result of the GET request
     * @throws Exception
     */
    public static JsonElement doGetRequestJson(String sUrl) throws Exception{
        return doGetRequestJson(sUrl,null);
    }

    /**
     * Function that does a get request to the url passed as parameter with the authentication received as second parameter
     * and return the JsonElement returned by the server
     * @param sUrl string of the url to do the request
     * @param auth authentication info to be used in the request
     * @return JsonElement with the result of the GET request
     * @throws Exception
     */
    public static JsonElement doGetRequestJson(String sUrl, String auth) throws Exception{
        String response = doGetRequest(sUrl,auth);
        Gson gson = new Gson();
        return gson.fromJson(response,JsonElement.class);
    }

    /**
     * Function that does a post request to the url passed a parameter sending in the body the jsonElement received as
     * second parameter
     * @param sUrl url to do the post
     * @param jsonElement jsonElement to be sent in the body
     * @return JsonElement with the result of the POST request
     * @throws Exception
     */
    public static JsonElement doPostRequestJson(String sUrl, JsonElement jsonElement) throws Exception{
        return doPostRequestJson(sUrl,null,jsonElement);
    }

    /**
     * Function that does a post request to the url passed a parameter using the authentication info passed as second
     * parameter and sending in the body the jsonElement received as third parameter
     * @param sUrl url to do the post
     * @param auth authentication info to be used in the request
     * @param jsonElement jsonElement to be sent in the body
     * @return JsonElement with the result of the POST request
     * @throws Exception
     */
    public static JsonElement doPostRequestJson(String sUrl, String auth, JsonElement jsonElement) throws Exception{
        JsonElement result = null;

        Gson gson = new Gson();
        StringEntity jsonDataEntity = new StringEntity(JsonUtils.serializeObject(jsonElement));
        try (CloseableHttpClient httpClient = NetUtils.buildUnsafeSslHttpClient()){
            HttpPost request = new HttpPost(sUrl);
            request.setHeader(HttpHeaders.ACCEPT, "application/json");
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setEntity(jsonDataEntity);

            if (StringUtils.isNotBlank(auth)) request.setHeader(HttpHeaders.AUTHORIZATION, auth);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode()/ 100 != 2) throw new RuntimeException("Failed : HTTP error code : "+ response.getStatusLine().getStatusCode());
                HttpEntity entity = response.getEntity();
                String retSrc = EntityUtils.toString(entity);
                result = gson.fromJson(retSrc,JsonElement.class);
            }
        }
        return result;
    }


    /**
     * Function to obtain a closeable unsafe http client (trust any machine)
     * @return CloseableHttpClient that will allow to send request to any machine without problems of trusting SSL
     * @throws Exception
     */
    public static CloseableHttpClient buildUnsafeSslHttpClient() throws Exception {
        // Create empty HostnameVerifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager and empty HostnameVerifier
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setSSLContext(sc)
                .setSSLHostnameVerifier(allHostsValid)
                .build();
        return client;
    }

    /**
     * Get the last segment of a URL
     * @param sURL string of the url to which we want to obtain the last segment
     * @return
     * @throws URISyntaxException
     */
    public static String getLastSegmentOfUrl(String sURL) throws URISyntaxException {
        URI uri = new URI(sURL);
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Function to get country information of the machine that is hosting the url passed as parameter
     * Note: It uses GeoLite2 country database to obtain the information from the IP address
     * @param sUrl url of the machine for which to get its country information
     * @return country information of the machine that is hosting the url
     * @throws IOException
     * @throws GeoIp2Exception
     */
    public static CountryResponse getCountryInfoFromUrl(String sUrl) throws IOException, GeoIp2Exception {
        InputStream geoLite2DbInputStream = null;
        try {
            geoLite2DbInputStream = Resources.getResource("GeoLite2-Country_20191029/GeoLite2-Country.mmdb").openStream();
            DatabaseReader dbReader = new DatabaseReader.Builder(geoLite2DbInputStream).build();
            InetAddress address = InetAddress.getByName(new URL(sUrl).getHost());
            return dbReader.country(address);
        } finally {
            if (geoLite2DbInputStream != null) {
                geoLite2DbInputStream.close();
            }
        }
    }

}
