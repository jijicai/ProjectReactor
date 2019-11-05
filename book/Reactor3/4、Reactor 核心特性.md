## Reactor 核心特性

    英文原文：https://projectreactor.io/docs/core/3.2.11.RELEASE/reference/#core-features
    
	Reactor 项目的主要产出物是 reactor-core，一个反应式库，它专注于反应式流规范和 目标执行环境 Java 8 。
	
	Reactor 引入了可组合的反应式类型来实现 Publisher 接口，并且提供了丰富的操作符词汇：Flux 和 Mono。
	一个 Flux 对象表示一个有 0-N 项的序列，而一个 Mono 对象表示一个单值或空结果（0-1）。
	
	这种区别将一些语义信息携带到类型中，指示异步处理的粗略基数。
	例如，一个 HTTP 请求只产生一个响应，所以进行计数 count 操作没有多大意义。
	因此，将这种 HTTP 调用的结果表示为 Mono<HttpResponse>，比将其表示为 Flux<HttpResponse> 更有意义，因为它只提供与 0-1 项上下文相关的操作符。
	
	更改处理的最大基数的操作符也切换到相关类型。例如，count 操作符存在于 Flux 中，但它返回一个 Mono<Long>。

	
## Flux 0-N 项的异步序列

![image](https://raw.githubusercontent.com/jijicai/ProjectReactor/master/img/flux.png)

	Flux<T> 是一个标准的 Publishe<T> ，它表示一个发出的 0-N 项的异步序列，它可以被完成信号或错误中断。
	在反应式流规范中，这 3 种类型的信号转换为对下游订阅者的 onNext、onComplete、onError 方法的调用。
	
	由于可能的信号范围很大，Flux 是通用的反应式类型。请注意：所有事件，甚至是终止事件都是可选的：
	除了 onComplete 事件之外，没有 onNext 事件表示空的有限序列，
	但是删除 onComplete后，你将得到一个无限的空序列（除了关于取消的测试之外，这不是特别有用）。
	同样，无限序列不一定是空的。例如，Flux.interval(Duration) 产生一个无限的 Flux<Long> 并从时钟发出规则的滴答声。


## Mono 异步的 0-1 个结果

![image](https://raw.githubusercontent.com/jijicai/ProjectReactor/master/img/mono.png)
	
	Mono<T> 是一个专门的 Publisher<T>，它最多只发出一个数据项的，然后可以选择使用一个 onComplete 或 onError 信号终止它。
	
	它只提供了对 Flux 可用的操作符的一个子集，和一些将其转换为 Flux 的操作符（特别是那些将 Mono 和 另一个 Publisher 结合在一起的操作符）。
	例如，Mono#concatWith(Publisher) 返回一个 Flux，而 Mono#then(Mono) 返回另一个 Mono。
	
	注意，Mono 可以用来表示只有完成概念（completion，类似于 Runnable）的无值的异步进程。要创建一个，请使用一个空 Mono<Void>。


## 创建 Flux 或 Mono 并订阅它的简单方式
	开始使用 Flux 和 Mono 的最简单方法是使用在各自类中找到的众多工厂方法之一。
	
	例如，创建一个 String 序列，可以列举它们，也可以把它们放到集合中，并从中创建Flux。如下：
```
	Flux<String> seq1 = Flux.just("foo", "bar", "foobar");

	List<String> iterable = Arrays.asList("foo", "bar", "foobar");
	Flux<String> seq2 = Flux.fromIterable(iterable);
```
	工厂方法的其他示例包括：
```
	Mono<String> noData = Mono.empty(); 

	Mono<String> data = Mono.just("foo");

	Flux<Integer> numbersFromFiveToSeven = Flux.range(5, 3); 
```
	（1）、即使泛型类型没有值，工厂方法也会使用它。
	（2）、第一个参数是范围的开始，第二个参数是要产生的数据项数目。
	
	当订阅时，Flux 和 Mono 可以利用 Java 8 拉姆达表达式（lambdas）。
	你可以选择多种 subscribe() 方法的变体，这些变体为不同的回调组合使用拉姆达表达式，如以下方法签名所示：
```
	subscribe(); 

	subscribe(Consumer<? super T> consumer); 

	subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer); 

	subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer,Runnable completeConsumer); 

	subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer,
			  Runnable completeConsumer,Consumer<? super Subscription> subscriptionConsumer);
```
	（1）、订阅并触发序列。
	（2）、使用每个产生的值做一些事情。
	（3）、处理值，但也对错误作出反应。
	（4）、处理值和错误，但也在序列成功完成时执行一些代码。
	（5）、处理值、错误和成功完成，但也要处理此订阅调用生成的订阅。
	
	提示：
	这些变体方法返回对订阅的引用，当不需要更多的数据时，你可以使用它取消订阅。
	取消后，数据源应当停止产生值，并清除掉创建的任何资源。
	在 Reactor 中，这种取消和清理行为在 Reactor 中由通用的一次性接口表示。
	
### subscribe 方法示例
	本小节包含了 subscribe 方法的五个签名中的每个签名的最小示例。下面的代码展示了一个无参基本方法的示例。
```
	Flux<Integer> ints = Flux.range(1, 3); 
	ints.subscribe(); 
```
	（1）、创建一个 Flux 对象，当订阅时，它可以产生 3 个值。
	（2）、以最简单的方式订阅。
	
	上面的代码没有产生任何可见的输出，但是它确实工作了。这 Flux 产生了 3 个值。
	如果我们提供一个拉姆达表达式（lambdas），我们就能看到这些值的输出。
	subscribe 方法的下一个例子展示了显示值的一种方法：
```
	Flux<Integer> ints = Flux.range(1, 3); 
	ints.subscribe(i -> System.out.println(i)); 
```
	（1）、创建一个 Flux 对象，当订阅时，它可以产生 3 个值。
	（2）、订阅者订阅后，将打印出这些值。（使用可以打印值的订阅者订阅）
	
	上面的代码产生了以下输出：
```
	1
	2
	3
```
	为了演示下一个签名方法，我们故意引入一个错误，如下面的示例所示：
```
	Flux<Integer> ints = Flux.range(1, 4) 
		  .map(i -> { 
			if (i <= 3) return i; 
			throw new RuntimeException("Got to 4"); 
		  });
	ints.subscribe(i -> System.out.println(i), 
		  error -> System.err.println("Error: " + error));
```
	（1）、创建一个 Flux 对象，当订阅时，它可以产生 3 个值。
	（2）、我们使用了 map 方法，这样我们就可以以不同的方式处理这些值。
	（3）、对于大部分值，返回值本身。
	（4）、对于一个值，将强制产生错误。
	（5）、使用包含错误处理程序的订阅者订阅。
	
	现在我们有 2 个拉姆达表达式：一个用于我们期望的内容、另一个用于错误。上述代码产生以下输出：
```
	1
	2
	3
	Error: java.lang.RuntimeException: Got to 4
```
	下一个 subscribe 签名方法包括一个错误处理程序和一个用于完成事件的处理程序，如下面的示例所示：
```
	Flux<Integer> ints = Flux.range(1, 4); 
	ints.subscribe(i -> System.out.println(i),
		error -> System.err.println("Error " + error),
		() -> System.out.println("Done"));
```
	（1）、创建一个 Flux 对象，当订阅时，它可以产生 3 个值。
	（2）、使用包含完成事件的处理程序的订阅者订阅。
	
	错误信号和完成信号都是终止事件，并且彼此是互斥的（你从不会同时得到它们）。
	要完成消费者工作，我们必须注意不要触发错误。
	
	完成（completion）回调没有输入，由一对空括号表示：它匹配 Runnable 接口中的 run 方法。
	上面的代码生成以下输出：
```
	1
	2
	3
	4
	Done
```
	最后一个 subscribe 的签名方法包括一个 Consumer<Subscription>。
	该变体需要你对订阅（对其执行一个 request(long)，或 cancel() ） 执行一些操作，否则 Flux 将挂起：
```
	Flux<Integer> ints = Flux.range(1, 4);
	ints.subscribe(i -> System.out.println(i),
		error -> System.err.println("Error " + error),
		() -> System.out.println("Done"),
		sub -> sub.request(10)); 
```
	（1）、当我们订阅时，我们收到一个订阅。发出信号，表示我们需要从源获取最多 10 个元素（该源实际将发出 4 个元素就完成了）。
	
### 用 Disposable 取消订阅（Cancelling a subscribe() with its Disposable）
	
	subscribe() 方法的所有这些基于拉姆达表达式的变体都有一个 Disposable 返回类型。
	在本例中，Disposable 接口表示可以通过调用其 dispose() 方法取消订阅。
	
	对于 Flux 或 Mono，取消是源应该停止产生元素的信号。
	但是，不能保证它是及时的：有些源可能生成元素的速度非常快，甚至可以在收到取消指令之前完成。

	在 Disposables 类中有一些实用的工具方法。
	其中，Disposable.swap() 可以创建一个 Disposable 包裹，允许你自动地取消并替换一个具体的 Disposable。
	例如，在一个 UI 场景中，当用户点击某个按钮时，你想取消一个请求并用一个新的替换它，这可能是有用的。
	释放包装器本身将关闭它，释放当前的具体值和将来尝试的所有替换。
	
	另一个有趣的工具方法是 Disposables.composite(...)。这个 composite 方法允许收集几个 Disposable，
	例如，与服务调用关联的多个执行中的请求，并在稍后立即处理所有这些请求。
	一旦调用了 Composite 接口的 dispose() 方法，任何添加另一个 Disposable 的尝试都会立即处理该方法。
	
### lambdas 表达式的替代者：BaseSubscriber（Alternative to lambdas: BaseSubscriber）
	
	
	另外还有一个更通用的 subscribe 方法，它采用成熟的 Subscriber，而不是从拉姆达表达式中组合一个。
	为了帮助你编写这样的一个 Subscriber，我们提供了一个可扩展的类 BaseSubscriber。
	
	让我们实现其中一个，类名为：SampleSubscriber。下面的例子展示了它是如何与 Flux 关联的。
```
	SampleSubscriber<Integer> ss = new SampleSubscriber<Integer>();
	Flux<Integer> ints = Flux.range(1, 4);
	ints.subscribe(i -> System.out.println(i),
		error -> System.err.println("Error " + error),
		() -> {System.out.println("Done");},
		s -> s.request(10));
	ints.subscribe(ss);
```
	
	现在让我们看一下 SampleSubscriber 是什么样子的，作为 BaseSubscriber 类的一个最小化实现。
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
	SampleSubscriber 类继承了 BaseSubscriber ,它是 Reactor 中用户定义的 Subscribers 推荐的抽象类。
	该类提供了可以被覆盖的钩子来调优订阅者的行为。默认情况下，它将触发一个无界请求，其行为与 subscribe() 完全相同。
	然而，当你需要自定义请求量时，继承 BaseSubscriber 要有用得多。
	
	对于自定义请求量，最基本的要求是像我们一样实现 hookOnSubscribe(Subscription subscription) 和 hookOnNext(T value) 。
	在我们的例子中，hookOnSubscribe 方法打印一条语句到标准输出并发出第一个请求。
	然后，hookOnNext 方法打印一条语句并执行其他的请求，每次一个请求。
	
	SampleSubscriber 类生成以下输出：
```
	Subscribed
	1
	2
	3
	4
```
	BaseSubscriber 也提供一个 requestUnbounded() 方法用于切换到无边界模式（相当于 request(Long.MAX_VALUE)），以及一个 cancel() 方法。
	
	还有其他的钩子：hookOnComplete、hookOnError、hookOnCancel 和 hookFinally
	（序列终止时总是调用这个方法，并将终止类型作为 SignalType 参数传入）。
	
	注意：
	你很可能想要实现 hookOnError、hookOnCancel 和 hookOnComplete方法。
	你也许也想要实现 hookFinally 方法。SampleSubscribe 是执行有界请求的 Subscriber 的绝对最小实现。
	
### 关于背压，以及更改请求的方式
	
	在 Reactor 中实现背压时，消费者压力传播回源的方式是向上游操作符发送请求。
	当前请求之和有时候被引用为当前“请求”或“挂起的请求”。请求量的上限是 Long.MAX_VALUE，表示一个无界的请求（尽可能快地生成数据，基本可以防止背压）。
	
	第一个请求来自最终订阅者，在订阅时，但是最直接的订阅方式都会立即触发一个无界请求（Long.MAX_VALUE）。
	（1）subscribe() 和大部分基于拉姆达表达式的变体（具有 Consumer<Subscription> 参数的变体除外）
	（2）block()、blockFirst() 和 blockLast()
	（3）使用方法 toIterable() 和 toStream() 执行迭代操作。
	
	自定义原始请求的最简单方式是用带有 hookOnSubscribe 重写方法的BaseSubscriber 订阅：
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
	当操作一个请求时，你必须小心地产生足够的请求，以使序列前进，否则 Flux 将会“卡住”。
	这就为什么 BaseSubscriber 在 hookOnSubscribe 方法中默认是无界请求的原因。
	当覆盖这个 hook 方法时，你通常应该至少调用一次 request 方法。
	
	操作符改变下游的需求
	
	要记住的一点是，在订阅级别上表达的请求可以被上游链条中每个操作符重新调整。
	教科书中案例是 buffer(N) 操作符：如果它收到一个 request(2)，它会被解释为对两个完整缓存区的请求。
	因此，由于缓冲区需要将 N 个元素视为已满，因此缓冲区操作符将请求改造为 2*N。
	
	你可能还注意到，有些操作符的变体接受一个名为 prefetch 方法的 int 型输入参数。
	这是修改下游请求的另一个一类操作符。这些通常处理内部序列的操作符，从每个传入元素（像 flatMap 方法）派生一个 Publisher。
	
	预取（Prefetch）是一种调整对这些内部序列发出的初始化请求的方法。如果未指定，大多数操作符以 32个请求为开始。
	
	这些操作符通常还实现一个补充优化：一旦操作符看到 25% 的预取请求完成，它就会从上游再请求 25%。
	这是一种启发式优化，使这些操作符能够主动预测上游请求。
	
	最后，有一对操作符可以让你直接调整请求：limitRate 和 limitRequest。
	
	limitRate(N) 拆分下游的请求，以便它们以较小的批量向上游传播。
	例如，向 limitRate(10) 发出 100 个请求，最多会将10个请求传播到上游。（a request of 100 made to limitRate(10) would result in at most 10 requests of 10 being propagated to the upstream.）
	注意，在这种形式中，limitRate 实际上实现了上面讨论的补充优化。
	
	这个操作符有一个变体，它允许你调整补充数量，在变体中被称为 lowTide：limitRate(highTide,lowTide)。
	选择 lowTide 为 0 ,这会导致严格的 highTide 批量的请求，而不是通过补充策略进一步返工的批次。
	
	另一方面，limitRequest(N) 将下游请求限制为最大总请求。它将请求相加到 N。
	如果单个请求没有使总需求超过 N，则该特定请求将完全向上游传播。
	在源发出该数量后，limitRequest 方法会考虑序列完成，并向下游发送一个 onComplete 信号，以取消源。



……………………

## 处理器（Processors）

处理器是一种特殊的 Publisher，也是 Subscriber。这意味着你可以订阅 Processor(通常，它们实现 Flux),但是你也可以调用方法来手动将数据注入序列或者终止它。

有几种处理器，每种都有一些特定的语义，但是在你开始研究这些处理器之前，你需要问自己以下问题：

### 你需要处理器吗？

大多数时候，你应该尽量避免使用 Processor。它们很难正确使用，而且容易出现一些死角情况。

如果你认为 Processor 可以很好地与你的用例匹配，那么问问你自己是否尝试过这两种方法：
（1）一个操作符或多个操作符的组合是否符合要求？（查看：我需要哪个操作符？）
（https://projectreactor.io/docs/core/release/reference/#which-operator）
（2）“generator”操作符能代替它工作吗？（通常，这些操作符是用来桥接非反应式 APIs 的，它提供了一个“sink”，在某种意义上，它类似于 Processor，允许你用数据手动填充序列或终止序列。）

如果在研究了上述替代方案之后，你仍然认为需要一个 Processor，请阅读下面的可用处理器概述部分，了解不同的实现。

### 通过使用 Sink Facade 从多个线程安全地生成

与其直接使用 Reactor Processors，不如通过调用一次 sink()来为处理器获取接收器。

FluxProcessor sinks 安全地访问多线程生产者，并可用于同时从多个线程生成数据的应用程序。例如，可以为 UnicastProcessor 创建线程安全的序列化 sink。

UnicastProcessor<Integer> processor = UnicastProcessor.create();
FluxSink<Integer> sink = processor.sink(overflowStrategy);

多个生产者线程可以在以下序列化sink 上并发地生成数据。
sink.next(n);

警告：尽管 FluxSink 适用于 Processor 的多线程手动馈送（feeding），但是将订阅者方法和sink 方法混合是不可能的：你必须将 FluxProcessor 订阅到源发布者，或者通过其 FluxSink 手动馈送它。

方法 next 的溢出行为有两种可能的方式，具体取决于 Processor 及其配置：
（1）无边界处理器通过删除或缓冲来处理溢出。
（2）有界处理器在 IGNORE 策略上阻塞或“旋转”，或应用为 sink 指定的 overflowStrategy 行为。

### 可用处理器概览

Reactor 核心有几种不同的 Processor。并不是所有处理器都具有相同的语义，但大致可以分为 3 类。以下列表简要描述了这 3 类处理器：
（1）直接的（direct）(DirectProcessor 和 UnicastProcessor)：这些处理器只能通过直接的用户操作来推送数据（直接调用它们的 sink 方法）。
（2）同步的（synchronous）（EmitterProcessor 和 ReplyProcessor）：这些处理器既可以通过用户交互推送数据，也可以订阅上游的 Publisher 并同步地将其删除。
（3）异步的（asynchronous）（WorkQueueProcessor 和 TopicProcessor）：这些处理器既可以推送从多个上游的 Publishers 获得的数据，也可以推送通过用户交互获得的数据。它们更强健，并由 RingBuffer 数据结构支持，以便处理它们的多个上游流。

异步处理器是最复杂的示例，有许多不同的选项。因此，它们公开了一个 Builder 接口。而更简单的处理器使用静态工厂方法。

提示：
发送事件到不同线程的方法之一是使用与 publishOn(Scheduler) 相结合的 EmitterProcessor。例如，这可以取代以前的 TopicProcessor，后者使用 Unsafe 操作，并在 3.3.0 中被转移到 reactor-extra 。

直接处理器（Direct Processor）

直接处理器是能够将信号发送给一或多个订阅者的处理器。它是最简单的实例化方法，使用单个 DirectProcessor#create() 静态工厂方法。另一个方面，它有不处理背压的局限性。因此，如果你通过 DirectProcessor 推送 N 个元素，但至少有一个订阅者的请求小于 N，则 DirectProcessor 会向其订阅者发出 IllegalStateException 信号。

一旦处理器终止（通常通过调用其 sink 的 error(Throwable) 或 complete() 方法），它会让更多的订阅者订阅，并且立即向它们回放终止信号。

单播处理器（Unicast Processor）

单播处理器可以使用内部缓存区来处理背压。代价是它最多只能有一个订阅者。

UnicastProcessor 有更多的选项，反映在一些 create 静态工厂方法上。例如，默认情况下它是无限制的：如果在订阅器还没有请求数据时通过它推送任何数量的数据，它将缓存所有数据。

这可以通过在 create 工厂方法中为内部缓冲提供自定义 Queue 实现来更改。如果该队列是有界的，那么当缓冲区已满并且没有收到下游的足够多的请求时，处理器可以拒绝值的推送。

在这种有界的情况下，处理器还可以通过在每个被拒绝的元素上调用的回调构建，从而允许清除这些被拒绝的元素。

发射器处理器（Emitter Processor）

一个发射器处理器能够向多个订阅者发射信号，同时为每个订阅者提供背压。它还可以订阅 Publisher 并同步转发其信号。最初，当它没有订阅者时，它仍然可以接受一些数据，将其推送到可配置的 bufferSize 。在此之后，如果没有订阅者进入并消费数据，则调用 onNext 块，直到处理器耗尽（此时只能并发发生）。

因此，第一个订阅的订阅者在订阅时将接收最多 bufferSize 个元素。但是，在此之后，处理器将停止向其他订阅者回放信号。而这些后续的订阅者只接收订阅后通过处理器推送的信号。内部缓冲区仍用于背压目的。

默认情况下，如果发射器处理器的所有订阅者都被取消（这基本上意味着它们都已取消订阅），它将清除其内部缓冲区并停止接收新的订阅者。这可以通过 create 静态工厂方法中的 autoCancel 参数进行调优。

回放处理器（Replay Processor）

回放处理器缓存直接通过其 sink() 方法推送的或来自上游发布者（Publisher）的元素，并向后期的订阅者回放它们。

它可以以多种配置创建：
（1）缓存单个元素（cacheLast）。
（2）缓存有限历史（create(int)），无线历史（create()）。
（3）缓存基于时间的回放窗口（createTimeout(Duration)）。
（4）缓存历史大小和时间窗口的组合（createSizeOrTimeout(int,Duration)）。

主题处理器（Topic Processor）

主题处理器是一种异步处理器，当在 shared 配置中创建时，它能够从多个上游 Publishers 中继元素（请参阅 builder() 的 share(boolean) 选项）。

请注意，如果你打算直接或者从一个并发的上游 Publisher 同时调用 TopicProcessor 的 onNext、onComplete 或 onError 方法，则 share 选项是必需的。

否则，这样的并发调用是非法的，因为处理器是完全符合 Reactive Streams 规范的。

TopicProcessor 能够分散到多个订阅者。它通过将一个线程关联到每个订阅者来做到这一点，这个线程将一直运行，直到一个 onError 或 onComplete 信号被推送到处理器中，或者直到关联的订阅者被取消。下游订阅者的最大数量由 executor 构建器（builder）选项驱动。提供一个有界 ExecutorService， 将其限制为特定的数量。

处理器由存储推送信号的 RingBuffer 数据结构支持。每个订阅者线程都跟踪其关联的需求和 RingBuffer 中的正确索引。

这个处理器还有一个 autoCancel 构建器选项：如果设置为 true(默认值)，它将导致在所有订阅者被取消时源发布者被取消。

工作队列处理器（WorkQueue Processor）

工作队列处理器也是一种异步处理器，当在 shared 配置中创建时，它能够中继来自多个上游发布者的元素（它与 TopicProcessor 共享大部分构建器选项）。

它放松了对反应式流规范的遵从，但是它获得了比 TopicProcessor 需要更少资源的好处。它仍然基于一个 RingBuffer ，但是避免了为每个 Subscriber 创建一个消费者线程的开销。因此，它的伸缩性比 TopicProcessor 更好。

代价是它的分布式模式有点不同：来自每个订阅者的请求都加在一起，并且处理器一次只向一个订阅者（Subscriber）传递信号，采用循环分布而不是扇出模式。

注意：不能保证公平的循环分布。

WorkQueueProcessor 的大部分构建器选项与 TopicProcessor 相同，例如：autoCancel、share 和 waitStrategy。下游订阅者的最大数量也是由可配置的带有 executor 选项的 ExecutorService 决定的。

警告：你应当注意不要向 WorkQueueProcessor 订阅太多 Subscribers，因为这样做可能会锁定处理器。如果你需要限定可能的订阅者数量，最好使用 ThreadPoolExecutor 或 ForkJoinPool。这个处理器能够检测它们的容量，并且在你订阅太多次时抛出异常。



