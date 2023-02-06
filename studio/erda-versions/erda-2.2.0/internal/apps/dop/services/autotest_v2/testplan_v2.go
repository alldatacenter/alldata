// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package autotestv2

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateTestPlanV2 create test plan
func (svc *Service) CreateTestPlanV2(req apistructs.TestPlanV2CreateRequest) (uint64, error) {
	// checkName Exist
	// if err := svc.db.CheckTestPlanV2NameExist(req.Name); err != nil {
	// 	return 0, err
	// }

	// create test plan
	testPlanV2 := &dao.TestPlanV2{
		Name:          req.Name,
		Desc:          req.Desc,
		CreatorID:     req.UserID,
		UpdaterID:     req.UserID,
		SpaceID:       req.SpaceID,
		ProjectID:     req.ProjectID,
		IterationID:   req.IterationID,
		ExecuteTime:   nil,
		PassRate:      float64(0),
		ExecuteApiNum: 0,
	}

	if err := svc.db.CreateTestPlanV2(testPlanV2); err != nil {
		return 0, err
	}

	// create owner
	var members []dao.AutoTestPlanMember
	for _, ownerID := range req.Owners {
		members = append(members, dao.AutoTestPlanMember{
			TestPlanID: testPlanV2.ID,
			Role:       apistructs.TestPlanMemberRoleOwner,
			UserID:     ownerID,
		})
	}
	if err := svc.db.BatchCreateAutoTestPlanMembers(members); err != nil {
		return 0, apierrors.ErrCreateTestPlanMember.InternalError(err)
	}

	return testPlanV2.ID, nil
}

// DeleteTestPlanV2 delete test plan
func (svc *Service) DeleteTestPlanV2(testPlanID uint64, identityInfo apistructs.IdentityInfo) error {
	testPlan, err := svc.db.GetTestPlanV2ByID(testPlanID)
	if err != nil {
		return err
	}

	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  testPlan.ProjectID,
			Resource: apistructs.TestPlanV2Resource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrDeleteTestPlan.AccessDenied()
		}
	}

	// Delete the test plan
	if err := svc.db.DeleteTestPlanV2ByID(testPlanID); err != nil {
		return err
	}

	// Delete test plan member
	return svc.db.DeleteAutoTestPlanMemberByPlanID(testPlanID)
}

// UpdateTestPlanV2 update testplan
func (svc *Service) UpdateTestPlanV2(req *apistructs.TestPlanV2UpdateRequest) error {
	testPlan, err := svc.db.GetTestPlanV2ByID(req.TestPlanID)
	if err != nil {
		return err
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  testPlan.ProjectID,
			Resource: apistructs.TestPlanV2Resource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrUpdateTestPlan.AccessDenied()
		}
	}

	// update members of test plan
	var members []dao.AutoTestPlanMember
	for _, v := range req.Owners {
		members = append(members, dao.AutoTestPlanMember{
			TestPlanID: req.TestPlanID,
			Role:       apistructs.TestPlanMemberRoleOwner,
			UserID:     v,
		})
	}
	if len(members) > 0 {
		if err := svc.db.OverwriteAutoTestPlanMembers(req.TestPlanID, members); err != nil {
			return err
		}
	}

	fields, err := svc.getChangedFields(req, testPlan)
	if err != nil {
		return err
	}

	return svc.db.UpdateTestPlanV2(req.TestPlanID, fields)
}

// PagingTestPlansV2 paging query testplan
func (svc *Service) PagingTestPlansV2(req *apistructs.TestPlanV2PagingRequest) (*apistructs.TestPlanV2PagingResponseData, error) {
	// 参数校验
	if req.ProjectID == 0 {
		return nil, apierrors.ErrPagingTestPlans.MissingParameter("projectID")
	}
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 || req.PageSize > 1000 {
		req.PageSize = 20
	}

	if len(req.Owners) != 0 {
		members, err := svc.db.ListAutoTestPlanOwnersByOwners(req.Owners)
		if err != nil {
			return nil, err
		}
		for _, m := range members {
			req.IDs = append(req.IDs, m.TestPlanID)
		}
	}

	// paging testplan
	total, list, userIDs, err := svc.db.PagingTestPlanV2(req)
	if err != nil {
		return nil, apierrors.ErrPagingTestPlans.InternalError(err)
	}

	return &apistructs.TestPlanV2PagingResponseData{
		Total:   total,
		List:    list,
		UserIDs: userIDs,
	}, nil
}

