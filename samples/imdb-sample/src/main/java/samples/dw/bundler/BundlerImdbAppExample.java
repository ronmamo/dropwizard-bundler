package samples.dw.bundler;

import com.google.common.io.Resources;
import com.google.inject.Injector;
import dev.dropwizard.bundler.BundlerCommand;
import io.dropwizard.setup.EnvironmentExtended;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 *
 */
public class BundlerImdbAppExample extends Application<Configuration> {

    private Injector injector;

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addCommand(new BundlerCommand<>(this));
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        injector = ((EnvironmentExtended) environment).injector();
    }

    //for test purposes
    public Injector getInjector() {
        return injector;
    }

    public static void main(String[] args) throws Exception {
        new BundlerImdbAppExample().run(new String[]{"bundler", Resources.getResource("imdb-example.yml").getFile()});
    }
}
