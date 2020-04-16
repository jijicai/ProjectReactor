# 4、Reactor 核心功能

Reactor 项目的主要工件是 reactor-core，一个反应式库，它专注于反应式流规范和目标是 Java 8 。

Reactor 引入了可组合的反应式类型来实现 Publisher 接口，并且还提供了丰富的操作符词汇：Flux 和 Mono。一个 Flux 对象表示一个有 0\~N 项的反应式序列，而一个 Mono 对象表示一个单值或空结果（0\~1）。

这种区别将一些语义信息携带到类型中，指示异步处理的粗略基数。例如，一个 HTTP 请求只产生一个响应，所以进行 count 操作没有多大意义。因此，将这种 HTTP 调用的结果表示为 Mono&lt;HttpResponse>，比将其表示为 Flux&lt;HttpResponse> 更有意义，因为它只提供与 0~1 项上下文相关的操作符。

更改处理的最大基数的操作符也切换到相关类型。例如，count 操作符存在于 Flux 中，但它返回一个 Mono&lt;Long>。


## 4.1、Flux，0~N 项的异步序列

![image](img/flux.png)

>图片中的文本：
>
>This is the timeline of the Flux. Time flows from left to right.
>
>这是 Flux 的时间线。时间从左到右流动。
>
>These are items emitted by the Flux.
>
>这些是 Flux 发出的项。
>
>This vertical line indicates that Flux has completed successfull.
>
>这条垂直线表示 Flux 已成功完成。
>
>This Flux is the result of the transformation.
>
>该 Flux 是转换的结果。
>
>If for some reason the Flux terminates abnormally, with an error, the vertical line is replaced by an X.
>
>如果由于某种原因，Flux 异常终止，并出现错误，垂直线将被 X 替换。
>
>These dotted lines and this box indicate that a transformation is being applied to the Flux. The text inside the box shows the nature of the transformation.
>
>这些虚线和此框表示正在对 Flux 应用转换。方框内的文本显示了转换的性质。

Flux&lt;T> 是一个标准的 Publishe&lt;T> ，它表示一个发出的 0-N 项的异步序列，它可以被完成信号或错误中断。在反应式流规范中，这 3 种类型的信号转换为对下游订阅者的 onNext、onComplete、onError 方法的调用。

由于可能的信号范围很大，Flux 是通用的反应式类型。请注意：所有事件，甚至是终止事件都是可选的：
除了 onComplete 事件之外，没有 onNext 事件表示空的有限序列，但是删除 onComplete 后，你将得到一个无限的空序列（除了关于取消的测试之外，这不是特别有用）。同样，无限序列不一定是空的。例如，Flux.interval(Duration) 产生一个无限的 Flux&lt;Long> 并从时钟发出规则的滴答声。


## 4.2、Mono，异步的 0~1 个结果

![image](img/mono.png)

>图片中的文本：
>
>This is the timeline of the Mono. Time flows from left to right.
>
>这是 Flux 的时间线。时间从左到右流动。
>
>This is the eventual item emitted by the Mono.
>
>这是 Mono 发出的最终项。
>
>This vertical line indicates that the Mono has completed successfully.
>
>这条垂直线表示 Mono 已成功完成。
>
>This Mono is the result of the transformation.
>
>该 Mono 是转换的结果。
>
>If for some reason the Mono terminates abnormally, with an error, the vertical line is replaced by a X.
>
>如果由于某种原因，Flux 异常终止，并出现错误，垂直线将被 X 替换。
>
>These dotted lines and this box indicate that a transformation is being applied to the Mono. The text inside the box shows the nature of the transformation.
>
>这些虚线和此框表示正在对 Mono 应用转换。方框内的文本显示了转换的性质。

Mono&lt;T> 是一个专门的 Publisher&lt;T>，它最多只发出一个数据项的，然后可以选择使用一个 onComplete 或 onError 信号终止它。

它只提供了对 Flux 可用的操作符的一个子集，和一些将其转换为 Flux 的操作符（特别是那些将 Mono 和 另一个 Publisher 结合在一起的操作符）。例如，Mono#concatWith(Publisher) 返回一个 Flux，而 Mono#then(Mono) 返回另一个 Mono。

注意，Mono 可以用来表示只有完成概念（类似于 Runnable）的无值的异步进程。要创建一个，请使用一个空 Mono&lt;Void>。

## 4.3、创建 Flux 或 Mono 并订阅它的简单方式

开始使用 Flux 和 Mono 的最简单方法是使用在各自类中找到的众多工厂方法之一。

例如，创建一个 String 序列，可以列举它们，也可以把它们放到集合中，并从中创建 Flux。如下：
```
Flux<String> seq1 = Flux.just("foo", "bar", "foobar");

List<String> iterable = Arrays.asList("foo", "bar", "foobar");
Flux<String> seq2 = Flux.fromIterable(iterable);
```
工厂方法的其他示例包括以下内容：
```
Mono<String> noData = Mono.empty(); 

Mono<String> data = Mono.just("foo");

Flux<Integer> numbersFromFiveToSeven = Flux.range(5, 3); 
```
    （1）注意，工厂方法承认泛型类型，即使它没有值。
    （2）第一个参数是范围的开始，第二个参数是要产生的数据项数目。

当订阅时，Flux 和 Mono 可以利用 Java 8 拉姆达表达式（lambdas）。你可以选择多种 .subscribe() 变体，这些变体为不同的回调组合使用拉姆达表达式，如以下方法签名所示：

基于 Lambda 的 Flux 订阅变体
```
subscribe(); 

subscribe(Consumer<? super T> consumer); 

subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer); 

subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer,Runnable completeConsumer); 

subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer,
          Runnable completeConsumer,Consumer<? super Subscription> subscriptionConsumer);
```
    （1）订阅并触发序列。
    （2）使用每个产生的值做一些事情。
    （3）处理值，但也对错误作出反应。
    （4）处理值和错误，但也在序列成功完成时执行一些代码。
    （5）处理值、错误和成功完成，但也要处理此 subscribe 调用生成的 Subscription。

提示：这些变体方法返回对订阅的引用，当不需要更多的数据时，你可以使用它取消订阅。取消后，数据源应当停止产生值，并清除掉创建的任何资源。这种取消和清理行为在 Reactor 中由通用的 Disposable 接口表示。

### 4.3.1、subscribe 方法示例

本小节包含 subscribe 方法的五个签名中每个的最小示例。下面的代码展示了一个无参基本方法的示例。
```
Flux<Integer> ints = Flux.range(1, 3); 
ints.subscribe(); 
```
    （1）设置一个 Flux，当订阅者连接时产生 3 个值。
    （2）以最简单的方式订阅。

上面的代码没有产生任何可见的输出，但是它确实工作了。这 Flux 产生了 3 个值。如果我们提供一个拉姆达表达式（lambdas），我们就能看到这些值的输出。
subscribe 方法的下一个例子展示了显示值的一种方法：
```
Flux<Integer> ints = Flux.range(1, 3); 
ints.subscribe(i -> System.out.println(i)); 
```
    （1）设置一个 Flux，当订阅者连接时产生 3 个值。
    （2）使用将打印这些值的订阅者进行订阅。

上面的代码产生以下输出：
```
1
2
3
```
为了演示下一个签名方法，我们故意引入一个错误，如下面示例所示：
```
Flux<Integer> ints = Flux.range(1, 4) 
      .map(i -> { 
        if (i <= 3) return i; 
        throw new RuntimeException("Got to 4"); 
      });
ints.subscribe(i -> System.out.println(i), 
      error -> System.err.println("Error: " + error));
```
    （1）设置一个 Flux，当订阅者连接时产生 3 个值。
    （2）我们使用了 map 方法，这样我们就可以以不同的方式处理这些值。
    （3）对于大部分值，返回值本身。
    （4）对于一个值，将强制产生错误。
    （5）使用包含错误处理程序的订阅者订阅。

