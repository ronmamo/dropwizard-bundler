package dev.dropwizard.bundler.elastic;

import dev.dropwizard.bundler.refmodel.RefModelBundle;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

/**
 *
 */
@Path("elastic/client")
public class ElasticClientResource {

    @Inject MapperHelper mapper;
    @Inject ElasticClient elasticClient;
    @Inject RefModelBundle refModelBundle;

    @GET
    @Path("{model}/{id}")
    @Produces("application/json")
    public Response get(@PathParam("model") String model, @PathParam("id") String id) {
        Class<?> modelClass = refModelBundle.getModel(Elastic.class, model);
        if (modelClass == null) return Response.status(Response.Status.BAD_REQUEST).build();
        Object entity = elasticClient.get(modelClass, id);
        return entity != null ? Response.ok(entity).build() : Response.noContent().build();
    }

    @PUT
    @Path("{model}")
    @Consumes("application/json")
    public <T> Response put(@PathParam("model") String model, Object object) {
        Class<?> modelClass = refModelBundle.getModel(Elastic.class, model);
        if (modelClass == null) return Response.status(Response.Status.BAD_REQUEST).build();
        String value = mapper.write(object); //map to string
        T t = (T) mapper.read(modelClass, value); //string to object
        boolean added = elasticClient.put(t);
        return Response.status(added ? Response.Status.CREATED : Response.Status.NOT_MODIFIED).build();
    }

    @GET
    @Path("{model}/{property}/{value}")
    @Produces("application/json")
    public Response searchByProperty(@PathParam("model") String model, @PathParam("property") String property,
                                  @PathParam("value") String value) {
        Class<?> modelClass = refModelBundle.getModel(Elastic.class, model);
        if (modelClass == null) return Response.status(Response.Status.BAD_REQUEST).build();
        Collection<String> properties = refModelBundle.getPropertyMap().get(modelClass);
        if (!properties.contains(property)) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Property '" + property + "' is invalid. Use one of " + refModelBundle.getRefModelKeys(modelClass)).
                    build();
        }
        List<?> result = elasticClient.getByProperty(modelClass, property, value);
        return !result.isEmpty() ? Response.ok(result).build() : Response.noContent().build();
    }
}
