package dev.dropwizard.bundler.samples;

import com.google.common.io.Resources;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import org.junit.Test;
import samples.dw.bundler.BundlerMinimalAppCommandExample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class BundlerMinimalAppOverridePropertiesTest {

    @Test
    public void testBundlerAppCommand() throws Exception {
        int port1 = 31227;
        System.setProperty("dw.server.connector.port", "" + port1);

        BundlerMinimalAppCommandExample app = new BundlerMinimalAppCommandExample();
        app.run(new String[]{"bundler", Resources.getResource("minimal.yml").getFile()});

        int port = ((HttpConnectorFactory) ((SimpleServerFactory) app.configuration.getServerFactory()).getConnector()).getPort();
        assertEquals(port, port1);
    }
}