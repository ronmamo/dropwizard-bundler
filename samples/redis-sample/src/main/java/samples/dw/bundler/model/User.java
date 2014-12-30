package samples.dw.bundler.model;

import dev.dropwizard.bundler.redis.Redis;

/**
 *
 */
@Redis
public class User {
    public String id;
    public String name;
    public int age;
}
