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
import org.apache.atlas.model.impexp.AttributeTransform;
import org.apache.commons.collections.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AtlasEntityTransformer {
    private final List<Condition> conditions;
    private final List<Action>    actions;


    public AtlasEntityTransformer(AttributeTransform attributeTransform, TransformerContext context) {
        this(attributeTransform.getConditions(), attributeTransform.getAction(), context);
    }

    public AtlasEntityTransformer(Map<String, String> conditions, Map<String, String> actions, TransformerContext context) {
        this.conditions = createConditions(conditions, context);
        this.actions    = createActions(actions, context);
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void transform(AtlasTransformableEntity entity) {
        if (entity != null) {
            boolean matches = true;

            for (Condition condition : conditions) {
                matches = matches && condition.matches(entity);
            }

            if (matches) {
                for (Action action : actions) {
                    action.apply(entity);
                }
            }
        }
    }

    private List<Condition> createConditions(Map<String, String> conditions, TransformerContext context) {
        List<Condition> ret = new ArrayList<>();

        if (MapUtils.isNotEmpty(conditions)) {
            for (Map.Entry<String, String> entry : conditions.entrySet()) {
                Condition condition = Condition.createCondition(entry.getKey(), entry.getValue(), context);

                ret.add(condition);
            }
        }

        return ret;
    }

    private List<Action> createActions(Map<String, String> actions, TransformerContext context) {
        List<Action> ret = new ArrayList<>();

        if (MapUtils.isNotEmpty(actions)) {
            for (Map.Entry<String, String> entry : actions.entrySet()) {
                Action action = Action.createAction(entry.getKey(), entry.getValue(), context);

                ret.add(action);
            }
        }

        return ret;
    }
}