现在我们有 2 个拉姆达表达式：一个用于我们期望的内容、另一个用于错误。上述代码产生以下输出：
```
1
2
3
Error: java.lang.RuntimeException: Got to 4
```
下一个 subscribe 签名方法包括一个错误处理程序和一个用于完成事件的处理程序，如下面示例所示：
```
Flux<Integer> ints = Flux.range(1, 4); 
ints.subscribe(i -> System.out.println(i),
    error -> System.err.println("Error " + error),
    () -> System.out.println("Done"));
```
    （1）创建一个 Flux 对象，当订阅时，它可以产生 3 个值。
    （2）使用包含完成事件的处理程序的订阅者订阅。

错误信号和完成信号都是终止事件，并且彼此是互斥的（你从不会同时得到它们）。要完成消费者工作，我们必须注意不要触发错误。

完成（completion）回调没有输入，由一对空括号表示：它匹配 Runnable 接口中的 run 方法。上面的代码生成以下输出：
```
1
2
3
4
Done
```
最后一个 subscribe 的签名方法包括一个 Consumer&lt;Subscription>。该变体需要你对 Subscription（对其执行一个 request(long)，或 cancel() ） 执行一些操作，否则 Flux 将挂起：
```
Flux<Integer> ints = Flux.range(1, 4);
ints.subscribe(i -> System.out.println(i),
    error -> System.err.println("Error " + error),
    () -> System.out.println("Done"),
    sub -> sub.request(10)); 
```
    （1）当我们订阅时，我们收到一个 Subscription。发出信号，表示我们需要从源获取最多 10 个元素（该源实际将发出 4 个元素就完成了）。

### 4.3.2、用 Disposable 取消 subscribe()

subscribe() 方法的所有这些基于拉姆达表达式的变体都有一个 Disposable 返回类型。在本例中，Disposable 接口表示可以通过调用其 dispose() 方法取消订阅。

对于 Flux 或 Mono，取消是源应该停止产生元素的信号。但是，不能保证它是及时的：有些源可能生成元素的速度非常快，甚至可以在收到取消指令之前完成。

在 Disposables 类中有一些有关 Disposable 的实用的工具方法。其中，Disposable.swap() 可以创建一个 Disposable 包裹，允许你自动地取消并替换一个具体的 Disposable。例如，在一个 UI 场景中，当用户点击某个按钮时，你想取消一个请求并用一个新的替换它，这可能是有用的。释放包装器本身将关闭它，释放当前的具体值和将来尝试的所有替换。

另一个有趣的工具方法是 Disposables.composite(...)。这个 composite 方法允许收集几个 Disposable，例如，与服务调用关联的多个执行中的请求，并在稍后立即处理所有这些请求。一旦调用了 composite 接口的 dispose() 方法，任何添加另一个 Disposable 的尝试都会立即处理该方法。

### 4.3.3、lambdas 表达式的替代者：BaseSubscriber

另外还有一个更通用的 subscribe 方法，它采用成熟的 Subscriber，而不是从拉姆达表达式中组合一个。为了帮助你编写这样的一个 Subscriber，我们提供了一个可扩展的名为 BaseSubscriber 的类。

让我们实现其中一个，我们称之为 SampleSubscriber。下面的例子展示它是如何与 Flux 关联的：
```
SampleSubscriber<Integer> ss = new SampleSubscriber<Integer>();
Flux<Integer> ints = Flux.range(1, 4);
ints.subscribe(i -> System.out.println(i),
    error -> System.err.println("Error " + error),
    () -> {System.out.println("Done");},
    s -> s.request(10));
ints.subscribe(ss);
```

现在让我们看一下 SampleSubscriber 是什么样子的，作为 BaseSubscriber 类的一个最小化实现：
```
package io.projectreactor.samples;

import org.reactivestreams.Subscription;

import reactor.core.publisher.BaseSubscriber;

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
```
SampleSubscriber 类继承了 BaseSubscriber ,它是 Reactor 中用户定义的 Subscribers 推荐的抽象类。该类提供了可以被覆盖的钩子来调优订阅者的行为。默认情况下，它将触发一个无界请求，其行为与 subscribe() 完全相同。然而，当你需要自定义请求量时，继承 BaseSubscriber 要有用得多。

对于自定义请求量，最基本的要求是像我们一样实现 hookOnSubscribe(Subscription subscription) 和 hookOnNext(T value) 。在我们的例子中，hookOnSubscribe 方法打印一条语句到标准输出并发出第一个请求。然后，hookOnNext 方法打印一条语句并执行其他的请求，每次一个请求。

SampleSubscriber 类生成以下输出：
```
Subscribed
1
2
3
4
```
BaseSubscriber 也提供一个 requestUnbounded() 方法用于切换到无边界模式（相当于 request(Long.MAX_VALUE)），以及一个 cancel() 方法。

还有其他的钩子：hookOnComplete、hookOnError、hookOnCancel 和 hookFinally（序列终止时总是调用这个方法，并将终止类型作为 SignalType 参数传入）。

注意：你很可能想要实现 hookOnError、hookOnCancel 和 hookOnComplete 方法。你也许也想要实现 hookFinally 方法。SampleSubscribe 是执行有界请求的 Subscriber 的绝对最小实现。

### 4.3.4、关于背压，以及更改请求的方式

在 Reactor 中实现背压时，消费者压力传播回源的方式是通过向上游操作符发送 request。当前请求之和有时候被引用为当前“请求”或“挂起的请求”。请求量的上限是 Long.MAX_VALUE，表示一个无界的请求（尽可能快地生成数据，基本上是禁用背压）。

在订阅时，第一个请求来自最终订阅者，但是最直接的订阅方式都会立即触发一个 Long.MAX_VALUE 的无界请求：

    （1）subscribe() 和大部分基于拉姆达表达式的变体（具有 Consumer<Subscription> 参数的变体除外）
    （2）block()、blockFirst() 和 blockLast()
    （3）使用方法 toIterable() 和 toStream() 执行迭代操作。

自定义原始请求的最简单方式是用带有 hookOnSubscribe 重写方法的 BaseSubscriber 去 subscribe：
```
Flux.range(1, 10)
.doOnRequest(r -> System.out.println("request of " + r))
.subscribe(new BaseSubscriber<Integer>() {

  @Override
  public void hookOnSubscribe(Subscription subscription) {
    request(1);
  }

  @Override
  public void hookOnNext(Integer integer) {
    System.out.println("Cancelling after having received " + integer);
    cancel();
  }
});
```
输出：
```
request of 1
Cancelling after having received 1
```
警告：
当操作一个请求时，你必须小心地产生足够的请求，以使序列前进，否则 Flux 将会“卡住”。这就为什么 BaseSubscriber 在 hookOnSubscribe 方法中默认是无界请求的原因。当覆盖这个 hook 方法时，你通常应该至少调用一次 request 方法。

**操作符改变下游的需求**

要记住的一点是，在订阅级别上表达的请求可以被上游链条中每个操作符重新调整。教科书中案例是 buffer(N) 操作符：如果它收到一个 request(2)，它会被解释为对两个完整缓存区的请求。因此，由于缓冲区需要将 N 个元素视为已满，buffer 操作符将请求改造为 2*N。

你可能还注意到，有些操作符的变体接受一个名为 prefetch 的 int 型输入参数。这是修改下游请求的另一类操作符。这些通常处理内部序列的操作符，从每个传入元素（像 flatMap 方法）派生一个 Publisher。

预取（Prefetch）是一种调整对这些内部序列发出的初始化请求的方法。如果未指定，大多数操作符以 32 个请求为开始。

这些操作符通常还实现一个补充优化：一旦操作符看到 25% 的预取请求完成，它就会从上游再请求 25%。这是一种启发式优化，使这些操作符能够主动预测上游请求。

最后，有一对操作符可以让你直接调整请求：limitRate 和 limitRequest。

