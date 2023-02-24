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

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.lang.StringUtils;
import org.apache.atlas.entitytransform.BaseEntityHandler.AtlasTransformableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;



public abstract class Action {
    private static final Logger LOG = LoggerFactory.getLogger(Action.class);

    private static final String ACTION_DELIMITER           = ":";
    private static final String ACTION_ADD_CLASSIFICATION  = "ADD_CLASSIFICATION";
    private static final String ACTION_NAME_SET            = "SET";
    private static final String ACTION_NAME_REPLACE_PREFIX = "REPLACE_PREFIX";
    private static final String ACTION_NAME_TO_LOWER       = "TO_LOWER";
    private static final String ACTION_NAME_TO_UPPER       = "TO_UPPER";
    private static final String ACTION_NAME_CLEAR          = "CLEAR";

    protected final EntityAttribute attribute;


    protected Action(EntityAttribute attribute) {
        this.attribute = attribute;
    }

    public EntityAttribute getAttribute() { return attribute; }

    public boolean isValid() {
        return true;
    }

    public abstract void apply(AtlasTransformableEntity entity);


    public static Action createAction(String key, String value, TransformerContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> Action.createAction(key={}, value={})", key, value);
        }

        final Action ret;

        int    idxActionDelim = value == null ? -1 : value.indexOf(ACTION_DELIMITER);
        String actionName     = idxActionDelim == -1 ? ACTION_NAME_SET : value.substring(0, idxActionDelim);
        String actionValue    = idxActionDelim == -1 ? value : value.substring(idxActionDelim + ACTION_DELIMITER.length());

        actionName  = StringUtils.trim(actionName);
        actionValue = StringUtils.trim(actionValue);
        value       = StringUtils.trim(value);

        EntityAttribute attribute = new EntityAttribute(StringUtils.trim(key), context);

        switch (actionName.toUpperCase()) {
            case ACTION_ADD_CLASSIFICATION:
                ret = new AddClassificationAction(attribute, actionValue, context);
            break;

            case ACTION_NAME_REPLACE_PREFIX:
                ret = new PrefixReplaceAction(attribute, actionValue);
            break;

            case ACTION_NAME_TO_LOWER:
                ret = new ToLowerCaseAction(attribute);
            break;

            case ACTION_NAME_TO_UPPER:
                ret = new ToUpperCaseAction(attribute);
            break;

            case ACTION_NAME_SET:
                ret = new SetAction(attribute, actionValue);
            break;

            case ACTION_NAME_CLEAR:
                ret = new ClearAction(attribute);
            break;

            default:
                ret = new SetAction(attribute, value); // treat unspecified/unknown action as 'SET'
            break;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== Action.createAction(key={}, value={}): actionName={}, actionValue={}, ret={}", key, value, actionName, actionValue, ret);
        }

