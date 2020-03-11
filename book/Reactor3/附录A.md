# 附录 A：我需要哪个操作符？

提示：在本节中，如果操作符是特定于 Flux 或 Mono 的，则相应地为其加前缀。常用操作符没有前缀。当一个特定的用例被操作符的组合覆盖时，它会以方法调用的形式呈现，带有前导点和在括号中的参数，如下所示：.methodCall(parameter)。

我想处理：

    （1）创建新序列…
    （2）转换现有序列
    （3）窥视序列
    （4）过滤序列
    （5）处理错误
    （6）处理时间
    （7）拆分 Flux
    （8）回到同步世界
    （9）向多个 Subscribers 多播 Flux

## A.1、创建新序列…

* 发出 T，已经有了：just

    - …从一个 Optional<T>: Mono#justOrEmpty(Optional<T>)
    - …从一个可能为 null 的 T：Mono#justOrEmpty(T)

* 发出方法返回的 T：还是 just

    - …但是赖捕获：使用 Mono#fromSupplier 或将 just 包装在 defer 中

* 发出可以显式地列举的几个 T：Flux#just(T…)

* 遍历它们的：

    - 一个数组： Flux#fromArray
    - 一个集合或可迭代对象：Flux#fromIterable
    - 一系列整数： Flux#range
    - 为每个 Subscription 提供 Stream：Flux#fromStream(Supplier<Stream>)

* 从各种单值源发出的，例如：

    - Supplier<T>: Mono#fromSupplier
    - 一个任务：Mono#fromCallable, Mono#fromRunnable
    - CompletableFuture<T>: Mono#fromFuture

* 完成的：empty

* 立即出错的：error

    - …但延迟构建 Throwable: error(Supplier<Throwable>)

* 不做任何事情的：never

* 在订阅时决定的：defer

* 依赖于一次性资源的：using

* 以编程方式生成事件的（可以使用状态）：

    - 同步地和逐个地：Flux#generate
    - 异步地（也可以是同步），一次可以产生多个发射：Flux#create（Mono#create 也一样，不会产生多个发射）

## A.2、转换现有序列

* 我要转换现有数据：

    - 1 到 1（例如字符串到它们的长度）：map
        - …通过强制转换它：cast
        - …为了具体化每个源值的索引：Flux#index
    - 在 1 到 n 的基础上（例如字符串到它们的字符）：flatMap + 使用工厂方法
    - 在 1 到 n 的基础上，对每个源元素和/或状态执行编程行为：handle
    - 为每个源项运行异步任务（如 http 请求的 url）：flatMap + 异步 Publisher 返回方法
        - …忽略一些数据：在 flatMap lambda 中有条件地返回 Mono.empty()
        - …保留原始序列顺序：Flux#flatMapSequential（这会立即触发异步进程，但会重新排序结果）
        - …异步任务可以从一个 Mono 源返回多个值：Mono#flatMapMany

* 我要将预设元素添加到现有序列：

    - 在开始位置：Flux#startWith(T…)
    - 在结尾位置：Flux#concatWith(T…)

* 我想聚合一个 Flux（以下假定具有Flux# 前缀）：

    - 成为一个 List：collectList、collectSortedList
    - 成为一个 Map：collectMap、collectMultiMap
    - 成为一个任意容器：collect
    - 成为该序列的大小：count
    - 通过在每个元素之间应用一个函数（例如运行 sum）:reduce
        - …但发出每个中间值：scan
    - 从谓词转换为布尔值：
        - 应用于所有值（AND）:all
        - 应用于至少一个值（OR）：any
        - 测试是否存在任何值：hasElements
        - 测试是否存在特定值：hasElement

* 我想合并多个发布者…

    - 按顺序：Flux#concat 或 .concatWith(other)
        - …但在发出剩余的发布者之前延迟任何错误：Flux#concatDelayError
        - …但急切地订阅后续的发布者：Flux#mergeSequential
    - 按发射顺序（组合项到达时发射）：Flux#merge / .mergeWith(other)
        - …具有不同类型（转换合并）：Flux#zip / Flux#zipWith
    - 通过配对值：
        - 从 2 个 Monos 到一个 Tuple2：Mono#zipWith
        - 从 n 个 Monos 完成时：Mono#zip
    - 通过协调其终止：
        - 从一个 Mono 和任意源到一个 Mono<Void>：Mono#and
        - 从 n 个源完成时：Mono#when
        - 成为一个任意容器类型：
            - 每次所有边都发出：Flux#zip（直到最小基数）
            - 每次一个新值到达任意一侧：Flux#combineLatest
    - 仅考虑最先发射的序列：Flux#first、Mono#first、mono.or (otherMono).or(thirdMono)、flux.or(otherFlux).or(thirdFlux)
    - 由源序列中的元素触发：switchMap（每个源元素都映射到 Publisher）
    - 由发布者序列中的下一个发布者的启动触发：switchOnNext

* 我想重复一个现有的序列：repeat

    - …但在时间间隔：Flux.interval(duration).flatMap(tick → myExistingPublisher)

* 我有一个空序列但是…

* 我想要一个值：defaultIfEmpty
* 我想要另一个序列：switchIfEmpty

