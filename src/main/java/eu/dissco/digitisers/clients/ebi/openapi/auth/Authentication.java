/*
 * The EBI Search: RESTful Web services
 * This is an API documentation for [EBI Search](https://www.ebi.ac.uk/ebisearch) RESTful Web services.
 *
 * The version of the OpenAPI document: all
 * Contact: www-prod@ebi.ac.uk
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package eu.dissco.digitisers.clients.ebi.openapi.auth;

import eu.dissco.digitisers.clients.ebi.openapi.Pair;

import java.util.Map;
import java.util.List;

public interface Authentication {
    /**
     * Apply authentication settings to header and query params.
     *
     * @param queryParams List of query parameters
     * @param headerParams Map of header parameters
     * @param cookieParams Map of cookie parameters
     */
    void applyToParams(List<Pair> queryParams, Map<String, String> headerParams, Map<String, String> cookieParams);
}