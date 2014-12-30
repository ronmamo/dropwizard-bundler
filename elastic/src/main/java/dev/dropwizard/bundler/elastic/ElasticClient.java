package dev.dropwizard.bundler.elastic;

import dev.dropwizard.bundler.refmodel.IdResolver;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

/**
*
*/
public class ElasticClient {
    private static final Logger log = LoggerFactory.getLogger(ElasticClient.class);

    @Inject Node node;
    @Inject IndexResolver indexResolver;
    @Inject IdResolver idResolver;
    @Inject MapperHelper mapper;

    public Object get(Class<?> objClass, String id) {
        try (CloseableClient client = getClient()) {

            GetResponse getResponse = client.get().prepareGet(indexResolver.getIndex(objClass), objClass.getSimpleName(), id).get();
            return mapper.read(objClass, getResponse.getSourceAsString());
        } catch (Exception e) {
            throw new RuntimeException("Could not search object [" + objClass + "]", e);
        }
    }

    public <T> boolean put(T object) {
        try (CloseableClient client = getClient()) {
            Class<T> objClass = (Class<T>) object.getClass();
            String id = idResolver.getId(object).toString();
            final String value = mapper.write(object);

            IndexRequestBuilder request = client.get().prepareIndex(indexResolver.getIndex(objClass), objClass.getSimpleName()).
                    setId(id).setSource(value);

            IndexResponse response = request.get();
            log.debug("indexed {} [id {}] in elastic", objClass.getSimpleName(), id);
            return response.isCreated();
        } catch (Exception e) {
            throw new RuntimeException("Could not put object [" + object + "]", e);
        }
    }

    public <T> List<T> getByProperty(Class<T> objClass, Object property, Object value) {
        try (CloseableClient client = getClient()) {
            SearchRequestBuilder request = client.get().prepareSearch(indexResolver.getIndex(objClass)).
                    setTypes(objClass.getSimpleName()).
                    setQuery(QueryBuilders.matchQuery("" + property, value).operator(MatchQueryBuilder.Operator.AND));

            SearchResponse response = request.get();
            return mapper.convertAll(objClass, response.getHits());
        } catch (Exception e) {
            throw new RuntimeException("Could not search object [" + objClass + "]", e);
        }
    }

    public <T> List<T> getAll(Class<T> objClass) {
        try (CloseableClient client = getClient()) {
            SearchRequestBuilder request = client.get().prepareSearch(indexResolver.getIndex(objClass)).
                    setTypes(objClass.getSimpleName()).
                    setQuery(QueryBuilders.matchAllQuery());

            SearchResponse response = request.get();
            return mapper.convertAll(objClass, response.getHits());
        } catch (Exception e) {
            throw new RuntimeException("Could not search object [" + objClass + "]", e);
        }
    }

    //internal
    public void refresh(String index) {
        try (CloseableClient client = getClient()) {
            client.get().admin().indices().prepareRefresh(index).get();
        }
    }

    //for test purposes
    public void deleteAll(String index) throws Exception {
        try (CloseableClient client = getClient()) {
            if (client.get().admin().indices().prepareExists(index).get().isExists()) {
                client.get().prepareDeleteByQuery(index).setQuery(QueryBuilders.matchAllQuery()).get();
            }
        }
    }

    //internal
    public CloseableClient getClient() {
        return new CloseableClient();
    }

    public class CloseableClient implements AutoCloseable {
        private Client client;

        public Client get() {
            if (client == null) client = node.client();
            return client;
        }

        @Override
        public void close() {
            if (client != null) client.close();
        }
    }
}
