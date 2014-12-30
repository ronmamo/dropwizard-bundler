package samples.dw.bundler;

import com.google.common.collect.Lists;
import com.sun.jersey.api.JResponse;
import com.sun.jersey.api.client.Client;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import dev.dropwizard.bundler.elastic.ElasticClient;
import dev.dropwizard.bundler.redis.MapperHelper;
import dev.dropwizard.bundler.redis.RedisClient;
import samples.dw.bundler.model.ImdbInfo;
import samples.dw.bundler.refmodel.RefModel;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
*/
@Path("imdb")
@Api("imdb")
public class ImdbController {

    @Inject RedisClient redisClient;
    @Inject ElasticClient elasticClient;
    @Inject MapperHelper mapperHelper;

    @GET
    @Path("/search")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Searches for IMDB media records with matching title",
            notes = "Searches for IMDB in local cache. If not exist, query the remote service and caches the result",
            response = ImdbInfo.class,
            responseContainer = "List"
    )
    public JResponse<List<ImdbInfo>> search(
            @QueryParam("title") @ApiParam("title") String title) {

        try {
            List<ImdbInfo> cached = redisClient.getByProperty(ImdbInfo.class, RefModel.ImdbInfo.Title.name(), title);

            if (cached == null || cached.isEmpty()) {
                ImdbInfo imdbInfo = queryOmdb(title);
                redisClient.put(imdbInfo);
                elasticClient.put(imdbInfo);
                cached = Lists.newArrayList(imdbInfo);
            }

            return JResponse.ok(cached).build();

        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/query")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Query www.omdbapi.com for media records with matching title",
            notes = "Query www.omdbapi.com for media, does not persist the result",
            response = ImdbInfo.class
    )
    public ImdbInfo queryOmdb(
            @QueryParam("title") @ApiParam("title") String title) {
        Client client = Client.create();
        String map = client.resource("http://www.omdbapi.com/").
                queryParam("t", title).queryParam("plot", "full").get(String.class);
        return mapperHelper.read(ImdbInfo.class, map);
    }
}
