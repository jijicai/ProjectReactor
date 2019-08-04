package com.jijicai.reactor3;

import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * @author: zshk
 * @version: V4.2
 * @describe:
 * @Title: Demo1.java
 * @Package package com.jijicai.reactor3;
 * @Copyright: Copyright @2017 中少红卡(北京)教育科技有限公司
 * @date 2019-08-04 16:30:58
 */
public class Demo1 {
    public static void main(String[] args) {
        Flux<String> seql= Flux.just("foo","bar","foobar");
        List<String> iterable= Arrays.asList("foo","bar","foobar");
        Flux<String> seq2=Flux.fromIterable(iterable);
    }
}
