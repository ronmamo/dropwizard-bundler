package samples.dw.bundler;

import com.google.common.io.Resources;
import com.google.inject.Injector;
import dev.dropwizard.bundler.BundlerCommand;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.EnvironmentExtended;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 *
 */
public class BundlerElasticAppExample extends Application<BundlerElasticAppExample.Conf> {

    private Injector injector;

    public static class Conf extends Configuration {
    }

    @Override
    public void initialize(Bootstrap<Conf> bootstrap) {
        bootstrap.addBundle(new BundlerCommand<>(this));
    }

    @Override
    public void run(Conf configuration, Environment environment) throws Exception {
        injector = ((EnvironmentExtended) environment).injector();
    }

    //for test purposes
    public Injector getInjector() {
        return injector;
    }

    public static void main(String[] args) throws Exception {
        new BundlerElasticAppExample().run(new String[]{"bundler", Resources.getResource("elastic-example.yml").getFile()});
    }
}
