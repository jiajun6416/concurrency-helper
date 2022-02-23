package com.jiajun.concurrent.future.mainstay;

import com.ximalaya.mainstay.common.InvokerHelper;
import com.ximalaya.mainstay.common.MainstayTimeoutException;
import com.ximalaya.mainstay.common.concurrent.Future;
import com.ximalaya.mainstay.common.concurrent.FutureCallback;
import com.ximalaya.mainstay.common.concurrent.Futures;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 将mainstay的future转成CompletableFuture
 * - 更好的异常处理
 * - 支持动态超时
 *
 * @author jiajun
 */
public class MainstayFutureConverter {

    public static <T> CompletableFuture<T> converter(Future<T> future,
                                                     Function<Throwable, Object> exceptionHandler) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Futures.addCallback(future, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
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
            }
        });
        return completableFuture;
    }

    /**
     * 可控的超时
     *
     * @param futureSupplier
     * @param endTime          超时的绝对时间. 精确控制整体接口的响应时间
     * @param exceptionHandler
     * @param <T>
     * @return
     */
    public static <T> CompletableFuture<T> converter(Supplier<Future<T>> futureSupplier, long endTime,
                                                     Function<Throwable, Object> exceptionHandler) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        long timeout = endTime - System.currentTimeMillis();
        if (timeout <= 0) {
            Exception t = new MainstayTimeoutException(" timeout [" + timeout + "] less then 0");
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
        InvokerHelper.setNcTimeout((int) timeout); // 动态超时
        Future<T> future = null;
        try {
            future = futureSupplier.get(); // 将非rpc异常转成future
        } catch (Throwable e) {
            future = Futures.immediateFailedFuture(e);
        }
        return converter(future, exceptionHandler);
    }
}
