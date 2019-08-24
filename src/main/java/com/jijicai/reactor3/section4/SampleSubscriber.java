package com.jijicai.reactor3.section4;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * @author: zshk
 * @version: V4.2
 * @describe:
 * @Title: SampleSubscriber.java
 * @Package package com.jijicai.reactor3.section4;
 * @Copyright: Copyright @2017 中少红卡(北京)教育科技有限公司
 * @date 2019-08-23 00:52:17
 */
public class SampleSubscriber<T> extends BaseSubscriber<T> {
    public void hookOnSubscribe(Subscription subscription) {
        System.out.println("Subscribed");
        request(1);
    }

    public void hookOnNext(T value) {
        System.out.println(value);
        request(1);
    }
}
