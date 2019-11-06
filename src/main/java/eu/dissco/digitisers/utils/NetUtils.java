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

    private final static Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static String doGetRequest(String sUrl) throws Exception{
        return doGetRequest(sUrl,null);
    }

    public static String doGetRequest(String sUrl, String auth) throws Exception{
        String result=null;
        try (CloseableHttpClient httpClient = buildUnsafeSslHttpClient()){
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

    public static File downloadFile(String sFileURL) throws Exception {
        return downloadFile(sFileURL,null);
    }

    public static File downloadFile(String sFileURL, String auth) throws Exception {
        logger.debug("Downloading file " + sFileURL);
        URL url = new URL(sFileURL);
        File tempFile = File.createTempFile(FilenameUtils.getBaseName(url.getPath()), FilenameUtils.getExtension(url.getPath()));

        try (CloseableHttpClient httpClient = buildUnsafeSslHttpClient()){
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

    public static JsonElement doGetRequestJson(String sUrl) throws Exception{
        return doGetRequestJson(sUrl,null);
    }

    public static JsonElement doGetRequestJson(String sUrl, String auth) throws Exception{
        String response = doGetRequest(sUrl,auth);
        Gson gson = new Gson();
        return gson.fromJson(response,JsonElement.class);
    }

    public static JsonElement doPostRequestJson(String sUrl, JsonElement jsonElement) throws Exception{
        return doPostRequestJson(sUrl,null,jsonElement);
    }

    public static JsonElement doPostRequestJson(String sUrl, String auth,JsonElement jsonElement) throws Exception{
        JsonElement result = null;

        Gson gson = new Gson();
        StringEntity jsonDataEntity = new StringEntity(gson.toJson(jsonElement));

        try (CloseableHttpClient httpClient = buildUnsafeSslHttpClient()){
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


    public static String getLastSegmentOfUrl(String sURL) throws URISyntaxException {
        URI uri = new URI(sURL);
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

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
