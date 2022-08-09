/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
* http://www.apache.org/licenses/LICENSE-2.0
 * 
* Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.ambari.server.testing;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Assert;

/**
 *
 * Test if DeadlockWarningThread can detect deadlocks properly
 */
public class DeadlockedThreadsTest {
  static Set<Thread> threads = new HashSet<>();
  
  /**
   *
   * Test should detect "flat" deadlock
   * This test commented because it is not testing any production code
   * In case if we change DeadlockWarningThread and need test of changes
   * we can add there @Test annotation
   */
  public void testDeadlocks() {
    
    // deadlock with three locks
    Object lock1 = new String("lock1");
    Object lock2 = new String("lock2");
    Object lock3 = new String("lock3");

    
    threads.add(new DeadlockingThread("t1", lock1, lock2));
    threads.add(new DeadlockingThread("t2", lock2, lock3));
    threads.add(new DeadlockingThread("t3", lock3, lock1));

    // deadlock with two locks
    Object lock4 = new String("lock4");
    Object lock5 = new String("lock5");

    threads.add(new DeadlockingThread("t4", lock4, lock5));
    threads.add(new DeadlockingThread("t5", lock5, lock4));
    DeadlockWarningThread wt = new DeadlockWarningThread(threads);

    

    while (true) {
      if(!wt.isAlive()) {
          break;
      }
    }
    if (wt.isDeadlocked()){
      Assert.assertTrue(wt.getErrorMessages().toString(), wt.isDeadlocked());
      Assert.assertFalse(wt.getErrorMessages().toString().equals(""));
    } else {
      Assert.assertTrue(wt.getErrorMessages().toString(), wt.isDeadlocked());
    }
    
  }

  /**
   *
   * Test should detect "hidden" deadlock
   * This test commented because it is not testing any production code
   * In case if we change DeadlockWarningThread and need test of changes
   * we can add there @Test annotation
   */
  public void testReadWriteDeadlocks() {
    
    // deadlock with three locks
    Object lock1 = new String("lock1");
    Object lock2 = new String("lock2");
    Object lock3 = new String("lock3");

    
    threads.add(new DeadlockingThreadReadWriteLock("t1", lock1, lock2));
    threads.add(new DeadlockingThreadReadWriteLock("t2", lock2, lock3));
    threads.add(new DeadlockingThreadReadWriteLock("t3", lock3, lock1));

    // deadlock with two locks
    Object lock4 = new String("lock4");
    Object lock5 = new String("lock5");

    threads.add(new DeadlockingThreadReadWriteLock("t4", lock4, lock5));
    threads.add(new DeadlockingThreadReadWriteLock("t5", lock5, lock4));
    DeadlockWarningThread wt = new DeadlockWarningThread(threads);

    

    while (true) {
      if(!wt.isAlive()) {
          break;
      }
    }
    if (wt.isDeadlocked()){
      Assert.assertTrue(wt.getErrorMessages().toString(), wt.isDeadlocked());
      Assert.assertFalse(wt.getErrorMessages().toString().equals(""));
    } else {
      Assert.assertTrue(wt.getErrorMessages().toString(), wt.isDeadlocked());
    }
    
  }
  
  
  /**
   * There is absolutely nothing you can do when you have
   * deadlocked threads.  You cannot stop them, you cannot
  * interrupt them, you cannot tell them to stop trying to
  * get a lock, and you also cannot tell them to let go of
  * the locks that they own.
  */
  private static class DeadlockingThread extends Thread {
    private final Object lock1;
    private final Object lock2;

    public DeadlockingThread(String name, Object lock1, Object lock2) {
      super(name);
      this.lock1 = lock1;
      this.lock2 = lock2;
      start();
    }
    public void run() {
      while (true) {
        f();
      }
    }
    private void f() {
      synchronized (lock1) {
        g();
      }
    }
    private void g() {
      synchronized (lock2) {
        // do some work...
        for (int i = 0; i < 1000 * 1000; i++) ;
      }
    }
  }
  
  private static class DeadlockingThreadReadWriteLock extends Thread {
    private final Object lock1;
    private final Object lock2;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public final Lock r = rwl.readLock();
    public final Lock w = rwl.writeLock();

    public DeadlockingThreadReadWriteLock(String name, Object lock1, Object lock2) {
      super(name);
      this.lock1 = lock1;
      this.lock2 = lock2;
      start();
    }
    public void run() {
      while (true) {
        f();
      }
    }
    private void f() {
      w.lock();
      g();
      w.unlock();
    }
    
    private void g() {
      r.lock();
      // do some work...
      for (int i = 0; i < 1000 * 1000; i++) ;
      r.unlock();
    }
  }
  
}