        return ret;
    }


    public static class SetAction extends Action {
        private final String attributeValue;

        public SetAction(EntityAttribute attribute, String attributeValue) {
            super(attribute);

            this.attributeValue = attributeValue;
        }

        @Override
        public void apply(AtlasTransformableEntity entity) {
            entity.setAttribute(attribute, attributeValue);
        }
    }

    public static class AddClassificationAction extends Action {
        private final String classificationName;

        public AddClassificationAction(EntityAttribute attribute, String classificationName, TransformerContext context) {
            super(attribute);

            this.classificationName = classificationName;

            createClassificationDefIfNotExists(classificationName, context);
        }

        @Override
        public void apply(AtlasTransformableEntity transformableEntity) {
            AtlasEntity entity = transformableEntity.getEntity();

            if (entity.getClassifications() == null) {
                entity.setClassifications(new ArrayList<>());
            }

            boolean hasClassification = false;

            for (AtlasClassification c : entity.getClassifications()) {
                hasClassification = c.getTypeName().equals(classificationName);

                if (hasClassification) {
                    break;
                }
            }

            if (!hasClassification) {
                entity.getClassifications().add(new AtlasClassification(classificationName));
            }
        }

        private void createClassificationDefIfNotExists(String classificationName, TransformerContext context) {
            AtlasTypeRegistry typeRegistry = context != null ? context.getTypeRegistry() : null;

            if (typeRegistry != null) {
                try {
                    AtlasClassificationDef classificationDef = typeRegistry.getClassificationDefByName(classificationName);

                    if (classificationDef == null) {
                        AtlasTypeDefStore typeDefStore = context.getTypeDefStore();

                        if (typeDefStore != null) {
                            classificationDef = new AtlasClassificationDef(classificationName);

                            AtlasTypesDef typesDef = new AtlasTypesDef();

                            typesDef.setClassificationDefs(Collections.singletonList(classificationDef));

                            typeDefStore.createTypesDef(typesDef);

                            LOG.info("created classification: {}", classificationName);
                        } else {
                            LOG.warn("skipped creation of classification {}. typeDefStore is null", classificationName);
                        }
                    }
                } catch (AtlasBaseException e) {
                    LOG.error("Error creating classification: {}", classificationName, e);
                }
            }
        }
    }

    public static class PrefixReplaceAction extends Action {
        private final String fromPrefix;
        private final String toPrefix;

        public PrefixReplaceAction(EntityAttribute attribute, String actionValue) {
            super(attribute);

            // actionValue => =:prefixToReplace=replacedValue
            if (actionValue != null) {
                int idxSepDelimiter = actionValue.indexOf(ACTION_DELIMITER);

                if (idxSepDelimiter == -1) { // no separator specified i.e. no value specified to replace; remove the prefix
                    fromPrefix = actionValue;
                    toPrefix   = "";
                } else {
                    String prefixSep    = StringUtils.trim(actionValue.substring(0, idxSepDelimiter));
                    int    idxPrefixSep = actionValue.indexOf(prefixSep, idxSepDelimiter + ACTION_DELIMITER.length());

                    if (idxPrefixSep == -1) { // separator not found i.e. no value specified to replace; remove the prefix
                        fromPrefix = actionValue.substring(idxSepDelimiter + ACTION_DELIMITER.length());
                        toPrefix   = "";
                    } else {
                        fromPrefix = actionValue.substring(idxSepDelimiter + ACTION_DELIMITER.length(), idxPrefixSep);
                        toPrefix   = actionValue.substring(idxPrefixSep + prefixSep.length());
                    }
                }
            } else {
                fromPrefix = null;
                toPrefix   = "";
            }
        }

        @Override
        public boolean isValid() {
            return super.isValid() && StringUtils.isNotEmpty(fromPrefix);
        }

        @Override
        public void apply(AtlasTransformableEntity entity) {
            if (isValid()) {
                Object currValue = entity.getAttribute(attribute);
                String strValue  = currValue != null ? currValue.toString() : null;

                if (strValue != null && strValue.startsWith(fromPrefix)) {
                    entity.setAttribute(attribute, StringUtils.replace(strValue, fromPrefix, toPrefix, 1));
                }
            }
        }
    }

    public static class ToLowerCaseAction extends Action {
        public ToLowerCaseAction(EntityAttribute attribute) {
            super(attribute);
        }

        @Override
        public void apply(AtlasTransformableEntity entity) {
            if (isValid()) {
                Object currValue = entity.getAttribute(attribute);
                String strValue  = currValue instanceof String ? (String) currValue : null;

                if (strValue != null) {
                    entity.setAttribute(attribute, strValue.toLowerCase());
                }
            }
        }
    }

    public static class ToUpperCaseAction extends Action {
        public ToUpperCaseAction(EntityAttribute attribute) {
            super(attribute);
        }

        @Override
        public void apply(AtlasTransformableEntity entity) {
            if (isValid()) {
                Object currValue = entity.getAttribute(attribute);
                String strValue  = currValue instanceof String ? (String) currValue : null;

                if (strValue != null) {
                    entity.setAttribute(attribute, strValue.toUpperCase());
                }
            }
        }
    }

    public static class ClearAction extends Action {
        public ClearAction(EntityAttribute attribute) {
            super(attribute);
        }

        @Override
        public void apply(AtlasTransformableEntity entity) {
            if (isValid() && entity.hasAttribute(attribute)) {
                entity.setAttribute(attribute, null);
            }
        }
    }
}
