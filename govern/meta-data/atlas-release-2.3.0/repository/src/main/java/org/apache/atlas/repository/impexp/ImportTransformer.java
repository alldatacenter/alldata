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
package org.apache.atlas.repository.impexp;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public abstract class ImportTransformer {
    private static final String TRANSFORMER_PARAMETER_SEPARATOR = "\\:";

    private static final String TRANSFORMER_NAME_ADD = "add";
    private static final String TRANSFORMER_NAME_CLEAR_ATTR = "clearAttrValue";
    private static final String TRANSFORMER_NAME_LOWERCASE = "lowercase";
    private static final String TRANSFORMER_NAME_UPPERCASE = "uppercase";
    private static final String TRANSFORMER_NAME_REMOVE_CLASSIFICATION = "removeClassification";
    private static final String TRANSFORMER_NAME_ADD_CLASSIFICATION = "addClassification";
    private static final String TRANSFORMER_NAME_REPLACE = "replace";
    private static final String TRANSFORMER_SET_DELETED = "setDeleted";

    private final String transformType;


    public static ImportTransformer getTransformer(String transformerSpec) throws AtlasBaseException {
        String[] params = StringUtils.split(transformerSpec, TRANSFORMER_PARAMETER_SEPARATOR);
        String   key    = (params == null || params.length < 1) ? transformerSpec : params[0];

        final ImportTransformer ret;

        if (StringUtils.isEmpty(key)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_VALUE, "Error creating ImportTransformer. Invalid transformer-specification: {}.", transformerSpec);
        } else if (key.equals(TRANSFORMER_NAME_REPLACE)) {
            String toFindStr  = (params == null || params.length < 2) ? "" : params[1];
            String replaceStr = (params == null || params.length < 3) ? "" : params[2];

            ret = new Replace(toFindStr, replaceStr);
        } else if (key.equals(TRANSFORMER_NAME_LOWERCASE)) {
            ret = new Lowercase();
        } else if (key.equals(TRANSFORMER_NAME_UPPERCASE)) {
            ret = new Uppercase();
        } else if (key.equals(TRANSFORMER_NAME_REMOVE_CLASSIFICATION)) {
            String name = (params == null || params.length < 1) ? "" : StringUtils.join(params, ":", 1, params.length);
            ret = new RemoveClassification(name);
        } else if (key.equals(TRANSFORMER_NAME_ADD)) {
            String name = (params == null || params.length < 1) ? "" : StringUtils.join(params, ":", 1, params.length);
            ret = new AddValueToAttribute(name);
        } else if (key.equals(TRANSFORMER_NAME_CLEAR_ATTR)) {
            String name = (params == null || params.length < 1) ? "" : StringUtils.join(params, ":", 1, params.length);
            ret = new ClearAttributes(name);
        } else if (key.equals(TRANSFORMER_SET_DELETED)) {
            ret = new SetDeleted();
        } else if (key.equals(TRANSFORMER_NAME_ADD_CLASSIFICATION)) {
            String name = (params == null || params.length < 1) ? "" : params[1];
            ret = new AddClassification(name, (params != null && params.length == 3) ? params[2] : "");
        } else {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_VALUE, "Error creating ImportTransformer. Unknown transformer: {}.", transformerSpec);
        }

        return ret;
    }

    public String getTransformType() { return transformType; }

    public abstract Object apply(Object o) throws AtlasBaseException;


    protected ImportTransformer(String transformType) {
        this.transformType = transformType;
    }

    static class Replace extends ImportTransformer {
        private final String toFindStr;
        private final String replaceStr;

        public Replace(String toFindStr, String replaceStr) {
            super(TRANSFORMER_NAME_REPLACE);

            this.toFindStr  = toFindStr;
            this.replaceStr = replaceStr;
        }

        public String getToFindStr() { return toFindStr; }

        public String getReplaceStr() { return replaceStr; }

        @Override
        public Object apply(Object o) {
            Object ret = o;

            if(o instanceof String) {
                ret = StringUtils.replace((String) o, toFindStr, replaceStr);
            }

            return ret;
        }
    }

    static class Lowercase extends ImportTransformer {
        public Lowercase() {
            super(TRANSFORMER_NAME_LOWERCASE);
        }

        @Override
        public Object apply(Object o) {
            Object ret = o;

            if(o instanceof String) {
                ret = StringUtils.lowerCase((String) o);
            }

            return ret;
        }
    }

    static class Uppercase extends ImportTransformer {
        public Uppercase() {
            super(TRANSFORMER_NAME_UPPERCASE);
        }

        @Override
        public Object apply(Object o) {
            Object ret = o;

            if(o instanceof String) {
                ret = StringUtils.upperCase((String) o);
            }

            return ret;
        }
    }

    static class AddClassification extends ImportTransformer {
        private static final String FILTER_SCOPE_TOP_LEVEL = "topLevel";

        private final String scope;
        private final String classificationName;
        private List<AtlasObjectId> filters;

        public AddClassification(String name, String scope) {
            super(TRANSFORMER_NAME_REMOVE_CLASSIFICATION);

            this.classificationName = name;
            this.scope = scope;
            filters = new ArrayList<>();
        }

        public void addFilter(AtlasObjectId objectId) {
            filters.add(objectId);
        }

        @Override
        public Object apply(Object o) {
            if (!(o instanceof AtlasEntity)) {
                return o;
            }

            AtlasEntity entity = (AtlasEntity) o;
            if(!passThruFilters(entity)) {
                return o;
            }

            if(entity.getClassifications() == null) {
                entity.setClassifications(new ArrayList<AtlasClassification>());
            }

            for (AtlasClassification c : entity.getClassifications()) {
                if (c.getTypeName().equals(classificationName)) {
                    return entity;
                }
            }

            entity.getClassifications().add(new AtlasClassification(classificationName));
            return entity;
        }

        private boolean passThruFilters(AtlasEntity entity) {
            if(StringUtils.isEmpty(scope) || !scope.equals(FILTER_SCOPE_TOP_LEVEL)) {
                return true;
            }

            for (AtlasObjectId filter : filters) {
                if(isMatch(filter, entity)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isMatch(AtlasObjectId objectId, AtlasEntity entity) {
            boolean ret = true;
            if (StringUtils.isEmpty(objectId.getGuid())) {
                ret = Objects.equals(objectId.getTypeName(), entity.getTypeName());
                if (ret) {
                    for (Map.Entry<String, Object> entry : objectId.getUniqueAttributes().entrySet()) {
                        ret = ret && Objects.equals(entity.getAttribute(entry.getKey()), entry.getValue());
                        if (!ret) {
                            break;
                        }
                    }
                }

                return ret;

            } else {
                return Objects.equals(objectId.getGuid(), entity.getGuid());
            }
        }

        @Override
        public String toString() {
            return String.format("%s=%s", "AddClassification", classificationName);
        }

        public String getClassificationName() {
            return classificationName;
        }
    }

    static class RemoveClassification extends ImportTransformer {
        private final String classificationToBeRemoved;

        public RemoveClassification(String name) {
            super(TRANSFORMER_NAME_REMOVE_CLASSIFICATION);

            this.classificationToBeRemoved = name;
        }

        @Override
        public Object apply(Object o) {
            if (!(o instanceof AtlasEntity)) {
                return o;
            }

            AtlasEntity entity = (AtlasEntity) o;
            if(entity.getClassifications().size() == 0) {
                return o;
            }

            List<AtlasClassification> toRemove = null;
            for (AtlasClassification classification : entity.getClassifications()) {
                if (classification.getTypeName().equals(classificationToBeRemoved)) {
                    if (toRemove == null) {
                        toRemove = new ArrayList<AtlasClassification>();
                    }


                    toRemove.add(classification);

                }
            }

            if (toRemove != null) {
                entity.getClassifications().removeAll(toRemove);
            }

            return entity;
        }

        @Override
        public String toString() {
            return String.format("%s=%s", "RemoveClassification", classificationToBeRemoved);
        }
    }

    static class AddValueToAttribute extends ImportTransformer {
        private final String nameValuePair;
        private String attrName;
        private String attrValueRaw;
        private Object attrValue;

        protected AddValueToAttribute(String nameValuePair) {
            super(TRANSFORMER_NAME_ADD);

            this.nameValuePair = nameValuePair;
            setAttrNameValue(this.nameValuePair);
        }

        private void setAttrNameValue(String nameValuePair) {
            String SEPARATOR_EQUALS = "=";
            if(!nameValuePair.contains(SEPARATOR_EQUALS)) return;

            String splits[] = StringUtils.split(nameValuePair, SEPARATOR_EQUALS);
            if(splits.length == 0) {
                return;
            }

            if(splits.length >= 1) {
                attrName = splits[0];
            }

            if(splits.length >= 1) {
                attrValueRaw = splits[1];
            }

            setAttrValue(attrValueRaw);
        }

        private void setAttrValue(String attrValueRaw) {
            final String type_prefix = "list:";

            if(attrValueRaw.startsWith(type_prefix)) {
                final String item = StringUtils.remove(attrValueRaw, type_prefix);
                attrValue = new ArrayList<String>() {{
                    add(item);
                }};
            } else {
                attrValue = attrValueRaw;
            }
        }

        @Override
        public Object apply(Object o) {
            if(o == null) {
                return o;
            }

            if(!(o instanceof AtlasEntity)) {
                return o;
            }

            AtlasEntity entity = (AtlasEntity) o;
            Object attrExistingValue = entity.getAttribute(attrName);
            if(attrExistingValue == null) {
                entity.setAttribute(attrName, attrValue);
            } else if(attrExistingValue instanceof List) {
                List list = (List) attrExistingValue;

                if(attrValue instanceof List) {
                    list.addAll((List) attrValue);
                } else {
                    list.add(attrValue);
                }
            } else {
                entity.setAttribute(attrName, attrValueRaw);
            }

            return entity;
        }
    }

    static class ClearAttributes extends ImportTransformer {
        private String[] attrNames;

        protected ClearAttributes(String attrNames) {
            super(TRANSFORMER_NAME_CLEAR_ATTR);

            this.attrNames = StringUtils.split(attrNames, ",");
        }

        @Override
        public Object apply(Object o) {
            if (o == null) {
                return o;
            }

            if (!(o instanceof AtlasEntity)) {
                return o;
            }

            AtlasEntity entity = (AtlasEntity) o;
            for (String attrName : attrNames) {
                entity.setAttribute(attrName, null);
            }

            return entity;
        }
    }

    static class SetDeleted extends ImportTransformer {
        protected SetDeleted() {
            super(TRANSFORMER_SET_DELETED);
        }

        @Override
        public Object apply(Object o) {
            if (o == null) {
                return o;
            }

            if (!(o instanceof AtlasEntity)) {
                return o;
            }

            AtlasEntity entity = (AtlasEntity) o;
            entity.setStatus(AtlasEntity.Status.DELETED);
            return entity;
        }
    }
}
