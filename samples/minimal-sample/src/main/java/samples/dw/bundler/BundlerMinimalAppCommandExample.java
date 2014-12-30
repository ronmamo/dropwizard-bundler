package samples.dw.bundler;

import com.google.common.io.Resources;
import dev.dropwizard.bundler.BundlerCommand;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 *
 */
public class BundlerMinimalAppCommandExample extends Application<MinimalAppConfiguration> {

    public Bootstrap bootstrap;
    public MinimalAppConfiguration configuration;

    @Override
    public void initialize(Bootstrap<MinimalAppConfiguration> bootstrap) {
        this.bootstrap = bootstrap;
        //add command
        bootstrap.addCommand(new BundlerCommand<>(this));
    }

    @Override
    public void run(MinimalAppConfiguration configuration, Environment environment) throws Exception {
        this.configuration = configuration;
    }

    public static void main(String[] args) throws Exception {
        new BundlerMinimalAppCommandExample().run(new String[]{"bundler", Resources.getResource("minimal.yml").getFile()});
    }
}
