# 5、Kotlin 支持

## 5.1、介绍

[Kotlin](https://kotlinlang.org/) 是一种针对 JVM（和其他平台）的静态类型语言，它允许编写简洁而优雅的代码，同时提供与用 Java 编写的现有库的良好[互操作性](https://kotlinlang.org/docs/reference/java-interop.html)。

Reactor 3.1 引入了对 Kotlin 的一级支持，本节将对此进行描述。

## 5.2、要求

Reactor 支持 Kotlin 1.1+，需要 kotlin-stdlib（或其 kotlin-stdlib-jre7/kotlin-stdlib-jre8 变体之一）。

## 5.3、扩展

由于其出色的 Java 互操作性和 Kotlin 扩展，Reactor Kotlin APIs 利用了常规的 Java APIs，并通过 Reacotr 工件中一些开箱即用的现成的 Kotlin 特定 APIs 得到了增强。

注意：请记住，Kotlin 扩展需要导入才能使用。这意味着，例如，仅当导入 import reactor.core.publisher.toFlux 时，Throwable.toFlux Kotlin 扩展才可用。也就是说，与静态导入类似，IDE 在大多数情况下应该自动建议进行导入。

例如，Kotlin 具体化的类型参数为 JVM 泛型类型擦除提供了一种解决方案，并且 Reactor 提供了一些扩展来利用这一特性。

下面是 Java 中的 Reactor 和 Kotlin + 扩展中的 Reactor 的快速比较。

|Java						                |Kotlin + 扩展|
|---|---|
|Mono.just("foo")				            |"foo".toMono()|
|Flux.fromIterable(list)				    |list.toFlux()|
|Mono.error(new RuntimeException())		    |RuntimeException().toMono()|
|Flux.error(new RuntimeException())		    |RuntimeException().toFlux()|
|flux.ofType(Foo.class)				        |flux.ofType<Foo> or flux.ofType(Foo::class)|
|StepVerifier.create(flux).verifyComplete()	|flux.test.verifyComplete()|

[Reactor KDoc API](https://projectreactor.io/docs/core/release/kdoc-api/) 列出并记录了所有可用的 Kotlin 扩展。

## 5.4、空（Null）安全

Kotlin 的关键特性之一是空安全———它在编译时干净地处理 null 值，而不是在运行时碰到著名的 NullPointerException。这使得应用程序通过可控性声明和表达“值或无值”语义而更加安全，而不必像 Optional 那样承担包装器的成本（Kotlin 允许使用带可空值的函数构造；请查看 Kotlin 空安全的综合手册）。

尽管 Java 不允许在其类型系统中表示空安全性，但是 Reactor 现在通过在 reactor.util.annotation 包中声明的工具友好的注解来提供整个 Reactor API 的空安全性。在默认情况下，Kotlin 中使用的 Java APIs 类型被认为是平台类型，对其空值检查是不严格的。Kotlin 对 JSR 305 注解 + Reactor 可控性注解的支持为 Kotlin 开发人员提供了整个 Reactor API 的空安全性，其优点是在编译时处理与空相关的问题。

可以通过添加带有以下选项的 -Xjsr305 编译器标志来配置JSR 305 检查：
-Xjsr 305={strict|warn|ignore}。

对于 Kotlin 版本 1.1.50+，默认行为与 -Xjsr305=warn 相同。strict 值需要考虑到 Reactor API 的完全空安全性，并且应该考虑到是实验性的，因为 Reactor API 的可空性声明可能在较小的发布版本之间演变，并且将来可能会增加更多的检查。

注意：泛型类型参数、变量参数和数组元素的可空性还未被支持，但会在未来版本中支持，请查看有关最新信息的[讨论](https://github.com/Kotlin/KEEP/issues/79 )。
