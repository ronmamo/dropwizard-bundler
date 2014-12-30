package dev.dropwizard.bundler.swagger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("swagger")
@Produces(MediaType.TEXT_HTML)
public class SwaggerResource {

    @Inject SwaggerBundle swaggerBundle;

    @GET
    @Path("")
    public SwaggerView get() {
        String domain = swaggerBundle.swaggerContextMap.keySet().iterator().next();
        return get(domain);
    }

    @GET
    @Path("{domain}")
    public SwaggerView get(@PathParam("domain") String domain) {
        if (swaggerBundle.isDomainRegistered(domain)) {
            SwaggerBundle.Context swaggerContext = swaggerBundle.getSwaggerContext(domain);
            SwaggerView swaggerView = new SwaggerView(swaggerContext, swaggerBundle, domain);
            return swaggerView;
        } else {
            throw new RuntimeException("domain " + domain + " was not registered in " + SwaggerBundle.class.getSimpleName());
        }
    }
}