// GetTestPlanV2 get testplan detail
func (svc *Service) GetTestPlanV2(testPlanID uint64, identityInfo apistructs.IdentityInfo) (*apistructs.TestPlanV2, error) {
	testPlan, err := svc.db.GetTestPlanV2ByID(testPlanID)
	if err != nil {
		return nil, err
	}

	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  testPlan.ProjectID,
			Resource: apistructs.TestPlanV2Resource,
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrUpdateTestPlan.AccessDenied()
		}
	}

	result := testPlan.Convert2DTO()
	// get owners
	members, err := svc.db.ListAutoTestPlanMembersByPlanID(testPlanID, apistructs.TestPlanMemberRoleOwner)
	if err != nil {
		return nil, err
	}
	for _, v := range members {
		result.Owners = append(result.Owners, v.UserID)
	}
	result.Owners = strutil.DedupSlice(result.Owners)
	// get steps in the test plan and sort they
	tmpSteps, total, err := svc.db.GetStepByTestPlanID(testPlanID, true)
	if err != nil {
		return nil, err
	}

	steps := make([]*apistructs.TestPlanV2Step, 0, total)
	idx := make(map[uint64]dao.TestPlanV2StepJoin, 0)
	for _, v := range tmpSteps {
		idx[v.PreID] = v
	}

	var p uint64 = 0
	for {
		if _, ok := idx[p]; !ok {
			break
		}
		steps = append(steps, idx[p].Convert2DTO())
		p = idx[p].ID
	}

	result.Steps = steps

	return &result, nil
}

// AddTestPlanV2Step Add a step in the test plan
func (svc *Service) AddTestPlanV2Step(req *apistructs.TestPlanV2StepAddRequest) (uint64, error) {
	testPlan, err := svc.db.GetTestPlanV2ByID(req.TestPlanID)
	if err != nil {
		return 0, err
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  testPlan.ProjectID,
			Resource: apistructs.TestPlanV2Resource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return 0, err
		}
		if !access.Access {
			return 0, apierrors.ErrUpdateTestPlan.AccessDenied()
		}
	}

	// Check the sceneSet is exists
	if err := svc.db.CheckSceneSetIsExists(req.SceneSetID); err != nil {
		return 0, err
	}

	return svc.db.AddTestPlanV2Step(req)
}

// DeleteTestPlanV2Step Delete a step in the test plan
func (svc *Service) DeleteTestPlanV2Step(req *apistructs.TestPlanV2StepDeleteRequest) error {
	testPlan, err := svc.db.GetTestPlanV2ByID(req.TestPlanID)
	if err != nil {
		return err
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  testPlan.ProjectID,
			Resource: apistructs.TestPlanV2Resource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrDeleteTestPlan.AccessDenied()
		}
	}

	return svc.db.DeleteTestPlanV2Step(req)
}

// MoveTestPlanV2Step move a step in the test plan
func (svc *Service) MoveTestPlanV2Step(req *apistructs.TestPlanV2StepMoveRequest) error {
	testPlan, err := svc.db.GetTestPlanV2ByID(req.TestPlanID)
	if err != nil {
		return err
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  testPlan.ProjectID,
			Resource: apistructs.TestPlanV2Resource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrUpdateTestPlan.AccessDenied()
		}
	}

	return svc.db.MoveTestPlanV2Step(req)
}

// UpdateTestPlanV2Step Update a step in the test plan
func (svc *Service) UpdateTestPlanV2Step(req *apistructs.TestPlanV2StepUpdateRequest) error {
	var step dao.TestPlanV2Step
	err := svc.db.First(&step, "id = ?", req.StepID).Error
	if err != nil {
		return err
	}
	step.SceneSetID = req.ScenesSetId

	plan, err := svc.db.GetTestPlanV2ByID(step.PlanID)
	if err != nil {
		return err
	}
	if !req.IsInternalClient() {
		// Authorize
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  plan.ProjectID,
			Resource: apistructs.TestPlanV2Resource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrUpdateTestPlan.AccessDenied()
		}
	}
	return svc.db.UpdateTestPlanV2Step(step)
}

