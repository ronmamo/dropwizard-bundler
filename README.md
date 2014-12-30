##Dropwizard-Bundler##

[![Build Status](https://travis-ci.org/ronmamo/dropwizard-bundler.svg?branch=master)](https://travis-ci.org/ronmamo/dropwizard-bundler)

----

Dropwizard-Bundler is a set of [Dropwizard](https://github.com/dropwizard/dropwizard) extensions, which provides 
 * [Guice](https://github.com/HubSpot/dropwizard-guice) support and auto discovery of modules, bundles, resources and more ([1](https://github.com/ronmamo/dropwizard-bundler/blob/master/bundler/src/main/java/dev/dropwizard/bundler/BundlerCommand.java#L72))
 * Auto Rest support and swagger ui for @Redis and @Elastic annotated model classes - ([1](https://github.com/ronmamo/dropwizard-bundler/blob/master/redis/src/main/java/dev/dropwizard/bundler/redis/RedisClientResource.java) [2](https://github.com/ronmamo/dropwizard-bundler/blob/master/elastic/src/main/java/dev/dropwizard/bundler/elastic/ElasticClientResource.java))
 * Auto persist based on model usage - ([1](https://github.com/ronmamo/dropwizard-bundler/blob/master/README.md#refmodel))
 
----
###KickStart###
For a kickstart introduction, take a look at the [ImdbSample](https://github.com/ronmamo/dropwizard-bundler/tree/master/samples/imdb-sample), and [BundlerImdbAppExampleTest](https://github.com/ronmamo/dropwizard-bundler/blob/master/samples/imdb-sample/src/test/java/samples/dw/bundler/BundlerImdbAppExampleTest.java)

To see it in action:
 * Install and start a local Redis server
 * Run the [BundlerImdbAppExample](https://github.com/ronmamo/dropwizard-bundler/tree/master/samples/imdb-sample/src/main/java/samples/dw/bundler/BundlerImdbAppExample.java) class
 * Then browse to [localhost:8090/swagger](http://localhost:8090/swagger) and play around with the Redis, Elastic and API tabs.

####WalkThrough ImdbSample####

Given a data model class, such as:

```
public class ImdbInfo {
    @Id public String imdbID;
    public String Title;
    public Integer Year;
    public String Director;
    ...
}
```
Adding @Redis/@Elastic on top:

```
@Redis
@Elastic
public class ImdbInfo {
    @Id public String imdbID;
    public String Title;
    public Integer Year;
    public String Director;
    ...
}
```
Enables you to use RedisClient/ElasticClient to easily persist the object:

```
@Inject RedisClient redisClient;
@Inject ElasticClient elasticClient;

...
  redisClient.put(imdbInfo);
  elasticClient.put(imdbInfo);
...
```
And Then get it back:

```
  redisClient.get(ImdbInfo.class, <id>);
```

#####RefModel#####

Now, in case you want to find an object based on some property, you can use:

```
  List<ImdbInfo> list = 
      redisClient.getByProperty(ImdbInfo.class, RefModel.ImdbInfo.Title.name(), "Pulp Fiction");
```
Where RefModel is an auto generated enum consists of ImdbInfo properties, which can be addressed statically.

```
//generated ...
public interface RefModel {
    @RefPackage("my.app.model...")
    public enum ImdbInfo { imdbID, Title, Year, Director, ... }
}
```
All is needed to index a model object by any property is just to use ```getByProperty``` somewhere in your code. Behind the scenes, when the application starts up, it looks up for code usages (!) of RefModel.* (such as above), and takes care of auto persist.
