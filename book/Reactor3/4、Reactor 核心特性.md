## Reactor 核心特性

    英文原文：https://projectreactor.io/docs/core/release/reference/#core-features
    
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