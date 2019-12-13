# 7、调试 Reactor

从命令式和同步编程范式转换到反应式和异步编程范式有时会令人望而生畏。学习曲线中最陡峭的步骤之一是如何在出错时进行分析和调试。

在命令式环境中，调试通常是非常简单的：只要阅读 stacetrace，你就会发现问题的根源，以及更多：这完全是代码的失败吗？失败是否发生在某些库代码中？如果是，那么是代码的哪一部分调用了这个库，可能传入了错误的参数，最终导致了失败。

## 7.1、典型的 Reactor 堆栈跟踪

当切换到异步代码时，事情会变得更加复杂。

请考虑以下堆栈跟踪：

一个典型的 Reactor 堆栈跟踪
```
java.lang.IndexOutOfBoundsException: Source emitted more than one item
    at reactor.core.publisher.MonoSingle$SingleSubscriber.onNext(MonoSingle.java:129)
    at reactor.core.publisher.FluxFlatMap$FlatMapMain.tryEmitScalar(FluxFlatMap.java:445)
    at reactor.core.publisher.FluxFlatMap$FlatMapMain.onNext(FluxFlatMap.java:379)
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:121)
    at reactor.core.publisher.FluxRange$RangeSubscription.slowPath(FluxRange.java:154)
    at reactor.core.publisher.FluxRange$RangeSubscription.request(FluxRange.java:109)
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.request(FluxMapFuseable.java:162)
    at reactor.core.publisher.FluxFlatMap$FlatMapMain.onSubscribe(FluxFlatMap.java:332)
    at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onSubscribe(FluxMapFuseable.java:90)
    at reactor.core.publisher.FluxRange.subscribe(FluxRange.java:68)
    at reactor.core.publisher.FluxMapFuseable.subscribe(FluxMapFuseable.java:63)
    at reactor.core.publisher.FluxFlatMap.subscribe(FluxFlatMap.java:97)
    at reactor.core.publisher.MonoSingle.subscribe(MonoSingle.java:58)
    at reactor.core.publisher.Mono.subscribe(Mono.java:3096)
    at reactor.core.publisher.Mono.subscribeWith(Mono.java:3204)
    at reactor.core.publisher.Mono.subscribe(Mono.java:3090)
    at reactor.core.publisher.Mono.subscribe(Mono.java:3057)
    at reactor.core.publisher.Mono.subscribe(Mono.java:3029)
    at reactor.guide.GuideTests.debuggingCommonStacktrace(GuideTests.java:995)
```
那里发生了很多事情。我们得到一个 IndexOutOfBoundsException，它告诉我们”源发出了多个项“。

我们可能很快就可以假设这个源是一个 Flux/Mono，正如下面提到 MonoSingle 的一行所证实的那样。所以这似乎是来自 single 操作符的抱怨。

参考 Mono#single 操作符的 Java 文档（Javadoc），我们看到 single 有一个契约：源必须恰好发出一个元素。看来我们有一个源发出了不止一个，因此违反了该契约。

我们能更深入地挖掘并找出源头吗？以下几行不是很有用。它们通过多次调用 subscribe 和 request ，带我们进入一个似乎是反应式链的内部。

通过浏览这些行，我们至少可以开始形成一幅出问题的链条的图片：它似乎涉及一个 MonoSingle、一个 FluxFlatMap 和一个 FluxRange（每个都在跟踪中得到几行，但总的来说涉及到这三个类）。所以一个 range().flatMap().single() 链可能吗？

但是如果我们在应用程序中经常使用这种模式呢？这仍然不能告诉我们太多，简单地寻找 single 并不能找到问题所在。最后一行是我们的一些代码。最后，我们接近了。

不过，等一下。当我们转到源文件时，我们看到的是一个预先存在的 Flux 被订阅了，如下所示：

toDebug.subscribe(System.out::println, Throwable::printStackTrace);

所有这些都发生在订阅的时候，但是 Flux 自身并没有被声明。更糟糕的是，当我们到变量声明的地方时，我们看到：

public Mono<String> toDebug; //please overlook the public class attribute

该变量在声明的地方没有被实例化。我们必须假设最坏的情况，即我们发现可能有几个不同的代码路径在应用程序中设置它。我们仍然不确定是哪一个引起了这个问题。

