package samples.dw.bundler;

import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import dev.dropwizard.bundler.elastic.ElasticClient;
import dev.dropwizard.bundler.elastic.IndexResolver;
import dev.dropwizard.bundler.redis.RedisClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import samples.dw.bundler.model.ImdbInfo;
import samples.dw.bundler.refmodel.RefModel;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class BundlerImdbAppExampleTest {

    private static BundlerImdbAppExample app;

    @Inject ElasticClient elasticClient;
    @Inject RedisClient redisClient;
    @Inject IndexResolver indexResolver;

    Client client;

    @BeforeClass
    public static void before() throws Exception {
        (app = new BundlerImdbAppExample()).run(new String[]{"bundler", Resources.getResource("imdb-example.yml").getFile()});
    }

    @Before
    public void beforeEach() throws Exception {
        app.getInjector().injectMembers(this);
        elasticClient.deleteAll(indexResolver.getIndex(ImdbInfo.class));
        redisClient.flushDB();
        client = Client.create();
    }

    @Test
    public void test() throws Exception {
        String host = "http://localhost:8090";
        String movieTitle = "Pulp Fiction";

        //query imdb
        List<ImdbInfo> imdbInfos = client.resource(host + "/imdb/search").queryParam("title", movieTitle).
                get(new GenericType<List<ImdbInfo>>() { });
        assertThat(imdbInfos, hasSize(1));
        ImdbInfo imdbInfo = imdbInfos.get(0);
        assertThat(imdbInfo.Title, equalTo(movieTitle));

        //get from redis
        ImdbInfo imdbInfo1 = redisClient.get(ImdbInfo.class, imdbInfo.imdbID);
        assertThat(imdbInfo1.Title, equalTo(movieTitle));

        ImdbInfo imdbInfo2 = client.resource(host).
                path("/redis/client/" + ImdbInfo.class.getSimpleName() + "/" + imdbInfo.imdbID + "/").
                get(ImdbInfo.class);
        assertThat(imdbInfo2.Title, equalTo(movieTitle));

        //get from elastic
        elasticClient.refresh(indexResolver.getIndex(ImdbInfo.class));
        List<ImdbInfo> imdbResponses1 = elasticClient.getByProperty(ImdbInfo.class,
                RefModel.ImdbInfo.Director, "Quentin Tarantino");
        assertThat(imdbResponses1, hasSize(1));
        assertThat(imdbResponses1.get(0).Title, equalTo(movieTitle));

        List<ImdbInfo> imdbResponses2 = elasticClient.getByProperty(ImdbInfo.class,
                RefModel.ImdbInfo.Director, "Tarantino");
        assertThat(imdbResponses2, hasSize(1));
        assertThat(imdbResponses2.get(0).Title, equalTo(movieTitle));

        List<ImdbInfo> imdbResponses3 = elasticClient.getByProperty(ImdbInfo.class,
                RefModel.ImdbInfo.Plot, "funny");
        assertThat(imdbResponses3, hasSize(1));
        assertThat(imdbResponses3.get(0).Title, equalTo(movieTitle));
    }
}