// UpdateTestPlanV2Step Update a step in the test plan
func (svc *Service) GetTestPlanV2Step(ID uint64) (*apistructs.TestPlanV2Step, error) {
	step, err := svc.db.GetTestPlanV2Step(ID)
	if err != nil {
		return nil, err
	}
	if step.SceneSetID > 0 {
		set, err := svc.db.GetSceneSet(step.SceneSetID)
		if err != nil {
			return nil, err
		}
		step.SceneSetName = set.Name
	}
	return step.Convert2DTO(), nil
}

// ListTestPlanV2Step list testPlan step
func (svc *Service) ListTestPlanV2Step(testPlanID, groupID uint64) ([]*apistructs.TestPlanV2Step, error) {
	steps, err := svc.db.ListTestPlanV2Step(testPlanID, groupID)
	if err != nil {
		return nil, err
	}

	var stepDtos []*apistructs.TestPlanV2Step
	for _, v := range steps {
		stepDtos = append(stepDtos, v.Convert2DTO())
	}

	return stepDtos, nil
}

// getChangedFields get changed fields
func (svc *Service) getChangedFields(req *apistructs.TestPlanV2UpdateRequest, model *dao.TestPlanV2) (map[string]interface{}, error) {
	fields := make(map[string]interface{}, 0)
	if req.Name != model.Name {
		fields["name"] = req.Name
	}

	if req.SpaceID != model.SpaceID {
		// todo 检查测试计划下是否还有场景集
		fields["space_id"] = req.SpaceID
	}

	if req.IterationID != model.IterationID {
		fields["iteration_id"] = req.IterationID
	}

	if req.Desc != model.Desc {
		fields["desc"] = req.Desc
	}

	if req.IsArchived != nil {
		fields["is_archived"] = *req.IsArchived
	}

	if len(fields) != 0 {
		fields["updater_id"] = req.UserID
	}

	return fields, nil
}

func (svc *Service) ExecuteDiceAutotestTestPlan(req apistructs.AutotestExecuteTestPlansRequest) (*apistructs.PipelineDTO, error) {
	testPlan, err := svc.GetTestPlanV2(req.TestPlan.ID, req.IdentityInfo)
	if err != nil {
		return nil, err
	}

	var spec pipelineyml.Spec
	spec.Version = "1.1"
	var stagesValue []*pipelineyml.Stage

	// get steps group by groupID
	stepGroupMap, groupIDs := getStepMapByGroupID(testPlan.Steps)

	for _, groupID := range groupIDs {
		var specStage pipelineyml.Stage
		for _, v := range stepGroupMap[groupID] {
			if v.SceneSetID <= 0 {
				continue
			}
			sceneSetJson, err := json.Marshal(v)
			if err != nil {
				return nil, err
			}
			action := map[pipelineyml.ActionType]*pipelineyml.Action{
				pipelineyml.Snippet: {
					Alias: pipelineyml.ActionAlias(strconv.Itoa(int(v.ID))),
					Type:  pipelineyml.Snippet,
					Labels: map[string]string{
						apistructs.AutotestSceneSet: base64.StdEncoding.EncodeToString(sceneSetJson),
						apistructs.AutotestType:     apistructs.AutotestSceneSet,
					},
					If: expression.LeftPlaceholder + " 1 == 1 " + expression.RightPlaceholder,
					SnippetConfig: &pipelineyml.SnippetConfig{
						Name:   apistructs.PipelineSourceAutoTestSceneSet.String() + "-" + strconv.Itoa(int(v.SceneSetID)),
						Source: apistructs.PipelineSourceAutoTest.String(),
						Labels: map[string]string{
							apistructs.LabelAutotestExecType: apistructs.SceneSetsAutotestExecType,
							apistructs.LabelSceneSetID:       strconv.Itoa(int(v.SceneSetID)),
							apistructs.LabelSpaceID:          strconv.Itoa(int(testPlan.SpaceID)),
							apistructs.LabelTestPlanID:       strconv.FormatUint(testPlan.ID, 10),
							apistructs.LabelIterationID:      req.Labels[apistructs.LabelIterationID],
						},
					},
				},
			}
			specStage.Actions = append(specStage.Actions, action)
		}
		stagesValue = append(stagesValue, &specStage)
	}
	spec.Stages = stagesValue
	yml, err := pipelineyml.GenerateYml(&spec)
	if err != nil {
		return nil, err
	}

	project, err := svc.bdl.GetProject(testPlan.ProjectID)
	if err != nil {
		return nil, err
	}
	org, err := svc.bdl.GetOrg(project.OrgID)
	if err != nil {
		return nil, err
	}

	var reqPipeline = apistructs.PipelineCreateRequestV2{
		PipelineYmlName: apistructs.PipelineSourceAutoTestPlan.String() + "-" + strconv.Itoa(int(req.TestPlan.ID)),
		PipelineSource:  apistructs.PipelineSourceAutoTest,
		AutoRun:         true,
		ForceRun:        true,
		ClusterName:     req.ClusterName,
		PipelineYml:     string(yml),
		Labels:          req.Labels,
		IdentityInfo:    req.IdentityInfo,
		NormalLabels:    map[string]string{apistructs.LabelOrgName: org.Name, apistructs.LabelOrgID: strconv.FormatUint(org.ID, 10)},
	}
	if req.ConfigManageNamespaces != "" {
		reqPipeline.ConfigManageNamespaces = append(reqPipeline.ConfigManageNamespaces, req.ConfigManageNamespaces)
	}

	if reqPipeline.ClusterName == "" {
		testClusterName, err := svc.GetTestClusterNameBySpaceID(testPlan.SpaceID)
		if err != nil {
			return nil, err
		}
		reqPipeline.ClusterName = testClusterName
	}

	pipelineDTO, err := svc.bdl.CreatePipeline(&reqPipeline)
	if err != nil {
		return nil, err
	}

	return pipelineDTO, nil
}

