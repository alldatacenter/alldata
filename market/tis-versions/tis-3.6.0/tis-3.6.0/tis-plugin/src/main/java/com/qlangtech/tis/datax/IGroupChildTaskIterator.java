/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.datax;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-09 18:23
 **/
public interface IGroupChildTaskIterator extends Iterator<IDataxReaderContext> {

    static IGroupChildTaskIterator create(IDataxReaderContext readerContext) {
        AtomicReference<IDataxReaderContext> ref = new AtomicReference(readerContext);
        return new IGroupChildTaskIterator() {
            @Override
            public boolean hasNext() {
                return ref.get() != null;
            }

            @Override
            public Map<String, List<String>> getGroupedInfo() {
                if (StringUtils.isEmpty(readerContext.getTaskName())) {
                    throw new IllegalStateException("readerContext.getTaskName() can not be empty");
                }
                return Collections.singletonMap(readerContext.getTaskName(), Lists.newArrayList(readerContext.getTaskName()));
            }

            @Override
            public IDataxReaderContext next() {
                return ref.getAndSet(null);
            }
        };
    }

    /**
     * 例如Mysql是分库的，对应一个表有两个子任务需要执行，那么统计信息就是Map<String, List<String>> 这样的结构 key为table名称，list<childTaskName>
     *
     * @return
     */
    default Map<String, List<String>> getGroupedInfo() {
        return Collections.emptyMap();
    }
}
