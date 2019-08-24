package com.jijicai.reactor3.section4;


import org.junit.jupiter.api.Test;
import reactor.core.Disposables;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class FluxDemo {

    @Test
    public void test() {
        Flux.just("北京", "郑州", "上海", "乌鲁木齐")
                .map(place -> "地点：" + place)
                .doOnNext(place -> System.out.println(place)) // 每个元素被处理后都会产生 next 信号，执行这个方法
                .doOnComplete(() -> System.out.println("已结束")) // 所有元素被处理完后会产生 complete 信号，执行这个方法
                .doOnError(error -> System.out.println(error)) // 当错误发生时会产生 error 信号，执行这个方法
                .subscribe(place -> System.out.println("订阅：" + place)) // 只有订阅后，管道中的数据才会流动
        ;
    }

    @Test
    public void test2() {
        Flux<String> seq1 = Flux.just("foo", "bar", "foobar");

        List<String> iterable = Arrays.asList("foo", "bar", "foobar");
        Flux<String> seq2 = Flux.fromIterable(iterable);
    }

    @Test
    public void test3() {
        Mono<String> noData = Mono.empty();
        Mono<String> data = Mono.just("foo");
        Flux<Integer> numbersFromFiveToSeven = Flux.range(5, 3);
    }

    @Test
    public void subscribeTest(){
        Flux<Integer> ints=Flux.range(1,3);
        ints.subscribe();
    }

    @Test
    public void subscribeTest2() {
        Flux<Integer> ints = Flux.range(1, 3);
        ints.subscribe(System.out::println);
    }

    @Test
    public void subscribeTest3(){
        Flux<Integer> ints=Flux.range(1,4)
                .map(i->{
                    if(i<=3) return i;
                    throw new RuntimeException("Got to 4");
                })
                ;
        ints.subscribe(System.out::println,
                error->System.err.println("Error: "+error));
    }

    @Test
    public void subscribeTest4() {
        Flux<Integer> ints = Flux.range(1, 4);
        ints.subscribe(System.out::println,
                error -> System.err.println("Error" + error),
                () -> System.out.println("Done"));
    }

    @Test
    public void subscribeTest5() {
        Flux<Integer> ints = Flux.range(1, 4);
        ints.subscribe(System.out::println,
                error -> System.err.println("Error" + error),
                () -> System.out.println("Done"),
                sub->sub.request(10))
        ;
    }

    @Test
    public void disposableTest(){
        Disposables.composite().dispose();
        BaseSubscriber<Integer> baseSubscriber=null;
    }

    @Test
    public void sampleSubscriberTest(){
        SampleSubscriber<Integer> ss=new SampleSubscriber<>();
        Flux<Integer> ints=Flux.range(1,4);
        ints.subscribe(System.out::println,
                error->System.err.println("Error "+error),
                ()->System.out.println("Done"),
                s->s.request(10));
        ints.subscribe(ss);
    }

    @Test
    public void sampleSubscriberTest2(){
        SampleSubscriber<Integer> ss=new SampleSubscriber<>();
        Flux<Integer> ints=Flux.range(1,4);
        ints.subscribe(System.out::println,
                error->System.err.println("Error "+error),
                ()->System.out.println("Done"),
                s->s.request(10));
        ints.limitRate(10);
        ints.subscribe(ss);
    }

}
