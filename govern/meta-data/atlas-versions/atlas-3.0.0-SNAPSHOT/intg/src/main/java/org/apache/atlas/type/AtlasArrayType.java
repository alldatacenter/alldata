/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.type;


import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.SERVICE_TYPE_ATLAS_CORE;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.COUNT_NOT_SET;

/**
 * class that implements behaviour of an array-type.
 */
public class AtlasArrayType extends AtlasType {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasArrayType.class);

    private final String elementTypeName;
    private int          minCount;
    private int          maxCount;
    private Cardinality  cardinality;

    private AtlasType elementType;

    public AtlasArrayType(AtlasType elementType) {
        this(elementType, COUNT_NOT_SET, COUNT_NOT_SET, Cardinality.LIST);
    }

    public AtlasArrayType(AtlasType elementType, int minCount, int maxCount, Cardinality cardinality) {
        super(AtlasBaseTypeDef.getArrayTypeName(elementType.getTypeName()), TypeCategory.ARRAY, SERVICE_TYPE_ATLAS_CORE);

        this.elementTypeName = elementType.getTypeName();
        this.minCount        = minCount;
        this.maxCount        = maxCount;
        this.cardinality     = cardinality;
        this.elementType     = elementType;
    }

    public AtlasArrayType(String elementTypeName, AtlasTypeRegistry typeRegistry) throws AtlasBaseException {
        this(elementTypeName, COUNT_NOT_SET, COUNT_NOT_SET, Cardinality.LIST, typeRegistry);
    }

    public AtlasArrayType(String elementTypeName, int minCount, int maxCount, Cardinality cardinality, AtlasTypeRegistry typeRegistry)
        throws  AtlasBaseException {
        super(AtlasBaseTypeDef.getArrayTypeName(elementTypeName), TypeCategory.ARRAY, SERVICE_TYPE_ATLAS_CORE);

        this.elementTypeName = elementTypeName;
        this.minCount        = minCount;
        this.maxCount        = maxCount;
        this.cardinality     = cardinality;

        this.resolveReferences(typeRegistry);
    }

    public String getElementTypeName() {
        return elementTypeName;
    }

    public void setMinCount(int minCount) { this.minCount = minCount; }

    public int getMinCount() {
        return minCount;
    }

    public void setMaxCount(int maxCount) { this.maxCount = maxCount; }

    public int getMaxCount() {
        return maxCount;
    }

    public void setCardinality(Cardinality cardinality) { this.cardinality = cardinality; }

    public Cardinality getCardinality() { return cardinality; }

    public AtlasType getElementType() {
        return elementType;
    }

    @Override
    void resolveReferences(AtlasTypeRegistry typeRegistry) throws AtlasBaseException {
        elementType = typeRegistry.getType(elementTypeName);
    }

    @Override
    public Collection<?> createDefaultValue() {
        Collection<Object> ret = new ArrayList<>();

        ret.add(elementType.createDefaultValue());

        if (minCount != COUNT_NOT_SET) {
            for (int i = 1; i < minCount; i++) {
                ret.add(elementType.createDefaultValue());
            }
        }

        return ret;
    }

    @Override
    public boolean isValidValue(Object obj) {
        if (obj != null) {
            if (obj instanceof List || obj instanceof Set) {
                Collection objList = (Collection) obj;

                if (!isValidElementCount(objList.size())) {
                    return false;
                }

                for (Object element : objList) {
                    if (!elementType.isValidValue(element)) {
                        return false;
                    }
                }
            } else if (obj.getClass().isArray()) {
                int arrayLen = Array.getLength(obj);

                if (!isValidElementCount(arrayLen)) {
                    return false;
                }

                for (int i = 0; i < arrayLen; i++) {
                    if (!elementType.isValidValue(Array.get(obj, i))) {
                        return false;
                    }
                }
            } else {
                return false; // invalid type
            }
        }

        return true;
    }

    @Override
    public boolean areEqualValues(Object val1, Object val2, Map<String, String> guidAssignments) {
        final boolean ret;

        if (cardinality == Cardinality.SET) {
            ret = areEqualSets(val1, val2, guidAssignments);
        } else {
            ret = areEqualLists(val1, val2, guidAssignments);
        }

        return ret;
    }

    @Override
    public boolean isValidValueForUpdate(Object obj) {
        if (obj != null) {
            if (obj instanceof List || obj instanceof Set) {
                Collection objList = (Collection) obj;

                if (!isValidElementCount(objList.size())) {
                    return false;
                }

                for (Object element : objList) {
                    if (!elementType.isValidValueForUpdate(element)) {
                        return false;
                    }
                }
            } else if (obj.getClass().isArray()) {
                int arrayLen = Array.getLength(obj);

                if (!isValidElementCount(arrayLen)) {
                    return false;
                }

                for (int i = 0; i < arrayLen; i++) {
                    if (!elementType.isValidValueForUpdate(Array.get(obj, i))) {
                        return false;
                    }
                }
            } else {
                return false; // invalid type
            }
        }

        return true;
    }

    @Override
    public Collection<?> getNormalizedValue(Object obj) {
        Collection<Object> ret = null;

        if (obj instanceof String) {
            obj = AtlasType.fromJson(obj.toString(), List.class);
        }

        if (obj instanceof List || obj instanceof Set) {
            Collection collObj = (Collection) obj;

            if (isValidElementCount(collObj.size())) {
                ret = new ArrayList<>(collObj.size());

                for (Object element : collObj) {
                    if (element != null) {
                        Object normalizedValue = elementType.getNormalizedValue(element);

                        if (normalizedValue != null) {
                            ret.add(normalizedValue);
                        } else {
                            ret = null; // invalid element value

                            break;
                        }
                    } else {
                        ret.add(element);
                    }
                }
            }
        } else if (obj != null && obj.getClass().isArray()) {
            int arrayLen = Array.getLength(obj);

            if (isValidElementCount(arrayLen)) {
                ret = new ArrayList<>(arrayLen);

                for (int i = 0; i < arrayLen; i++) {
                    Object element = Array.get(obj, i);

                    if (element != null) {
                        Object normalizedValue = elementType.getNormalizedValue(element);

                        if (normalizedValue != null) {
                            ret.add(normalizedValue);
                        } else {
                            ret = null; // invalid element value

                            break;
                        }
                    } else {
                        ret.add(element);
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public Collection<?> getNormalizedValueForUpdate(Object obj) {
        Collection<Object> ret = null;

        if (obj instanceof String) {
            obj = AtlasType.fromJson(obj.toString(), List.class);
        }

        if (obj instanceof List || obj instanceof Set) {
            Collection objList = (Collection) obj;

            if (isValidElementCount(objList.size())) {
                ret = new ArrayList<>(objList.size());

                for (Object element : objList) {
                    if (element != null) {
                        Object normalizedValue = elementType.getNormalizedValueForUpdate(element);

                        if (normalizedValue != null) {
                            ret.add(normalizedValue);
                        } else {
                            ret = null; // invalid element value

                            break;
                        }
                    } else {
                        ret.add(element);
                    }
                }
            }
        } else if (obj != null && obj.getClass().isArray()) {
            int arrayLen = Array.getLength(obj);

            if (isValidElementCount(arrayLen)) {
                ret = new ArrayList<>(arrayLen);

                for (int i = 0; i < arrayLen; i++) {
                    Object element = Array.get(obj, i);

                    if (element != null) {
                        Object normalizedValue = elementType.getNormalizedValueForUpdate(element);

                        if (normalizedValue != null) {
                            ret.add(normalizedValue);
                        } else {
                            ret = null; // invalid element value

                            break;
                        }
                    } else {
                        ret.add(element);
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public boolean validateValue(Object obj, String objName, List<String> messages) {
        boolean ret = true;

        if (obj != null) {
            if (obj instanceof List || obj instanceof Set) {
                Collection objList = (Collection) obj;

                if (!isValidElementCount(objList.size())) {
                    ret = false;

                    messages.add(objName + ": incorrect number of values. found=" + objList.size()
                            + "; expected: minCount=" + minCount + ", maxCount=" + maxCount);
                }

                int idx = 0;
                for (Object element : objList) {
                    ret = elementType.validateValue(element, objName + "[" + idx + "]", messages) && ret;
                    idx++;
                }
            } else if (obj.getClass().isArray()) {
                int arrayLen = Array.getLength(obj);

                if (!isValidElementCount(arrayLen)) {
                    ret = false;

                    messages.add(objName + ": incorrect number of values. found=" + arrayLen
                            + "; expected: minCount=" + minCount + ", maxCount=" + maxCount);
                }

                for (int i = 0; i < arrayLen; i++) {
                    ret = elementType.validateValue(Array.get(obj, i), objName + "[" + i + "]", messages) && ret;
                }
            } else {
                ret = false;

                messages.add(objName + "=" + obj + ": invalid value for type " + getTypeName());
            }
        }

        return ret;
    }

    @Override
    public boolean validateValueForUpdate(Object obj, String objName, List<String> messages) {
        boolean ret = true;

        if (obj != null) {
            if (obj instanceof List || obj instanceof Set) {
                Collection objList = (Collection) obj;

                if (!isValidElementCount(objList.size())) {
                    ret = false;

                    messages.add(objName + ": incorrect number of values. found=" + objList.size()
                            + "; expected: minCount=" + minCount + ", maxCount=" + maxCount);
                }

                int idx = 0;
                for (Object element : objList) {
                    ret = elementType.validateValueForUpdate(element, objName + "[" + idx + "]", messages) && ret;
                    idx++;
                }
            } else if (obj.getClass().isArray()) {
                int arrayLen = Array.getLength(obj);

                if (!isValidElementCount(arrayLen)) {
                    ret = false;

                    messages.add(objName + ": incorrect number of values. found=" + arrayLen
                            + "; expected: minCount=" + minCount + ", maxCount=" + maxCount);
                }

                for (int i = 0; i < arrayLen; i++) {
                    ret = elementType.validateValueForUpdate(Array.get(obj, i), objName + "[" + i + "]", messages) && ret;
                }
            } else {
                ret = false;

                messages.add(objName + "=" + obj + ": invalid value for type " + getTypeName());
            }
        }

        return ret;
    }

    @Override
    public AtlasType getTypeForAttribute() {
        AtlasType elementAttributeType = elementType.getTypeForAttribute();

        if (elementAttributeType == elementType) {
            return this;
        } else {
            AtlasType attributeType = new AtlasArrayType(elementAttributeType, minCount, maxCount, cardinality);

            if (LOG.isDebugEnabled()) {
                LOG.debug("getTypeForAttribute(): {} ==> {}", getTypeName(), attributeType.getTypeName());
            }

            return attributeType;
        }
    }

    private boolean isValidElementCount(int count) {
        if (minCount != COUNT_NOT_SET) {
            if (count < minCount) {
                return false;
            }
        }

        if (maxCount != COUNT_NOT_SET) {
            if (count > maxCount) {
                return false;
            }
        }

        return true;
    }

    private boolean isEmptyArrayValue(Object val) {
        if (val instanceof Collection) {
            return ((Collection) val).isEmpty();
        } else if (val.getClass().isArray()) {
            return Array.getLength(val) == 0;
        } else if (val instanceof String){
            List list = AtlasType.fromJson(val.toString(), List.class);

            return list == null || list.isEmpty();
        }

        return false;
    }

    private boolean areEqualSets(Object val1, Object val2, Map<String, String> guidAssignments) {
        boolean ret  = true;

        if (val1 == null) {
            ret = val2 == null;
        } else if (val2 == null) {
            ret = false;
        } else if (val1 == val2) {
            ret = true;
        } else {
            Set set1 = getSetFromValue(val1);
            Set set2 = getSetFromValue(val2);

            if (set1.size() != set2.size()) {
                ret = false;
            } else {
                for (Object elem1 : set1) {
                    boolean foundInSet2 = false;

                    for (Object elem2 : set2) {
                        if (elementType.areEqualValues(elem1, elem2, guidAssignments)) {
                            foundInSet2 = true;

                            break;
                        }
                    }

                    if (!foundInSet2) {
                        ret = false;

                        break;
                    }
                }
            }
        }

        return ret;
    }

    private boolean areEqualLists(Object val1, Object val2, Map<String, String> guidAssignments) {
        boolean ret = true;

        if (val1 == null) {
            ret = val2 == null;
        } else if (val2 == null) {
            ret = false;
        } else if (val1 == val2) {
            ret = true;
        } else if (val1.getClass().isArray() && val2.getClass().isArray()) {
            int len = Array.getLength(val1);

            if (len != Array.getLength(val2)) {
                ret = false;
            } else {
                for (int i = 0; i < len; i++) {
                    if (!elementType.areEqualValues(Array.get(val1, i), Array.get(val2, i), guidAssignments)) {
                        ret = false;

                        break;
                    }
                }
            }
        } else {
            List list1 = getListFromValue(val1);
            List list2 = getListFromValue(val2);

            if (list1.size() != list2.size()) {
                ret = false;
            } else {
                int len = list1.size();

                for (int i = 0; i < len; i++) {
                    if (!elementType.areEqualValues(list1.get(i), list2.get(i), guidAssignments)) {
                        ret = false;

                        break;
                    }
                }
            }
        }

        return ret;
    }

    private List getListFromValue(Object val) {
        final List ret;

        if (val instanceof List) {
            ret = (List) val;
        } else if (val instanceof Collection) {
            ret = new ArrayList<>((Collection) val);
        } else if (val.getClass().isArray()) {
            int len = Array.getLength(val);

            ret = new ArrayList<>(len);

            for (int i = 0; i < len; i++) {
                ret.add(Array.get(val, i));
            }
        } else if (val instanceof String){
            ret = AtlasType.fromJson(val.toString(), List.class);
        } else {
            ret = null;
        }

        return ret;
    }

    private Set getSetFromValue(Object val) {
        final Set ret;

        if (val instanceof Set) {
            ret = (Set) val;
        } else if (val instanceof Collection) {
            ret = new HashSet<>((Collection) val);
        } else if (val.getClass().isArray()) {
            int len = Array.getLength(val);

            ret = new HashSet<>(len);

            for (int i = 0; i < len; i++) {
                ret.add(Array.get(val, i));
            }
        } else if (val instanceof String){
            ret = AtlasType.fromJson(val.toString(), Set.class);
        } else {
            ret = null;
        }

        return ret;
    }
}
