package com.rajesh.util.reactor;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import reactor.core.publisher.Flux;

public class Reactors {

    @Test
    public void testFlux() {
        List<Integer> list = new ArrayList<>();
        Flux.just(1, 2, 3, 4)
                .filter(n -> n % 2 == 0)
                .map(n -> n * 2)
                .log()
                .subscribe(n -> list.add(n));

        MatcherAssert.assertThat(list, Matchers.contains(4, 8));

    }
}
