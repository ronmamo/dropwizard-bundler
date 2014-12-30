package dev.dropwizard.bundler.elastic;

import com.google.common.collect.Lists;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MapperHelper extends dev.dropwizard.bundler.refmodel.MapperHelper {

    public <T> T convert(Class<T> objClass, SearchHit searchHit) throws IOException {
        if (searchHit != null) {
            String value = searchHit.sourceAsString();
            return value != null ? objectMapper.readValue(value, objClass) : null;
        } else {
            return null;
        }
    }

    public <T> List<T> convertAll(Class<T> objClass, SearchHits searchHits) throws IOException {
        List<T> results = Lists.newArrayList();
        for (SearchHit hit : searchHits.getHits()) {
            results.add(convert(objClass, hit));
        }
        return results;
    }
}