* 我有一个序列，但我对值不感兴趣：ignoreElements

    - …并且我想把完成过程表示成一个 Mono：then
    - …并且我想等待另一项任务在最后完成：thenEmpty
    - …并且我想在最后切换到另一个 Mono：Mono#then(mono)
    - …并且我想在最后发出一个值：Mono#thenReturn(T)
    - …并且我想在最后切换到一个 Flux：thenMany

* 我有一个 Mono，我想推迟完成…

    - …直到从该值派生的另一个发布者完成：Mono#delayUntil(Function)

* 我想将元素递归地扩展成一个序列图，并发出组合…

    - …先扩展图形宽度：expand(Function)
    - …先扩展图形深度：expandDeep(Function)

## A.3、窥视序列

* 在不修改最终序列的情况下，我想：

    - 得到通知/执行额外的行为在：
        - 排放：doOnNext
        - 完成：Flux#doOnComplete, Mono#doOnSuccess（如果有，包括结果）
        - 错误终止：doOnError
        - 取消：doOnCancel
        - 序列的“开始”：doFirst
            - 这绑定到 Publisher#subscribe(Subscriber)
        - 订阅（如 subscribe 后的 Subscription 确认）：doOnSubscribe **（绑定到 Subscriber#onSubscribe(Subscription)）
        - 请求：doOnRequest
        - 完成或错误：doOnTerminate（Mono 版本包括结果，如果有的话）
            - 但在它被传播到下游之后：doAfterTerminate
        - 任何类型的信号，表示为一个 Signal：Flux#doOnEach
        - 任何终止条件（完成、错误、取消）：doFinally
        - 记录内部发生的事情：log

* 我想知道所有的事件：

    - 每个都表示为 Signal 对象：
        - 在序列外的回调中：doOnEach
        - 代替原始的 onNext 排放：materialize
            - …并且回到 onNexts：dematerialize
    - 作为日志中的一行：log

## A.4、过滤序列

* 我想过滤一个序列：

    - 基于任意条件：filter
        - …这是异步计算的：filterWhen
    - 限制发出对象的类型：ofType
    - 完全忽略这些值：ignoreElements
    - 忽略重复项：
        - 在整个序列中（逻辑集）：Flux#distinct
        - 在随后发出的项之间（重复数据消除）：Flux#distinctUntilChanged

* 我只想保留序列的一个子集：

    - 取 N 个元素：
        - 在序列的开始：Flux#take(long)
            - …基于持续时间：Flux#take(Duration)
            - …只有第一个元素，作为Mono： Mono: Flux#next()
            - …使用 request(N) 而不是取消：Flux#limitRequest(long)
        - 在序列的结尾：Flux#takeLast
        - 直到满足一个条件（包括）：Flux#takeUntil（基于谓词），Flux#takeUntilOther（基于发布者的同伴）
        - 当满足一个条件时（不包括）：Flux#takeWhile
    - 最多取1个元素：
        - 在特定位置：Flux#elementAt
        - 在结尾：.takeLast(1)
            - …如果为空则发出错误：Flux#last()
            - …如果为空则发出默认值：Flux#last(T)
    - 通过跳过元素：
        - 在序列的开头：Flux#skip(long)
            - …基于持续时间：Flux#skip(Duration)
        - 在序列的末尾：Flux#skipLast
        - 直到满足条件（包括）：Flux#skipUntil（基于谓词）、Flux#skipUntilOther（基于发布者的同伴）
        - 当满足一个条件时（不包括）：Flux#skipWhile
    - 通过抽样数据项：
        - 按持续时间：Flux#sample(Duration)
            - 但是在采样窗口中保留第一个元素而不是最后一个：sampleFirst
        - 通过基于发布者的窗口：Flux#sample(Publisher)
        - 基于发布者“超时”：Flux#sampleTimeout（每个元素触发一个发布者，如果该发布者与下一个发布者不重叠，则发出）

* 我最多期望一个元素 (如果不止一个，就会出错)…

    - 如果序列是空的，我想要一个错误：Flux#single()
    - 如果序列是空的，我需要一个默认值：Flux#single(T)
    - 我也接受一个空序列：Flux#singleOrEmpty

## A.5、处理错误

* 我想创建一个错误序列：error…

    - …替换成功的 Flux 的完成：.concat(Flux.error(e))
    - …替换成功的 Mono 的排放：.then(Mono.error(e))
    - …如果两个连接之间的时间间隔过长：timeout
    - …懒惰地：error(Supplier<Throwable>)

* 我想要的 try/catch 相当于：

    - 抛出：error
    - 捕捉异常：
        - 并且返回到默认值：onErrorReturn
        - 并且回到另一个 Flux 或 Mono：onErrorResume
        - 并且包装和重新抛出：.onErrorMap(t → new RuntimeException(t))
    - finally 块：doFinally
    - Java 7 的使用模式：using 工厂方法

* 我想从错误中恢复…

    - 通过回退：
        - 到一个值：onErrorReturn
        - 到一个 Publisher 或 Mono，可能根据错误而不同：Flux#onErrorResume 和 Mono#onErrorResume
    - 通过重试：retry
        - …由伴生控制 Flux 触发：retryWhen
        - …使用标准回退策略（带抖动的指数回退）：retryBackoff

