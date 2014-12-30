package dev.dropwizard.bundler.redis;

import dev.dropwizard.bundler.refmodel.RefModelBundle;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

/**
 *
 */
@Path("redis/client")
public class RedisClientResource {

    @Inject MapperHelper mapper;
    @Inject RedisClient redisClient;
    @Inject RefModelBundle refModelBundle;

    @GET
    @Path("{model}/{id}")
    @Produces("application/json")
    public Response get(@PathParam("model") String model, @PathParam("id") String id) {
        Class<?> modelClass = refModelBundle.getModel(Redis.class, model);
        if (modelClass == null) return Response.status(Response.Status.BAD_REQUEST).build();
        Object entity = redisClient.get(modelClass, id);
        return entity != null ? Response.ok(entity).build() : Response.noContent().build();
    }

    @PUT
    @Path("{model}")
    @Consumes("application/json")
    public <T> Response put(@PathParam("model") String model, Object object) {
        Class<T> modelClass = (Class<T>) refModelBundle.getModel(Redis.class, model);
        if (modelClass == null) return Response.status(Response.Status.BAD_REQUEST).build();
        String value = mapper.write(object); //map to string
        T t = mapper.read(modelClass, value); //string to object
        boolean added = redisClient.put(t);
        return Response.status(added ? Response.Status.CREATED : Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @GET
    @Path("{model}/{property}/{value}")
    @Produces("application/json")
    public Response getByProperty(@PathParam("model") String model, @PathParam("property") String property,
                                     @PathParam("value") String value) {
        Class<?> modelClass = refModelBundle.getModel(Redis.class, model);
        if (modelClass == null) return Response.status(Response.Status.BAD_REQUEST).build();
        Collection<String> properties = refModelBundle.getPropertyMap().get(modelClass);
        if (!properties.contains(property)) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Property '" + property + "' is invalid. Use one of " + refModelBundle.getRefModelKeys(modelClass)).
                    build();
        }
        try {
            List<?> result = redisClient.getByProperty(modelClass, property, value);
            return result.isEmpty() ? Response.noContent().build() : Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
