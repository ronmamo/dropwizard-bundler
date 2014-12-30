package dev.dropwizard.bundler.swagger;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.core.util.ModelUtil;
import com.wordnik.swagger.model.*;
import com.wordnik.swagger.reader.ClassReader;
import scala.Option;
import scala.Some;
import scala.collection.immutable.List;

/**
*
*/
public class ModelClassReader implements ClassReader {

    private String basePath;
    private Multimap<Class<?>, String> propertyMap;

    public ModelClassReader(String basePath, Multimap<Class<?>, String> propertyMap) {
        this.basePath = basePath;
        this.propertyMap = propertyMap;
    }

    @Override
    public Option<ApiListing> read(String docRoot, Class<?> cls, SwaggerConfig config) {
        String modelName = cls.getSimpleName();
        java.util.List<String> properties = propertyMap.get(cls) != null ? Lists.newArrayList(propertyMap.get(cls)) : Lists.<String>newArrayList();
        ApiDescription[] apiDescriptions = new ApiDescription[2 + properties.size()];
        apiDescriptions[0] = getById(cls, modelName);
        apiDescriptions[1] = putWithBody(cls, modelName);
        for (int i = 0; i < properties.size(); i++) {
            apiDescriptions[i + 2] = searchByProperty(cls, modelName, properties.get(i));
        }
        return apiListing(config, modelName, config.basePath() + "/", apiDescriptions);
    }

    private ApiDescription getById(Class<?> cls, String modelName) {
        return api("GET", basePath + "/" + modelName + "/" + "{id}", cls, path("id", String.class));
    }

    private ApiDescription putWithBody(Class<?> cls, String modelName) {
        return api("PUT", basePath + "/" + modelName, cls, body("body", cls));
    }

    private ApiDescription searchByProperty(Class<?> cls, String modelName, String property) {
        return api("GET", basePath + "/" + modelName + "/" + property + "/" + "{value}", cls, path("value", String.class));
    }

    private Some<ApiListing> apiListing(SwaggerConfig config, String modelName, String path, ApiDescription... apis) {
        return new Some<>(new ApiListing(
                config.apiVersion(), config.swaggerVersion(), path, "/" + modelName, list("application/json"),
                EMPTY, EMPTY, null, ModelUtil.stripPackages(list(apis)), ModelUtil.modelsFromApis(list(apis)), NONE, 0));
    }

    private ApiDescription api(String method, String path, Class<?> response, Parameter... params) {
        Operation[] operations = new Operation[0];
        if (params != null) {
            operations = new Operation[params.length];
            for (int i = 0; i < params.length; i++) {
                operations[i] = operation(method, response, params[i]);
            }
        }
        return api(path, "api-desc", operations);
    }

    private ApiDescription api(String path, String description, Operation... operations) {
        return new ApiDescription(path, new Some(description), list(operations), false);
    }

    private Operation operation(String method, Class<?> responseType, Parameter... parameters) {
        return new Operation(method, "", null, responseType.getName(), method.toLowerCase(), 0, EMPTY, EMPTY, EMPTY,
                List.<Authorization>empty(), list(parameters), List.<ResponseMessage>empty(), NONE);
    }

    private Parameter path(String name, Class<String> dataType) {
        return parameter(name, dataType, "path");
    }

    private Parameter query(String name, Class<String> dataType) {
        return parameter(name, dataType, "query");
    }

    private Parameter body(String name, Class<?> cls) {
        return parameter(name, cls, "body");
    }

    private Parameter parameter(String name, Class<?> dataType, String parameterType) {
        return new Parameter(name, NONE, NONE, true, false, dataType.getName(), null, parameterType, NONE);
    }

    private static final Option<String> NONE = Option.apply(null);
    private static final List<String> EMPTY = List.empty();

    private static <T> List<T> list(T... elements) {
        return elements != null ? List.<T>fromArray(elements) : List.<T>empty();
    }
}
