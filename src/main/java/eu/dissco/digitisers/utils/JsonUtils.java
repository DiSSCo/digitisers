package eu.dissco.digitisers.utils;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;

public class JsonUtils {

    private final static Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static boolean validateJsonAgainstSchema(String json, String schema, boolean checkRequiredId){
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            JsonObject jsonSchema = gson.fromJson(schema, JsonObject.class);

            return validateJsonAgainstSchema(jsonObject,jsonSchema,checkRequiredId);
        } catch(Exception e){
            logger.warn("Error validating json against schema",e);
            return false;
        }
    }

    public static boolean validateJsonAgainstSchema(JsonObject jsonObject, JsonObject jsonSchema, boolean checkRequiredId){
        try {
            if (!checkRequiredId){
                //If we want to validate the json against the schema in order to create the object,
                // the attribute "id" should not considered as required
                JsonArray requiredFields = jsonSchema.getAsJsonArray("required");
                JsonElement jsonElementToRemove=null;
                for (JsonElement jsonElement:requiredFields) {
                    if (jsonElement.getAsString().equals("id")){
                        jsonElementToRemove=jsonElement;
                        break;
                    }
                };
                requiredFields.remove(jsonElementToRemove);
            }

            //Gson doesn't currently offer the functionality to validate an json object against a schema
            //so we need to use the library https://github.com/everit-org/json-schema to validate them,
            //and because the library works with org.json.JSONObject we need to convert our gson.JsonObjects
            org.json.JSONObject orgJsonObject = convertGsonToOrgJson(jsonObject);
            org.json.JSONObject orgJsonSchema = convertGsonToOrgJson(jsonSchema);

            //Load the schema
            SchemaLoader loader = SchemaLoader.builder()
                    .schemaJson(orgJsonSchema)
                    .draftV6Support() // or draftV7Support()
                    .build();
            Schema schema = loader.load().build();

            // Validate json against schema. Throws a ValidationException if this object is invalid
            schema.validate(orgJsonObject);

            return true;
        } catch(Exception e){
            logger.warn("Error validating json against schema",e);
            return false;
        }
    }

    public static org.json.JSONObject convertGsonToOrgJson(JsonObject gson){
        return new org.json.JSONObject(gson.getAsJsonObject().toString());
    }


    public static Object filterJson(JsonElement jsonElement, String jsonPathExpression){
        Gson gson = new Gson();
        String json = gson.toJson(jsonElement);
        return filterJson(json,jsonPathExpression);
    }

    public static Object filterJson(String json, String jsonPathExpression){
        return JsonPath.parse(json).read(jsonPathExpression, Object.class);
    }

    public static MapDifference<String, Object> compareJsonElements(JsonElement leftJsonElem, JsonElement rightJsonElem){
        Gson gson = new Gson();
        String leftJson = gson.toJson(leftJsonElem);
        String rightJson = gson.toJson(rightJsonElem);
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> leftMap = gson.fromJson(leftJson, mapType);
        Map<String, Object> rightMap = gson.fromJson(rightJson, mapType);

        //flatten the maps. It will provide better comparison results especially for nested objects and arrays.
        leftMap = FlatMapUtil.flatten(leftMap);
        rightMap = FlatMapUtil.flatten(rightMap);

        return Maps.difference(leftMap, rightMap);
    }


}
