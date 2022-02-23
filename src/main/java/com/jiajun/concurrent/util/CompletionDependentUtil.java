package com.jiajun.concurrent.util;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * 解析由CompletionFuture延迟出的所有Future(类似通过tree的root找到所有叶子)
 *
 * @author jiajun
 */
public class CompletionDependentUtil {

    private static Field STACK, NEXT, DEPENDENT;

    private static Class<?> CLASS_UNI_COMPLETION;

    private static Class<?> CLASS_UNI_COMPOSE;

    static {
        try {
            STACK = CompletableFuture.class.getDeclaredField("stack");
            STACK.setAccessible(true);
            NEXT = STACK.getType().getDeclaredField("next");
            NEXT.setAccessible(true);

            CLASS_UNI_COMPLETION = Class.forName("java.util.concurrent.CompletableFuture$UniCompletion");
            CLASS_UNI_COMPOSE = Class.forName("java.util.concurrent.CompletableFuture$UniCompose");
            DEPENDENT = CLASS_UNI_COMPLETION.getDeclaredField("dep");
            DEPENDENT.setAccessible(true);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * 广度遍历.
     * 获取所有非UniCompose的completion对应的dep
     *
     * @param completableFuture
     * @return
     */
    public static List<CompletableFuture> dependentLeafBfs(CompletableFuture completableFuture) {
        List<CompletableFuture> result = new LinkedList<>();
        Queue<CompletableFuture> queue = new LinkedList<>();
        result.add(completableFuture);
        try {
            queue.offer(completableFuture);
            while (!queue.isEmpty()) {
                CompletableFuture future = queue.poll();
                Object completion = STACK.get(future);
                // 先加入当期stack上所有completion dep的future
                while (completion != null) {
                    Class<?> completionClass = completion.getClass();
                    if (CLASS_UNI_COMPLETION.isAssignableFrom(completionClass)) {
                        // 获取dep的CompletableFuture
                        CompletableFuture depFuture = (CompletableFuture) DEPENDENT.get(completion);
                        if (depFuture != null) {
                            queue.offer(depFuture);
                            if (completionClass != CLASS_UNI_COMPOSE) {
                                // UniCompose特殊处理: Compose后面通常是跟随者一个消费的操作, 很少会在compose中组装数据. 所以一般有stack, 只要加入stack中的dep future就行.
                                result.add(depFuture);
                            }
                        }
                    }
                    completion = NEXT.get(completion);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return result;
    }

    /**
     * 优化版本: Java8中栈的末尾是最后触发的, 只要判断栈末尾的Future完成了即可
     *
     * @param completableFuture
     * @return
     */
    public static List<CompletableFuture> dependentLeafBfsV2(CompletableFuture completableFuture) {
        try {
            List<CompletableFuture> result = new LinkedList<>();
            if (STACK.get(completableFuture) == null) {
                result.add(completableFuture);
            }
            Queue<CompletableFuture> queue = new LinkedList<>();
            queue.offer(completableFuture);
            while (!queue.isEmpty()) {
                CompletableFuture future = queue.poll();
                Object completion = STACK.get(future);
                // 先加入当期stack上所有completion dep的future
                CompletableFuture stackLastDep = null;
                while (completion != null) {
                    Class<?> completionClass = completion.getClass();
                    if (CLASS_UNI_COMPLETION.isAssignableFrom(completionClass)) {
                        // 获取dep的CompletableFuture
                        CompletableFuture depFuture = (CompletableFuture) DEPENDENT.get(completion);
                        if (depFuture != null) {
                            queue.offer(depFuture);
                            stackLastDep = depFuture;
                        }
                    }
                    completion = NEXT.get(completion);
                }
                if (stackLastDep != null && STACK.get(stackLastDep) == null) {
                    // 是最后一个, 并且这个Future没有再衍生出栈
                    result.add(stackLastDep);
                }
            }
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 获取所有非UniCompose的completion对应的dep
     *
     * @param completableFuture
     * @return
     */
    public static List<CompletableFuture> dependentLeafDfs(CompletableFuture completableFuture) {
        List<CompletableFuture> list = new LinkedList<>();
        try {
            Object stack = STACK.get(completableFuture);
            traversalPre(stack, list);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    static void traversalPre(Object node, List<CompletableFuture> list) throws IllegalAccessException {
        if (node == null) {
            return;
        }
        traversalPre(NEXT.get(node), list);
        Class<?> nodeClass = node.getClass();
        if (CLASS_UNI_COMPLETION.isAssignableFrom(nodeClass)) {
            CompletableFuture depCompletion = (CompletableFuture) DEPENDENT.get(node);
            if (depCompletion == null) {
                return;
            }
            if (nodeClass != CLASS_UNI_COMPOSE) {
                // 过滤掉thenCompose操作产的Completion, 等待这个没有意义.
                // thenCompose生成的CompletableFuture一般会跟随这一个非`thenCompose`操作, 只要等待那个生成的Future即可, 这里可以加个预警
                list.add(depCompletion);
            }
            traversalPre(STACK.get(depCompletion), list);
        }
    }
}
