package dev.dropwizard.bundler.refmodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Equivalence;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.google.common.hash.*;
import com.google.common.io.Files;
import dev.dropwizard.bundler.features.ReflectionsBundle;
import dev.dropwizard.bundler.features.PartialConfigFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import org.apache.maven.project.MavenProject;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Generate RefModel and RefScheme for given model package
 * <p>Integrate with Maven using:
 * <pre>
     &#60build>
         &#60plugins>
            &#60plugin>
                &#60groupId>org.codehaus.gmaven&#60/groupId>
                &#60artifactId>gmaven-plugin&#60/artifactId>
                &#60version>1.3&#60/version>
                &#60executions>
                    &#60execution>
                        &#60phase>process-classes&#60/phase>
                        &#60goals>&#60goal>execute&#60/goal>&#60/goals>
                        &#60configuration>
                            &#60source>
                                 new dev.dropwizard.bundler.refmodel.GenerateRefModel(
                                     project, "test.model", "ref")
                            &#60/source>
                        &#60/configuration>
                    &#60/execution>
                &#60/executions>
            &#60/plugin>
        &#60/plugins>
     &#60/build>
 * </pre>
 */
public class GenerateRefModel {
    private static final Logger log = LoggerFactory.getLogger(GenerateRefModel.class);

    public GenerateRefModel(MavenProject project, String configPath) {

        PartialConfigFactory configFactory = new PartialConfigFactory(null);

        ObjectMapper objectMapper = Jackson.newObjectMapper();
        Validator validator = null;
//                Validation.byProvider(HibernateValidator.class)
//                        .configure()
//                        .addValidatedValueHandler(new OptionalValidatedValueUnwrapper())
//                        .buildValidatorFactory().getValidator();

        try {
            URLClassLoader classLoader = mavenClassLoader(project);
            ConfigurationSourceProvider configurationSourceProvider =
                    new File(configPath).exists() ? new FileConfigurationSourceProvider() :
                            new ClasspathConfigurationSourceProvider(classLoader);
            ReflectionsBundle.Configuration.Ref refConf = configFactory.
                    buildConfiguration(ReflectionsBundle.Configuration.class, configPath, configurationSourceProvider,
                            objectMapper, validator).reflections;

            generate(refConf, project.getBuild().getSourceDirectory(), classLoader);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
   }

    public void generate(ReflectionsBundle.Configuration.Ref refConf, String srcDir, ClassLoader classLoader) throws Exception {
        String refPath = srcDir + "/" + refConf.refPackage;

        // generate RefModel
        Reflections reflectionsRefModel = new Reflections(refConf.modelPackages, new TypeElementsScanner(), classLoader);
        File refModelFile1 = new File(refPath + "/" + "RefModel");
        String existingSha11 = null;
        if (refModelFile1.exists()) {
            existingSha11 = checkValidSha1("RefModel", refModelFile1, existingSha11);
        }

        File refModel = new RefModelSerializer(refConf.modelPackages, "RefModel").save(reflectionsRefModel, refPath);
        log.info("Saved RefModel in " + refModel.getAbsolutePath());

        // generate RefScheme usages map
        Multimap<String, String> usagesMap = buildRefModelUsagesMap(refConf, classLoader);
        File refModelFile = new File(refPath + "/" + "RefScheme");
        String existingSha1 = null;
        if (refModelFile.exists()) {
            existingSha1 = checkValidSha1("RefScheme", refModelFile, existingSha1);
        }
        checkForSchemeDiff(refConf, usagesMap);

        // save RefScheme
        File refScheme = new RefModelSerializer(refConf.modelPackages, "RefScheme").save(usagesMap, refPath);
        log.info("Saved RefScheme in " + refScheme.getAbsolutePath());
    }

    private String checkValidSha1(String refFile, File refModelFile, String existingSha1) throws IOException {
        List<String> refModelLines = Files.readLines(refModelFile, Charset.defaultCharset());
        int i = refModelLines.get(1).indexOf("//SHA1: ");
        if (i != -1) {
            existingSha1 = refModelLines.get(1).substring(i);
            String sha1 = Hashing.sha1().hashString(Joiner.on("\n").join(refModelLines.subList(2, refModelLines.size())), Charset.defaultCharset()).toString();
            if (!sha1.equals(existingSha1)) {
                throw new RuntimeException(refFile + " file seem to be corrupted, found non matching SHA1 value in" +
                        " existing " + refFile + " file [" + refModelFile.getCanonicalPath() + "]");
            }
        }
        return existingSha1;
    }

    private void checkForSchemeDiff(ReflectionsBundle.Configuration.Ref refConf, Multimap<String, String> usagesMap) {
        HashMultimap<Object, Object> existingUsageMap = HashMultimap.create();
        Class<?> existingRefScheme = null;
        try {
            existingRefScheme = ReflectionUtils.forName(refConf.refPackage + ".RefScheme");
        } catch (Exception e) {
            return;
        }
        for (Class<?> scheme : existingRefScheme.getDeclaredClasses()) {
            String fqn = scheme.getAnnotation(RefPackage.class).value() + "." + scheme.getSimpleName();
            for (Object element : scheme.getEnumConstants()) {
                existingUsageMap.put(fqn, ((Enum) element).name());
            }
        }

        MapDifference<Object, Collection<?>> difference = Maps.difference(existingUsageMap.asMap(), usagesMap.asMap(),
                new Equivalence<Collection<? extends Object>>() {
                    @Override
                    protected boolean doEquivalent(Collection<? extends Object> a, Collection<? extends Object> b) {
                        return sort(a).equals(sort(b));
                    }

                    private List sort(Collection<? extends Object> a) {
                        List asort = new ArrayList();
                        asort.addAll(a);
                        Collections.sort(asort);
                        return asort;
                    }

                    @Override
                    protected int doHash(Collection<? extends Object> objects) {
                        return sort(objects).hashCode();
                    }
                }
        );

        if (!difference.areEqual()) {
//            log.info("Scheme diff - " + difference.toString());
//            Map<Object, MapDifference.ValueDifference<Collection<?>>> diff = difference.entriesDiffering();
//            for (Object key : diff.keySet()) {
//                MapDifference.ValueDifference<Collection<?>> d = diff.get(key);
//                HashSet<Object> current = Sets.newHashSet(d.rightValue());
//                HashSet<Object> previous = Sets.newHashSet(d.leftValue());
//
//                Sets.SetView<Object> added = Sets.difference(current, previous);
//                Sets.SetView<Object> removed = Sets.difference(previous, current);
//
//            }
            throw new UnsupportedOperationException("\nScheme diff\n\tLeft:\n\t" + difference.entriesOnlyOnLeft() +
                    "\n\tRight:\n\t" + difference.entriesOnlyOnRight());
        }
    }

    private Multimap<String, String> buildRefModelUsagesMap(ReflectionsBundle.Configuration.Ref refConf, ClassLoader classLoader) {
        Reflections reflectionsRefScheme =
                new Reflections(refConf.basePackages, refConf.refPackage,
                        new MemberUsageScanner().
                                filterResultsBy(new FilterBuilder().
                                        includePackage(refConf.refPackage).
                                        exclude(FilterBuilder.prefix(refConf.refPackage) + "\\.\\$VALUES")),
                        classLoader);

        Multimap<String, String> mmap = ArrayListMultimap.create();
        Multimap<String, String> schemeMap = reflectionsRefScheme.getStore().get(MemberUsageScanner.class.getSimpleName());
        for (String key : schemeMap.keySet()) {
            int i = key.lastIndexOf(".");
            String type = key.substring(0, i);
            String element = key.substring(i + 1);
            String schemeKey = ReflectionUtils.forName(type).getAnnotation(RefPackage.class).value() +
                    type.replace(refConf.refPackage + ".RefModel", "").replace("$", ".");
            mmap.put(schemeKey, element);
        }
        return mmap;
    }

    private URLClassLoader mavenClassLoader(MavenProject project) throws Exception {
        List<String> elements = Lists.newArrayList(project.getRuntimeClasspathElements());
        elements.addAll(project.getTestClasspathElements());
        URL[] urls = new URL[elements.size()];
        int i = 0;
        for (String element : elements) urls[i++] = new File(element).toURI().toURL();
        return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }

    private static class ClasspathConfigurationSourceProvider implements ConfigurationSourceProvider {
        private final URLClassLoader classLoader;

        public ClasspathConfigurationSourceProvider(URLClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public InputStream open(String path) throws IOException {
            return classLoader.getResource(path).openStream();
        }
    }
}
