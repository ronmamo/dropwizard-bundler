package samples.dw.bundler;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import dev.dropwizard.bundler.redis.RedisClient;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import samples.dw.bundler.model.User;
import samples.dw.bundler.ref.RefModel;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BundlerRedisAutoAppExampleTest {

    @Inject RedisClient redisClient;

    User user1 = new User(); {
        user1.id = "id1";
        user1.name = "username1";
        user1.age = 30;
    }
    User user2 = new User(); {
        user2.id = "id2";
        user2.name = "username2";
        user2.age = 30;
    }

    private static BundlerRedisAutoAppExample app;

    @BeforeClass
    public static void before() throws Exception {
        (app = new BundlerRedisAutoAppExample()).run(new String[]{"bundler", Resources.getResource("redis-example.yml").getFile()});
    }

    @Before
    public void beforeEach() throws Exception {
        app.getInjector().injectMembers(this);
        redisClient.flushDB();
    }

    @Test
    public void testPutByProperty() throws Exception {
        //according to MemberUsageScanner, RefModel.User.name and RefModel.User.age are used (just below...),
        // so name and age will be auto persisted as well by RedisRefModelBundle.PutByPropertyRefModelInterceptor
        WebResource resource = Client.create().resource("http://localhost:8090");
        resource.path("/redis/client/User").type(MediaType.APPLICATION_JSON_TYPE).put(user1);
        resource.path("/redis/client/User").type(MediaType.APPLICATION_JSON_TYPE).put(user2);

        List<User> byName1 = redisClient.getByProperty(User.class, RefModel.User.name, user1.name);
        assertThat(ids(byName1), contains(user1.id));
        List<User> byName2 = redisClient.getByProperty(User.class, RefModel.User.name, user2.name);
        assertThat(ids(byName2), contains(user2.id));
        List<User> byAge1 = redisClient.getByProperty(User.class, RefModel.User.age, 30);
        assertThat(ids(byAge1), containsInAnyOrder(user1.id, user2.id));
    }

    @Test
    public void testDup() throws Exception {
        WebResource resource = Client.create().resource("http://localhost:8090");
        resource.path("/redis/client/User").type(MediaType.APPLICATION_JSON_TYPE).put(user1);
        ClientResponse response = resource.path("/redis/client/User").type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, user1);
        assertThat(response.getStatus(), Matchers.equalTo(ClientResponse.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void swagger() {
        WebResource resource = Client.create().resource("http://localhost:8090");
        assertThat((List<?>) resource.path("/api-docs/redis").get(Map.class).get("apis"), hasSize(1));
        assertThat(resource.path("/swagger/redis").get(String.class), containsString("Swagger UI"));
    }

    private Iterable<String> ids(List<User> users) {
        return Iterables.transform(users, new Function<User, String>() {
            @Override
            public String apply(User input) {
                return input.id;
            }
        });
    }
}