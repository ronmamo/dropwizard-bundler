##Dropwizard-Bundler##

[![Build Status](https://travis-ci.org/ronmamo/dropwizard-bundler.svg?branch=master)](https://travis-ci.org/ronmamo/dropwizard-bundler)

----

Dropwizard-Bundler is a set of [Dropwizard](https://github.com/dropwizard/dropwizard) extensions, which heavily uses [Guice](https://github.com/google/guice), [Reflections](https://github.com/ronmamo/reflections) and [Swagger](https://github.com/swagger-api) to provide a dynamic, spontaneous and modular Dropwizard application.

It provides with an out-of-the-box support for:
 * Full Google Guice support
 * Classpath scanning and auto discovery of Guice modules and Dropwizard bundles
 * Dynamic configuration of Dropwizard ConfiguredBundles derived form the primary yaml config file
 * Auto discovery of Jersey resources, providers and othets - courtesy of [Dropwizard-Guice](https://github.com/HubSpot/dropwizard-guice)
 * Redis and ElasticSearch auto persistency - just add @Redis or @Elastic
 * RefModel - automatically index persisted data model objects based on getByProperty usage (WTF?! *1)
 * Seamless Swagger presentation of your data model - Redis, Elastic and API
 
----
###KickStart###
For a kickstart introduction, take a look at the [ImdbSample](https://github.com/ronmamo/dropwizard-bundler/tree/master/samples/imdb-sample), and in particular [BundlerImdbAppExampleTest](https://github.com/ronmamo/dropwizard-bundler/blob/master/samples/imdb-sample/src/test/java/samples/dw/bundler/BundlerImdbAppExampleTest.java)

To see it in action:
 * Install and start a local Redis server
 * Run the [BundlerImdbAppExample](https://github.com/ronmamo/dropwizard-bundler/tree/master/samples/imdb-sample/src/main/java/samples/dw/bundler/BundlerImdbAppExample.java) class
 * Then browse to [localhost:8090/swagger](http://localhost:8090/swagger) and play around with the Redis, Elastic and API tabs.

----
###Intro###
Basically, in order to use a Dropwizard-Bundler application, you would have to:

 * In the ```pom.xml``` file, add this dependency (assuming Maven is used):
```
  <dependency>
    <groupId>dev.dropwizard.bundler</groupId>
    <artifactId>dw-bundler</artifactId>
    <version>0.1-SNAPSHOT</version>
 </dependency>
```

**The jars are not uploaded yet to repo1, so you'd need to build it on your own. Just clone this repo and run mvn clean install**

 * In the Application class, add the BundlerCommand:
```
  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addCommand(new BundlerCommand<>(this));
  }
```
 * In the application's yaml config file, add pointer to your packages (*1):
```
  reflections:
    basePackages:
      - com.my.app
    modelPackages:
      - com.my.app.model
    refPackage: com.my.app.ref
```
 * Then start the application using the "bundler" command name:
```
    public static void main(String[] args) throws Exception {
        new MyApp().run(new String[]{"bundler", Resources.getResource("myapp-conf.yml").getFile()});
    }
```

----
###WalkThrough ImdbSample###
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

####RefModel (WTF?! *1)####
Now, in case you want to find an object based on some property, you can use:
```
  List<ImdbInfo> list = redisClient.getByProperty(ImdbInfo.class, RefModel.ImdbInfo.Title.name(), "Pulp Fiction");
```
Where RefModel is an auto generated enum consists of ImdbInfo properties, which can be addressed statically.
```
//generated ...
public interface RefModel {
	@RefPackage("my.app.model...")
	public enum ImdbInfo { imdbID, Title, Year, Director, ... }
}
```
Behind the scenes, when the application starts up, it looks up for code usages (!) of RefModel.* (such as above), and when persisting a model object, it is also indexed by the used properties, allowing getByProperty auto magically.

Confused? here's what it takes:
1. In the yaml config file, at the 'reflections' section, [add refPackage](http://github.com/ronmamo/dropwizard-bundler/blob/master/samples/imdb-sample/src/main/resources/imdb-example.yml#L19). This is where the generated RefModel will be located.
2. In you pom.xml file [integrate 'GenerateRefModel'](http://github.com/ronmamo/dropwizard-bundler/blob/master/samples/imdb-sample/pom.xml#L60) to auto generate RefModel class
3. In your code, use [getByProperty as needed](http://github.com/ronmamo/dropwizard-bundler/blob/master/samples/imdb-sample/src/main/java/samples/dw/bundler/ImdbController.java#L43)

*So all is needed to index a model object by any property is just to use getByProperty somewhere in your code*

This, of course, also applies to ```ElasticClient```

###WhatElse###
Contributions of code/thoughs/better doc would be highly appreciated

*Cheers*