limitRate(N) 拆分下游的请求，以便它们以较小的批量向上游传播。例如，向 limitRate(10) 发出 100 的请求，最多会将 10 个 10 的请求传播到上游。注意，在这种形式中，limitRate 实际上实现了上面讨论的补充优化。

这个操作符有一个变体，它允许你调整补充数量，在变体 limitRate(highTide,lowTide) 中被称为 lowTide。选择 lowTide 为 0 ,这会导致严格的 highTide 批量的请求，而不是通过补充策略进一步返工的批次。

另一方面，limitRequest(N) 将下游请求限制为最大总请求。它将请求相加到 N。如果单个 request 没有使总需求超过 N，则该特定请求将完全向上游传播。在源发出该数量后，limitRequest 会考虑序列完成，并向下游发送一个 onComplete 信号，以取消源。

## 4.4、以编程方式创建一个序列

在本节中，我们将通过以编程方式定义其关联事件（onNext、onError 和 onComplete）来介绍 Flux 或 Mono 的创建。所有这些方法都共享这样一个事实：它们公开一个 API 来触发我们称为 sink 的事件。实际上，有几个 sink 的变体，我们很快就会讲到。

### 4.4.1、同步方法：generate

以编程方式创建 Flux 的最简单方式是通过 generate 方法，该方法采用生成器函数。

这是针对同步和逐个排放的，这意味着 sink 是一个 SynchronousSink，并且它的 next() 方法在每次回调调用中最多只能调用一次。然后你可以另外调用 error(Throwable) 或 complete()，但这是可选的。

最有用的变体可能是这样一个变体，它还允许你保持一个状态，你可以在 sink 使用中引用该状态来决定下一个要发出什么。然后，生成器函数变为 BiFunction&lt;S,SynchronousSink&lt;T>,S>，其中 &lt;S> 是状态对象的类型。你必须为初始状态提供一个 Supplier&lt;S>，并且生成器函数现在在每一轮返回一个新状态。

例如，你可以使用 int 作为状态：

基于状态的 generate 方法示例
```
Flux<String> flux = Flux.generate(
    () -> 0, 
    (state, sink) -> {
      sink.next("3 x " + state + " = " + 3*state); 
      if (state == 10) sink.complete(); 
      return state + 1; 
    });
```
    （1）我们提供初始状态值：0
    （2）我们使用状态来选择发出什么（3 的乘法表中的一行）
    （3）我们也用它来选择何时停止。
    （4）我们返回在下一次调用中使用的新状态。（除非序列在此调用中终止）

上面的代码生成了 3 的乘法表，如下所示：

    3 x 0 = 0
    3 x 1 = 3
    3 x 2 = 6
    3 x 3 = 9
    3 x 4 = 12
    3 x 5 = 15
    3 x 6 = 18
    3 x 7 = 21
    3 x 8 = 24
    3 x 9 = 27
    3 x 10 = 30

还可以使用可变的 &lt;S>。例如，可以使用一个 AtomicLong 作为状态重写上面的示例，并在每一轮中对其进行修改：

可变状态变量
```
Flux<String> flux = Flux.generate(
    AtomicLong::new, 
    (state, sink) -> {
      long i = state.getAndIncrement(); 
      sink.next("3 x " + i + " = " + 3*i);
      if (i == 10) sink.complete();
      return state; 
    });
```
    （1）这次，我们生成一个可变对象作为状态。
    （2）我们改变了这里的状态。
    （3）返回与新状态相同的实例。

注意：如果状态对象需要清理一些资源，则可以使用 generate(Supplier&lt;S>,BiFunction,Consumer&lt;S>) 变体来清理最后一个状态实例。

下面是使用包含一个 Consumer 的 generate 方法的示例：
```
Flux<String> flux = Flux.generate(
    AtomicLong::new,
      (state, sink) -> { 
      long i = state.getAndIncrement(); 
      sink.next("3 x " + i + " = " + 3*i);
      if (i == 10) sink.complete();
      return state; 
    }, (state) -> System.out.println("state: " + state)); 
}
```
    （1）同样，生成一个可变对象作为状态。
    （2）我们改变了这里的状态。
    （3）返回与新状态相同的实例。
    （4）我们将最后一个状态值（11）作为 Consumer 拉姆达表达式的输出。

在包含数据库连接或其他资源的状态对象需要在进程结束时处理的情况下，Consumer 拉姆达表达式可以关闭连接，或以其他方式处理应该在进程结束时完成的任何任务。

### 4.4.2、异步且多线程的方法：create

create 方法是一种更高级的程序化创建 Flux 的形式，它适合于每轮多次发送，甚至来自多个线程。

它提供了 FluxSink 类，其中包含：next、error 和 complete 方法。与 generate 方法相反，它没有基于状态的变体。另一方面，它可以在回调中触发多线程事件。

注意：create 方法对于将现有的 API 与反应式世界（例如基于监听器的异步 API）桥接起来非常有用。

警告：尽管 create 方法可以与异步 API 一起使用，但它不会将代码并行化，也不会使其成为异步的。如果在 create 方法的拉姆达表达式中阻塞，则会使自己暴露在死锁和类似的副作用中。即使使用 subscribeOn，也需要注意，一个长时间阻塞的 create 方法的拉姆达表达式（例如调用 sink.next(t) 的无限循环）可以锁定管道：由于循环耗尽了请求应该运行的同一线程，因此永远不会执行这些请求。使用 subscribeOn(Scheduler,false) 变体：requestOnSeparateThread=false 会使用 create 方法的 Scheduler 线程，并仍然通过在原始线程中执行请求让数据流动。

假设你使用一个基于监听器的 API。它按块处理数据，并有两个事件：（1）数据块已准备好，（2）处理已完成（终端事件），如 MyEventListener 接口中所示：
```
interface MyEventListener<T> {
    void onDataChunk(List<T> chunk);
    void processComplete();
}
```
你可以使用 create 方法将这个接口桥接到 Flux&lt;T>：
```
Flux<String> bridge = Flux.create(sink -> {
    myEventProcessor.register( 
      new MyEventListener<String>() { 

        public void onDataChunk(List<String> chunk) {
          for(String s : chunk) {
            sink.next(s); 
          }
        }

        public void processComplete() {
            sink.complete(); 
        }
    });
});
```
    （1）桥接到 MyEventListener API
    （2）块中的每个元素都成为 Flux 中的一个元素。
    （3）processComplete 事件被转换为 onComplete 。
    （4）所有这些都是在 myEventProcessor 执行时异步完成的。

此外，由于 create 方法可以桥接异步 APIs 并管理背压，因此可以通过指明 OverflowStrategy 来改进如何明智地处理背压。

IGNORE：完全忽略下游的背压请求。当队列在下游排满时，可能会产生 IllegalStateException。

ERROR：当下游无法跟上时，发出 IllegalStateException 信号。

DROP：如果下游没有准备好接收传入信号，则将其删除。

LATEST：让下游只接收来自上游的最新信号。

BUFFER：如果下游无法跟上，则缓存所有信号。这是默认情况。（这将执行无限制的缓冲，并可能导致 OutOfMemoryError）

注意：Mono 也有一个 create 生成器。Mono 的 create 的 MonoSink 不允许多次排放。它会在第一个信号之后丢弃所有信号。

### 4.4.3、异步且单线程的方法：push

push 方法位于 generat 和 create 之间，适合于处理来自单个生产者的事件。它类似于 create，因为它也可以是异步的，并且可以使用 create 支持的任何溢出策略来管理背压。但是，一次只能有一个生产线程可以调用 next、complete 或 error。
```
Flux<String> bridge = Flux.push(sink -> {
    myEventProcessor.register(
      new SingleThreadEventListener<String>() { 

        public void onDataChunk(List<String> chunk) {
          for(String s : chunk) {
            sink.next(s); 
          }
        }

        public void processComplete() {
            sink.complete(); 
        }

        public void processError(Throwable e) {
            sink.error(e); 
        }
    });
});
```
    （1）桥接到 SingleThreadEventListener API。
    （2）使用来自单个监听器线程的 next 方法推送事件到接收器（sink）。
    （3）complete 事件产生自相同的监听器线程。
    （4）error 事件也产生自相同的监听器线程。

