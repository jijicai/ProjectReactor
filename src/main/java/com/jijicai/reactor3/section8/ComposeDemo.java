package com.jijicai.reactor3.section8;

import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ComposeDemo {
    public static void main(String[] args) {
        AtomicInteger ai = new AtomicInteger();
        Function<Flux<String>, Flux<String>> filterAndMap = f -> {
            if (ai.incrementAndGet() == 1) {
                return f.filter(color -> !color.equals("orange"))
                        .map(String::toUpperCase);
            }
            return f.filter(color -> !color.equals("purple"))
                    .map(String::toUpperCase);
        };

        Flux<String> composedFlux =
                Flux.fromIterable(Arrays.asList("bule", "green", "orange", "purple"))
                        .doOnNext(System.out::println)
                        .compose(filterAndMap);
        composedFlux.subscribe(d -> System.out.println("Subscriber 1 to Composed MapAndFilter：" + d));
        composedFlux.subscribe(d -> System.out.println("Subscriber 2 to Composed MapAndFilter：" + d));
    }
}
