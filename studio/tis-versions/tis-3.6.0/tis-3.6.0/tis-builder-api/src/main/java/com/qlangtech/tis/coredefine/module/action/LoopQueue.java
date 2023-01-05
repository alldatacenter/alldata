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
package com.qlangtech.tis.coredefine.module.action;

import java.lang.reflect.Array;

/**
 * 循环队列，一个不断向队列中写，另外一个队列可以随时从循环队列中读到
 * 最新的n条记录，结果中有旧到新排列
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-01-09 23:29
 */
public class LoopQueue<T> {

    private final T[] buffer;

    private final int size;

    private int index = 0;

    public LoopQueue(T[] buffer) {
        this.buffer = buffer;
        this.size = buffer.length;
    }

    public void write(T data) {
        synchronized (this) {
            this.buffer[index++ % size] = data;
        }
    }

    public T[] readBuffer() {
        T[] result = (T[]) Array.newInstance(buffer.getClass().getComponentType(), size);
        synchronized (this) {
            int collectIndex = this.index;
            T tmp = null;
            int collect = 0;
            for (int count = 0; count < this.size; count++) {
                if ((tmp = this.buffer[collectIndex++ % size]) != null) {
                    result[collect++] = tmp;
                    if (collect >= (result.length)) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public void cleanBuffer() {
        synchronized (this) {
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = null;
            }
            this.index = 0;
        }
    }
}
