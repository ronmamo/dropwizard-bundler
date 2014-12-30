package dev.dropwizard.bundler.samples;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.google.common.io.Resources;
import org.junit.Test;
import samples.dw.bundler.BundlerMinimalAppBundleExample;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
*
*/
public class BundlerMinimalAppBundlerTest {

    @Test
    public void testBundlerAppBundle() throws Exception {
        BundlerMinimalAppBundleExample app = new BundlerMinimalAppBundleExample();
        app.run(new String[]{"bundler", Resources.getResource("minimal.yml").getFile()});

        Map map = (Map) new Yaml().load(Resources.getResource("minimal.yml").openStream());
        assertEquals(map.get("unused"), "NA");
        assertEquals(app.configuration.unknown, null);
        assertEquals(app.configuration.extraString, "works!");
    }
}