package dev.dropwizard.bundler.refmodel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates a RefModel meta annotation (such as @Redis, @Elastic)
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RefModel {
}
