/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RangerReadWriteLock {

    private static final RangerLock NO_OP_LOCK = new RangerLock(null);

    private final ReentrantReadWriteLock lock;

    public RangerReadWriteLock(boolean isUseLock) {
        lock = isUseLock ? new ReentrantReadWriteLock(true) : null;
    }

    public RangerLock getReadLock() {
        final RangerLock ret;
        if (lock != null) {
            ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
            readLock.lock();
            ret = new RangerLock(readLock);
        } else {
            ret = NO_OP_LOCK;
        }
        return ret;
    }

    public RangerLock getWriteLock() {
        final RangerLock ret;
        if (lock != null) {
            boolean isLocked = false;
            ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
            while (!isLocked) {
                isLocked = writeLock.tryLock();
                // Another thread has acquired write lock. Hint to scheduler to schedule some other thread if needed
                if (!isLocked && lock.getReadLockCount() == 0) {
                    Thread.yield();
                }
            }
            ret = new RangerLock(writeLock);
        } else {
            ret = NO_OP_LOCK;
        }
        return ret;
    }

    @Override
    public String toString() {
        if (lock != null) {
            return "ReadWriteLock:[" + lock.toString() + "], ReadLock:[" + lock.readLock().toString() + "], WriteLock:[" + lock.writeLock().toString() + "]";
        } else {
            return "ReadWriteLock:[null]";
        }
    }

    static final public class RangerLock implements AutoCloseable {
        private final Lock lock;

        private RangerLock(Lock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            if (lock != null) {
                lock.unlock();
            }
        }

        public boolean isLockingEnabled() {
            return lock != null;
        }

        @Override
        public String toString() {
            return lock == null ? "null" : lock.toString();
        }
    }
}