注释：这类似于 Reactor 的运行时错误，而不是编译错误。

我们想更容易找出的是操作符在哪里被加入到链中——也就是说，Flux 在哪里被声明。我们通常称之为 Flux 的组装。

## 7.2、激活调试模式

尽管堆栈跟踪仍然能够为有经验的人传递一些信息，但是我们可以看到，在更高级的情况下，它本身并不理想。

幸运的是，Reactor 提供了一个面向调试的装配时仪表功能。

这是通过在应用程序启动时定制 Hooks.onOperator 钩子来完成的（或者至少在引入的 Flux 或 Mono 可以被实例化之前），如下所示：
```
Hooks.onOperatorDebug();
```
这将通过包装操作符的结构并捕获那里的堆栈跟踪来开始检测对 Flux（和 Mono）操作符的调用（在那里它们被组装到链中）。由于这是在声明操作符链时完成的，所以应该在此之前激活钩子，所以最安全的方法是在应用程序开始时激活它。

稍后，如果发生异常，则失败的运算符可以引用该捕获并将其附加到堆栈跟踪。

在下一节中，我们将了解堆栈跟踪的不同之处以及如何解释这些新信息。

## 7.3、在调试模式下读取堆栈跟踪

当我们重用初始示例并且激活 operatorStacktrace 调试功能时，堆栈跟踪如下所示：
```
java.lang.IndexOutOfBoundsException: Source emitted more than one item
    at reactor.core.publisher.MonoSingle$SingleSubscriber.onNext(MonoSingle.java:129)
    at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.onNext(FluxOnAssembly.java:375) 
...

...
    at reactor.core.publisher.Mono.subscribeWith(Mono.java:3204)
    at reactor.core.publisher.Mono.subscribe(Mono.java:3090)
    at reactor.core.publisher.Mono.subscribe(Mono.java:3057)
    at reactor.core.publisher.Mono.subscribe(Mono.java:3029)
    at reactor.guide.GuideTests.debuggingActivated(GuideTests.java:1000)
    Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Assembly trace from producer [reactor.core.publisher.MonoSingle] : 
    reactor.core.publisher.Flux.single(Flux.java:6676)
    reactor.guide.GuideTests.scatterAndGather(GuideTests.java:949)
    reactor.guide.GuideTests.populateDebug(GuideTests.java:962)
    org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:55)
    org.junit.rules.RunRules.evaluate(RunRules.java:20)
Error has been observed by the following operator(s): 
    |_	Flux.single ⇢ reactor.guide.GuideTests.scatterAndGather(GuideTests.java:949) 
```
    （1）这是新的：我们看到捕获堆栈的包装器操作符。
    （2）除此之外，堆栈跟踪的第一部分在大多数情况下仍然是相同的，它显示了一点操作符的内部结构（因此我们在这里删除了一点代码片段）。
    （3）这就是调试模式的新内容开始出现的地方。
    （4）首先，我们得到一些关于操作符是在哪里装配的细节。
    （5）我们还可以从头至尾（从错误位置到订阅位置）在错误通过运算符链传播时获得错误的跟踪。
    （6）看到错误的每个操作符都会与使用错误的用户类和行一起被提到。

如你所见，捕获的堆栈跟踪作为抑制的 OnAssemblyException 附加到原始错误。它有两个部分，但第一部分是最有趣的。它显示了触发异常的操作符的构造路径。在这里，它显示了导致我们问题的 single 是在 scatterAndGather 方法中创建的，其本身是从通过 JUnit 执行的 populateDebug 方法调用的。

现在我们已经掌握了足够的信息来找到罪魁祸首，我们可以对 scatterAndGather 方法进行有意义的研究。
```
private Mono<String> scatterAndGather(Flux<String> urls) {
    return urls.flatMap(url -> doRequest(url))
        .single(); 
}
```
    （1）果然，这是我们的 single。

现在我们可以看到错误的根本原因是执行对几个 URL 的多个 HTTP 调用的 flatMap 和 single 链接在一起，这太过限制了。在简短的指责和与该行作者的快速讨论之后，我们发现他打算使用限制性较小的 take(1) 来代替。

我们已经解决我们的问题。

