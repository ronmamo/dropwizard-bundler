package samples.dw.bundler;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import dev.dropwizard.bundler.elastic.ElasticClient;
import dev.dropwizard.bundler.elastic.IndexResolver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import samples.dw.bundler.model.User;
import samples.dw.bundler.ref.RefModel;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class BundlerElasticAppExampleTest {

    User user1 = new User(); {
        user1.id = "id1";
        user1.name = "user number one";
        user1.age = 30;
    }
    User user2 = new User(); {
        user2.id = "id2";
        user2.name = "user number two";
        user2.age = 30;
    }

    private static BundlerElasticAppExample app;

    @Inject ElasticClient elasticClient;
    @Inject IndexResolver indexResolver;

    @BeforeClass
    public static void before() throws Exception {
        (app = new BundlerElasticAppExample()).run(new String[]{"bundler", Resources.getResource("elastic-example.yml").getFile()});
    }

    @Before
    public void beforeEach() throws Exception {
        app.getInjector().injectMembers(this);
        elasticClient.deleteAll(indexResolver.getIndex(User.class));
    }

    @Test
    public void testPutByProperty() throws Exception {
        //according to MemberUsageScanner, RefModel.User.name and RefModel.User.age are used (just below...),
        // so name and age will be auto persisted as well by RedisRefModelBundle.PutByPropertyRefModelInterceptor
        elasticClient.put(user1);
        elasticClient.put(user2);
        elasticClient.refresh(indexResolver.getIndex(User.class));

        List<User> all = elasticClient.getAll(User.class);
        assertThat(ids(all), containsInAnyOrder(user1.id, user2.id));

        List<User> search1 = elasticClient.getByProperty(User.class, RefModel.User.name, "user number one");
        assertThat(ids(search1), contains(user1.id));

        List<User> search2 = elasticClient.getByProperty(User.class, RefModel.User.name, "user number two");
        assertThat(ids(search2), contains(user2.id));

        List<User> search3 = elasticClient.getByProperty(User.class, RefModel.User.name, "user number");
        assertThat(ids(search3), containsInAnyOrder(user1.id, user2.id));

        List<User> search4 = elasticClient.getByProperty(User.class, RefModel.User.age, "30");
        assertThat(ids(search4), containsInAnyOrder(user1.id, user2.id));
    }

    private Iterable<String> ids(List<User> users) {
        return Iterables.transform(users, new Function<User, String>() {
            @Nullable
            @Override
            public String apply(@Nullable User input) {
                return input.id;
            }
        });
    }
}