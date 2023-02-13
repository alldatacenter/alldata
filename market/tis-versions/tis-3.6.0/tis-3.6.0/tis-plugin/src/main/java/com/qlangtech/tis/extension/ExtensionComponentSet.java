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
package com.qlangtech.tis.extension;

import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents the components that's newly discovered during {@link ExtensionFinder#refresh()}.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class ExtensionComponentSet {

    /**
     * Discover extensions of the given type.
     *
     * @return Can be empty but never null.
     */
    public abstract <T> Collection<ExtensionComponent<T>> find(Class<T> type);

    public final ExtensionComponentSet filtered() {
        final ExtensionComponentSet base = this;
        return new ExtensionComponentSet() {

            @Override
            public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                // List<ExtensionComponent<T>> a = Lists.newArrayList();
                return base.find(type);
                // for (ExtensionComponent<T> c : base.find(type)) {
                // if (ExtensionFilter.isAllowed(type,c))
                // a.add(c);
                // }
                // return a;
            }
        };
    }

    /**
     * Constant that has zero component in it.
     */
    public static final ExtensionComponentSet EMPTY = new ExtensionComponentSet() {

        @Override
        public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
            return Collections.emptyList();
        }
    };

    /**
     * Computes the union of all the given delta.
     */
    public static ExtensionComponentSet union(final Collection<? extends ExtensionComponentSet> base) {
        return new ExtensionComponentSet() {

            @Override
            public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                List<ExtensionComponent<T>> r = Lists.newArrayList();
                for (ExtensionComponentSet d : base) { r.addAll(d.find(type));}
                return r;
            }
        };
    }

    public static ExtensionComponentSet union(ExtensionComponentSet... members) {
        return union(Arrays.asList(members));
    }

    /**
     * Wraps {@link ExtensionFinder} into {@link ExtensionComponentSet}.
     */
    public static ExtensionComponentSet allOf(final ExtensionFinder f) {
        return new ExtensionComponentSet() {

            @Override
            public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                return f.find(type, TIS.get());
            }
        };
    }
}