下列操作符发现了错误：

在本例中，调试堆栈跟踪的第二部分并不一定有趣，因为错误实际上发生在链中的最后一个操作符（最接近 subscribe 的操作符）中。再举一个例子可能会更清楚：
```
FakeRepository.findAllUserByName(Flux.just("pedro", "simon", "stephane"))
    .transform(FakeUtils1.applyFilters)
    .transform(FakeUtils2.enrichUser)
    .blockLast();
```
现在想象一下，在 findAllUserByName 中，有一个失败的映射。在这里，我们将看到以下最后的跟踪：
```
Error has been observed by the following operator(s):
    |_	Flux.map ⇢ reactor.guide.FakeRepository.findAllUserByName(FakeRepository.java:27)
    |_	Flux.map ⇢ reactor.guide.FakeRepository.findAllUserByName(FakeRepository.java:28)
    |_	Flux.filter ⇢ reactor.guide.FakeUtils1.lambda$static$1(FakeUtils1.java:29)
    |_	Flux.transform ⇢ reactor.guide.GuideDebuggingExtraTests.debuggingActivatedWithDeepTraceback(GuideDebuggingExtraTests.java:40)
    |_	Flux.elapsed ⇢ reactor.guide.FakeUtils2.lambda$static$0(FakeUtils2.java:30)
    |_	Flux.transform ⇢ reactor.guide.GuideDebuggingExtraTests.debuggingActivatedWithDeepTraceback(GuideDebuggingExtraTests.java:41)	
```
这对应于得到错误通知的操作符链的一部分：

    （1）异常源于第一个 map。
    （2）可以通过第二个 map 查看它（两者实际上都对应于 findAllUserByName 方法）。
    （3）然后通过 filter 和 transform 来查看它，这表明链的一部分是通过一个可重用的转换函数（这里是 applyFilters 工具方法）构建的。
    （4）最后，通过 elapsed 和 transform 查看它。再一次，第二个转换的转换函数应用了 elapsed。

我们在这里处理某种形式的检测，创建堆栈跟踪的成本很高。这就是为什么作为最后手段，这个调试特性只能以受控的方式激活。

### 7.3.1、checkpoint() 的替代者

调试模式是全局的，它影响到应用程序内组装成 Flux 或 Mono 的每个操作符。这样做的好处是允许事后调试：无论错误是什么，我们都将获得调试它的附加信息。

正如我们前面看到的，这种全局知道是以影响性能为代价的（由于填充的堆栈跟踪的数据）。如果我们知道可能有问题的操作符，那么这个成本可以降低。但是，我们通常不知道哪些运算符可能有问题，除非我们很容易地观察到错误，看到我们失去了装配信息，然后修改代码来激活装配跟踪，希望再次观察到相同的错误。

在这种情况下，我们必须切换到调试模式并做好准备，以便更好地观察错误的第二次发生，这一次捕获所有附加信息。

如果你能够识别在应用程序中组装的、对可维护性至关重要的反应式链，那么可以使用 checkpoint() 操作符来实现这两种技术的混合。

你可以将这个操作符链接到一个方法链中。checkpoint 操作符的工作方式与钩子版本类似，但只是针对特定链的链接。 	

还有一个 checkpoint(String) 变体，它允许你向装配回溯（assembly traceback）添加唯一的字符串标识符。通过这种方式，堆栈跟踪将被省略，你将依赖于描述来标识装配位置。与常规 checkpoint 相比，checkpoint(String) 的处理成本更低。

checkpoint(String) 在其输出中包含“light”（在搜索时很方便），如下所示：
```
...
    Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
Assembly site of producer [reactor.core.publisher.ParallelSource] is identified by light checkpoint [light checkpoint identifier].
```
最后但并非最不重要的一点是，如果你希望向检查点添加更一般的描述，但仍然依赖堆栈跟踪机制来标识装配位置，则可以使用 checkpoint("description",true) 版本强制执行该行为。我们现在返回到回溯的初始消息，并添加了一个 description，如下面示例所示：
```
Assembly trace from producer [reactor.core.publisher.ParallelSource], described as [descriptionCorrelation1234] : 
    reactor.core.publisher.ParallelFlux.checkpoint(ParallelFlux.java:215)
    reactor.core.publisher.FluxOnAssemblyTest.parallelFluxCheckpointDescriptionAndForceStack(FluxOnAssemblyTest.java:225)
Error has been observed by the following operator(s):
    |_	ParallelFlux.checkpoint ⇢ reactor.core.publisher.FluxOnAssemblyTest.parallelFluxCheckpointDescriptionAndForceStack(FluxOnAssemblyTest.java:225) 
```
    （1）descriptionCorrelation1234 是 checkpoint 中提供的描述。

