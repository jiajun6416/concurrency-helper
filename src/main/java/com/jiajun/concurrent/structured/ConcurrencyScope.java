package com.jiajun.concurrent.structured;

import com.jiajun.concurrent.future.jdk.CompletableFutureConverter;
import com.jiajun.concurrent.future.mainstay.MainstayFutureConverter;
import com.jiajun.concurrent.util.CompletionDependentUtil;
import com.ximalaya.mainstay.common.concurrent.Future;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 结构化并发的概念: @see https://openjdk.java.net/jeps/8277129
 * 结构化并发通常是使用协程框架实现, 结构化并发是一种异步编程的理念, 让整个异步过程存在明确的入口和出口点.
 * scope: 所有的异步任务都必须有自己的scope, 衍生出的子任务可以通过scope构建出父子关系, 在框架级别将所有的任务构建成一个任务树结构.
 * 功能:
 * - suspend: 父任务可以等待其衍生出的子任务完成后再返回, 而无需关心spawn的子任务是否完成, 在框架内构造整体的依赖关系
 * - endTime: 总体的结束时间, 实现可控的超时
 *
 * @author jiajun
 */
public class ConcurrencyScope {

    private List<CompletableFuture> rootCompletions = new LinkedList<>(); // root scope的任务

    private Thread thread = Thread.currentThread();

    private long endTime = -1; // 所有任务的最大执行时间, 默认是不设置

    public ConcurrencyScope() {
    }

    /**
     * 整个scope返回的最晚时间
     *
     * @param endTime
     */
    public ConcurrencyScope(long endTime) {
        if (endTime < System.currentTimeMillis()) {
            throw new IllegalArgumentException("input endTime [" + endTime + "] less then now.");
        }
        this.endTime = endTime;
    }

    /**
     * 将JDK中的CompletableFuture加入管理
     *
     * @param futureSupplier
     * @param exceptionHandler 异常处理. 返回异常则表示向上抛出, 否则是降级数据
     * @param <T>
     * @return
     */
    public <T> CompletableFuture<T> launch(Supplier<CompletableFuture<T>> futureSupplier, Function<Throwable, Object> exceptionHandler) {
        CompletableFuture<T> completableFuture;
        if (endTime > 0) {
            completableFuture = CompletableFutureConverter.wrapper(futureSupplier, endTime, exceptionHandler);
        } else {
            completableFuture = CompletableFutureConverter.wrapper(futureSupplier.get(), exceptionHandler);
        }
        addRootCompletionNode(completableFuture);
        return completableFuture;
    }

    /**
     * 使用mainstay(喜马内部rpc框架)执行一次rpc调用
     *
     * @param futureSupplier
     * @param exceptionHandler 异常处理. 返回异常则表示向上抛出, 否则是降级数据.
     * @param <T>
     * @return
     */
    public <T> CompletableFuture<T> invoke(Supplier<Future<T>> futureSupplier, Function<Throwable, Object> exceptionHandler) {
        CompletableFuture<T> completableFuture;
        if (endTime > 0) {
            completableFuture = MainstayFutureConverter.converter(futureSupplier, endTime, exceptionHandler);
        } else {
            completableFuture = MainstayFutureConverter.converter(futureSupplier.get(), exceptionHandler);
        }
        addRootCompletionNode(completableFuture);
        return completableFuture;
    }

    /*    *//**
     * 使用dubbo执行一次rpc调用
     *
     * @param futureSupplier
     * @param exceptionHandler 异常处理. 返回异常则表示向上抛出, 否则是吃掉异常.
     * @param <T>
     * @return
     *//*
    public <T> CompletableFuture<T> invoke(Supplier<CompletableFuture<T>> futureSupplier, Function<Throwable, Object> exceptionHandler) {
        return null;
    }*/

    /**
     * 找到root衍生的所有future, 构建树状的job关系. 这里并不阻塞, 返回的Future需要等待scope下的所有job
     *
     * @return
     */
    public CompletableFuture<Void> suspend() {
        CompletableFuture[] allDependents = rootCompletions.stream().flatMap(it -> CompletionDependentUtil.dependentLeafBfsV2(it).stream()).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(allDependents);
    }

    private void addRootCompletionNode(CompletableFuture completableFuture) {
        if (Thread.currentThread() == thread) {
            rootCompletions.add(completableFuture);
        }
    }
}
