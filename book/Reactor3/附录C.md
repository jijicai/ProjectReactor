# 附录 C：Reactor 扩展

reactor-extra 工件包含额外的操作符和实用程序，适用于具有高级需求的 reactor-extra 的用户。

由于这是一个单独的工件，你需要将其明确添加到你的构建中：
```
dependencies {
     compile 'io.projectreactor:reactor-core'
     compile 'io.projectreactor.addons:reactor-extra' 
}
```
（1）除了核心之外，还添加 reactor 扩展工件。有关使用 BOM 时不需要指定版本的原因、Meven 中的用法等详细信息，请参见[入门 Reactor](https://projectreactor.io/docs/core/3.2.11.RELEASE/reference/#getting)。

## C.1、TupleUtils 和 函数式接口

reactor.function 包包含函数式接口，这些接口补充了 Java 8 Function、Predicate 和 Consumer 接口的 3 到 8 个值。

TupleUtils 提供静态方法，作为这些函数式接口的 lambdas 与相应 Tuple 上的类似接口之间的桥梁。

这允许轻松地处理任何 Tuple 的独立部分，例如：
```
.map(tuple -> {
  String firstName = tuple.getT1();
  String lastName = tuple.getT2();
  String address = tuple.getT3();

  return new Customer(firstName, lastName, address);
});
```
可以改为写成
```
.map(TupleUtils.function(Customer::new)); 
```
（1）因为 Customer 构造函数符合 Consumer3 函数式接口签名。

## C.2、带 MathFlux 的数学操作符

reactor.math 包包含一个 MathFlux 专用版本的 Flux，它提供了数学操作符，比如 max、min、sumInt、averageDouble...

## C.3、重复和重试工具类

reactor.retry 包包含有助于编写 Flux#repeatWhen 和 Flux#retryWhen 函数的工具类。入口点分别是 Repeat 和 Retry 接口中的工厂方法。

两个接口都可以用作可变生成器 AND，它们都实现了正确的 Function 签名，以便在其对应的操作符中使用。

自 3.2.0 以来，这些工具类提供的最先进的重试策略之一也直接是 reactor-core 主要工件的一部分：指数退避可用作Flux#retryBackoff 操作符。

## C.4、调度器

reactor-extra 配有几个专门的调度器：

reactor.scheduler.forkjoin 包中的 ForkJoinPoolScheduler：使用 Java ForkJoinPool 执行任务。

reactor.swing 包中的 SwingScheduler 执行 Swing UI 事件循环线程 EDT 中的任务。

reactor.swing 包中的 SwtScheduler 执行 SWT UI 事件循环线程中的任务。

注脚：

    （1）有时被称为“副作用”
    （2）向上游请求最大值，当下游没有产生足够的请求时应用该策略