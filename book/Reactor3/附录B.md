# 附录 B：FAQ、最佳实践和“我该如何...？”

## B.1、如何包装同步阻塞调用？

通常情况下，信息源是同步和阻塞的。在 Reactor 应用程序中要处理此类源，请应用以下模式：
```
Mono blockingWrapper = Mono.fromCallable(() -> { 
    return /* make a remote synchronous call */ 
});
blockingWrapper = blockingWrapper.subscribeOn(Schedulers.elastic()); 
```
    （1）使用 fromCallable 创建一个新的 Mono。
    （2）返回异步阻塞资源。
    （3）确保每个订阅都发生在 Schedulers.elastic() 的专用单线程工作进程上。

你应该使用 Mono，因为源返回一个值。你应该使用 Schedulers.elastic，因为它创建了一个专用线程来等待阻塞资源，而不占用其他资源。

注意 subscribeOn 不订阅 Mono。它指定发生订阅调用时使用哪种 Scheduler。

## B.2、我在我的 Flux 上使用了操作符但好像没有应用。出了什么事？

确保 .subscribe() 的变量已经受到你认为应该应用到它的操作符的影响。

Reactor 操作符是装饰者。它们返回一个包装源序列并添加行为的不同实例。这就是为什么使用操作符的首选方法是链接调用。

比较以下两个示例：

没有链接(错误的)
```
Flux<String> flux = Flux.just("foo", "chain");
flux.map(secret -> secret.replaceAll(".", "*")); 
flux.subscribe(next -> System.out.println("Received: " + next));
```
（1）错误就在这里。结果没有附加到 flux 变量。

没有链接(正确的)
```
Flux<String> flux = Flux.just("foo", "chain");
flux = flux.map(secret -> secret.replaceAll(".", "*"));
flux.subscribe(next -> System.out.println("Received: " + next));
```
这个示例甚至更好（因为它更简单）：

用链接(最好的)
```
Flux<String> secrets = Flux
  .just("foo", "chain")
  .map(secret -> secret.replaceAll(".", "*"))
  .subscribe(next -> System.out.println("Received: " + next));
```
第一个版本将输出：

    Received: foo
    Received: chain

而其他两个版本将输出预期的：

    Received: ***
    Received: *****

## B.3、我的 Mono zipWith/zipWhen 从未被调用

例子
```
myMethod.process("a") // this method returns Mono<Void>
        .zipWith(myMethod.process("b"), combinator) //this is never called
        .subscribe();
```
如果源 Mono 是空的或 Mono<Void>（Mono<Void> 对于所有意图和目的都是空的），则永远不会调用某些组合。

对于 zip 静态方法或 zipWith/zipWhen 操作符之类的任何转换器，这都是典型的情况，根据定义，这些运算符需要来自每个源的元素来生成其输出。

因此，在 zip 源上使用数据抑制操作符是有问题的。数据抑制操作符的示例包括：then()、thenEmpty(Publisher<Void>)、ignoreElements() 和 ignoreElement()、when(Publisher…)。

类似地，使用 Function<T,?> 来调整其行为的操作符，如 flatMap，确实需要至少发出一个元素才能使 Function 有机会应用。在空序列（或 <Void>）上应用这些函数永远不会生成元素。

你可以使用 .defaultIfEmpty(T) 和 .switchIfEmpty(Publisher<T>) 分别用默认值或回退 Publisher<T> 替换空的 T 序列，这有助于避免某些情况。注意，这不适用于 Flux<Void>/Mono<Void> 源，因为你只能切换到另一个 Publisher<Void>，它仍然保证为空。下面是 defaultIfEmpty 的一个示例：

在 zipWhen 之前使用 defaultIfEmpty
```
myMethod.emptySequenceForKey("a") // this method returns empty Mono<String>
        .defaultIfEmpty("") // this converts empty sequence to just the empty String
        .zipWhen(aString -> myMethod.process("b")) //this is called with the empty String
        .subscribe();
```
## B.4、如何使用 retryWhen 模拟 retry(3)

retryWhen 操作符可能相当复杂。希望这段代码可以通过尝试模拟更简单的 retry(3) 来帮助你理解它是如何工作的：
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
    （1）技巧一：使用 zip 和“可接受重试次数+1”的范围。
    （2）zip 函数允许你计算重试次数，同时跟踪原始错误。
    （3）为了允许三次重试，4 之前的索引返回一个要发出的值。
    （4）为了错误地终止序列，我们在这三次重试后抛出原始异常。

## B.5、如何使用 retryWhen 指数退避？

指数退避会产生重试尝试，每次尝试之间的延迟会越来越大，这样就不会使源系统过载，并可能导致全面崩溃。其理由是，如果源产生错误，则它已经处于不稳定状态，不太可能立即从中恢复。因此，盲目地立即重试可能会产生另一个错误，并增加不稳定性。

自 3.2.0.RELEASE 以来，Reactor 提供了这样一个重新烘焙（retry baked）：Flux.retryBackoff。

对于好奇的人来说，这里是如何用 retryWhen 实现指数退避，它延迟重试并增加每次尝试之间的延迟（伪代码: 延迟 = 尝试数 * 100 毫秒）：
```
Flux<String> flux =
Flux.<String>error(new IllegalArgumentException())
    .retryWhen(companion -> companion
        .doOnNext(s -> System.out.println(s + " at " + LocalTime.now())) 
        .zipWith(Flux.range(1, 4), (error, index) -> { 
          if (index < 4) return index;
          else throw Exceptions.propagate(error);
        })
        .flatMap(index -> Mono.delay(Duration.ofMillis(index * 100))) 
        .doOnNext(s -> System.out.println("retried at " + LocalTime.now())) 
    );
```
    （1）我们记录错误发生的时间。
    （2）我们使用 retryWhen + zipWith 技巧在 3 次重试后传播错误。
    （3）通过 flatMap，我们造成的延迟取决于尝试的索引。
    （4）我们还记录重试发生的时间。

当订阅时，这将失败并在打印出来后终止：

    java.lang.IllegalArgumentException at 18:02:29.338
    retried at 18:02:29.459 
    java.lang.IllegalArgumentException at 18:02:29.460
    retried at 18:02:29.663 
    java.lang.IllegalArgumentException at 18:02:29.663
    retried at 18:02:29.964 
    java.lang.IllegalArgumentException at 18:02:29.964
```
（1）大约 100 毫秒后首次重试
（2）大约 200 毫秒后的第二次重试
（3）大约 300 毫秒后第三次重试
```
## B.6、如何使用 publishOn() 确保线程关联性？

如调度程序中所述，publishOn() 可用于切换执行上下文。publishOn 操作符影响线程上下文，它下面的链中的其他操作符将在其中执行，直到 publishOn 的新出现。因此，publishOn 的位置是重要的。

例如，在下面的示例中，map() 中的 transform 函数是在 scheduler1 的工作线程上执行的，而 doOnNext() 中的 processNext 方法是在 scheduler2 的工作线程上执行的。每个 subscription 都有自己的工作线程，因此推送到相应订阅服务器的所有元素都发布在相同 Thread 上。

单线程调度程序可用于确保链中不同阶段或不同订阅者的线程相关性。
```
EmitterProcessor<Integer> processor = EmitterProcessor.create();
processor.publishOn(scheduler1)
         .map(i -> transform(i))
         .publishOn(scheduler2)
         .doOnNext(i -> processNext(i))
         .subscribe();
```