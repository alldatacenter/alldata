/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.full.dump;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年7月13日
 */
public class TestThread extends TestCase {

    private static ExecutorService service;

    static void shutdownAndAwaitTermination(ExecutorService pool) {
        System.out.println("shutdownAndAwaitTermination");
        // Disable new tasks from being submitted
        pool.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                pool.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public void test() throws Exception {
        service = Executors.newCachedThreadPool(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        System.out.println("uncaughtException execute");
                        shutdownAndAwaitTermination(service);
                    // try {
                    // service.shutdown();
                    // System.out.println("service.shutdown()");
                    // } catch (Exception e1) {
                    // e1.printStackTrace();
                    // }
                    // e.printStackTrace();
                    }
                });
                return t;
            }
        });
        // ExecutorCompletionService<Object> completionService = new
        // ExecutorCompletionService<Object>(
        // service);
        Future<String> xxxx = service.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                Future<String> f = service.submit(new Callable<String>() {

                    @Override
                    public String call() throws Exception {
                        while (true) {
                            System.out.println("inner checkStatus");
                            Thread.sleep(1000);
                        }
                    }
                });
                try {
                    f.get();
                } catch (Exception e) {
                    f.cancel(true);
                    e.printStackTrace(System.err);
                }
                System.out.println("i inner was be cancel");
                // }
                return "xxxxx";
            }
        });
        Future<String> bbb = service.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                int i = 0;
                boolean isInterrupted = false;
                while (true && !(isInterrupted = Thread.currentThread().isInterrupted())) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("receive excpeiton InterruptedException ");
                        e.printStackTrace();
                        return "aaaaa-over";
                    }
                    System.out.println("isInterrupted():" + isInterrupted);
                // System.out.println("i am bbbb working i:" + i++);
                // if (i > 15) {
                // System.out.println("throw a exception");
                // throw new RuntimeException("xxxxxx");
                // }
                }
                return "aaaaa";
            }
        });
        Thread.sleep(6000);
        System.out.println("cancel");
        System.out.println(xxxx.cancel(true));
        System.out.println(bbb.cancel(true));
        // Future<Object> o = completionService.take();
        // System.out.println("f.get():" + o.get() + ",f.isDone():" +
        // o.isDone());
        // shutdownAndAwaitTermination(service);
        Thread.sleep(9999999999l);
    }
}
