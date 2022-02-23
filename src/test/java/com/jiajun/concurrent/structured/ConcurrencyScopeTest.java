package com.jiajun.concurrent.structured;

import com.alibaba.fastjson.JSON;
import com.jiajun.concurrent.util.CompletableFutures;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 结构化并发测试
 */
public class ConcurrencyScopeTest {

    @Test
    public void jdkCompletableFuture() {
        long start = System.currentTimeMillis();
        long endTime = start + 3000; // 指定最大耗时

        ConcurrencyScope scope = new ConcurrencyScope(endTime); // 结构化并发的入口, 只要拿着Scope就能控制整个异步
        // 以播放页为例
        Map<String, String> result = new HashMap<>();
        long trackId = 10086;

        // 声音信息
        CompletableFuture<String> trackFuture = scope.launch(() -> CompletableFuture.supplyAsync(() -> {
                    sleep(500);
                    return "this is track " + trackId + " detailInfo";
                }, executor),
                throwable -> throwable)
                .thenApply(s -> result.put("track", s));

        // 专辑信息
        CompletableFuture<String> albumFuture = trackFuture.thenCompose(trackInfo -> scope.launch(() -> CompletableFuture.supplyAsync(() -> {
                    sleep(500);
                    return "this is album info";
                }, executor),
                throwable -> null))
                .thenApply(s -> result.put("album", s));

        // 主播信息
        trackFuture.thenCompose(trackInfo -> scope.launch(() -> CompletableFuture.supplyAsync(() -> {
                    sleep(800);
                    return "this is author info";
                }, executor),
                throwable -> null))
                .thenApply(s -> result.put("author", s));

        // 相关推荐
        CompletableFuture<String> recommendIdFuture = CompletableFutures.compose(trackFuture, track -> albumFuture, (track, album) -> scope.launch(() -> CompletableFuture.supplyAsync(() -> {
                    sleep(800);
                    return "this is recommend ids";
                }, executor),
                throwable -> null))
                .thenApply(s -> result.put("recommendId", s));
        recommendIdFuture.thenCompose(ids -> scope.launch(() -> CompletableFuture.supplyAsync(() -> {
                    sleep(500);
                    result.put("recommendDetail", "this is recommend details");
                    return "this is recommend details";
                }, executor),
                throwable -> null));
        // .thenApply(s -> result.put("recommendDetail", s));

        scope.suspend().join();

        System.out.println(JSON.toJSONString(result) + ", cost: " + (System.currentTimeMillis() - start));
    }

    static ExecutorService executor = Executors.newCachedThreadPool();

    private void sleep(long mills) {
        try {
            TimeUnit.MILLISECONDS.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}