**一个混合的 push/pull 模型**

大多数 Reactor 操作符，像 create，遵循混合的 push/pull 模型。我们的意思是，尽管大多数处理是异步的（建议使用 push 方法），但它有一个小的 pull 组件：请求。

消费者从源拉取数据，也就是说，在第一次请求之前，它不会发出任何数据。当数据可用时，源推送数据到消费者，但在其请求量的范围内。

请注意，push() 和 create() 都允许设置 onRequest 消费者，以便管理请求量，并确保只有在存在挂起的请求时才通过接收器（sink）推送数据。
```
Flux<String> bridge = Flux.create(sink -> {
    myMessageProcessor.register(
      new MyMessageListener<String>() {

        public void onMessage(List<String> messages) {
          for(String s : messages) {
            sink.next(s); 
          }
        }
    });
    sink.onRequest(n -> {
        List<String> messages = myMessageProcessor.getHistory(n); 
        for(String s : message) {
           sink.next(s); 
        }
    });
});
```
    （1）当发出请求时轮询消息。
    （2）如果消息立即可用，则推送它们到接送器（sink）。
    （3）稍后异步到达的其余消息也将被发送。

**在 push() 或 create() 后清理数据**

两个回调，onDispose 和 onCancel，在取消或终止时执行清理。当 Flux 完成、错误消除，或者被取消时，onDispose 可用于执行清理。onCancel 可用于在使用 onDispose 清理之前执行任何特定于取消的操作。
```
Flux<String> bridge = Flux.create(sink -> {
    sink.onRequest(n -> channel.poll(n))
        .onCancel(() -> channel.cancel()) 
        .onDispose(() -> channel.close())  
    });
```
    （1）首先调用 onCancel，仅用于取消信号。
    （2）调用 onDispose 处理完成、错误或取消信号。

### 4.4.4、方法：handle

handle 方法有点不同：它是一个实例方法，这意味着它被连接到一个现有的源上（就像常见的操作符一样）。Mono 和 Flux 中都有这个方法。

它与 generate 相似，在这个意义上，它使用 SynchronousSink 并且只允许一个一个地发送数据。但是，handle 可以用于从每个源元素中生成任意值，可能跳过某些元素。通过这种方式，它可以作为 map 和 filter 的结合。handle 签名如下：

    Flux<R> handle(BiConsumer<T, SynchronousSink<R>>);

让我们看一个例子。反应式流规范不允许序列中有 null 值。如果你想执行 map，但是你想使用现有方法作为 map 函数，而该方法有时候返回 null，那该怎么办？

例如，下面的方法可以安全地应用到整数源：
```
public String alphabet(int letterNumber) {
    if (letterNumber < 1 || letterNumber > 26) {
        return null;
    }
    int letterIndexAscii = 'A' + letterNumber - 1;
    return "" + (char) letterIndexAscii;
}
```
然后，我们可以使用 handle 方法去移除任何 null 值：

为“映射和消除 null”场景使用 handle 方法
```
Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
    .handle((i, sink) -> {
        String letter = alphabet(i); 
        if (letter != null) 
            sink.next(letter); 
    });

alphabet.subscribe(System.out::println);
```
    （1）映射到字母。
    （2）如果 “map 函数”返回 null...
    （3）通过不调用 sink.next 来过滤它。
```
输出：
M
I
T
```
## 4.5、线程和调度器

像 RxJava 一样，Reactor 可以被认为是并发不可知的。也就是说，它不强制并发模型。相反，它让开发者你来负责。但是，这并不妨碍库帮助你处理并发。

获取 Flux 或 Mono 并不一定意味着它将运行在专有 Thread 中。相反，大多数操作符继续在前一个操作符执行的 Thread 中工作。如果不指定，最上面的操作符（源）自身在产生 subscribe() 调用的 Thread 上运行。
```
public static void main(String[] args) {
  final Mono<String> mono = Mono.just("hello "); 

  new Thread(() -> mono
      .map(msg -> msg + "thread ")
      .subscribe(v -> 
          System.out.println(v + Thread.currentThread().getName()) 
      )
  ).join();

}
```
    （1）Mono<String> 在 main 线程中组装的...
    （2）...但在 Thread-0 线程中订阅它。
    （3）因此，map 和 onNext 回调实际上都运行在 Thread-0 线程中。

上面的代码生成以下输出：

    hello thread Thread-0

在 Reactor 中，执行模型和执行发生的位置由所使用的 Scheduler 决定。Scheduler 的调度职责与 ExecutorService 类似，但是有一个专用的抽象允许做更多的工作，尤其是充当时钟和支持更广泛的实现（测试、trampolining（蹦床）或即时调度等的虚拟时间）。

Schedulers 类有静态方法，可以访问以下执行上下文：

（1）当前线程（Schedulers.immediate()）

（2）单个的可复用的线程（Schedulers.single()）。请注意，在释放 Scheduler 之前，此方法对所有调用者重复使用同一线程。如果你想要每个调用使用单独的线程，则对每个调用使用 Schedulers.newSingle()。

