package datart.server.service;

import datart.core.entity.RelVariableSubject;
import datart.core.entity.Variable;
import datart.core.mappers.ext.VariableMapperExt;
import datart.security.base.SubjectType;
import datart.server.base.dto.VariableValue;
import datart.server.base.params.VariableCreateParam;
import datart.server.base.params.VariableRelUpdateParam;
import datart.server.base.params.VariableUpdateParam;

import java.util.List;
import java.util.Set;

public interface VariableService extends BaseCRUDService<Variable, VariableMapperExt> {

    boolean checkUnique(String orgId, String viewId, String name);

    List<Variable> listOrgVariables(String orgId);

    List<RelVariableSubject> listViewVariableRels(String viewId);

    List<VariableValue> listViewVarValuesByUser(String userId, String viewId);

    List<Variable> listByView(String viewId);

    List<RelVariableSubject> listSubjectValues(SubjectType subjectType, String subjectId, String orgId);

    List<RelVariableSubject> listVariableValues(String variableId);

    List<VariableValue> listOrgValue(String orgId);

    List<Variable> listOrgQueryVariables(String orgId);

    List<Variable> listViewQueryVariables(String viewId);

    boolean deleteByIds(Set<String> variableIds);

    boolean batchUpdate(List<VariableUpdateParam> updateParams);

    List<Variable> batchInsert(List<VariableCreateParam> createParams);

    boolean updateRels(VariableRelUpdateParam updateParam);

    RelVariableSubject createRel(RelVariableSubject relVariableSubject);

    List<RelVariableSubject> createRels(List<RelVariableSubject> relVariableSubjects);

    boolean updateRels(List<RelVariableSubject> relVariableSubjects);

    boolean deleteRel(Set<String> relIds);

    boolean delViewVariables(String viewId);


}

