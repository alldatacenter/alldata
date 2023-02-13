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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.impl.ExtensionRefreshException;
import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Discovers the implementations of an extension point.
 * <p>
 * This extension point allows you to write your implementations of
 * in arbitrary DI containers, and have Hudson discover them.
 * <p>
 * {@link ExtensionFinder} itself is an extension point, but to avoid infinite recursion,
 * Jenkins discovers {@link ExtensionFinder}s through {@link Sezpoz} and that alone.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 * @since 1.286
 */
public abstract class ExtensionFinder {

    // @Restricted(NoExternalUse.class)
    @Deprecated
    public <T> Collection<T> findExtensions(Class<T> type, TIS hudson) {
        return Collections.emptyList();
    }

    /**
     * 将已有的插件对象删除
     *
     * @param superType
     */
    public abstract void removeByType(Class<?> superType);
    /**
     * Returns true if this extension finder supports the {@link #refresh()} operation.
     */
    //  public boolean isRefreshable() {
//        try {
//            return getClass().getMethod("refresh").getDeclaringClass() != ExtensionFinder.class;
//        } catch (NoSuchMethodException e) {
//            return false;
//        }
    //     return true;
    // }

    /**
     * Rebuilds the internal index, if any, so that future
     * will discover components newly added to {@link PluginManager#uberClassLoader}.
     *
     * <p>
     * The point of the refresh operation is not to disrupt instances of already loaded {@link ExtensionComponent}s,
     * and only instantiate those that are new. Otherwise this will break the singleton semantics of various
     * objects, such as {@link Descriptor}s.
     *
     * <p>
     * The behaviour is undefined if {@link #'isRefreshable()'} is returning false.
     *
     * @return never null
     * @see #'isRefreshable()'
     * @since 1.442
     */
    public abstract ExtensionComponentSet refresh() throws ExtensionRefreshException;

    public abstract <T> Collection<ExtensionComponent<T>> find(Class<T> type, TIS jenkins);

    @Deprecated
    public <T> Collection<ExtensionComponent<T>> _find(Class<T> type, TIS hudson) {
        return find(type, hudson);
    }

    /**
     * Performs class initializations without creating instances.
     * <p>
     * If two threads try to initialize classes in the opposite order, a dead lock will ensue,
     * and we can get into a similar situation with {@link ExtensionFinder}s.
     *
     * <p>
     * That is, one thread can try to list extensions, which results in {@link ExtensionFinder}
     * loading and initializing classes. This happens inside a context of a lock, so that
     * another thread that tries to list the same extensions don't end up creating different
     * extension instances. So this activity locks extension list first, then class initialization next.
     *
     * <p>
     * In the mean time, another thread can load and initialize a class, and that initialization
     * can eventually results in listing up extensions, for example through static initializer.
     * Such activity locks class initialization first, then locks extension list.
     *
     * <p>
     * This inconsistent locking order results in a dead lock, you see.
     *
     * <p>
     *
     * <p>
     * See https://bugs.openjdk.java.net/browse/JDK-4993813 for how to force a class initialization.
     * Also see http://kohsuke.org/2010/09/01/deadlock-that-you-cant-avoid/ for how class initialization
     * can results in a dead lock.
     */
    public void scout(Class extensionType, TIS hudson) {
    }

    /**
     * The bootstrap implementation that looks for the marker.
     *
     * <p>
     * Uses Sezpoz as the underlying mechanism.
     */
    public static final class Sezpoz extends ExtensionFinder {

        private volatile List<IndexItem<TISExtension, Object>> indices;

        private List<IndexItem<TISExtension, Object>> getIndices() {
            // 5. dead lock
            if (indices == null) {
                ClassLoader cl = TIS.get().getPluginManager().uberClassLoader;
                indices = ImmutableList.copyOf(Index.load(TISExtension.class, Object.class, cl));
            }
            return indices;
        }

