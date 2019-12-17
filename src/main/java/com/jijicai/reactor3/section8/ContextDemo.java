package com.jijicai.reactor3.section8;

import reactor.util.context.Context;

public class ContextDemo {

    public static void main(String[] args) {
        Context.of("key1", "value1", "key2", "value2", "key3", "value3", "key4", "value4", "key5", "value5");
    }
}
