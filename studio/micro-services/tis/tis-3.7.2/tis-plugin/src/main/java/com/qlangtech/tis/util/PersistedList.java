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

package com.qlangtech.tis.util;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-09 10:31
 **/

import com.google.common.collect.ImmutableSet;
import com.qlangtech.tis.extension.Saveable;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collection whose change is notified to the parent object for persistence.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.333
 */
public class PersistedList<T> extends AbstractList<T> {

    private static final Logger LOGGER = Logger.getLogger(PersistedList.class.getName());

    protected final CopyOnWriteList<T> data = new CopyOnWriteList<>();
    protected Saveable owner = Saveable.NOOP;

    protected PersistedList() {
    }

    protected PersistedList(Collection<? extends T> initialList) {
        data.replaceBy(initialList);
    }

    public PersistedList(Saveable owner) {
        setOwner(owner);
    }

    public void setOwner(Saveable owner) {
        this.owner = owner;
    }

    @Override
    public boolean add(T item) {
        data.add(item);
        _onModified();
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> items) {
        data.addAll(items);
        _onModified();
        return true;
    }

    public void replaceBy(Collection<? extends T> col) throws IOException {
        data.replaceBy(col);
        onModified();
    }

    @Override
    public T get(int index) {
        return data.get(index);
    }

    public <U extends T> U get(Class<U> type) {
        for (T t : data)
            if(type.isInstance(t))
                return type.cast(t);
        return null;
    }

    /**
     * Gets all instances that matches the given type.
     */
    public <U extends T> List<U> getAll(Class<U> type) {
        List<U> r = new ArrayList<>();
        for (T t : data)
            if(type.isInstance(t))
                r.add(type.cast(t));
        return r;
    }

    @Override
    public int size() {
        return data.size();
    }

    /**
     * Removes an instance by its type.
     */
    public void remove(Class<? extends T> type) throws IOException {
        for (T t : data) {
            if(t.getClass()==type) {
                data.remove(t);
                onModified();
                return;
            }
        }
    }

    /**
     * A convenience method to replace a single item.
     *
     * This method shouldn't be used when you are replacing a lot of stuff
     * as copy-on-write semantics make this rather slow.
     */
    public void replace(T from, T to) throws IOException {
        List<T> copy = new ArrayList<>(data.getView());
        for (int i=0; i<copy.size(); i++) {
            if (copy.get(i).equals(from))
                copy.set(i,to);
        }
        data.replaceBy(copy);
    }

    @Override
    public boolean remove(Object o) {
        boolean b = data.remove((T)o);
        if (b)  _onModified();
        return b;
    }

    public void removeAll(Class<? extends T> type) throws IOException {
        boolean modified=false;
        for (T t : data) {
            if(t.getClass()==type) {
                data.remove(t);
                modified=true;
            }
        }
        if(modified)
            onModified();
    }


    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    /**
     * Called when a list is mutated.
     */
    protected void onModified() throws IOException {
        try {
            owner.save();
        } catch (IOException x) {
            Optional<T> ignored = stream().filter(PersistedList::ignoreSerializationErrors).findAny();
            if (ignored.isPresent()) {
                LOGGER.log(Level.WARNING, "Ignoring serialization errors in " + ignored.get() + "; update your parent POM to 4.8 or newer", x);
            } else {
                throw x;
            }
        }
    }

    // TODO until https://github.com/jenkinsci/jenkins-test-harness/pull/243 is widely adopted:
    private static final Set<String> IGNORED_CLASSES = ImmutableSet.of("org.jvnet.hudson.test.TestBuilder", "org.jvnet.hudson.test.TestNotifier");
    // (SingleFileSCM & ExtractResourceWithChangesSCM would also be nice to suppress, but they are not kept in a PersistedList.)
    private static boolean ignoreSerializationErrors(Object o) {
        if (o != null) {
            for (Class<?> c = o.getClass(); c != Object.class; c = c.getSuperclass()) {
                if (IGNORED_CLASSES.contains(c.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Version of {@link #onModified()} that throws an unchecked exception for compliance with {@link List}.
     */
    private void _onModified() {
        try {
            onModified();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the snapshot view of instances as list.
     */
    public List<T> toList() {
        return data.getView();
    }

    /**
     * Gets all the {@link 'Describable'}s in an array.
     */
    @Override
    public <T> T[] toArray(T[] array) {
        return data.toArray(array);
    }

    public void addAllTo(Collection<? super T> dst) {
        data.addAllTo(dst);
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean contains(Object item) {
        return data.contains(item);
    }

    @Override public String toString() {
        return toList().toString();
    }

    /**
     * {@link Converter} implementation for XStream.
     *
     * Serialization form is compatible with plain {@link List}.
     */
    public static class ConverterImpl extends AbstractCollectionConverter {
        CopyOnWriteList.ConverterImpl copyOnWriteListConverter;

        public ConverterImpl(Mapper mapper) {
            super(mapper);
            copyOnWriteListConverter = new CopyOnWriteList.ConverterImpl(mapper());
        }

        @Override
        public boolean canConvert(Class type) {
            // handle subtypes in case the onModified method is overridden.
            return PersistedList.class.isAssignableFrom(type);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            for (Object o : (PersistedList) source)
                writeItem(o, context, writer);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            CopyOnWriteList core = copyOnWriteListConverter.unmarshal(reader, context);

            try {
                PersistedList r = (PersistedList)context.getRequiredType().newInstance();
                r.data.replaceBy(core);
                return r;
            } catch (InstantiationException e) {
                InstantiationError x = new InstantiationError();
                x.initCause(e);
                throw x;
            } catch (IllegalAccessException e) {
                IllegalAccessError x = new IllegalAccessError();
                x.initCause(e);
                throw x;
            }
        }
    }
}