        public void removeByType(Class<?> superType) {
            if (superType == null) {
                throw new IllegalArgumentException("param className can not be null");
            }
            try {
                List<IndexItem<TISExtension, Object>> mask = Lists.newArrayList();
                for (IndexItem<TISExtension, Object> index : indices) {
                    if (index.kind() == ElementType.TYPE && superType.isAssignableFrom((Class) index.element())) {
                        mask.add(index);
                    }
                }
                if (mask.size() > 0) {
                    indices = ImmutableList.copyOf(indices.stream().filter((i) -> !mask.contains(i)).collect(Collectors.toList()));
                }
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * SezPoz implements value-equality of {@link IndexItem}, so
         */
        @Override
        public synchronized ExtensionComponentSet refresh() {
            final List<IndexItem<TISExtension, Object>> old = indices;
            // we haven't loaded anything
            if (old == null) {
                return ExtensionComponentSet.EMPTY;
            }
            final List<IndexItem<TISExtension, Object>> delta = listDelta(TISExtension.class, old);
            List<IndexItem<TISExtension, Object>> r = Lists.newArrayList(old);
            r.addAll(delta);
            indices = ImmutableList.copyOf(r);
            return new ExtensionComponentSet() {

                @Override
                public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                    return _find(type, delta);
                }
            };
        }

        static <T extends Annotation> List<IndexItem<T, Object>> listDelta(
                Class<T> annotationType, List<? extends IndexItem<?, Object>> old) {
            // list up newly discovered components
            final List<IndexItem<T, Object>> delta = Lists.newArrayList();
            ClassLoader cl = TIS.get().getPluginManager().uberClassLoader;
            for (IndexItem<T, Object> ii : Index.load(annotationType, Object.class, cl)) {
                if (!old.contains(ii)) {
                    delta.add(ii);
                }
            }
            return delta;
        }

        public <T> Collection<ExtensionComponent<T>> find(Class<T> type, TIS tis) {
            return _find(type, getIndices());
        }

        /**
         * Finds all the matching {@link IndexItem}s that match the given type and instantiate them.
         */
        private <T> Collection<ExtensionComponent<T>> _find(Class<T> type, List<IndexItem<TISExtension, Object>> indices) {
            List<ExtensionComponent<T>> result = new ArrayList<>();
            for (IndexItem<TISExtension, Object> item : indices) {
                try {
                    AnnotatedElement e = item.element();
                    Class<?> extType;
                    if (e instanceof Class) {
                        extType = (Class) e;
                    } else if (e instanceof Field) {
                        extType = ((Field) e).getType();
                    } else if (e instanceof Method) {
                        extType = ((Method) e).getReturnType();
                    } else
                        throw new AssertionError();
                    if (type.isAssignableFrom(extType)) {
                        Object instance = item.instance();
                        if (instance != null)
                            result.add(new ExtensionComponent<>(type.cast(instance), item.annotation()));
                    }
                } catch (LinkageError | Exception e) {
                    // sometimes the instantiation fails in an indirect classloading failure,
                    // which results in a LinkageError
                    LOGGER.log(logLevel(item), "Failed to load " + item.className(), e);
                }
            }
            return result;
        }

        @Override
        public void scout(Class extensionType, TIS tis) {
            for (IndexItem<TISExtension, Object> item : getIndices()) {
                try {
                    // we might end up having multiple threads concurrently calling into element(),
                    // but we can't synchronize this --- if we do, the one thread that's supposed to load a class
                    // can block while other threads wait for the entry into the element call().
                    // looking at the sezpoz code, it should be safe to do so
                    AnnotatedElement e = item.element();
                    Class<?> extType;
                    if (e instanceof Class) {
                        extType = (Class) e;
                    } else if (e instanceof Field) {
                        extType = ((Field) e).getType();
                    } else if (e instanceof Method) {
                        extType = ((Method) e).getReturnType();
                    } else
                        throw new AssertionError();
                    // according to JDK-4993813 this is the only way to force class initialization
                    Class.forName(extType.getName(), true, extType.getClassLoader());
                } catch (Exception | LinkageError e) {
                    LOGGER.log(logLevel(item), "Failed to scout " + item.className(), e);
                }
            }
        }

        private Level logLevel(IndexItem<TISExtension, Object> item) {
            return item.annotation().optional() ? Level.FINE : Level.WARNING;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ExtensionFinder.class.getName());
}