描述可以是静态标识符或用户可读的描述，也可以是更广泛的相关ID（例如，在 HTTP 请求的情况下来自报头）。

注释：当启用全局调试和本地 checkpoint() 时，检查点快照堆栈将作为被抑制的错误输出追加到观察操作符图之后，并遵循相同的声明顺序。

## 7.4、记录序列

除了堆栈跟踪调试和分析之外，工具箱中的另一个强大工具是能够以异步序列跟踪和记录事件。

log() 操作符可以做到这一点。在一个序列中链接，它将查看其上游的 Flux 或 Mono 的每个事件（包括 onNext、onError 和 onComplete 与订阅、取消和请求）。

**关于日志实现的补充说明**

log 操作符使用 Loggers 工具类，该类通过 SLF4J 获取 Log4J 和 Logback 等常用日志框架，并默认在 SLF4J 不可用时记录到控制台。

控制台回退使用 System.err 作为 WARN 和 ERROR 日志级别，使用 System.out 作为其他级别。

如果你喜欢 JDK java.util.logging 回退，如 3.0.x 中所示，可以通过将 reactor.logging.fallback 系统属性设置为 JDK 来获得它。

在所有情况下，在生产环境中进行日志记录时，都应该注意配置底层日志框架，以使用其最异步和非阻塞的方法。例如，logback 中的 AsyncAppender 或 Log4j 2 中的 AsyncLogger。

例如，假设我们激活并配置了logback，并且有一个类似于 range(1,10).take(3) 的链。通过在 take 之前放置一个 log()，我们可以了解它是如何工作的，以及它向上传播到 range 的事件类型，如下面示例所示：
```
Flux<Integer> flux = Flux.range(1, 10)
                         .log()
                         .take(3);
flux.subscribe();
```
打印出来（通过记录器的控制台添加器）：

    10:45:20.200 [main] INFO  reactor.Flux.Range.1 - | onSubscribe([Synchronous Fuseable] FluxRange.RangeSubscription) 
    10:45:20.205 [main] INFO  reactor.Flux.Range.1 - | request(unbounded) 
    10:45:20.205 [main] INFO  reactor.Flux.Range.1 - | onNext(1) 
    10:45:20.205 [main] INFO  reactor.Flux.Range.1 - | onNext(2)
    10:45:20.205 [main] INFO  reactor.Flux.Range.1 - | onNext(3)
    10:45:20.205 [main] INFO  reactor.Flux.Range.1 - | cancel() 

在这里，除了记录器自己的格式器（时间、线程、级别、信息）之外，log() 操作符还以自己的格式输出一些内容：

    （1）reactor.Flux.Range.1 是日志的自动分类，以防在一个链中多次使用操作符。它允许你区分记录了哪些操作符的事件（在本例中是 range）。通过使用 log(String) 方法签名，可以用你自己的自定义类别覆盖标识符。在几个分隔字符之后，实际的事件被打印出来。这里有一个 onSubscribe 调用、一个 request 调用、三个 onNext 调用和一个 cancel 调用。对于第一行 onSubscribe，我们得到 Subscriber 的实现，它通常对应于特定操作符的实现。在方括号内，我们得到了额外的信息，包括是否可以通过同步或异步融合来自动优化操作符。
    （2）在第二行，我们可以看到一个无边界的请求是从下游向上传播的。
    （3）然后这个 range 连续发送 3 个值。
    （4）在最后一行，我们看到了 cancel()。

最后一行，(4)，是最有趣的。我们可以看到 take 在那里活动。它的工作方式是在看到足够多的元素发出之后，将序列缩短。简而言之，take() 一旦发出用户请求的数量，它就会使源调用 cancel()。