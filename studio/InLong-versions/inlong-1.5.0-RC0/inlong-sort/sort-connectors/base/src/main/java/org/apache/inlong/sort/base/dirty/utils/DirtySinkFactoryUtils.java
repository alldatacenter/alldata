/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.base.dirty.utils;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.factories.DynamicTableFactory.Context;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.dirty.sink.DirtySinkFactory;

/**
 * Dirty sink facotry utils, it helps to create dirty sink
 */
public final class DirtySinkFactoryUtils {

    private DirtySinkFactoryUtils() {
    }

    public static <T> DirtySink<T> createDirtySink(Context context, DirtyOptions dirtyOptions) {
        if (dirtyOptions == null) {
            dirtyOptions = DirtyOptions.fromConfig(Configuration.fromMap(context.getCatalogTable().getOptions()));
        }
        dirtyOptions.validate();
        DirtySink<T> dirtySink = null;
        if (dirtyOptions.ignoreDirty() && dirtyOptions.enableDirtySideOutput()) {
            DirtySinkFactory dirtySinkFactory = FactoryUtil.discoverFactory(context.getClassLoader(),
                    DirtySinkFactory.class, dirtyOptions.getDirtyConnector());
            dirtySink = dirtySinkFactory.createDirtySink(context);
        }
        return dirtySink;
    }
}
