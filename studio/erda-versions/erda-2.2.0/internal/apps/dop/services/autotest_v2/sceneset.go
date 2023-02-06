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
)

const MAX_SIZE int = 200

func (svc *Service) CreateSceneSet(req apistructs.SceneSetRequest) (uint64, error) {
	sets, err := svc.GetSceneSetsBySpaceID(req.SpaceID)
	if err != nil {
		return 0, err
	}

	if len(sets) == MAX_SIZE {
		return 0, apierrors.ErrCreateAutoTestSceneSet.InternalError(err)
	}

	preID := uint64(0)
	if len(sets) > 0 {
		preID = sets[len(sets)-1].ID
	}

	sceneSet := dao.SceneSet{
		Name:        req.Name,
		Description: req.Description,
		SpaceID:     req.SpaceID,
		PreID:       preID,
		CreatorID:   req.UserID,
		UpdaterID:   req.UserID,
	}

	if err := svc.db.CreateSceneSet(&sceneSet); err != nil {
		return 0, apierrors.ErrCreateAutoTestSceneSet.InternalError(err)
	}
	return sceneSet.ID, nil
}

func (svc *Service) GetSceneSetsBySpaceID(spaceID uint64) ([]apistructs.SceneSet, error) {
	sceneSets, err := svc.db.SceneSetsBySpaceID(spaceID)
	if err != nil {
		return nil, apierrors.ErrListAutoTestSceneSet.InternalError(err)
	}

	setMap := make(map[uint64]apistructs.SceneSet)
	for _, v := range sceneSets {
		setMap[v.PreID] = *mapping(&v)
	}

	var res []apistructs.SceneSet
	for head := uint64(0); ; {
		s, ok := setMap[head]
		if !ok {
			break
		}
		head = s.ID
		res = append(res, s)
	}

	// res := make([]apistructs.SceneSet, 0, len(sceneSets))
	// for _, item := range sceneSets {
	// 	res = append(res, *mapping(&item))
	// }
	return res, nil
}

func (svc *Service) UpdateSceneSet(setID uint64, req apistructs.SceneSetRequest) (*apistructs.SceneSet, error) {
	s, err := svc.db.GetSceneSet(setID)
	if err != nil {
		return nil, apierrors.ErrGetAutoTestSceneSet.InternalError(err)
	}

	if len(req.Name) > 0 {
		s.Name = req.Name
	}
	if len(req.Description) > 0 {
		s.Description = req.Description
	}

	res, err := svc.db.UpdateSceneSet(s)
	if err != nil {
		return nil, apierrors.ErrUpdateAutoTestSceneSet.InternalError(err)
	}

	return mapping(res), nil
}

func (svc *Service) GetSceneSet(setID uint64) (*apistructs.SceneSet, error) {
	s, err := svc.db.GetSceneSet(setID)
	if err != nil {
		return nil, apierrors.ErrGetAutoTestSceneSet.InternalError(err)
	}
	return mapping(s), nil
}

func mapping(s *dao.SceneSet) *apistructs.SceneSet {
	return &apistructs.SceneSet{
		ID:          s.ID,
		Name:        s.Name,
		SpaceID:     s.SpaceID,
		PreID:       s.PreID,
		Description: s.Description,
		CreatorID:   s.CreatorID,
		UpdaterID:   s.UpdaterID,
		CreatedAt:   s.CreatedAt,
		UpdatedAt:   s.UpdatedAt,
	}
}

func (svc *Service) ExecuteAutotestSceneSet(req apistructs.AutotestExecuteSceneSetRequest) (*apistructs.PipelineDTO, error) {
	var spec pipelineyml.Spec
	spec.Version = "1.1"
	var stagesValue []*pipelineyml.Stage

	scenes, err := svc.ListAutotestScenes([]uint64{req.AutoTestSceneSet.ID})
	if err != nil {
		return nil, err
	}
	if _, ok := scenes[req.AutoTestSceneSet.ID]; !ok {
		return nil, fmt.Errorf("not find SceneSet ID: %v", req.AutoTestSceneSet.ID)
	}
	sceneList := svc.sortAutoTestSceneList(scenes[req.AutoTestSceneSet.ID], 1, 10000)
	sceneGroupMap, groupIDs := getSceneMapByGroupID(sceneList)

	for _, groupID := range groupIDs {
		var specStage pipelineyml.Stage
		for _, v := range sceneGroupMap[groupID] {
			inputs := v.Inputs

			var params = make(map[string]interface{})
			for _, input := range inputs {
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
								//apistructs.LabelIsRefSet:         "true",
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
								// apistructs.LabelIsRefSet:         isRefSetMap[key],
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

	sceneSet, err := svc.GetSceneSet(req.AutoTestSceneSet.ID)
	if err != nil {
		return nil, err
	}
	space, err := svc.GetSpace(sceneSet.SpaceID)
	if err != nil {
		return nil, err
	}
	project, err := svc.bdl.GetProject(uint64(space.ProjectID))
	if err != nil {
		return nil, err
	}
	org, err := svc.bdl.GetOrg(project.OrgID)
	if err != nil {
		return nil, err
	}

	var reqPipeline = apistructs.PipelineCreateRequestV2{
		PipelineYmlName: apistructs.PipelineSourceAutoTestSceneSet.String() + "-" + strconv.Itoa(int(req.AutoTestSceneSet.ID)),
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
		testClusterName, err := svc.GetTestClusterNameBySpaceID(req.AutoTestSceneSet.SpaceID)
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

func getSceneMapByGroupID(scenes []apistructs.AutoTestScene) (map[uint64][]*apistructs.AutoTestScene, []uint64) {
	sceneGroupMap := make(map[uint64][]*apistructs.AutoTestScene, 0)
	groupIDs := make([]uint64, 0)
	for i, v := range scenes {
		if v.GroupID == 0 {
			v.GroupID = v.ID
		}
		if _, ok := sceneGroupMap[v.GroupID]; ok {
			sceneGroupMap[v.GroupID] = append(sceneGroupMap[v.GroupID], &scenes[i])
		} else {
			sceneGroupMap[v.GroupID] = []*apistructs.AutoTestScene{&scenes[i]}
			groupIDs = append(groupIDs, v.GroupID)
		}
	}
	return sceneGroupMap, groupIDs
}
