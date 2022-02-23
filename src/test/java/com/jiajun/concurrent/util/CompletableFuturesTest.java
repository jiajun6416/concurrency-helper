package com.jiajun.concurrent.util;

import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CompletableFuturesTest {

    @Test
    public void combine() {
        CompletableFutures.combine(CompletableFuture.completedFuture(1), r1 -> {
            System.out.println("r1: " + r1);
            return r1;
        }).join();

        CompletableFutures.combine(CompletableFuture.completedFuture(1), null, (r1, r2) -> {
            System.out.println("r1: " + r1);
            System.out.println("r2: " + r2);
            return r1;
        }).join();

        CompletableFutures.combine(null, null, null, null, null, CompletableFuture.completedFuture(1), CompletableFuture.completedFuture(1), null, null,
                (r1, r2, r3, r4, r5, r6, r7, r8, r9) -> {
                    System.out.println("r1: " + r1);
                    System.out.println("r2: " + r2);
                    System.out.println("r3: " + r3);
                    System.out.println("r4: " + r4);
                    System.out.println("r5: " + r5);
                    System.out.println("r6: " + r6);
                    System.out.println("r7: " + r7);
                    System.out.println("r8: " + r8);
                    System.out.println("r9: " + r9);
                    return 0;
                }).join();

        List<CompletableFuture<Integer>> futures = IntStream.range(0, 10).boxed().map(CompletableFuture::completedFuture).collect(Collectors.toList());
        System.out.println(CompletableFutures.combine(futures, integers -> integers.stream().map(Object::toString).collect(Collectors.joining(", "))).join());
    }

    @Test
    public void consumer() {
        CompletableFutures.acceptBoth(CompletableFuture.completedFuture(1), r1 -> {
            System.out.println("r1: " + r1);
        }).join();

        CompletableFutures.acceptBoth(CompletableFuture.completedFuture(1), null, (r1, r2) -> {
            System.out.println("r1: " + r1);
            System.out.println("r2: " + r2);
        }).join();

        CompletableFutures.acceptBoth(null, null, null, null, null, CompletableFuture.completedFuture(1), CompletableFuture.completedFuture(1), null, null,
                (r1, r2, r3, r4, r5, r6, r7, r8, r9) -> {
                    System.out.println("r1: " + r1);
                    System.out.println("r2: " + r2);
                    System.out.println("r3: " + r3);
                    System.out.println("r4: " + r4);
                    System.out.println("r5: " + r5);
                    System.out.println("r6: " + r6);
                    System.out.println("r7: " + r7);
                    System.out.println("r8: " + r8);
                    System.out.println("r9: " + r9);
                }).join();

        List<CompletableFuture<Integer>> futures = IntStream.range(0, 10).boxed().map(CompletableFuture::completedFuture).collect(Collectors.toList());
        CompletableFutures.acceptBoth(futures, integers -> System.out.println(integers.stream().map(Objects::toString).collect(Collectors.joining(", "))));
    }

    @Test
    public void thenComposeNull() {
        CompletableFuture<Integer> future1 = CompletableFuture.completedFuture(1);
        CompletableFuture<Object> future = future1.thenCompose(integer -> CompletableFuture.completedFuture(null));
        System.out.println(future);
        System.out.println(future.join());
    }

    @Test
    public void allOf() {
        //System.out.println(CompletableFutures.allOf(null).join());
        System.out.println(CompletableFutures.allOf(null, null).join());
        System.out.println(CompletableFutures.allOf(null, null, CompletableFuture.completedFuture(1)).join());
        System.out.println(CompletableFutures.allOf(null, CompletableFuture.completedFuture(1), CompletableFuture.completedFuture(1)).join());
    }

    @Test
    public void compose() {
        Object value = CompletableFutures.compose(null
                , integer -> {
                    System.out.println(integer);
                    return CompletableFuture.completedFuture("str");
                }
           /*     , (integer, s) -> {
                    System.out.println(integer);
                    System.out.println(s);
                    return CompletableFuture.completedFuture(null);
                }*/
           /*     , (integer, s, aLong) -> {
                    System.out.println(integer);
                    System.out.println(s);
                    System.out.println(aLong);
                    return CompletableFuture.completedFuture(null);
                }*/
             /*   , (integer, s, aLong, date) -> {
                    System.out.println(integer);
                    System.out.println(s);
                    System.out.println(aLong);
                    System.out.println(date);
                    return CompletableFuture.completedFuture(null);
                }*/
        ).join();
        System.out.println(value);
    }
}