package com.jiajun.concurrent.future.jdk;

import com.jiajun.concurrent.timer.TimerHolder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 对JDK中CompletableFuture的处理
 * - 更好的异常处理
 * - 支持动态超时
 *
 * @author jiajun
 */
public class CompletableFutureConverter {

    /**
     * 增加异常处理器
     *
     * @param future
     * @param exceptionHandler
     * @param <T>
     * @return
     */
    public static <T> CompletableFuture<T> wrapper(CompletableFuture<T> future, Function<Throwable, Object> exceptionHandler) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        future.whenComplete((t, throwable) -> {
            if (t != null) {
                completableFuture.complete(t);
            } else {
                Object value = null;
                try {
                    value = exceptionHandler.apply(throwable);
                } catch (Throwable e) {
                    value = e;
                }
                if (value != null && value instanceof Throwable) {
                    completableFuture.completeExceptionally((Throwable) value);
                } else {
                    completableFuture.complete((T) value);
                }
            }
        });
        return completableFuture;
    }

    /**
     * 增加可控的超时 + 异常处理器
     *
     * @param futureSupplier
     * @param endTime
     * @param exceptionHandler
     * @param <T>
     * @return
     */
    public static <T> CompletableFuture<T> wrapper(Supplier<CompletableFuture<T>> futureSupplier, long endTime,
                                                   Function<Throwable, Object> exceptionHandler) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        long timeout = endTime - System.currentTimeMillis();
        if (timeout <= 0) {
            Exception t = new TimeoutException(" timeout [" + timeout + "] less then 0");
            Object value = null;
            try {
                value = exceptionHandler.apply(t);
            } catch (Throwable e) {
                value = e;
            }
            if (value != null && value instanceof Throwable) {
                completableFuture.completeExceptionally((Throwable) value);
            } else {
                completableFuture.complete((T) value);
            }
            return completableFuture;
        }

        CompletableFuture<T> future = null;
        try {
            future = futureSupplier.get();
            // 超时管理, 通常RPC框架都会有超时机制, 这里需要自己实现
            timeoutCheck(future, timeout);
        } catch (Exception e) {
            future = new CompletableFuture<>();
            future.completeExceptionally(e);
        }
        return wrapper(future, exceptionHandler);
    }

    private static void timeoutCheck(CompletableFuture future, long timeoutTs) {
        TimerHolder.instance().newTimeout(timeout -> {
            if (future == null || future.isDone()) {
                return;
            }
            future.completeExceptionally(new TimeoutException("future timeout, timeout is: " + timeoutTs));
        }, timeoutTs, TimeUnit.MILLISECONDS);
    }
}
