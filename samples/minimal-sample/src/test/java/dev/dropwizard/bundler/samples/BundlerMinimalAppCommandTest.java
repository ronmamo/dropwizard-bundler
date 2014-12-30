//package dev.dropwizard.bundler.samples;
//
//import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
//import com.google.common.io.Resources;
//import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
//import org.junit.Test;
//import samples.dw.bundler.BundlerMinimalAppCommandExample;
//
//import java.util.Map;
//
//import static org.junit.Assert.*;
//
///**
// *
// */
//public class BundlerMinimalAppCommandTest {
//
//    @Test
//    public void testBundlerAppCommand() throws Exception {
//        BundlerMinimalAppCommandExample app = new BundlerMinimalAppCommandExample();
//        app.run(new String[]{"bundler", Resources.getResource("minimal.yml").getFile()});
//
//        Map map = (Map) new Yaml().load(Resources.getResource("minimal.yml").openStream());
//        assertEquals(map.get("unused"), "NA");
//        assertEquals(app.configuration.unknown, null);
//        assertEquals(app.configuration.extraString, "works!");
//    }
//}