// getStepMapByGroupID get step group by groupID
func getStepMapByGroupID(steps []*apistructs.TestPlanV2Step) (map[uint64][]*apistructs.TestPlanV2Step, []uint64) {
	// stepGroupMap key: groupID, if groupID is 0, set id as groupID
	stepGroupMap := make(map[uint64][]*apistructs.TestPlanV2Step, 0)
	groupIDs := make([]uint64, 0) // make sure the order remains the same
	for _, v := range steps {
		if v.SceneSetID <= 0 {
			continue
		}
		if v.GroupID == 0 {
			v.GroupID = v.ID
		}
		if _, ok := stepGroupMap[v.GroupID]; ok {
			stepGroupMap[v.GroupID] = append(stepGroupMap[v.GroupID], v)
		} else {
			stepGroupMap[v.GroupID] = []*apistructs.TestPlanV2Step{v}
			groupIDs = append(groupIDs, v.GroupID)
		}
	}
	return stepGroupMap, groupIDs
}

func (svc *Service) GetTestClusterNameBySpaceID(spaceID uint64) (string, error) {
	space, err := svc.db.GetAutoTestSpace(spaceID)
	if err != nil {
		return "", err
	}
	project, err := svc.bdl.GetProject(uint64(space.ProjectID))
	if err != nil {
		return "", err
	}
	testClusterName, ok := project.ClusterConfig[string(apistructs.TestWorkspace)]
	if !ok {
		return "", fmt.Errorf("not found cluster")
	}
	return testClusterName, nil
}

func (svc *Service) CancelDiceAutotestTestPlan(req apistructs.AutotestCancelTestPlansRequest) error {
	var pipelinePageListRequest = apistructs.PipelinePageListRequest{
		PageNum:  1,
		PageSize: 1,
		Sources: []apistructs.PipelineSource{
			apistructs.PipelineSourceAPITest,
		},
		YmlNames: []string{
			strconv.Itoa(int(req.TestPlan.ID)),
		},
	}
	pages, err := svc.bdl.PageListPipeline(pipelinePageListRequest)
	if err != nil {
		return err
	}

	for _, v := range pages.Pipelines {
		if v.Status.IsReconcilerRunningStatus() {
			var pipelineCancelRequest apistructs.PipelineCancelRequest
			pipelineCancelRequest.PipelineID = v.ID
			return svc.bdl.CancelPipeline(pipelineCancelRequest)
		}
		break
	}
	return nil
}

