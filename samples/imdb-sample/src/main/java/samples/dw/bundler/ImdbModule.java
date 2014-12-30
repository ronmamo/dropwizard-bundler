package samples.dw.bundler;

import com.google.inject.AbstractModule;
import dev.dropwizard.bundler.redis.KeyResolver;

/**
 */
public class ImdbModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(KeyResolver.class).to(KeyResolver.Lowercase.class);
    }
}
