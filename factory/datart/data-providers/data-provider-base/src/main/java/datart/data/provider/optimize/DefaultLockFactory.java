/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.data.provider.optimize;

import org.apache.commons.collections4.map.LRUMap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultLockFactory {
    private static final Map<String, ReentrantLock> LOCK_MAP = Collections.synchronizedMap(new LRUMap<>(1000));

    public static ReentrantLock getLock(String taskKey) {
        synchronized (taskKey.intern()) {
            if (LOCK_MAP.containsKey(taskKey)) {
                return LOCK_MAP.get(taskKey);
            } else {
                ReentrantLock reentrantLock = new ReentrantLock(true);
                LOCK_MAP.put(taskKey, reentrantLock);
                return reentrantLock;
            }
        }
    }
}