func (svc *Service) BatchQuerySceneSetPipelineSnippetYaml(configs []apistructs.SnippetConfig) ([]apistructs.BatchSnippetConfigYml, error) {

	var setIds []uint64
	isRefSetMap := make(map[uint64]string)
	for _, conf := range configs {
		sceneSetIDStr := conf.Labels[apistructs.LabelSceneSetID]
		if sceneSetIDStr == "" {
			return nil, fmt.Errorf("not find labels sceneSetID")
		}
		sceneSetIDInt, err := strconv.Atoi(sceneSetIDStr)
		if err != nil {
			return nil, err
		}
		setIds = append(setIds, uint64(sceneSetIDInt))
		isRefSetMap[uint64(sceneSetIDInt)] = conf.Labels[apistructs.LabelIsRefSet]
	}
	results, err := svc.ListAutotestScenes(setIds)
	if err != nil {
		return nil, err
	}
	for _, v := range setIds {
		_, ok := results[v]
		if !ok {
			return nil, fmt.Errorf("not find SceneSet snippet: %v", v)
		}
	}

	var resultConfigs []apistructs.BatchSnippetConfigYml
	for index, key := range setIds {
		resultsScenes := results[key]
		var spec pipelineyml.Spec
		spec.Version = "1.1"

		scenes := svc.sortAutoTestSceneList(resultsScenes, 1, 10000)
		sceneGroupMap, groupIDs := getSceneMapByGroupID(scenes)
		var stagesValue []*pipelineyml.Stage
		for _, groupID := range groupIDs {
			var specStage pipelineyml.Stage
			for _, v := range sceneGroupMap[groupID] {
				inputs := v.Inputs

				var params = make(map[string]interface{})
				for _, input := range inputs {
					// replace mock random param before return to pipeline
					// and so steps can use the same random value
					replacedValue := expression.ReplaceRandomParams(input.Value)
					params[input.Name] = replacedValue
				}

				sceneJson, err := json.Marshal(v)
				if err != nil {
					return nil, err
				}

				if v.RefSetID > 0 {
					// scene reference scene set
					specStage.Actions = append(specStage.Actions, map[pipelineyml.ActionType]*pipelineyml.Action{
						pipelineyml.Snippet: {
							Alias: pipelineyml.ActionAlias(strconv.Itoa(int(v.ID))),
							Type:  pipelineyml.Snippet,
							Labels: map[string]string{
								apistructs.AutotestScene: base64.StdEncoding.EncodeToString(sceneJson),
								apistructs.AutotestType:  apistructs.AutotestScene,
							},
							If: expression.LeftPlaceholder + " 1 == 1 " + expression.RightPlaceholder,
							SnippetConfig: &pipelineyml.SnippetConfig{
								Name:   apistructs.PipelineSourceAutoTestSceneSet.String() + "-" + strconv.Itoa(int(v.RefSetID)),
								Source: apistructs.PipelineSourceAutoTest.String(),
								Labels: map[string]string{
									apistructs.LabelAutotestExecType: apistructs.SceneSetsAutotestExecType,
									apistructs.LabelSceneSetID:       strconv.Itoa(int(v.RefSetID)),
									apistructs.LabelSpaceID:          strconv.Itoa(int(v.SpaceID)),
									apistructs.LabelSceneID:          strconv.Itoa(int(v.ID)),
									apistructs.LabelIsRefSet:         "true",
								},
							},
							Policy: &pipelineyml.Policy{Type: v.Policy},
						},
					})
				} else {
					specStage.Actions = append(specStage.Actions, map[pipelineyml.ActionType]*pipelineyml.Action{
						pipelineyml.Snippet: {
							Alias:  pipelineyml.ActionAlias(strconv.Itoa(int(v.ID))),
							Type:   pipelineyml.Snippet,
							Params: params,
							Labels: map[string]string{
								apistructs.AutotestType:  apistructs.AutotestScene,
								apistructs.AutotestScene: base64.StdEncoding.EncodeToString(sceneJson),
							},
							If: expression.LeftPlaceholder + " 1 == 1 " + expression.RightPlaceholder,
							SnippetConfig: &pipelineyml.SnippetConfig{
								Name:   strconv.Itoa(int(v.ID)),
								Source: apistructs.PipelineSourceAutoTest.String(),
								Labels: map[string]string{
									apistructs.LabelAutotestExecType: apistructs.SceneAutotestExecType,
									apistructs.LabelSceneID:          strconv.Itoa(int(v.ID)),
									apistructs.LabelSpaceID:          strconv.Itoa(int(v.SpaceID)),
									apistructs.LabelIsRefSet:         isRefSetMap[key],
								},
							},
						},
					})
				}
			}
			stagesValue = append(stagesValue, &specStage)
			for _, v := range sceneGroupMap[groupID] {
				for _, output := range v.Output {
					spec.Outputs = append(spec.Outputs, &pipelineyml.PipelineOutput{
						Name: fmt.Sprintf("%v_%v", v.ID, output.Name),
						Ref:  fmt.Sprintf("%s %s.%d.%s %s", expression.LeftPlaceholder, expression.Outputs, v.ID, output.Name, expression.RightPlaceholder),
					})
				}
			}
		}
		spec.Stages = stagesValue
		yml, err := pipelineyml.GenerateYml(&spec)
		if err != nil {
			return nil, err
		}
		resultConfigs = append(resultConfigs, apistructs.BatchSnippetConfigYml{
			Yml:    string(yml),
			Config: configs[index],
		})
	}

	return resultConfigs, nil
}

