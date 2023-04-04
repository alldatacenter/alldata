/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.service.impl;

import com.alibaba.fastjson.JSON;
import datart.core.base.consts.VariableTypeEnum;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.ParamException;
import datart.core.common.UUIDGenerator;
import datart.core.entity.RelVariableSubject;
import datart.core.entity.Role;
import datart.core.entity.Variable;
import datart.core.mappers.ext.RelVariableSubjectMapperExt;
import datart.core.mappers.ext.RoleMapperExt;
import datart.core.mappers.ext.VariableMapperExt;
import datart.security.base.SubjectType;
import datart.server.base.dto.VariableValue;
import datart.server.base.params.*;
import datart.server.service.BaseService;
import datart.server.service.VariableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class VariableServiceImpl extends BaseService implements VariableService {

    private final VariableMapperExt variableMapper;

    private final RelVariableSubjectMapperExt rvsMapper;

    private final RoleMapperExt roleMapper;

    public VariableServiceImpl(VariableMapperExt variableMapper,
                               RelVariableSubjectMapperExt rvsMapper,
                               RoleMapperExt roleMapper) {
        this.variableMapper = variableMapper;
        this.rvsMapper = rvsMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public void requirePermission(Variable entity, int permission) {

    }

    @Override
    public boolean checkUnique(String orgId, String viewId, String name) {
        if (variableMapper.checkVariableName(orgId, viewId, name) != 0) {
            Exceptions.tr(ParamException.class, "error.param.exists.name");
        }
        return true;
    }

    @Override
    @Transactional
    public Variable create(BaseCreateParam createParam) {
        VariableCreateParam variableCreateParam = (VariableCreateParam) createParam;
        checkUnique(variableCreateParam.getOrgId(), null, variableCreateParam.getName());
        return VariableService.super.create(createParam);
    }

    @Override
    public List<Variable> listOrgVariables(String orgId) {
        return variableMapper.selectOrgVariables(orgId);
    }

    @Override
    public List<RelVariableSubject> listViewVariableRels(String viewId) {
        return rvsMapper.selectByView(viewId);
    }

    @Override
    public List<VariableValue> listViewVarValuesByUser(String userId, String viewId) {
        List<Variable> variables = variableMapper.selectViewVariables(viewId);
        return convertVariables(variables);
    }

    @Override
    public List<Variable> listByView(String viewId) {
        return variableMapper.selectViewVariables(viewId);
    }

    @Override
    public List<RelVariableSubject> listSubjectValues(SubjectType subjectType, String subjectId, String orgId) {
        return rvsMapper.selectBySubject(orgId, subjectType.name(), subjectId);
    }

    @Override
    public List<RelVariableSubject> listVariableValues(String variableId) {
        return rvsMapper.selectVariableRels(variableId);
    }

    @Override
    public List<VariableValue> listOrgValue(String orgId) {
        List<Variable> variables;
        if (securityManager.isOrgOwner(orgId)) {
            variables = variableMapper.selectOrgQueryVariables(orgId);
        } else {
            variables = variableMapper.selectOrgVariables(orgId);
        }
        return convertVariables(variables);
    }

    @Override
    public List<Variable> listOrgQueryVariables(String orgId) {
        return variableMapper.selectOrgQueryVariables(orgId);
    }

    @Override
    public List<Variable> listViewQueryVariables(String viewId) {
        return variableMapper.selectViewQueryVariables(viewId);
    }


    @Override
    @Transactional
    public boolean deleteByIds(Set<String> variableIds) {
        variableMapper.deleteByPrimaryKeys(Variable.class, variableIds);
        rvsMapper.deleteByVariables(variableIds);
        return true;
    }

    @Override
    @Transactional
    public boolean batchUpdate(List<VariableUpdateParam> updateParams) {
        if (CollectionUtils.isEmpty(updateParams)) {
            return true;
        }
        for (VariableUpdateParam updateParam : updateParams) {
            update(updateParam);
        }
        return true;
    }

    @Override
    @Transactional
    public List<Variable> batchInsert(List<VariableCreateParam> createParams) {
        if (CollectionUtils.isEmpty(createParams)) {
            return Collections.emptyList();
        }
        List<RelVariableSubject> rels = new LinkedList<>();
        List<Variable> variables = createParams.stream().map(p -> {

            // check name
            checkUnique(p.getOrgId(), p.getViewId(), p.getName());

            Variable variable = new Variable();
            BeanUtils.copyProperties(p, variable);
            variable.setCreateBy(getCurrentUser().getId());
            variable.setCreateTime(new Date());
            variable.setId(UUIDGenerator.generate());

            if (!CollectionUtils.isEmpty(p.getRelVariableSubjects())) {
                for (RelVariableSubject r : p.getRelVariableSubjects()) {
                    r.setCreateBy(getCurrentUser().getId());
                    r.setCreateTime(new Date());
                    r.setId(UUIDGenerator.generate());
                    r.setVariableId(variable.getId());
                    if (r.getUseDefaultValue() == null) {
                        r.setUseDefaultValue(false);
                    }
                    rels.add(r);
                }
            }
            return variable;
        }).collect(Collectors.toList());

        variableMapper.batchInsert(variables);
        if (!CollectionUtils.isEmpty(rels)) {
            rvsMapper.batchInsert(rels);
        }
        return variables;
    }

    @Override
    @Transactional
    public boolean updateRels(VariableRelUpdateParam updateParam) {
        if (!CollectionUtils.isEmpty(updateParam.getRelToUpdate())) {
            updateRels(updateParam.getRelToUpdate());
        }
        if (!CollectionUtils.isEmpty(updateParam.getRelToCreate())) {
            createRels(updateParam.getRelToCreate());
        }
        if (!CollectionUtils.isEmpty(updateParam.getRelToDelete())) {
            deleteRel(updateParam.getRelToDelete());
        }
        return true;
    }

    @Override
    public RelVariableSubject createRel(RelVariableSubject relVariableSubject) {
        relVariableSubject.setId(UUIDGenerator.generate());
        relVariableSubject.setCreateBy(getCurrentUser().getId());
        relVariableSubject.setCreateTime(new Date());
        rvsMapper.insert(relVariableSubject);
        return relVariableSubject;
    }

    @Override
    public List<RelVariableSubject> createRels(List<RelVariableSubject> relVariableSubjects) {
        if (CollectionUtils.isEmpty(relVariableSubjects)) {
            return null;
        }
        return relVariableSubjects
                .stream()
                .map(this::createRel)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateRels(List<RelVariableSubject> relVariableSubjects) {
        if (CollectionUtils.isEmpty(relVariableSubjects)) {
            return true;
        }
        for (RelVariableSubject relVariableSubject : relVariableSubjects) {
            updateRel(relVariableSubject);
        }
        return false;
    }

    private boolean updateRel(RelVariableSubject relVariableSubject) {
        RelVariableSubject update = new RelVariableSubject();
        BeanUtils.copyProperties(relVariableSubject, update);
        update.setUpdateBy(getCurrentUser().getId());
        update.setUpdateTime(new Date());
        update.setCreateTime(null);
        update.setCreateBy(null);
        return 1 == rvsMapper.updateByPrimaryKeySelective(update);
    }

    @Override
    public boolean deleteRel(Set<String> relIds) {
        return rvsMapper.deleteByPrimaryKeys(RelVariableSubject.class, relIds) > 0;
    }

    @Override
    public boolean delViewVariables(String viewId) {
        return variableMapper.deleteByView(viewId) >= 0;
    }

    @Override
    @Transactional
    public boolean update(BaseUpdateParam updateParam) {
        VariableUpdateParam param = (VariableUpdateParam) updateParam;
        Variable retrieve = retrieve(updateParam.getId());
        if (!param.getName().equalsIgnoreCase(retrieve.getName())) {
            checkUnique(retrieve.getOrgId(), retrieve.getViewId(), param.getName());
        }
        if (param.getRelVariableSubjects() != null) {
            rvsMapper.deleteByVariables(Collections.singleton(param.getId()));
            if (param.getRelVariableSubjects().size() > 0) {
                List<RelVariableSubject> rels = new LinkedList<>();
                for (RelVariableSubject r : param.getRelVariableSubjects()) {
                    r.setCreateBy(getCurrentUser().getId());
                    r.setCreateTime(new Date());
                    r.setId(UUIDGenerator.generate());
                    r.setVariableId(retrieve.getId());
                    rels.add(r);
                }
                rvsMapper.batchInsert(rels);
            }
        }

        return VariableService.super.update(updateParam);
    }

    private List<VariableValue> convertVariables(List<Variable> variables) {
        if (CollectionUtils.isEmpty(variables)) {
            return Collections.emptyList();
        }
        // find permission variable default value
        List<String> varIds = variables.stream()
                .filter(variable -> VariableTypeEnum.PERMISSION.name().equals(variable.getType()))
                .map(Variable::getId).collect(Collectors.toList());

        Set<String> subjectIds = getCurrentSubjectIds(variables.get(0).getOrgId());
        List<RelVariableSubject> relVariableSubjects;
        if (!CollectionUtils.isEmpty(varIds) && !CollectionUtils.isEmpty(subjectIds)) {
            relVariableSubjects = rvsMapper.selectByVarAndSubject(varIds, subjectIds);
        } else {
            relVariableSubjects = Collections.emptyList();
        }

        Map<String, List<RelVariableSubject>> varValues = relVariableSubjects
                .stream()
                .collect(Collectors.groupingBy(RelVariableSubject::getVariableId));

        return variables.stream()
                .filter(var -> varValues.containsKey(var.getId()) || VariableTypeEnum.QUERY.name().equals(var.getType()))
                .map(var -> {
                    VariableValue variableValue = new VariableValue();
                    BeanUtils.copyProperties(var, variableValue);
                    List<RelVariableSubject> rels = varValues.get(var.getId());
                    Set<String> values = Collections.emptySet();
                    if (rels != null) {
                        values = rels.stream().flatMap(rel -> {
                            if (rel.getUseDefaultValue()) {
                                try {
                                    return new HashSet<>(JSON.parseArray(var.getDefaultValue(), String.class)).stream();
                                } catch (Exception e) {
                                    return Stream.empty();
                                }
                            } else {
                                try {
                                    return new HashSet<>(JSON.parseArray(rel.getValue(), String.class)).stream();
                                } catch (Exception e) {
                                    return Stream.empty();
                                }
                            }
                        }).collect(Collectors.toSet());
                    } else {
                        if (var.getDefaultValue() != null) {
                            values = JSON.parseArray(var.getDefaultValue())
                                    .stream()
                                    .map(Object::toString)
                                    .collect(Collectors.toSet());
                        }
                    }
                    variableValue.setValues(values);
                    return variableValue;
                }).collect(Collectors.toList());
    }

    private Set<String> getCurrentSubjectIds(String orgId) {
        List<Role> roles = roleMapper.selectUserRoles(orgId, getCurrentUser().getId());
        Set<String> subjectIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
        subjectIds.add(getCurrentUser().getId());
        return subjectIds;
    }

}
