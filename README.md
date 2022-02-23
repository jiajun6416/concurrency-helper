## 结构化并发
- Java中没有协程, 实现高并发的唯一方式就是纯异步, 其的底层都是基于(`NIO`或`AIO`), 依次迭代出了三种编码方式.
    1. Callback(`ListenableFuture`)
    2. Promise(`CompletableFuture`)
    3. 观察者模式(`RxJava Project-Reactor`)

### `结构化并发`是怎么样的
- 协程中的每个`task`都有作用域(`scope`), 也只有带有`scope`的`task`才能衍生(`Spawn`)新的`task`
    1. 在结构化的并发中，每个并发操作都有自己的作用域
    2. 在父作用域内新建作用域都属于它的子作用域
    3. 父作用域和子作用域具有级联关系(`树状的层级`)
    4. 父作用域的生命周期持续到所有子作用域执行完
    5. 当主动结束父作用域时，会级联结束它的各个子作用域


### Java中如何实现`结构化并发`
#### 增加Scope的概念, 构建整体的任务树
- java中的异步请求通常都是等待`I/O`线程触发的`ListenableFuture/CompletableFuture`. 这些`Future`都是不带`Scope`概念的, 也没有层级关系(`等待触发可以理解为层级`)
- 无论是`ListenableFuture`还是`CompletableFuture`都通过`stack`存储了`dep`的`Future`, 可以自己解析`stack`构建出整体的`scope`关系

#### 可控的响应时间
- 业务场景都是rpc调用, 只能依赖底层的`nio`框架保证整体的响应时间.


## 使用
- 参考测试用例

## 参考
- [JEP draft: Structured Concurrency (Incubator)](https://openjdk.java.net/jeps/8277129)
- [解决并发编程之痛的良药--结构化并发编程 - 知乎](https://zhuanlan.zhihu.com/p/108759542)
- [Structured Concurrency - Structured Concurrency - OpenJDK Wiki](https://wiki.openjdk.java.net/display/loom/Structured+Concurrency)