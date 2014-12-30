package dev.dropwizard.bundler.refmodel;

/**
 *
 */
public class AutoEmbed {

    class A {
        String id;
    }

    class B {
        String id;
        A a;
    }

    /*
    given a model object A with inner model field B
    motivation is to try to avoid structure like:
    class B {
        id
        a_id
    }

    by embedding the inner model such that:
    B is an embedded composite object
    on save -
      if specify game.id only
        verify game exist by id
        save partial game object in challenge, only with its id
      if specified more than game.id
        save new game object with the transaction
          might fail if duplicate id
        clear challenge game object to be partial (id only)
    on load -
      if embedding is set to on, load inner object by its id
    */

}
