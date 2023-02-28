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
package com.qlangtech.tis.extension.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;

/**
 * Reflective access to various {@link ClassLoader} methods which are otherwise {@code protected}.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@SuppressWarnings("all")
public class ClassLoaderReflectionToolkit {

    private static final Method FIND_CLASS, FIND_LOADED_CLASS, FIND_RESOURCE, FIND_RESOURCES, GET_CLASS_LOADING_LOCK;

    static {
        try {
            FIND_CLASS = ClassLoader.class.getDeclaredMethod("findClass", String.class);
            FIND_CLASS.setAccessible(true);
            FIND_LOADED_CLASS = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            FIND_LOADED_CLASS.setAccessible(true);
            FIND_RESOURCE = ClassLoader.class.getDeclaredMethod("findResource", String.class);
            FIND_RESOURCE.setAccessible(true);
            FIND_RESOURCES = ClassLoader.class.getDeclaredMethod("findResources", String.class);
            FIND_RESOURCES.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        Method gCLL = null;
        try {
            gCLL = ClassLoader.class.getDeclaredMethod("getClassLoadingLock", String.class);
            gCLL.setAccessible(true);
        } catch (NoSuchMethodException x) {
            throw new AssertionError(x);
        }
        GET_CLASS_LOADING_LOCK = gCLL;
    }

    private static <T extends Exception> Object invoke(Method method, Class<T> exception, Object obj, Object... args) throws T {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException x) {
            throw new AssertionError(x);
        } catch (InvocationTargetException x) {
            Throwable x2 = x.getCause();
            if (x2 instanceof RuntimeException) {
                throw (RuntimeException) x2;
            } else if (x2 instanceof Error) {
                throw (Error) x2;
            } else if (exception.isInstance(x2)) {
                throw exception.cast(x2);
            } else {
                throw new AssertionError(x2);
            }
        }
    }

    private static Object getClassLoadingLock(ClassLoader cl, String name) {
        return invoke(GET_CLASS_LOADING_LOCK, RuntimeException.class, cl, name);
    }

    public static Class<?> _findLoadedClass(ClassLoader cl, String name) {
        synchronized (getClassLoadingLock(cl, name)) {
            return (Class) invoke(FIND_LOADED_CLASS, RuntimeException.class, cl, name);
        }
    }

    public static Class<?> _findClass(ClassLoader cl, String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(cl, name)) {
            return (Class) invoke(FIND_CLASS, ClassNotFoundException.class, cl, name);
        }
    }

    public static URL _findResource(ClassLoader cl, String name) {
        return (URL) invoke(FIND_RESOURCE, RuntimeException.class, cl, name);
    }

    public static Enumeration<URL> _findResources(ClassLoader cl, String name) throws IOException {
        return (Enumeration<URL>) invoke(FIND_RESOURCES, IOException.class, cl, name);
    }

    /**
     * @deprecated unsafe
     */
    @Deprecated
    public ClassLoaderReflectionToolkit() {
    }

    /**
     * @deprecated unsafe
     */
    @Deprecated
    public Class findLoadedClass(ClassLoader cl, String name) throws InvocationTargetException {
        try {
            return (Class) FIND_LOADED_CLASS.invoke(cl, name);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    /**
     * @deprecated unsafe
     */
    @Deprecated
    public Class findClass(ClassLoader cl, String name) throws InvocationTargetException {
        try {
            return (Class) FIND_CLASS.invoke(cl, name);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    /**
     * @deprecated unsafe
     */
    @Deprecated
    public URL findResource(ClassLoader cl, String name) throws InvocationTargetException {
        try {
            return (URL) FIND_RESOURCE.invoke(cl, name);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    /**
     * @deprecated unsafe
     */
    @Deprecated
    public Enumeration<URL> findResources(ClassLoader cl, String name) throws InvocationTargetException {
        try {
            return (Enumeration<URL>) FIND_RESOURCES.invoke(cl, name);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
