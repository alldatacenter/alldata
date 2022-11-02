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
package org.apache.atlas.entitytransform;

import org.apache.atlas.entitytransform.BaseEntityHandler.AtlasTransformableEntity;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;



public abstract class Condition {
    private static final Logger LOG = LoggerFactory.getLogger(Condition.class);

    private static final String CONDITION_DELIMITER                    = ":";
    private static final String CONDITION_ENTITY_OBJECT_ID             = "OBJECTID";
    private static final String CONDITION_ENTITY_TOP_LEVEL             = "TOPLEVEL";
    private static final String CONDITION_ENTITY_ALL                   = "ALL";
    private static final String CONDITION_NAME_EQUALS                  = "EQUALS";
    private static final String CONDITION_NAME_EQUALS_IGNORE_CASE      = "EQUALS_IGNORE_CASE";
    private static final String CONDITION_NAME_STARTS_WITH             = "STARTS_WITH";
    private static final String CONDITION_NAME_STARTS_WITH_IGNORE_CASE = "STARTS_WITH_IGNORE_CASE";
    private static final String CONDITION_NAME_HAS_VALUE               = "HAS_VALUE";

    protected final EntityAttribute attribute;


    protected Condition(EntityAttribute attribute) {
        this.attribute = attribute;
    }

    public EntityAttribute getAttribute() { return attribute; }

    public abstract boolean matches(AtlasTransformableEntity entity);


    public static Condition createCondition(String key, String value, TransformerContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> Condition.createCondition(key={}, value={})", key, value);
        }

        final Condition ret;

        int    idxConditionDelim = value == null ? -1 : value.indexOf(CONDITION_DELIMITER);
        String conditionName     = idxConditionDelim == -1 ? CONDITION_NAME_EQUALS : value.substring(0, idxConditionDelim);
        String conditionValue    = idxConditionDelim == -1 ? value : value.substring(idxConditionDelim + CONDITION_DELIMITER.length());

        conditionName  = StringUtils.trim(conditionName);
        conditionValue = StringUtils.trim(conditionValue);
        value          = StringUtils.trim(value);

        EntityAttribute attribute = new EntityAttribute(StringUtils.trim(key), context);

        switch (conditionName.toUpperCase()) {
            case CONDITION_ENTITY_ALL:
                ret = new ObjectIdEquals(attribute, CONDITION_ENTITY_ALL, context);
                break;

            case CONDITION_ENTITY_TOP_LEVEL:
                ret = new ObjectIdEquals(attribute, CONDITION_ENTITY_TOP_LEVEL, context);
                break;

            case CONDITION_ENTITY_OBJECT_ID:
                ret = new ObjectIdEquals(attribute, conditionValue, context);
                break;

            case CONDITION_NAME_EQUALS:
                ret = new EqualsCondition(attribute, conditionValue);
            break;

            case CONDITION_NAME_EQUALS_IGNORE_CASE:
                ret = new EqualsIgnoreCaseCondition(attribute, conditionValue);
            break;

            case CONDITION_NAME_STARTS_WITH:
                ret = new StartsWithCondition(attribute, conditionValue);
            break;

            case CONDITION_NAME_STARTS_WITH_IGNORE_CASE:
                ret = new StartsWithIgnoreCaseCondition(attribute, conditionValue);
            break;

            case CONDITION_NAME_HAS_VALUE:
                ret = new HasValueCondition(attribute);
                break;

            default:
                ret = new EqualsCondition(attribute, value); // treat unspecified/unknown condition as 'EQUALS'
            break;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== Condition.createCondition(key={}, value={}): actionName={}, actionValue={}, ret={}", key, value, conditionName, conditionValue, ret);
        }