func (svc *Service) QuerySceneSetPipelineSnippetYaml(req apistructs.SnippetConfig) (string, error) {
	sceneSetIDStr := req.Labels[apistructs.LabelSceneSetID]
	if sceneSetIDStr == "" {
		return "", fmt.Errorf("not find labels sceneSetID")
	}
	sceneSetIDInt, err := strconv.Atoi(sceneSetIDStr)
	if err != nil {
		return "", err
	}

	var sceneListReq apistructs.AutotestSceneRequest
	sceneListReq.SetID = uint64(sceneSetIDInt)
	_, scenes, err := svc.ListAutotestScene(sceneListReq)
	if err != nil {
		return "", err
	}

	var spec pipelineyml.Spec
	spec.Version = "1.1"
	spec.Stages = make([]*pipelineyml.Stage, len(scenes))
	for index, v := range scenes {
		var specStage pipelineyml.Stage

		var sceneReq apistructs.AutotestSceneRequest
		sceneReq.SceneID = v.ID
		inputs, err := svc.ListAutoTestSceneInput(sceneReq.SceneID)
		if err != nil {
			return "", err
		}

		var params = make(map[string]interface{})
		for _, input := range inputs {
			params[input.Name] = input.Value
		}

		sceneJson, err := json.Marshal(v)
		if err != nil {
			return "", nil
		}

		specStage.Actions = append(specStage.Actions, map[pipelineyml.ActionType]*pipelineyml.Action{
			pipelineyml.Snippet: {
				Alias:  pipelineyml.ActionAlias(strconv.Itoa(int(v.ID))),
				Type:   pipelineyml.Snippet,
				Params: params,
				Labels: map[string]string{
					apistructs.AutotestType:  apistructs.AutotestScene,
					apistructs.AutotestScene: base64.StdEncoding.EncodeToString(sceneJson),
				},
				If: expression.LeftPlaceholder + " 1 == 1 " + expression.RightPlaceholder,
				SnippetConfig: &pipelineyml.SnippetConfig{
					Name:   strconv.Itoa(int(v.ID)),
					Source: apistructs.PipelineSourceAutoTest.String(),
					Labels: map[string]string{
						apistructs.LabelAutotestExecType: apistructs.SceneAutotestExecType,
						apistructs.LabelSceneID:          strconv.Itoa(int(v.ID)),
						apistructs.LabelSpaceID:          strconv.Itoa(int(v.SpaceID)),
						apistructs.LabelTestPlanID:       req.Labels[apistructs.LabelTestPlanID],
						apistructs.LabelIterationID:      req.Labels[apistructs.LabelIterationID],
					},
				},
			},
		})
		spec.Stages[index] = &specStage
	}

	yml, err := pipelineyml.GenerateYml(&spec)
	if err != nil {
		return "", err
	}

	return string(yml), nil
}

func (svc *Service) BatchQueryScenePipelineSnippetYaml(configs []apistructs.SnippetConfig) ([]apistructs.BatchSnippetConfigYml, error) {

	var configsMap = map[uint64]apistructs.SnippetConfig{}

	var setIds []uint64
	for _, req := range configs {
		sceneIDStr := req.Labels[apistructs.LabelSceneID]
		if sceneIDStr == "" {
			return nil, fmt.Errorf("not find labels sceneSetID")
		}
		sceneSetIDInt, err := strconv.Atoi(sceneIDStr)
		if err != nil {
			return nil, err
		}
		setIds = append(setIds, uint64(sceneSetIDInt))
		configsMap[uint64(sceneSetIDInt)] = req
	}

	results, err := svc.GetAutotestScenesByIDs(setIds)
	if err != nil {
		return nil, err
	}
	for _, v := range setIds {
		_, ok := results[v]
		if !ok {
			return nil, fmt.Errorf("not find scene snippet: %v", v)
		}
	}

	var resultConfigs []apistructs.BatchSnippetConfigYml

	for key, v := range results {
		yml, err := svc.DoSceneToYml(v.Steps, v.Inputs, v.Output, configsMap[key])
		if err != nil {
			return nil, err
		}
		resultConfigs = append(resultConfigs, apistructs.BatchSnippetConfigYml{
			Yml:    yml,
			Config: configsMap[key],
		})
	}

	return resultConfigs, nil
}

