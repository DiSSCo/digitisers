package eu.dissco.digitisers.utils;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.*;

public class NetUtilsTest {

    private final static Logger logger = LoggerFactory.getLogger(NetUtilsTest.class);

    @Test
    public void getCountryInfoFromUrl_foundHost() throws IOException, GeoIp2Exception {
        String url = "https://www.cardiff.ac.uk";
        CountryResponse countryResponse = NetUtils.getCountryInfoFromUrl(url);

        String countryName = countryResponse.getCountry().getName();
        String continentName = countryResponse.getContinent().getName();

        assertEquals("The country name should be ","United Kingdom",countryName);
    }

    @Test(expected = java.net.UnknownHostException.class)
    public void getCountryInfoFromUrl_notFoundHost() throws IOException, GeoIp2Exception {
        String url = "http://www.testhostnotfound.ac.uk";
        CountryResponse countryResponse = NetUtils.getCountryInfoFromUrl(url);
    }

    @Test(expected = java.net.MalformedURLException.class)
    public void getCountryInfoFromUrl_invalidURL() throws IOException, GeoIp2Exception {
        String url = "htp://testhostnotfound.kk";
        CountryResponse countryResponse = NetUtils.getCountryInfoFromUrl(url);
    }

}