        return ret;
    }


    public static class EqualsCondition extends Condition {
        protected final String attributeValue;

        public EqualsCondition(EntityAttribute attribute, String attributeValue) {
            super(attribute);

            this.attributeValue = attributeValue;
        }

        @Override
        public boolean matches(AtlasTransformableEntity entity) {
            Object attributeValue = entity != null ? entity.getAttribute(attribute) : null;

            return attributeValue != null && StringUtils.equals(attributeValue.toString(), this.attributeValue);
        }
    }


    public static class EqualsIgnoreCaseCondition extends Condition {
        protected final String attributeValue;

        public EqualsIgnoreCaseCondition(EntityAttribute attribute, String attributeValue) {
            super(attribute);

            this.attributeValue = attributeValue;
        }

        @Override
        public boolean matches(AtlasTransformableEntity entity) {
            Object attributeValue = entity != null ? entity.getAttribute(attribute) : null;

            return attributeValue != null && StringUtils.equalsIgnoreCase(attributeValue.toString(), this.attributeValue);
        }
    }


    public static class StartsWithCondition extends Condition {
        protected final String prefix;

        public StartsWithCondition(EntityAttribute attribute, String prefix) {
            super(attribute);

            this.prefix = prefix;
        }

        @Override
        public boolean matches(AtlasTransformableEntity entity) {
            Object attributeValue = entity != null ? entity.getAttribute(attribute) : null;

            return attributeValue != null && StringUtils.startsWith(attributeValue.toString(), this.prefix);
        }
    }


    public static class StartsWithIgnoreCaseCondition extends Condition {
        protected final String prefix;

        public StartsWithIgnoreCaseCondition(EntityAttribute attribute, String prefix) {
            super(attribute);

            this.prefix = prefix;
        }

        @Override
        public boolean matches(AtlasTransformableEntity entity) {
            Object attributeValue = entity != null ? entity.getAttribute(attribute) : null;

            return attributeValue != null && StringUtils.startsWithIgnoreCase(attributeValue.toString(), this.prefix);
        }
    }

    static class ObjectIdEquals extends Condition {
        private final boolean             isMatchAll;
        private final List<AtlasObjectId> objectIds;

        public ObjectIdEquals(EntityAttribute attribute, String scope, TransformerContext context) {
            super(attribute);

            this.isMatchAll = StringUtils.equals(scope, CONDITION_ENTITY_ALL);
            this.objectIds  = new ArrayList<>();

            if (!isMatchAll && context != null && context.getExportRequest() != null) {
                AtlasExportRequest request = context.getExportRequest();

                for(AtlasObjectId objectId : request.getItemsToExport()) {
                    addObjectId(objectId);
                }
            }
        }

        @Override
        public boolean matches(AtlasTransformableEntity entity) {
            if (isMatchAll) {
                return true;
            } else {
                for (AtlasObjectId objectId : objectIds) {
                    if (isMatch(objectId, entity.getEntity())) {
                        return true;
                    }
                }

                return false;
            }
        }

        void addObjectId(AtlasObjectId objId) {
            this.objectIds.add(objId);
        }

        private boolean isMatch(AtlasObjectId objectId, AtlasEntity entity) {
            if (!StringUtils.isEmpty(objectId.getGuid())) {
                return Objects.equals(objectId.getGuid(), entity.getGuid());
            }

            boolean ret = Objects.equals(objectId.getTypeName(), entity.getTypeName());

            if (ret) {
                for (Map.Entry<String, Object> entry : objectId.getUniqueAttributes().entrySet()) {
                    ret = ret && Objects.equals(entity.getAttribute(entry.getKey()), entry.getValue());

                    if (!ret) {
                        break;
                    }
                }
            }

            return ret;
        }
    }


    public static class HasValueCondition extends Condition {
        public HasValueCondition(EntityAttribute attribute) {
            super(attribute);
        }

        @Override
        public boolean matches(AtlasTransformableEntity entity) {
            Object attributeValue = entity != null ? entity.getAttribute(attribute) : null;

            return attributeValue != null ? StringUtils.isNotEmpty(attributeValue.toString()) : false;
        }
    }
}