（3）可伸缩的线程池（Schedulers.elastic()）。它根据需要创建新的 worker 池，并重用空闲的。空闲时间过长（默认：60秒）的 worker 池将被释放。例如，这对 I/O 阻塞工作来说是一个好选择。Schedulers.elastic() 是一种方便的方法，可以让阻塞进程拥有自己的线程，这样它就不会占用其他资源。查看：[如何包装同步的阻塞调用？](https://projectreactor.io/docs/core/release/reference/#faq.wrap-blocking)


（4）为并行工作而调优的固定工作线程池（Schedulers.parallel()）。它创建和 CPU 核心数一样多的 worker。

另外，通过使用 Schedulers.fromExecutorService(ExecutorService)，你可以从任何预先存在的 ExecutorService 创建 Scheduler 。（你也可以从 Executor 创建一个，尽管不鼓励这样做）

还可以使用 newXXX 方法创建各种调度器类型的新实例。例如，Schedulers.newElastic(yourScheduleName)，创建一个名称为 yourScheduleName 的新弹性调度器。

>警告：
>
>如果不能避免遗留阻塞代码，那么使用 elastic 可以帮助处理它们，而 single 和 parallel 不能。因此，在默认的单个和并行 Schedulers 中使用 Reactor 的阻塞 APIs（block()、blockFirst()、blockLast()，以及迭代 toIterable() 或 toStream()）将导致抛出 IllegalStateException。
>
>通过创建实现了 NonBlocking 标记接口的 Thread 实例，自定义的 Schedulers 也可以被标记为“仅非阻塞”。


一些操作符默认使用来自 Schedulers 的特定调度器（并且通常为你提供不同的调度器选项）。例如，调用工厂方法 Flux.interval(Duration.ofMillis(300)) 会生成一个 Flux&lt;Long>，每 300毫秒滴答一次。默认情况下，这是由 Schedulers.parallel() 启用的。以下行将调度器（scheduler）更改为类似 Schedulers.single() 的新实例：
```
Flux.interval(Duration.ofMillis(300), Schedulers.newSingle("test"))
```
Reactor 提供在反应式链中切换执行上下文（或 Scheduler）的两种方法：publishOn 和 subscribeOn。两者都采用 Scheduler，并允许你将执行上下文切换到该调度器。但 publishOn 在链中的位置很重要，而 subscribeOn 的位置却不重要。要理解这一区别，你首先要记住：[在 subscribe() 之前什么都不会发生](https://projectreactor.io/docs/core/3.2.11.RELEASE/reference/#reactive.subscribe)。

在 Reactor 中，当你链接操作符时，你可以根据需要将多个 Flux 和 Mono 实现彼此封装在一起。订阅之后，将创建 Subscriber 对象的链，向后（向上）到第一个发布者。这实际上是对你隐藏的。你所能看到的只是 Flux（或 Mono）和 Subscription 的外层，但这些中间的特定于操作符的订阅者才是真正的工作发生的地方。

有了这些知识，我们可以更深入地了解 publishOn 和 subscribeOn 操作符。

### 4.5.1、publishOn

publishOn 以与订阅者链中间的任何其他操作符相同的方式应用。它接收来自上游的信号，并在下游重放它们，同时对来自关联的 Scheduler 的 worker 执行回调。因此，它会影响后续操作符的执行位置（直到另一个 publishOn 被链接进来）：

    （1）将执行上下文更改为 Scheduler 选择的一个 Thread。
    （2）按照规范， onNext 是按顺序发生的，因此这将使用单个线程。
    （3）如果它们没有在特定的 Scheduler 上工作，则 publishOn 之后的操作符继续在同一线程上执行。
```
Scheduler s = Schedulers.newParallel("parallel-scheduler", 4); 

final Flux<String> flux = Flux
    .range(1, 2)
    .map(i -> 10 + i)  
    .publishOn(s)  
    .map(i -> "value " + i);  

new Thread(() -> flux.subscribe(System.out::println));
```
    （1）创建一个由 4 个 Thread 支持的新的 Scheduler 
    （2）第一个 map 运行在 <5> 中的匿名线程上
    （3）publishOn 在从 <1> 中选择的 Thread 上切换整个序列
    （4）第二个 map 在 <1> 的 Thread 上运行
    （5）这个匿名 Thread 是订阅发生的线程。打印发生在最新的执行上下文上，该上下文来自 publishOn。

### 4.5.2、subscribeOn

当该反向链被构造时，subscribeOn 应用于订阅进程。因此，不论把 subscribeOn 放在链条中的哪个位置，它总是会影响源排放的上下文。但是，这不会影响后续对 publishOn 的调用行为。它们仍然会切换之后链部分的执行上下文。

    （1）更改上面整个运算符链订阅的 Thread
    （2）从 Scheduler 选择一个线程

注意：实际上，仅考虑在链条中最早的 subscribeOn 调用。
```
Scheduler s = Schedulers.newParallel("parallel-scheduler", 4); 

final Flux<String> flux = Flux
    .range(1, 2)
    .map(i -> 10 + i)  
    .subscribeOn(s)  
    .map(i -> "value " + i);  

new Thread(() -> flux.subscribe(System.out::println)); 
```
    （1）创建一个由 4 个 Thread 支持的新 Scheduler
    （2）第一个 map 运行在这 4 个线程之一上
    （3）因为 subscribeOn 将从订阅时间（<5>）开始切换整个序列
    （4）第二个 map 也运行在同一线程上
    （5）这个匿名 Thread 是订阅最初发生的线程，但 subscribeOn 立即将其转移到 4 个调度器线程之一。

## 4.6、处理错误

提示：要快速查找错误处理的可用操作符，请查看：[相关操作符决策树](https://projectreactor.io/docs/core/release/reference/#which.errors)。

在 反应式流中，错误是终止事件。只要发生错误，它会停止序列，并沿着操作符链条向下传播到最后一步，即你定义的 Subscriber 和其 onError 方法。

此类错误仍应在应用级别被处理。例如，你可以在 UI 中显示错误通知，或者在 REST 端点中发送有意义的错误负载。因此，订阅者的 onError 方法应该始终是被定义的。

警告：如果未定义，onError 会抛出异常 UnsupportedOperationException。你可以使用方法 Exceptions.isErrorCallbackNotImplemented 更进一步地检测和归类错误。

Reactor 也提供了处理链中间错误的替代方法，如错误处理操作符。示例如下：
```
Flux.just(1, 2, 0)
    .map(i -> "100 / " + i + " = " + (100 / i)) //this triggers an error with 0
    .onErrorReturn("Divided by zero :("); // error handling example
```
注意：在你学习有关错误处理操作符之前，你必须记住，反应式序列中的任何错误都是终止事件。即使使用了错误处理操作符，它也不允许原始序列继续。相反，它将 onError 信号转换成一个新序列（回退序列）的开始。换言之，它将替换其上游的终止序列。

现在我们可以逐一考虑每种错误的处理方法。当相关的时候，我们使用命令式编程的 try 模式进行并行。

### 4.6.1、错误处理的操作符

你可能熟悉 try-catch 代码块中异常处理的几种方法。最有可能是，其中包括以下内容：

    （1）捕获并返回静态默认值。
    （2）捕获并使用回退方法执行另一个线路。
    （3）捕获并动态计算回退值。
    （4）捕获，封装到一个 BusinessException 中，然后重新抛出。
    （5）捕获，记录特定的错误信息，然后重新抛出。
    （6）使用 finally 代码块来清理资源，或使用 Java 7 的“try-with-resource”结构。

所有这些在 Reactor 中都有等价的，以错误处理操作符的形式存在。在研究这些操作符之前，让我们先在反应式链和 try-catch 块之间建立一个并行关系。

订阅时，链末端的 onError 回调类似于 catch 代码块。在那里，执行跳到 catch 代码块，以防抛出 Exception。
```
Flux<String> s = Flux.range(1, 10)
    .map(v -> doSomethingDangerous(v)) 
    .map(v -> doSecondTransform(v)); 
s.subscribe(value -> System.out.println("RECEIVED " + value), 
            error -> System.err.println("CAUGHT " + error) 
);
```
    （1）执行一个可以抛出异常的转换。
    （2）如果一切顺利，将执行第二个转换。
    （3）打印出每个成功转换的值。
    （4）若发生错误，这个序列将终止并显示错误信息。

这个在概念上类似于下面的 try/catch 代码块：
```
try {
    for (int i = 1; i < 11; i++) {
        String v1 = doSomethingDangerous(i); 
        String v2 = doSecondTransform(v1); 
        System.out.println("RECEIVED " + v2);
    }
} catch (Throwable t) {
    System.err.println("CAUGHT " + t); 
}
```
    （1）如果在此抛出异常…​
    （2）…​跳过循环的其余部分…​
    （3）…​执行直接跳到这里

现在我们已经建立了一个并行关系，我们将查看不同的错误处理案例及其等价的操作符。

**静态回退值**

“捕获并返回静态默认值”等价于 onErrorReturn。
```
try {
  return doSomethingDangerous(10);
}
catch (Throwable error) {
  return "RECOVERED";
}
```
改造为：
```
Flux.just(10)
    .map(this::doSomethingDangerous)
    .onErrorReturn("RECOVERED");
```
你还可以选择对异常应用 Predicate 来决定是否恢复：
```
Flux.just(10)
    .map(this::doSomethingDangerous)
    .onErrorReturn(e -> e.getMessage().equals("boom10"), "recovered10"); 
```
    （1）只有在异常消息为“boom10”时才能恢复。

**回退方法**

如果你想要一个以上的默认值，并且有另一个更安全的处理数据的方式，则可以使用 onErrorResume。这相当于“捕获并使用回退方法执行另一个线路”。

例如，如果你的名义进程是从外部不可靠的服务获取数据，但是你也保留了相同数据的本地缓存，这些数据可能有些过时，但更可靠，你可以执行以下操作：
```
String v1;
try {
  v1 = callExternalService("key1");
}
catch (Throwable error) {
  v1 = getFromCache("key1");
}

String v2;
try {
  v2 = callExternalService("key2");
}
catch (Throwable error) {
  v2 = getFromCache("key2");
}
```
改造为：
```
Flux.just("key1", "key2")
    .flatMap(k -> callExternalService(k) 
        .onErrorResume(e -> getFromCache(k)) 
    );
```
    （1）对于每个键，我们异步地调用外部服务。
    （2）如果外部服务调用失败，我们将回退到该键的缓存。注意，我们总是应用相同的回退，不管源错误 e 是什么。

与 onErrorReturn 一样，onErrorResume 也有一些变体，可以根据异常类或 Predicate 过滤要回退的异常。它接受一个 Function，这也允许你根据所遇到的错误选择要切换到的不同回退序列：
```
Flux.just("timeout1", "unknown", "key2")
    .flatMap(k -> callExternalService(k)
        .onErrorResume(error -> { 
            if (error instanceof TimeoutException) 
                return getFromCache(k);
            else if (error instanceof UnknownKeyException)  
                return registerNewEntry(k, "DEFAULT");
            else
                return Flux.error(error); 
        })
    );
```
    （1）函数允许动态地选择如何继续。
    （2）如果源超时，返回本地缓存。
    （3）如果源中不存在这个 key，则创建一个新实体。
    （4）在所有其他情况下，“重新抛出”异常。

**动态回退值**

即使没有其他更安全的方法来处理数据，也可能希望从接收到的异常中计算回退值。这相当于“捕获并动态计算回退值”。

例如，如果返回类型 MyWrapper 有一个变量专用于保存异常（想想 Future.complete(T success) 和 Future.completeExceptionally(Throwable error))），则可以实例化错误保持变量并传递异常。

命令式示例如下所示：
```
try {
  Value v = erroringMethod();
  return MyWrapper.fromValue(v);
}
catch (Throwable error) {
  return MyWrapper.fromError(error);
}
```
这可以使用与回退方法解决方案相同的方式进行，使用 onErrorResume，并使用少量样板文件。
```
erroringFlux.onErrorResume(error -> Mono.just( 
        MyWrapper.fromError(error) 
));
```
（1）因为你期望得到错误的 MyWrapper 表示，所以你需要获得一个 Mono&lt;MyWrapper> 用于 onErrorResume。我们使用 Mono.just 做这。

（2）我们需要计算异常的值，在这里，我们通过使用一个相关的 MyWrapper 工厂方法包装异常来实现这一点。

**捕获并重新抛出（Catch and Rethrow）**

“捕获，包装到一个业务异常（BusinessException）中，然后重新抛出”，命令式中的这看起来是这样的：
```
try {
  return callExternalService(k);
}
catch (Throwable error) {
  throw new BusinessException("oops, SLA exceeded", error);
}
```
在“回退方法”的示例中，flatMap 中的最后一行给出了一个提示：我们如何以反应式方式实现相同的结果。
```
Flux.just("timeout1")
    .flatMap(k -> callExternalService(k))
    .onErrorResume(original -> Flux.error(
            new BusinessException("oops, SLA exceeded", original))
    );
```
然而，有一种更直接的方法可以用 onErrorMap 实现同样的效果。
```
Flux.just("timeout1")
    .flatMap(k -> callExternalService(k))
    .onErrorMap(original -> new BusinessException("oops, SLA exceeded", original));
```
**Log or React on the Side**

对于你希望错误继续传播，但仍希望不修改序列（例如，记录它）就能对其作出反应的情况，有 doOnError 运算符。这等价于“捕获，记录特定的错误信息，然后重新抛出”模式，如下所示：
```
try {
  return callExternalService(k);
}
catch (RuntimeException error) {
  //make a record of the error
  log("uh oh, falling back, service failed for key " + k);
  throw error;
}
```
doOnError 运算符以及所有以 doOn 为前缀的运算符有时被称为“side-effect”（附带的作用）。它们可以让你窥视序列中的事件内部而无需修改它们。

与上面的命令式示例一样，下面的示例仍然传播错误，但确保至少记录了外部服务发生的故障。我们还可以想象我们有统计计数器作为第二个错误“side-effect”来递增。
```
LongAdder failureStat = new LongAdder();
Flux<String> flux =
Flux.just("unknown")
    .flatMap(k -> callExternalService(k) 
        .doOnError(e -> {
            failureStat.increment();
            log("uh oh, falling back, service failed for key " + k); 
        })
        
    );
```
    （1）外部服务调用失败...
    （2）...带有日志和统计的“side-effect”
    （3）如果我们在这里没有使用错误恢复操作符，在此之后，它仍然会以错误结束。

**使用资源和 finally 代码块（Using Resource and the Finally Block）**

使用命令式编程绘制的最后一个并行部分是清理，可以通过“使用 finally 块清理资源”或“Java 7 的 try-with-resource 代码结构”来完成，两者都如下所示：

命令式地使用 finally
```
Stats stats = new Stats();
stats.startTimer();
try {
  doSomethingDangerous();
}
finally {
  stats.stopTimerAndRecordTiming();
}
```
命令式地使用 try-with-resource 
```
try (SomeAutoCloseable disposableInstance = new SomeAutoCloseable()) {
  return disposableInstance.toString();
}
```
两者都有 Reactor 等价方法，doFinally 和 using。

doFinally 是关于你希望在序列终止（带有 onComplete 或 onError）或被取消时执行的“side-effect”。它给你一个提示，什么样的终止触发了“side-effect”：

反应式 finally：doFinally()
```
Stats stats = new Stats();
LongAdder statsCancel = new LongAdder();

Flux<String> flux =
Flux.just("foo", "bar")
    .doOnSubscribe(s -> stats.startTimer())
    .doFinally(type -> { 
        stats.stopTimerAndRecordTiming();
        if (type == SignalType.CANCEL) 
          statsCancel.increment();
    })
    .take(1);
```
    （1）doFinally 使用 SignalType 作为终止类型。
    （2）与 finally 模块类似，我们总是记录时间。
    （3）在这里，我们也只在取消的情况下增加统计数据。
    （4）take(1) 将在发出 1 个数据项后取消。

另一方面，using 处理从资源生成的 Flux 的情况，并且无论何时处理完成，都必须对该资源进行操作。让我们用 Disposable 替换“try-with-resource”的 AutoCloseable 接口：

可支配的资源
```
AtomicBoolean isDisposed = new AtomicBoolean();
Disposable disposableInstance = new Disposable() {
    @Override
    public void dispose() {
        isDisposed.set(true); 
    }

    @Override
    public String toString() {
        return "DISPOSABLE";
    }
};
```
现在，我们可以对它执行与“try-with-resource”对应的反应式操作，如下所示：

反应式 try-with-resource：using()
```
Flux<String> flux =
Flux.using(
        () -> disposableInstance, 
        disposable -> Flux.just(disposable.toString()), 
        Disposable::dispose 
);
```
    （1）第一个拉姆达表达式生成资源。在这里，我们返回我们的模拟 Disposable。
    （2）第二个拉姆达表达式处理资源，返回一个 Flux<T>。
    （3）当来自（2）中的 Flux 终止或者被取消时，调用第三个拉姆达表达式来清理资源。
    （4）在订阅和执行序列后，isIdsposed 变量的原子布尔值将变为 true。

**演示 onError 的终端切面（Demonstrating the Terminal Aspect of onError）**

为了演示错误发生时，所有这些操作符都会造成上游的原始序列终止，我们可以使用一个更直观的例子，其中包含 Flux.interval。interval 操作符每 x 时间单位递增一个 Long 值：
```
Flux<String> flux =
Flux.interval(Duration.ofMillis(250))
    .map(input -> {
        if (input < 3) return "tick " + input;
        throw new RuntimeException("boom");
    })
    .onErrorReturn("Uh oh");

flux.subscribe(System.out::println);
Thread.sleep(2100);
```
（1）请注意，默认情况下，interval 在计时调度器（timer Scheduler）上执行。假设我们想在一个主类中运行这个例子，我们在这里添加一个 sleep 方法调用，这样应用程序就不会在无任何值产生的情况下立即退出。

每 250ms 打印一行，如下：

    tick 0
    tick 1
    tick 2
    Uh oh

即使多了一秒钟的运行时间，interval 中也不会有更多的滴答。这个错误确实终止了序列。

**重试（Retrying）**

关于错误处理还有另一个有趣的操作符，你可能会在上面的情况中使用它。retry，顾名思义，让你重试一个产生错误的序列。

需要记住的是，它是通过重新订阅上游的 Flux 来工作的。这实际上是一个不同的序列，而原来的序列仍然被终止。为了验证这一点，我们可以重用之前的示例，并附加一个 retry(1) 来重试一次，而不是使用 onErrorReturn：
```
Flux.interval(Duration.ofMillis(250))
    .map(input -> {
        if (input < 3) return "tick " + input;
        throw new RuntimeException("boom");
    })
    .retry(1)
    .elapsed() 
    .subscribe(System.out::println, System.err::println); 

Thread.sleep(2100); 
```
    （1）elapsed 方法将每个值与前一个值发出后的持续时间关联起来。
    （2）我们还想看看什么时候有 onError。
    （3）确保我们有足够的时间进行 4x2 次滴答。

这将生成以下输出：

    259,tick 0
    249,tick 1
    251,tick 2
    506,tick 0 
    248,tick 1
    253,tick 2
    java.lang.RuntimeException: boom

（1）一个新的 interval 方法从滴答 0 开始。另外的 250ms 持续时间来自第 4 次滴答，这造成异常和和随后的重试。

正如你上面所看到的，retry(1) 只是重新订阅一次原来的 interval，从 0 开始滴答。第二轮，由于异常仍会发生，它放弃并将错误传播到下游。

有一个更高级的 retry 版本（称为 retryWhen），它使用一个“辅助” Flux 来判断某个特定的故障是否应该重试。此辅助 Flux 由操作符创建，但由用户修饰，以便自定义重试条件。

辅助 Flux 是 Flux&lt;Throwable>，它被传递给 Function，它是 retryWhen 方法的唯一参数。作为用户，定义该函数并让它返回一个新的 Publisher&lt;?>。重试循环是这样的：

    （1）每次错误（重试的可能性）发生时，该错误就会被发送给辅助 Flux，该 Flux 已经由函数修饰过。这里的 Flux 可以让我们鸟瞰到目前为止的所有尝试。
    （2）如果辅助 Flux 发出一个值，则重试发生。
    （3）如果辅助 Flux 完成，则错误被吞咽，重试循环停止，结果序列也完成。
    （4）如果辅助 Flux 产生一个错误 e，则重试循环停止，并产生与 e 有关的序列错误。

前两种情况之间的区别很重要。简单地完成辅助（companion ）将有效地吞下一个错误。考虑以下使用 retryWhen 模拟 retry(3) 的方法：
```
Flux<String> flux = Flux
    .<String>error(new IllegalArgumentException()) 
    .doOnError(System.out::println) 
    .retryWhen(companion -> companion.take(3)); 
```
    （1）这会持续产生错误，要求重试。
    （2）doOnError 在重试之前会让我们记录并查看所有失败。
    （3）在这里，我们认为前 3 个错误是可重试的，然后放弃。

实际上，这会导致一个空 Flux，但它成功地完成了。由于相同 Flux 上的 retry(3) 将以最新的错误终止，因此 retryWhen 示例与 retry(3) 并不完全相同。

达到同样的行为需要一些额外的技巧：
```
Flux<String> flux =
Flux.<String>error(new IllegalArgumentException())
    .retryWhen(companion -> companion
    .zipWith(Flux.range(1, 4), 
          (error, index) -> { 
            if (index < 4) return index; 
            else throw Exceptions.propagate(error); 
          })
    );
```
    （1）技巧一：使用 zip 和 “可接受重试次数+1” 的 range。
    （2）zip 函数允许你统计重试次数，同时跟踪原始错误。
    （3）为了允许重试 3 次，4 之前的索引返回要发出的值。
    （4）为了在发生错误时终止序列，我们在三次重试之后抛出原始异常。

提示：类似的代码可用于实现指数回退（ exponential backoff）和重试模式，如 [FAQ](https://projectreactor.io/docs/core/release/reference/#faq.exponentialBackoff) 所示。

### 4.6.2、在操作符或函数中处理异常

通常，所有操作符本身都可以包含一些代码，这些代码可能触发异常或调用用户自定义的回调函数，这些回调函数也可能失败，因此它们都包含某种形式的错误处理。

一般来说，一个未经检查的异常总是通过 onError 传播。例如，在 map 函数中抛出一个 RuntimeException 将被转换为 onError 事件，如下面代码所示：
```
Flux.just("foo")
    .map(s -> { throw new IllegalArgumentException(s); })
    .subscribe(v -> System.out.println("GOT VALUE"),
               e -> System.out.println("ERROR: " + e));
```
上面的代码打印出来：

    ERROR: java.lang.IllegalArgumentException: foo

提示：通过使用[钩子（hook）](https://projectreactor.io/docs/core/release/reference/#hooks-internal)，可以在 Exception 传递给 onError 之前对其进行调优。

然而，Reactor 定义一组异常（例如：OutOfMemoryError），这些异常总是被认为是致命的。查看 Exceptions.throwIfFatal 方法。这些错误意味着 Reactor 不能继续运行，并且被抛出而不是传播。

注意：在内部，也有未经检查的异常仍无法传播的情况（最明显的是订阅和请求阶段），原因是并发竞争可能导致双倍的 onError 或 onComplete 条件。当这些竞争发生时，无法传播的错误将被“丢弃”。这些情况在一定程度上仍然可以通过可定制的钩子来管理，请参阅 [Dropping Hooks](https://projectreactor.io/docs/core/release/reference/#hooks-dropping)。

你也许会问：“可检查的异常的情况如何？”

例如，如果你需要调用某些声明抛出异常的方法，则仍然必须在 try-catch 块中处理这些异常。不过，你有几个选择：

    （1）捕获异常并从中恢复。序列正常继续。
    （2）捕获异常并将其包装成未检查的异常，然后抛出它（中断序列）。Exceptions 工具类可以在这方面帮助你。（我们将在接下来讨论）
    （3）如果你希望返回一个 Flux（例如，在 flatMap），请将异常包装到一个产生错误的 Flux中:return Flux.error(checkedException)。（序列也会终止）

Reactor 有一个 Exceptions 工具类，你可以使用它来确保只有在检查异常时才包装异常：

（1）若需要，可以使用 Exceptions.propagate 方法来包装异常。它还首先调用 throwIfFatal,并且不包装 RuntimeException。

（2）使用 Exception.unwrap 方法获取原始的未包装异常（回到特定于 Reactor 异常的层次结构的根本原因）。

考虑一个使用转换方法的 map 的例子，该转换方法可以抛出 IOException：
```
public String convert(int i) throws IOException {
    if (i > 3) {
        throw new IOException("boom " + i);
    }
    return "OK " + i;
}
```
现在想象一下你想在 map 函数中使用这种方法。现在必须显式捕获异常，并且 map 函数无法重新抛出它。因此，你可以将 RuntimeException 传播到 map 的 onError 方法：
```
Flux<String> converted = Flux
    .range(1, 10)
    .map(i -> {
        try { return convert(i); }
        catch (IOException e) { throw Exceptions.propagate(e); }
    });
```
稍后，当订阅上面的 Flux 并对错误作出反应（例如在 UI 中）时，如果你想对 IOException 做一些特殊的事情，可以恢复到原始异常，如下所示：
```
converted.subscribe(
    v -> System.out.println("RECEIVED: " + v),
    e -> {
        if (Exceptions.unwrap(e) instanceof IOException) {
            System.out.println("Something bad happened with I/O");
        } else {
            System.out.println("Something bad happened");
        }
    }
);
```
## 4.7、处理器（Processors）

处理器是一种特殊的 Publisher，也是 Subscriber。这意味着你可以订阅 Processor(通常，它们实现 Flux),但是你也可以调用方法来手动将数据注入序列或者终止它。

有几种处理器，每种都有一些特定的语义，但是在你开始研究这些处理器之前，你需要问自己以下问题：

### 4.7.1、你需要处理器吗？

大多数时候，你应该尽量避免使用 Processor。它们很难正确使用，而且容易出现一些死角情况。

如果你认为 Processor 可以很好地与你的用例匹配，那么问问你自己是否尝试过这两种方法：

（1）一个操作符或多个操作符的组合是否符合要求？（查看：[我需要哪个操作符？](https://projectreactor.io/docs/core/release/reference/#which-operator)）

（2）“generator”操作符能代替它工作吗？（通常，这些操作符是用来桥接非反应式 APIs 的，它提供了一个“sink”，在某种意义上，它类似于 Processor，允许你用数据手动填充序列或终止序列。）

如果在研究了上述替代方案之后，你仍然认为需要一个 Processor，请阅读下面的[可用处理器概述](https://projectreactor.io/docs/core/3.2.11.RELEASE/reference/#processor-overview)部分，了解不同的实现。

### 4.7.2、通过使用 Sink Facade 从多个线程安全地生成

与其直接使用 Reactor Processors，不如通过调用一次 sink()来为处理器获取接收器。

FluxProcessor sinks 安全地访问多线程生产者，并可用于同时从多个线程生成数据的应用程序。例如，可以为 UnicastProcessor 创建线程安全的序列化 sink：
```
UnicastProcessor<Integer> processor = UnicastProcessor.create();
FluxSink<Integer> sink = processor.sink(overflowStrategy);
```
多个生产者线程可以在以下序列化sink 上并发地生成数据：
```
sink.next(n);
```
警告：尽管 FluxSink 适用于 Processor 的多线程手动馈送（feeding），但是将订阅者方法和sink 方法混合是不可能的：你必须将 FluxProcessor 订阅到源发布者，或者通过其 FluxSink 手动馈送它。

方法 next 的溢出行为有两种可能的方式，具体取决于 Processor 及其配置：

    （1）无边界处理器通过删除或缓冲来处理溢出。
    （2）有界处理器在 IGNORE 策略上阻塞或“旋转”，或应用为 sink 指定的 overflowStrategy 行为。

### 4.7.3、可用处理器概览

Reactor 核心有几种不同的 Processor。并不是所有处理器都具有相同的语义，但大致可以分为 3 类。以下列表简要描述了这 3 类处理器：

（1）直接的（direct）(DirectProcessor 和 UnicastProcessor)：这些处理器只能通过直接的用户操作来推送数据（直接调用它们的 sink 方法）。

（2）同步的（synchronous）（EmitterProcessor 和 ReplyProcessor）：这些处理器既可以通过用户交互推送数据，也可以订阅上游的 Publisher 并同步地将其删除。

（3）异步的（asynchronous）（WorkQueueProcessor 和 TopicProcessor）：这些处理器既可以推送从多个上游的 Publishers 获得的数据，也可以推送通过用户交互获得的数据。它们更强健，并由 RingBuffer 数据结构支持，以便处理它们的多个上游流。

异步处理器是最复杂的示例，有许多不同的选项。因此，它们公开了一个 Builder 接口。而更简单的处理器使用静态工厂方法。

**直接处理器（Direct Processor）**

直接处理器是能够将信号发送给一或多个订阅者的处理器。它是最简单的实例化方法，使用单个 DirectProcessor#create() 静态工厂方法。另一个方面，它有不处理背压的局限性。因此，如果你通过 DirectProcessor 推送 N 个元素，但至少有一个订阅者的请求小于 N，则 DirectProcessor 会向其订阅者发出 IllegalStateException 信号。

一旦处理器终止（通常通过调用其 sink 的 error(Throwable) 或 complete() 方法），它会让更多的订阅者订阅，并且立即向它们回放终止信号。

**单播处理器（Unicast Processor）**

单播处理器可以使用内部缓存区来处理背压。代价是它最多只能有一个订阅者。

UnicastProcessor 有更多的选项，反映在一些 create 静态工厂方法上。例如，默认情况下它是无限制的：如果在订阅器还没有请求数据时通过它推送任何数量的数据，它将缓存所有数据。

这可以通过在 create 工厂方法中为内部缓冲提供自定义 Queue 实现来更改。如果该队列是有界的，那么当缓冲区已满并且没有收到下游的足够多的请求时，处理器可以拒绝值的推送。

在这种有界的情况下，处理器还可以通过在每个被拒绝的元素上调用的回调构建，从而允许清除这些被拒绝的元素。

**发射器处理器（Emitter Processor）**

一个发射器处理器能够向多个订阅者发射信号，同时为每个订阅者提供背压。它还可以订阅 Publisher 并同步转发其信号。最初，当它没有订阅者时，它仍然可以接受一些数据，将其推送到可配置的 bufferSize 。在此之后，如果没有订阅者进入并消费数据，则调用 onNext 块，直到处理器耗尽（此时只能并发发生）。

因此，第一个订阅的订阅者在订阅时将接收最多 bufferSize 个元素。但是，在此之后，处理器将停止向其他订阅者回放信号。而这些后续的订阅者只接收订阅后通过处理器推送的信号。内部缓冲区仍用于背压目的。

默认情况下，如果发射器处理器的所有订阅者都被取消（这基本上意味着它们都已取消订阅），它将清除其内部缓冲区并停止接收新的订阅者。这可以通过 create 静态工厂方法中的 autoCancel 参数进行调优。

**回放处理器（Replay Processor）**

回放处理器缓存直接通过其 sink() 方法推送的或来自上游发布者（Publisher）的元素，并向后期的订阅者回放它们。

它可以以多种配置创建：

    （1）缓存单个元素（cacheLast）。
    （2）缓存有限历史（create(int)），无线历史（create()）。
    （3）缓存基于时间的回放窗口（createTimeout(Duration)）。
    （4）缓存历史大小和时间窗口的组合（createSizeOrTimeout(int,Duration)）。

**主题处理器（Topic Processor）**

主题处理器是一种异步处理器，当在 shared 配置中创建时，它能够从多个上游 Publishers 中继元素（请参阅 builder() 的 share(boolean) 选项）。

请注意，如果你打算直接或者从一个并发的上游 Publisher 同时调用 TopicProcessor 的 onNext、onComplete 或 onError 方法，则 share 选项是必需的。

否则，这样的并发调用是非法的，因为处理器是完全符合 Reactive Streams 规范的。

TopicProcessor 能够分散到多个订阅者。它通过将一个线程关联到每个订阅者来做到这一点，这个线程将一直运行，直到一个 onError 或 onComplete 信号被推送到处理器中，或者直到关联的订阅者被取消。下游订阅者的最大数量由 executor 构建器（builder）选项驱动。提供一个有界 ExecutorService， 将其限制为特定的数量。

处理器由存储推送信号的 RingBuffer 数据结构支持。每个订阅者线程都跟踪其关联的需求和 RingBuffer 中的正确索引。

这个处理器还有一个 autoCancel 构建器选项：如果设置为 true(默认值)，它将导致在所有订阅者被取消时源发布者被取消。

**工作队列处理器（WorkQueue Processor）**

工作队列处理器也是一种异步处理器，当在 shared 配置中创建时，它能够中继来自多个上游发布者的元素（它与 TopicProcessor 共享大部分构建器选项）。

它放松了对反应式流规范的遵从，但是它获得了比 TopicProcessor 需要更少资源的好处。它仍然基于一个 RingBuffer ，但是避免了为每个 Subscriber 创建一个消费者线程的开销。因此，它的伸缩性比 TopicProcessor 更好。

代价是它的分布式模式有点不同：来自每个订阅者的请求都加在一起，并且处理器一次只向一个订阅者（Subscriber）传递信号，采用循环分布而不是扇出模式。

注意：不能保证公平的循环分布。

WorkQueueProcessor 的大部分构建器选项与 TopicProcessor 相同，例如：autoCancel、share 和 waitStrategy。下游订阅者的最大数量也是由可配置的带有 executor 选项的 ExecutorService 决定的。

警告：你应当注意不要向 WorkQueueProcessor 订阅太多 Subscribers，因为这样做可能会锁定处理器。如果你需要限定可能的订阅者数量，最好使用 ThreadPoolExecutor 或 ForkJoinPool。这个处理器能够检测它们的容量，并且在你订阅太多次时抛出异常。