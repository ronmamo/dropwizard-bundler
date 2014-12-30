package dev.dropwizard.bundler.refmodel;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.reflections.Reflections;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.serializers.Serializer;
import org.reflections.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.reflections.util.Utils.prepareFile;
import static org.reflections.util.Utils.repeat;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class RefModelSerializer implements Serializer {

    private static final String pathSeparator = "_";
    private static final String dotSeparator = ".";
    private static final String tokenSeparator = "_";
    private final String[] basePackages;
    private String className;

    public RefModelSerializer(String[] basePackages, String className) {
        this.basePackages = basePackages;
        this.className = className;
    }

    public Reflections read(InputStream inputStream) {
        throw new UnsupportedOperationException("read is not implemented on JavaCodeSerializer");
    }

    /**
     * name should be in the pattern: path/path/path/package.package.classname,
     * for example <pre>/data/projects/my/src/main/java/org.my.project.MyStore</pre>
     * would create class MyStore in package org.my.project in the path /data/projects/my/src/main/java
     */
    public File save(Reflections reflections, String name) {
        return save(reflections.getStore().get(TypeElementsScanner.class.getSimpleName()), name);
    }

    public File save(Multimap<String, String> mmap, String name) {
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1); //trim / at the end
        }

        String filename = name.replace(".", "/") + "/" + className + ".java";
        File file = prepareFile(filename);

        String packageName = name.substring(name.lastIndexOf("/") + 1);
        String className = this.className;

        String string = toString(mmap);

        StringBuilder sb = new StringBuilder();
        sb.append("//generated using " + getClass().getName() + " [" + new Date() + "]\n");
        String sha1 = Hashing.sha1().hashString(string, Charset.defaultCharset()).toString();
        sb.append("//SHA1: ").append(sha1).append("\n");
        if (packageName.length() != 0) {
            sb.append("package " + packageName + ";\n\n");
        }
        sb.append("import " + RefPackage.class.getName() + ";\n\n");
        sb.append("public interface " + className + " {\n\n");
        sb.append(string);
        sb.append("}\n");

        try {
            Files.write(sb.toString(), new File(filename), Charset.defaultCharset());
            return file;
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public String toString(Reflections reflections) {
        return toString(reflections.getStore().get(TypeElementsScanner.class.getSimpleName()));
    }

    public String toString(Multimap<String, String> mmap) {
        StringBuilder sb = new StringBuilder();
        List<String> prevPaths = Lists.newArrayList();
        int indent = 1;
        List<String> keys = Lists.newArrayList(mmap.keySet());
        Collections.sort(keys);

        for (String fqn : keys) {
            String fqn1 = fqn;
            for (String basePackage : basePackages) {
                if (fqn1.startsWith(basePackage)) {
                    fqn1 = fqn1.substring(basePackage.length() + 1);
                    break;
                }
            }
            List<String> typePaths = Lists.newArrayList(fqn1.split("\\."));

            int i = 0;
            while (i < Math.min(typePaths.size(), prevPaths.size()) && typePaths.get(i).equals(prevPaths.get(i))) i++;
            for (int j = prevPaths.size(); j > i; j--) sb.append(repeat("\t", --indent) + "}\n");
            for (int j = i; j < typePaths.size() - 1; j++)
                sb.append(repeat("\t", indent++) + "public interface " + uniqueName(typePaths.get(j), typePaths, j) + " {\n");

            String className = typePaths.get(typePaths.size() - 1);

            List<String> fields = Lists.newArrayList();
            for (String element : mmap.get(fqn)) {
                if (!element.startsWith("@")) {
                    if (!element.contains("(")) {
                        if (!Utils.isEmpty(element)) {
                            fields.add(element);
                        }
                    }
                }
            }

            sb.append(repeat("\t", indent) + "@RefPackage(\"" + fqn.substring(0, fqn.lastIndexOf(".")) + "\")\n");
            sb.append(repeat("\t", indent++) + "public enum " + uniqueName(className, typePaths, typePaths.size() - 1) + " {\n");
            for (String field : fields) {
                sb.append(repeat("\t", indent) + uniqueName(field, typePaths) + ", \n");
            }

            prevPaths = typePaths;
        }

        for (int j = prevPaths.size(); j >= 1; j--) sb.append(repeat("\t", j) + "}\n");

        return sb.toString();
    }

    private String uniqueName(String candidate, List<String> prev, int offset) {
        String normalized = candidate.replace(dotSeparator, pathSeparator);
        for (int i = 0; i < offset; i++) {
            if (normalized.equals(prev.get(i))) {
                return uniqueName(normalized + tokenSeparator, prev, offset);
            }
        }

        return normalized;
    }

    private String uniqueName(String candidate, List<String> prev) {
        return uniqueName(candidate, prev, prev.size());
    }
}
