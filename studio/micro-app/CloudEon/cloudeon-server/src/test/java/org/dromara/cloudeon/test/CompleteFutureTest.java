package org.dromara.cloudeon.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

@Slf4j
public class CompleteFutureTest {
    @Test
    public void thenAsync() {
        CompletableFuture<Void> task1 = CompletableFuture
                .runAsync(() -> System.out.println(Thread.currentThread().getName() + ":task1"));

        CompletableFuture<Void> task2 = task1.thenRunAsync(new Runnable() {
            @Override
            public void run() {
                doSleep(5000);
                System.out.println(Thread.currentThread().getName() + ":task2");
            }
        });

        CompletableFuture<Void> task3 = task2.thenRunAsync(new Runnable() {
            @Override
            public void run() {
                doSleep(5000);
                System.out.println(Thread.currentThread().getName() + ":task3");
            }
        });


//        doSleep(15000);


        System.out.println("main");
    }

    @Test
    public void then1() {
        CompletableFuture<Void> task3 = CompletableFuture
                .runAsync(() -> System.out.println("task1"))
                .thenAccept(unused -> {
                    doSleep(5000);
                    System.out.println("task2");
                }).thenAccept(unused -> {
                    doSleep(2000);
                    System.out.println("task3");
                });

        System.out.println("main");
    }

    @Test
    public void then2() {
        CompletableFuture<Void> task3 = CompletableFuture
                .runAsync(() -> System.out.println("task1"))
                .thenAcceptAsync(unused -> {
                    doSleep(5000);
                    System.out.println("task2");
                }).thenAcceptAsync(unused -> {
                    doSleep(10000);
                    System.out.println("task3");
                });
        task3.join();

        System.out.println("main");
    }

    @Test
    public void then3() {

        CompletableFuture<String> task = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "hello world | peter";
            }
        }).thenApply(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.toUpperCase(Locale.ROOT);
            }
        }).thenApply(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.split("\\|")[0];
            }
        });

        System.out.println("计算结果：" + task.join());
        System.out.println("main");
    }

    @Test
    public void combine() {
        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> {
            System.out.println("[task1] 取茶杯");
            doSleep(5000);
            System.out.println("[task1] 洗茶杯");
            System.out.println("[task1] 热一热茶杯");
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                System.out.println("[task2] 去商城买茶叶");
                doSleep(3000);
                System.out.println("[task2] 家里选茶叶");
                String teas = "[task2] 买了茶叶:红茶、普洱、铁观音";
                String[] teasArr = teas.split(":")[1].split("、");
                return teasArr[1];
            }
        });

        CompletableFuture<Object> task3 = task1.thenCombineAsync(task2, new BiFunction<Void, String, Object>() {
            @Override
            public Object apply(Void unused, String s) {
                System.out.println("[task3] 取到茶杯。。。和茶叶：" + s);
                System.out.println("[task3] 开始泡茶。。。");
                return null;
            }
        });

        task3.join();

        System.out.println("main");
    }

    private void doSleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void combine2() {
        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> {
            System.out.println("[task1] 取茶杯");
        }).thenAcceptAsync(unused -> {
            doSleep(5000);
            System.out.println("[task1] 洗茶杯");
        }).thenAcceptAsync(unused -> {
            System.out.println("[task1] 热一热茶杯");
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                System.out.println("[task2] 去商城买茶叶");
                doSleep(2000);
                return "[task2] 买了茶叶:红茶、普洱、铁观音";
            }
        }).thenApplyAsync(new Function<String, String>() {
            @Override
            public String apply(String s) {
                String category = s.split(":")[1];
                System.out.println("[task2] 家里选茶叶:" + category);
                String[] teas = category.split("、");
                return teas[1];
            }
        });

        CompletableFuture<Object> task3 = task1.thenCombineAsync(task2, new BiFunction<Void, String, Object>() {
            @Override
            public Object apply(Void unused, String s) {
                System.out.println("[task3] 取到茶杯。。。和茶叶：" + s);
                System.out.println("[task3] 开始泡茶。。。");
                return null;
            }
        });

        task3.join();

        System.out.println("main");
    }

    @Test
    public void combineOr() {
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                doSleep(2000);
                return "[t1]hello";
            }
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "[t2]pk001";
            }
        });

        CompletableFuture<Void> task3 = task1.acceptEither(task2, new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println("[t3]" + s);
            }
        });

        task3.join();


        System.out.println("main");
    }

    @Test
    public void error() {
        CompletableFuture<Void> task = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return 8;
            }
        }).thenApply(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer integer) {
//                return integer / 0;
                return integer / 10;
            }
        }).thenAccept(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println("计算结果：" + integer);
            }
        }).whenComplete(new BiConsumer<Void, Throwable>() {
            @Override
            public void accept(Void unused, Throwable throwable) {
                System.out.println("无论是否异常，都会执行。。。。" + throwable);
            }
        }).exceptionally(new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable throwable) {

                System.out.println("处理异常" + throwable.getMessage());
                return null;
            }
        });

        task.join();

        System.out.println("main");
    }


    @Test
    public void all() {
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                doSleep(5000);
                return "[t1]hello";
            }
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                doSleep(3000);
                return "[t2]pk001";
            }
        });

        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                doSleep(2000);
                return "[t3]jk0334";
            }
        });

        CompletableFuture<Void> task4 = CompletableFuture
                .allOf(task1, task2, task3)
                .thenAccept(new Consumer<Void>() {
                    @Override
                    public void accept(Void unused) {
                        System.out.println("[t4]" + task1.join());
                        System.out.println("[t4]" + task2.join());
                        System.out.println("[t4]" + task3.join());
                    }
                });

        task4.join();

        CompletableFuture<Void> task5 = CompletableFuture.runAsync(() -> {
            doSleep(3000);
            System.out.println("[t5]dwxxx");
        });

        CompletableFuture<Void> task6 = CompletableFuture.runAsync(() -> {
            doSleep(2000);
            System.out.println("[t6]ok1113");
        });

        CompletableFuture.allOf(task5, task6).join();

        System.out.println("main");
    }
}
