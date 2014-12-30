package dev.dropwizard.bundler.swagger;

import com.google.common.base.Charsets;
import com.google.common.collect.Multimap;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReader;
import com.wordnik.swagger.reader.ClassReaders;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

public class SwaggerBundle implements ConfiguredBundle<Configuration> {
    private static final Logger log = LoggerFactory.getLogger(SwaggerBundle.class);

    public static final String PATH = "/swagger-static";
    public static final String ASSETS = "assets";

    @Inject Reflections reflections;

    Map<String, Context> swaggerContextMap = new HashMap<>();
    String domain;

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.addBundle(new ViewBundle());
    }

    public void run(Configuration configuration, Environment environment) throws Exception {
        environment.jersey().register(new ApiDeclarationProvider());
        environment.jersey().register(new ResourceListingProvider());
        environment.servlets().addServlet(ASSETS, new AssetServlet(PATH, PATH + "/", "index.htm", Charsets.UTF_8)).
                addMapping(PATH + "/*");

        Set<Class<?>> apiClasses = reflections.getTypesAnnotatedWith(Api.class);
        configure(configuration, "localhost", "API", apiClasses, new DefaultJaxrsApiReader());
    }

    public void configure(Configuration configuration, String host, String domain, Collection<Class<?>> modelClasses, Multimap<Class<?>, String> propertyMap) {
        configure(configuration, host, domain, modelClasses, new ModelClassReader(domain + "/" + "client", propertyMap));
    }

    public void configure(Configuration configuration, String host, String domain, Collection<Class<?>> modelClasses,
                          ClassReader classReader) {
        ModelClassScanner scanner = new ModelClassScanner(modelClasses);
        String swaggerBasePath = getSwaggerBasePath(configuration, host);
        Context context = configure(new Context(domain, scanner, classReader, swaggerBasePath, modelClasses));
        swaggerContextMap.put(domain, context); //todo swagger context is actually needed?
        log.info("Registering domain swagger: " + swaggerBasePath + "/swagger/" + domain);
    }

    public void switchSwagger(String domain) {
        if (!isDomainRegistered(domain)) {
            throw new RuntimeException("domain not registered " + domain);
        }
        Context context = swaggerContextMap.get(domain);
        configure(context);
        this.domain = domain;
    }

    public Context configure(Context context) {
        ScannerFactory.setScanner(context.scanner);
        ClassReaders.setReader(context.classReader);

        SwaggerConfig config = ConfigFactory.config();
        config.setBasePath(context.swaggerBasePath);
        config.setApiPath(context.swaggerBasePath);

        return context;
    }

    public boolean isDomainRegistered(String domain) {
        return swaggerContextMap.containsKey(domain);
    }

    public Context getSwaggerContext(String domain) {
        return isDomainRegistered(domain) ? swaggerContextMap.get(domain) : null;
    }

    private static String getSwaggerBasePath(Configuration configuration, String host) {
        String applicationContextPath = null;
        ServerFactory serverFactory = configuration.getServerFactory();
        HttpConnectorFactory httpConnectorFactory = null;

        if (serverFactory instanceof SimpleServerFactory) {
            applicationContextPath = ((SimpleServerFactory) serverFactory).getApplicationContextPath();
            ConnectorFactory cf = ((SimpleServerFactory) serverFactory).getConnector();
            if (cf instanceof HttpsConnectorFactory) {
                httpConnectorFactory = (HttpConnectorFactory) cf;
            } else if (cf instanceof HttpConnectorFactory) {
                httpConnectorFactory = (HttpConnectorFactory) cf;
            }
        } else if (serverFactory instanceof DefaultServerFactory) {
            List<ConnectorFactory> applicationConnectors = ((DefaultServerFactory) serverFactory).getApplicationConnectors();
            for (ConnectorFactory connectorFactory : applicationConnectors) {
                if (connectorFactory instanceof HttpsConnectorFactory) {
                    httpConnectorFactory = (HttpConnectorFactory) connectorFactory;
                }
            }
            if (httpConnectorFactory == null) { // not https
                for (ConnectorFactory connectorFactory : applicationConnectors) {
                    if (connectorFactory instanceof HttpConnectorFactory) {
                        httpConnectorFactory = (HttpConnectorFactory) connectorFactory;
                    }
                }
            }
        }

        if (httpConnectorFactory == null) {
            throw new IllegalStateException("Could not get HttpConnectorFactory");
        }

        String protocol = httpConnectorFactory instanceof HttpsConnectorFactory ? "https" : "http";

        if (applicationContextPath != null && !"/".equals(applicationContextPath)) {
            return String.format("%s://%s:%s%s", protocol, host, httpConnectorFactory.getPort(), applicationContextPath);
        } else {
            return String.format("%s://%s:%s", protocol, host, httpConnectorFactory.getPort());
        }
    }

    static class Context {
        final String domain;
        final ModelClassScanner scanner;
        final ClassReader classReader;
        String swaggerBasePath;
        Collection<Class<?>> modelClasses;

        public Context(String domain, ModelClassScanner scanner, ClassReader classReader, String swaggerBasePath,
                       Collection<Class<?>> modelClasses) {
            this.domain = domain;
            this.scanner = scanner;
            this.classReader = classReader;
            this.swaggerBasePath = swaggerBasePath;
            this.modelClasses = modelClasses;
        }
    }
}
