/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.storm.hook;

import org.apache.commons.lang.StringUtils;
import org.apache.storm.generated.Bolt;
import org.apache.storm.generated.GlobalStreamId;
import org.apache.storm.generated.Grouping;
import org.apache.storm.generated.StormTopology;
import com.google.common.base.Joiner;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A storm topology utility class.
 */
public final class StormTopologyUtil {
    public static final Logger LOG = org.slf4j.LoggerFactory.getLogger(StormTopologyUtil.class);

    private StormTopologyUtil() {
    }

    public static Set<String> getTerminalUserBoltNames(StormTopology topology) {
        Set<String> terminalBolts = new HashSet<>();
        Set<String> inputs = new HashSet<>();
        for (Map.Entry<String, Bolt> entry : topology.get_bolts().entrySet()) {
            String name = entry.getKey();
            Set<GlobalStreamId> inputsForBolt = entry.getValue().get_common().get_inputs().keySet();
            if (!isSystemComponent(name)) {
                for (GlobalStreamId streamId : inputsForBolt) {
                    inputs.add(streamId.get_componentId());
                }
            }
        }

        for (String boltName : topology.get_bolts().keySet()) {
            if (!isSystemComponent(boltName) && !inputs.contains(boltName)) {
                terminalBolts.add(boltName);
            }
        }

        return terminalBolts;
    }

    public static boolean isSystemComponent(String componentName) {
        return componentName.startsWith("__");
    }

    public static Map<String, Set<String>> getAdjacencyMap(StormTopology topology,
                                                           boolean removeSystemComponent) {
        Map<String, Set<String>> adjacencyMap = new HashMap<>();

        for (Map.Entry<String, Bolt> entry : topology.get_bolts().entrySet()) {
            String boltName = entry.getKey();
            Map<GlobalStreamId, Grouping> inputs = entry.getValue().get_common().get_inputs();
            for (Map.Entry<GlobalStreamId, Grouping> input : inputs.entrySet()) {
                String inputComponentId = input.getKey().get_componentId();
                Set<String> components = adjacencyMap.containsKey(inputComponentId)
                        ? adjacencyMap.get(inputComponentId) : new HashSet<String>();
                components.add(boltName);
                components = removeSystemComponent ? removeSystemComponents(components)
                        : components;
                if (!removeSystemComponent || !isSystemComponent(inputComponentId)) {
                    adjacencyMap.put(inputComponentId, components);
                }
            }
        }

        return adjacencyMap;
    }

    public static Set<String> removeSystemComponents(Set<String> components) {
        Set<String> userComponents = new HashSet<>();
        for (String component : components) {
            if (!isSystemComponent(component))
                userComponents.add(component);
        }

        return userComponents;
    }

    private static final Set<Class> WRAPPER_TYPES = new HashSet<Class>() {{
        add(Boolean.class);
        add(Character.class);
        add(Byte.class);
        add(Short.class);
        add(Integer.class);
        add(Long.class);
        add(Float.class);
        add(Double.class);
        add(Void.class);
        add(String.class);
    }};

    public static boolean isWrapperType(Class clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    public static boolean isCollectionType(Class clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    public static boolean isMapType(Class clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    public static Map<String, String> getFieldValues(Object instance,
                                                     boolean prependClassName,
                                                     Set<Object> objectsToSkip) {
        if (objectsToSkip == null) {
            objectsToSkip = new HashSet<>();
        }

        Map<String, String> output = new HashMap<>();

        try {
            if (objectsToSkip.add(instance)) {
                Class clazz = instance.getClass();
                for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                    Field[] fields = c.getDeclaredFields();
                    for (Field field : fields) {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }

                        String key;
                        if (prependClassName) {
                            key = String.format("%s.%s", clazz.getSimpleName(), field.getName());
                        } else {
                            key = field.getName();
                        }

                        boolean accessible = field.isAccessible();
                        if (!accessible) {
                            field.setAccessible(true);
                        }
                        Object fieldVal = field.get(instance);
                        if (fieldVal == null) {
                            continue;
                        } else if (fieldVal.getClass().isPrimitive() ||
                                isWrapperType(fieldVal.getClass())) {
                            if (toString(fieldVal, false).isEmpty()) continue;
                            output.put(key, toString(fieldVal, false));
                        } else if (isMapType(fieldVal.getClass())) {
                            //TODO: check if it makes more sense to just stick to json
                            // like structure instead of a flatten output.
                            Map map = (Map) fieldVal;
                            for (Object entry : map.entrySet()) {
                                Object mapKey = ((Map.Entry) entry).getKey();
                                Object mapVal = ((Map.Entry) entry).getValue();

                                String keyStr = getString(mapKey, false, objectsToSkip);
                                String valStr = getString(mapVal, false, objectsToSkip);
                                if (StringUtils.isNotEmpty(valStr)) {
                                    output.put(String.format("%s.%s", key, keyStr), valStr);
                                }
                            }
                        } else if (isCollectionType(fieldVal.getClass())) {
                            //TODO check if it makes more sense to just stick to
                            // json like structure instead of a flatten output.
                            Collection collection = (Collection) fieldVal;
                            if (collection.size() == 0) continue;
                            String outStr = "";
                            for (Object o : collection) {
                                outStr += getString(o, false, objectsToSkip) + ",";
                            }
                            if (outStr.length() > 0) {
                                outStr = outStr.substring(0, outStr.length() - 1);
                            }
                            output.put(key, String.format("%s", outStr));
                        } else {
                            Map<String, String> nestedFieldValues = getFieldValues(fieldVal, false, objectsToSkip);
                            for (Map.Entry<String, String> entry : nestedFieldValues.entrySet()) {
                                output.put(String.format("%s.%s", key, entry.getKey()), entry.getValue());
                            }
                        }
                        if (!accessible) {
                            field.setAccessible(false);
                        }
                    }
                }
            }
        }
        catch (Exception e){
            LOG.warn("Exception while constructing topology", e);
        }
        return output;
    }

    private static String getString(Object instance,
                                    boolean wrapWithQuote,
                                    Set<Object> objectsToSkip) {
        if (instance == null) {
            return null;
        } else if (instance.getClass().isPrimitive() || isWrapperType(instance.getClass())) {
            return toString(instance, wrapWithQuote);
        } else {
            return getString(getFieldValues(instance, false, objectsToSkip), wrapWithQuote);
        }
    }

    private static String getString(Map<String, String> flattenFields, boolean wrapWithQuote) {
        String outStr = "";
        if (flattenFields != null && !flattenFields.isEmpty()) {
            if (wrapWithQuote) {
                outStr += "\"" + Joiner.on(",").join(flattenFields.entrySet()) + "\",";
            } else {
                outStr += Joiner.on(",").join(flattenFields.entrySet()) + ",";
            }
        }
        if (outStr.length() > 0) {
            outStr = outStr.substring(0, outStr.length() - 1);
        }
        return outStr;
    }

    private static String toString(Object instance, boolean wrapWithQuote) {
        if (instance instanceof String)
            if (wrapWithQuote)
                return "\"" + instance + "\"";
            else
                return instance.toString();
        else
            return instance.toString();
    }
}
