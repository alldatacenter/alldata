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

import com.qlangtech.tis.util.Util;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ExtensionComponent<T> implements Comparable<ExtensionComponent<T>> {

    private static final Logger LOG = Logger.getLogger(ExtensionComponent.class.getName());

    private final T instance;

    private final double ordinal;

    public ExtensionComponent(T instance, double ordinal) {
        this.instance = instance;
        this.ordinal = ordinal;
    }

    public ExtensionComponent(T instance, TISExtension annotation) {
        this(instance, annotation.ordinal());
    }

    public ExtensionComponent(T instance) {
        this(instance, 0);
    }

    /**
     */
    public double ordinal() {
        return ordinal;
    }

    /**
     * The instance of the discovered extension.
     *
     * @return never null.
     */
    public T getInstance() {
        return instance;
    }

    /**
     * Checks if this component is a {@link Descriptor} describing the given type
     *
     * For example, {@code component.isDescriptorOf(Builder.class)}
     */
    public boolean isDescriptorOf(Class<? extends Describable> c) {
        return instance instanceof Descriptor && ((Descriptor) instance).isSubTypeOf(c);
    }

    /**
     * Sort {@link ExtensionComponent}s in the descending order of {@link #ordinal()}.
     */
    public int compareTo(ExtensionComponent<T> that) {
        double a = this.ordinal();
        double b = that.ordinal();
        if (a > b)
            return -1;
        if (a < b)
            return 1;
        // make the order bit more deterministic among extensions of the same ordinal
        if (this.instance instanceof Descriptor && that.instance instanceof Descriptor) {
            try {
                return Util.fixNull(((Descriptor) this.instance).getDisplayName()).compareTo(Util.fixNull(((Descriptor) that.instance).getDisplayName()));
            } catch (RuntimeException x) {
                LOG.log(Level.WARNING, null, x);
            } catch (LinkageError x) {
                LOG.log(Level.WARNING, null, x);
            }
        }
        return this.instance.getClass().getName().compareTo(that.instance.getClass().getName());
    }
}
