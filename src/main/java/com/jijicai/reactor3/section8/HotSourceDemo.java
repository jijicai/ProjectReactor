package com.jijicai.reactor3.section8;

import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

public class HotSourceDemo {
    public static void main(String[] args) {
        DirectProcessor<String> hotSource = DirectProcessor.create();
        Flux<String> hotFlux = hotSource.map(String::toUpperCase);
        hotFlux.subscribe(d -> System.out.println("Subscribe 1 to Hot Source: " + d));

        hotSource.onNext("blue");
        hotSource.onNext("green");

        hotFlux.subscribe(d -> System.out.println("Subscriber 2 to Hot Source：" + d));

        hotSource.onNext("orange");
        hotSource.onNext("purple");
        hotSource.onComplete();
    }
}