* 我想处理背压“错误”…

    - 通过抛出一个特殊的 IllegalStateException：Flux#onBackpressureError
    - 通过删除多余值：Flux#onBackpressureDrop
        - …除了最后一次看到的：Flux#onBackpressureLatest
    - 通过缓冲多余的值（有界或无界）：Flux#onBackpressureBuffer
        - …并在有界缓冲区也溢出时应用策略：带一个 BufferOverflowStrategy 的 Flux#onBackpressureBuffer

## A.6、处理时间

* 我想将排放与测量的时间（Tuple2<Long,T>）联系起来…

    - 自订阅以来：elapsed
    - 从时间的黎明开始（嗯，计算机时间）：timestamp

* 如果两次排放之间有太多延迟，我希望中断我的序列：timeout

* 我想从一个时钟中得到滴答声，有规律的时间间隔：Flux#interval

* 我想在初始延迟后发出一个 0 ：static Mono.delay

* 我想引入一个延迟：

    - 每个 onNext 信号之间：Mono#delayElement、Flux#delayElements
    - 在订阅发生之前：delaySubscription

## A.7、拆分 Flux

* 我想用一个边界条件把 Flux<T> 拆分成 Flux<Flux<T>>：

    - 大小：window(int)
        - …具有重叠或删除窗口：window(int,int)
    - 时间：window(Duration)
        - …具有重叠或删除窗口：window(Duration,Duration)
    - 大小或时间（达到计数或超时时窗口关闭）：windowTimeout(int, Duration)
    - 基于元素上的谓词：windowUntil
        - …正在发出在下一个窗口中触发边界的元素（cutBefore 变体）：.windowUntil(predicate, true)
        - …在元素与谓词匹配时保持窗口打开：windowWhile（不发出不匹配的元素）
    - 由控件 Publisher 中的 onNexts 表示的任意边界驱动：window(Publisher)、windowWhen

* 我想拆分 Flux<T>，并且将边界内的元素一起缓冲到…

    - List：
        - 通过大小边界：buffer(int)
            - …具有重叠或丢弃缓冲区：buffer(int,int)
        - 通过持续时间边界：buffer(Duration)
            - …具有重叠或丢弃缓冲区：buffer(Duration, Duration)
        - 通过大小或持续时间边界：bufferTimeout(int, Duration)
        - 通过任意条件边界：bufferUntil(Predicate)
            - …正在将触发边界的元素放入下一个缓冲区：.bufferUntil(predicate, true)
            - …当谓词匹配时缓冲，并丢弃触发边界的元素：bufferWhile(Predicate)
        - 由控件 Publisher 中的 onNexts 表示的任意边界驱动：buffer(Publisher)、bufferWhen
    - 任意的“集合”类型C：使用类似 buffer(int,Supplier<C>) 的变体

* 我想拆分 Flux<T>，这样具有同一个特征的元素最终会出现在相同的子 flux中：groupBy(Function<T,K>)

提示：请注意，这将返回一个 Flux<GroupedFlux<K, T>>，每个内部的 GroupedFlux 共享通过 key() 可访问的相同 K 键。

## A.8、回到同步世界

注意：如果从标记为“仅非阻塞”的 Scheduler（默认情况下为 parallel() 和 single()）中调用，则除 Mono#toFuture 之外的所有这些方法都将抛出 UnsupportedOperatorException。

* 我有一个 Flux<T>，我想：

    - 阻塞直到我能得到第一个元素：Flux#blockFirst
        - …带一个超时：Flux#blockFirst(Duration)
    - 阻塞直到我可以得到最后一个元素（如果为空则为 null）：Flux#blockLast
        - …带一个超时：Flux#blockLast(Duration)
    - 同步地切换到 Iterable<T>：Flux#toIterable
    - 同步地切换到 Java 8 Stream<T>：Flux#toStream

* 我有一个 Mono<T>，我想：

    - 阻塞直到我得到值：Mono#block
        - …带一个超时：Mono#block(Duration)
    - 要一个 CompletableFuture<T>：Mono#toFuture

## A.9、向多个 Subscribers 多播 Flux

* 我想将多个 Subscriber 连接到一个 Flux：

    - 并且决定何时使用connect() 触发源：publish()（返回一个 ConnectableFlux）
    - 并且立即触发源（后期订阅者查看后期数据）：share()
    - 并且当有足够的订阅者注册时，永久连接源：.publish().autoConnect(n)
    - 并且当订阅者高于/低于阈值时，自动连接和取消源：.publish().refCount(n)
        - …但是在取消之前给新订阅者一个机会：.publish().refCountGrace(n, Duration)

* 我要从 Publisher 缓存数据并将其重播给以后的订阅者：

    - 最多 n 个元素：cache(int)
    - 缓存 Duration（生存时间）内看到的最新元素：cache(Duration)
        - …但保留不超过 n 个元素：cache(int, Duration)
    - 但不立即触发源：Flux#replay（返回 ConnectableFlux）
