package com.jijicai.reactor3.section6;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class StepVerifierTest {

    public <T> Flux<T> appendBoomError(Flux<T> source) {
        return source.concatWith(Mono.error(new IllegalArgumentException("boom")));
    }

    @Test
    public void testAppendBoomError() {
        Flux<String> source = Flux.just("foo", "bar");
        StepVerifier.create(
                appendBoomError(source)
        )
                .expectNext("foo")
                .expectNext("bar")
                .expectErrorMessage("boom")
                .verify();

    }
}
