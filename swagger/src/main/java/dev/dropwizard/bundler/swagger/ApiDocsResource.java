package dev.dropwizard.bundler.swagger;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.jaxrs.listing.ApiListingCache;
import com.wordnik.swagger.jaxrs.listing.ApiListingResource;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/api-docs/{domain}")
@Api("/api-docs/{domain}")
@Produces("application/json")
public class ApiDocsResource extends ApiListingResource {

    @Inject SwaggerBundle swaggerBundle;

    @Override
    public Response resourceListing(Application app, ServletConfig sc, HttpHeaders headers, UriInfo uriInfo) {
        String domain = uriInfo.getPathParameters().get("domain").get(0);
        if (!swaggerBundle.isDomainRegistered(domain)) {
            throw new RuntimeException("domain not registered " + domain);
        }
        if (!domain.equals(swaggerBundle.domain)) {
            ApiListingCache.invalidateCache();
            swaggerBundle.switchSwagger(domain);
        }
        return super.resourceListing(app, sc, headers, uriInfo);
    }
}