func isRefSet(config apistructs.SnippetConfig) string {
	if config.Labels[apistructs.LabelAutotestExecType] != apistructs.SceneSetsAutotestExecType {
		return "false"
	}
	if config.Labels[apistructs.LabelSceneID] == "" {
		return "false"
	}
	return "true"
}

func (svc *Service) QueryScenePipelineSnippetYaml(req apistructs.SnippetConfig) (string, error) {
	sceneIDStr := req.Labels[apistructs.LabelSceneID]
	if sceneIDStr == "" {
		return "", fmt.Errorf("not find labels sceneSetID")
	}
	sceneSetIDInt, err := strconv.Atoi(sceneIDStr)
	if err != nil {
		return "", err
	}

	yml, err := svc.SceneToYml(uint64(sceneSetIDInt), req)
	if err != nil {
		return "", err
	}

	return yml, nil
}

// ListStepByPlanID .
func (svc *Service) ListStepByPlanID(planIDs ...uint64) (steps []apistructs.TestPlanV2Step, err error) {
	list, err := svc.db.ListStepByPlanID(planIDs...)
	if err != nil {
		return nil, err
	}
	for _, v := range list {
		steps = append(steps, *v.Convert2DTO())
	}
	return
}

// ListTestPlanV2ByID .
func (svc *Service) ListTestPlanV2ByID(planIDs ...uint64) (steps []apistructs.TestPlanV2, err error) {
	list, err := svc.db.ListTestPlanV2ByID(planIDs...)
	if err != nil {
		return nil, err
	}
	for _, v := range list {
		steps = append(steps, v.Convert2DTO())
	}
	return
}

// ListAutoTestExecHistory .
func (svc *Service) ListAutoTestExecHistory(timeStart, timeEnd string, planIDs ...uint64) (history []apistructs.AutoTestExecHistoryDto, err error) {
	list, err := svc.db.ListAutoTestExecHistory(timeStart, timeEnd, planIDs...)
	if err != nil {
		return nil, err
	}
	for _, v := range list {
		history = append(history, v.Convert())
	}
	return
}

// GetAutoTestExecHistoryByPipelineID .
func (svc *Service) GetAutoTestExecHistoryByPipelineID(pipelineID uint64) (*apistructs.AutoTestExecHistoryDto, error) {
	history, err := svc.db.GetAutoTestExecHistoryByPipelineID(pipelineID)
	if err != nil {
		return nil, err
	}
	historyDto := history.Convert()
	return &historyDto, nil
}

// ExecHistorySceneAvgCostTime .
func (svc *Service) ExecHistorySceneAvgCostTime(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistorySceneAvgCostTime, err error) {
	return svc.db.ExecHistorySceneAvgCostTime(req)
}

// ExecHistorySceneStatusCount .
func (svc *Service) ExecHistorySceneStatusCount(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistorySceneStatusCount, err error) {
	return svc.db.ExecHistorySceneStatusCount(req)
}

// ExecHistorySceneApiStatusCount .
func (svc *Service) ExecHistorySceneApiStatusCount(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistorySceneApiStatusCount, err error) {
	return svc.db.ExecHistorySceneApiStatusCount(req)
}

// ExecHistoryApiAvgCostTime .
func (svc *Service) ExecHistoryApiAvgCostTime(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistoryApiAvgCostTime, err error) {
	return svc.db.ExecHistoryApiAvgCostTime(req)
}

// ExecHistoryApiStatusCount .
func (svc *Service) ExecHistoryApiStatusCount(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistoryApiStatusCount, err error) {
	return svc.db.ExecHistoryApiStatusCount(req)
}

// ListExecHistorySceneSetByParentPID .
func (svc *Service) ListExecHistorySceneSetByParentPID(parentPID uint64) (history []apistructs.AutoTestExecHistoryDto, err error) {
	list, err := svc.db.ListExecHistorySceneSetByParentPID(parentPID)
	if err != nil {
		return nil, err
	}
	for _, v := range list {
		history = append(history, v.Convert())
	}
	return
}
