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
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/cookiejar"
	"regexp"
	"strconv"
	"strings"
	"time"

	"golang.org/x/net/publicsuffix"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/autotest"
	"github.com/erda-project/erda/internal/apps/dop/utils"
	"github.com/erda-project/erda/pkg/apitestsv2"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/http/customhttp"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pexpr"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	maxSize       int = 500
	spaceMaxSize  int = 50000
	nameMaxLength int = 50
	descMaxLength int = 1000
)

// CreateAutotestScene 创建场景
func (svc *Service) CreateAutotestScene(req apistructs.AutotestSceneRequest) (uint64, error) {
	set, err := svc.GetSceneSet(req.SetID)
	if err != nil {
		return 0, err
	}
	sp, err := svc.GetSpace(set.SpaceID)
	if err != nil {
		return 0, err
	}
	if !sp.IsOpen() {
		return 0, fmt.Errorf("the space is locked")
	}
	req.SpaceID = sp.ID

	// 鉴权
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.IdentityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return 0, err
		}
		if !access.Access {
			return 0, apierrors.ErrUpdateAutoTestScene.AccessDenied()
		}
	}

	if err = strutil.Validate(req.Name, strutil.MaxRuneCountValidator(nameMaxLength)); err != nil {
		return 0, err
	}
	if err = strutil.Validate(req.Description, strutil.MaxRuneCountValidator(descMaxLength)); err != nil {
		return 0, err
	}
	if ok, _ := regexp.MatchString("^[a-zA-Z\u4e00-\u9fa50-9_-]*$", req.Name); !ok {
		return 0, apierrors.ErrCreateAutoTestScene.InvalidState("只可输入中文、英文、数字、中划线或下划线")
	}

	if ok := svc.checkSceneSetSameNameScene(req.SetID, req.Name, 0); !ok {
		return 0, apierrors.ErrCreateAutoTestScene.AlreadyExists()
	}

	// check whether the depth of the reference scene set is greater than 10
	checkScene := dao.AutoTestScene{
		SpaceID:  req.SpaceID,
		SetID:    req.SetID,
		RefSetID: req.RefSetID,
		Name:     req.Name,
	}
	if err := svc.checkCycle(checkScene, nil, nil, nil); err != nil {
		return 0, apierrors.ErrCreateAutoTestScene.InternalError(err)
	}

	// 一个场景集下500个场景
	total, scenes, err := svc.ListAutotestScene(req)
	if err != nil {
		return 0, err
	}
	if int(total) >= maxSize {
		return 0, fmt.Errorf("一个场景集合下，限制500个测试场景")
	}

	// 一个测试空间下50000个场景
	total, err = svc.db.CountSceneBySpaceID(req.SpaceID)
	if err != nil {
		return 0, err
	}
	if int(total) >= spaceMaxSize {
		return 0, fmt.Errorf("一个空间下，限制五万个场景")
	}

	var preID uint64
	if req.PreID > 0 {
		preID = req.PreID
	} else {
		if len(scenes) > 0 {
			preID = scenes[len(scenes)-1].ID
		} else {
			preID = 0
		}
	}

	// Check if in the same scene set
	if preID > 0 {
		preScene, err := svc.db.GetAutotestScene(preID)
		if err != nil {
			return 0, err
		}
		if preScene.SetID != req.SetID {
			return 0, fmt.Errorf("operation is not in the same scene set")
		}
	}

	scene := &dao.AutoTestScene{
		Name:        req.Name,
		Description: req.Description,
		SpaceID:     req.SpaceID,
		SetID:       req.SetID,
		PreID:       preID,
		CreatorID:   req.UserID,
		Status:      apistructs.DefaultSceneStatus,
		RefSetID:    req.RefSetID,
		GroupID:     req.SceneGroupID,
	}
	if scene.RefSetID > 0 && req.Policy != "" {
		scene.Policy = req.Policy
	}
	if err := svc.db.CreateAutotestScene(scene); err != nil {
		return 0, err
	}
	return scene.ID, nil
}

// check whether the nesting depth of the scene reference scene set is greater than a certain value
func (svc *Service) checkCycle(scene dao.AutoTestScene, allNodes []uint64, allNodesMap map[uint64]bool, tx *gorm.DB) error {
	// Initialization data, add begin data
	if allNodes == nil && allNodesMap == nil {
		allNodes = append(allNodes, scene.SetID)
		allNodesMap = map[uint64]bool{
			scene.SetID: true,
		}
	}

	if scene.RefSetID <= 0 {
		return nil
	}

	// checkCycle
	if _, ok := allNodesMap[scene.RefSetID]; !ok {
		allNodesMap[scene.RefSetID] = true
		allNodes = append(allNodes, scene.RefSetID)
	} else {
		return fmt.Errorf("error scene reference scene set cycle error, scene reference path : %v", allNodes)
	}

	_, refScenes, err := svc.db.ListAutotestSceneTx(apistructs.AutotestSceneRequest{SetID: scene.RefSetID}, tx)
	if err != nil {
		return err
	}

	// depth first, meet the bifurcation record backtracking
	for _, refScene := range refScenes {
		if refScene.RefSetID <= 0 {
			continue
		}
		err := svc.checkCycle(refScene, allNodes, allNodesMap, tx)
		if err != nil {
			return err
		}
		if len(allNodes) > 0 {
			allNodes = allNodes[:len(allNodes)-1]
		}
		delete(allNodesMap, refScene.RefSetID)
	}

	return nil
}

// UpdateAutotestScene 更新场景
func (svc *Service) UpdateAutotestScene(req apistructs.AutotestSceneSceneUpdateRequest) (uint64, error) {
	sc, err := svc.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: req.SceneID})
	if err != nil {
		return 0, err
	}
	req.SetID = sc.SetID
	sp, err := svc.GetSpace(sc.SpaceID)
	if err != nil {
		return 0, err
	}
	if !sp.IsOpen() {
		return 0, fmt.Errorf("the space is locked")
	}

	// check permission
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.IdentityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return 0, err
		}
		if !access.Access {
			return 0, apierrors.ErrUpdateAutoTestScene.AccessDenied()
		}
	}
	if err := strutil.Validate(req.Name, strutil.MaxRuneCountValidator(nameMaxLength)); err != nil {
		return 0, err
	}
	if err := strutil.Validate(req.Description, strutil.MaxRuneCountValidator(descMaxLength)); err != nil {
		return 0, err
	}
	if ok, _ := regexp.MatchString("^[a-zA-Z\u4e00-\u9fa50-9_-]*$", req.Name); !ok {
		return 0, apierrors.ErrUpdateAutoTestScene.InvalidState("只可输入中文、英文、数字、中划线或下划线")
	}

	scene, err := svc.db.GetAutotestScene(req.SceneID)
	if err != nil {
		return 0, err
	}

	if ok := svc.checkSceneSetSameNameScene(req.SetID, req.Name, scene.ID); !ok {
		return 0, apierrors.ErrUpdateAutoTestScene.AlreadyExists()
	}

	if req.Name != "" {
		scene.Name = req.Name
	}
	if req.Status != "" {
		scene.Status = req.Status
	}
	scene.Description = req.Description
	if !req.IsStatus {
		scene.UpdaterID = req.IdentityInfo.UserID
	}
	if scene.RefSetID > 0 && req.Policy != "" {
		scene.Policy = req.Policy
	}
	if err = svc.db.UpdateAutotestScene(scene); err != nil {
		return 0, err
	}
	go svc.db.AfterUpdateAutoTestSpaceElements(sc.SpaceID)
	return scene.ID, nil
}

// MoveAutotestScene 移动场景
func (svc *Service) MoveAutotestScene(req apistructs.AutotestSceneRequest) (uint64, error) {
	var preID uint64
	// 移动到另一scene set
	if req.GroupID > 0 {

		if ok := svc.checkSceneSetSameNameScene(uint64(req.GroupID), req.Name, req.ID); !ok {
			return 0, apierrors.ErrMoveAutoTestScene.AlreadyExists()
		}

		_, scenes, err := svc.ListAutotestScene(apistructs.AutotestSceneRequest{SetID: uint64(req.GroupID)})
		if err != nil {
			return 0, err
		}
		preID = 0
		if len(scenes) > 0 {
			preID = scenes[len(scenes)-1].ID
		}
		if preID == req.ID {
			return 0, nil
		}
	} else {
		setID, id, ok, err := svc.db.GetAutoTestScenePreByPosition(req)
		if err != nil {
			return 0, err
		}
		if ok == true {
			return req.ID, nil
		}
		preID = id
		req.GroupID = int64(setID)

		if ok := svc.checkSceneSetSameNameScene(uint64(req.GroupID), req.Name, req.ID); !ok {
			return 0, apierrors.ErrMoveAutoTestScene.AlreadyExists()
		}
	}

	err := svc.db.Transaction(func(tx *gorm.DB) error {
		err := svc.db.MoveAutoTestScene(req.ID, preID, uint64(req.GroupID), tx)
		if err != nil {
			return err
		}

		scene, err := svc.db.GetAutotestSceneTx(req.ID, tx)
		if err != nil {
			return err
		}

		// scene reference scene set is moved
		if scene.RefSetID > 0 && req.GroupID > 0 {
			checkScene := dao.AutoTestScene{
				SpaceID:  scene.SpaceID,
				SetID:    scene.SetID,
				RefSetID: scene.RefSetID,
			}
			err := svc.checkCycle(checkScene, nil, nil, tx)
			if err != nil {
				return err
			}
		}
		return nil
	})
	if err != nil {
		return 0, err
	}

	return req.ID, nil
}

// MoveAutotestSceneV2  Move scene between scene set, include the group drag
func (svc *Service) MoveAutotestSceneV2(req apistructs.AutotestSceneMoveRequest) error {
	sc, err := svc.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: req.FirstID})
	if err != nil {
		return err
	}

	sp, err := svc.GetSpace(sc.SpaceID)
	if err != nil {
		return err
	}
	if !sp.IsOpen() {
		return fmt.Errorf("the space is locked")
	}
	// check the permission
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.IdentityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrMoveAutoTestScene.AccessDenied()
		}
	}
	return svc.db.MoveAutoTestSceneV2(req)
}

func (svc *Service) checkSceneSetSameNameScene(sceneSetID uint64, sceneName string, sceneID uint64) bool {
	dbScenes, err := svc.db.FindSceneBySetAndName(sceneSetID, sceneName)
	if err != nil {
		return false
	}
	var checkExistScenes []dao.AutoTestScene
	for _, rangeScene := range dbScenes {
		// mysql not case sensitive
		if rangeScene.Name != sceneName {
			continue
		}
		if rangeScene.ID == sceneID {
			continue
		}
		checkExistScenes = append(checkExistScenes, rangeScene)
	}
	if len(checkExistScenes) >= 1 {
		return false
	}
	return true
}

// GetAutotestScene 获取场景
func (svc *Service) GetAutotestScene(req apistructs.AutotestSceneRequest) (*apistructs.AutoTestScene, error) {
	sc, err := svc.db.GetAutotestScene(req.SceneID)
	if err != nil {
		return nil, err
	}
	scene := sc.Convert()
	return &scene, nil
}

// ListAutotestScene 获取场景列表
func (svc *Service) ListAutotestScene(req apistructs.AutotestSceneRequest) (uint64, []apistructs.AutoTestScene, error) {
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 500
	}
	total, scene, err := svc.db.ListAutotestScene(req)
	if err != nil {
		return 0, nil, err
	}

	list := getList(scene, req.PageNo, req.PageSize)

	var sceneIDs []uint64
	// 通过pre_id获取顺序列表
	sceneMap := make(map[uint64]int)
	for i, v := range list {
		sceneMap[v.ID] = i
		sceneIDs = append(sceneIDs, v.ID)
	}

	stepIDs, err := svc.db.GetAutoTestSceneStepCount(sceneIDs)
	if err != nil {
		return 0, nil, err
	}
	for _, v := range stepIDs {
		list[sceneMap[v.SceneID]].StepCount++
	}
	return total, list, nil
}

// ListAutotestScenes 批量获取场景列表
func (svc *Service) ListAutotestScenes(setIDs []uint64) (map[uint64][]apistructs.AutoTestScene, error) {

	scene, err := svc.db.ListAutotestScenes(setIDs)
	if err != nil {
		return nil, err
	}
	var sceneIDs []uint64
	for _, each := range scene {
		sceneIDs = append(sceneIDs, each.ID)
	}
	inputs, err := svc.ListAutoTestSceneInputByScenes(sceneIDs)
	if err != nil {
		return nil, err
	}
	outputs, err := svc.ListAutoTestSceneOutputByScenes(sceneIDs)
	if err != nil {
		return nil, err
	}

	lists := map[uint64][]apistructs.AutoTestScene{}
	for _, each := range scene {

		var inputsList []apistructs.AutoTestSceneInput
		for inputsNo := 0; inputsNo < len(inputs); inputsNo++ {
			if each.ID == inputs[inputsNo].SceneID {
				inputsList = append(inputsList, inputs[inputsNo])
			}
		}

		var outputList []apistructs.AutoTestSceneOutput
		for outputNo := 0; outputNo < len(outputs); outputNo++ {
			if each.ID == outputs[outputNo].SceneID {
				outputList = append(outputList, outputs[outputNo])
			}
		}

		lists[each.SetID] = append(lists[each.SetID], apistructs.AutoTestScene{
			AutoTestSceneParams: apistructs.AutoTestSceneParams{
				ID:        each.ID,
				SpaceID:   each.SpaceID,
				CreatorID: each.CreatorID,
				UpdaterID: each.UpdaterID,
			},
			Name:        each.Name,
			Description: each.Description,
			PreID:       each.PreID,
			SetID:       each.SetID,
			CreateAt:    &each.CreatedAt,
			UpdateAt:    &each.UpdatedAt,
			Status:      each.Status,
			Inputs:      inputsList,
			Output:      outputList,
			RefSetID:    each.RefSetID,
			GroupID:     each.GroupID,
			Policy:      each.Policy,
		})
	}
	return lists, nil
}

func (svc *Service) IsExistSceneSetName(spaceID uint64, sceneSetName string) bool {
	sceneSets, err := svc.db.SceneSetsBySpaceID(spaceID)
	if err != nil {
		return false
	}
	for _, set := range sceneSets {
		if set.Name == sceneSetName {
			return true
		}
	}
	return false
}

// CutAutotestScene 移除场景
func (svc *Service) CutAutotestScene(sc *dao.AutoTestScene) error {
	next, err := svc.db.GetAutotestSceneByPreID(sc.ID)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return err
		}
		next = nil
	}
	if next != nil {
		next.PreID = sc.PreID
	}
	return svc.db.UpdateAutotestScene(next)
}

// DeleteAutotestScene 删除场景
func (svc *Service) DeleteAutotestScene(id uint64, identityInfo apistructs.IdentityInfo) error {
	sc, err := svc.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: id})
	if err != nil {
		return err
	}
	sp, err := svc.GetSpace(sc.SpaceID)
	if err != nil {
		return err
	}
	if !sp.IsOpen() {
		return fmt.Errorf("the space is locked")
	}
	// check permission
	if !identityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return err
		}
		if !access.Access {
			return apierrors.ErrDeleteAutoTestScene.AccessDenied()
		}
	}
	err = svc.db.DeleteAutoTestScene(id)
	if err == nil {
		go svc.db.AfterUpdateAutoTestSpaceElements(sc.SpaceID)
	}
	return err
}

// UpdateAutotestSceneUpdater 更新场景更新人
func (svc *Service) UpdateAutotestSceneUpdater(sceneID uint64, userID string) error {
	return svc.db.UpdateAutotestSceneUpdater(sceneID, userID)
}

// UpdateAutotestSceneUpdateTime 更新场景更新时间
func (svc *Service) UpdateAutotestSceneUpdateTime(sceneID uint64) error {
	return svc.db.UpdateAutotestSceneUpdateAt(sceneID, time.Now())
}

func (svc *Service) ExecuteDiceAutotestScene(req apistructs.AutotestExecuteSceneRequest) (*apistructs.PipelineDTO, error) {
	var autotestSceneRequest apistructs.AutotestSceneRequest
	autotestSceneRequest.SceneID = req.AutoTestScene.ID
	scene, err := svc.GetAutotestScene(autotestSceneRequest)
	if err != nil {
		return nil, err
	}

	sceneInputs, err := svc.ListAutoTestSceneInput(scene.ID)
	if err != nil {
		return nil, err
	}

	yml, err := svc.SceneToYml(scene.ID, apistructs.SnippetConfig{})
	if err != nil {
		return nil, err
	}

	var params []apistructs.PipelineRunParam
	for _, input := range sceneInputs {
		// replace mock temp before create pipeline
		// and so steps can use the same mock temp
		replacedTemp := expression.ReplaceRandomParams(input.Temp)
		params = append(params, apistructs.PipelineRunParam{
			Name:  input.Name,
			Value: replacedTemp,
		})
	}

	space, err := svc.GetSpace(scene.SpaceID)
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
		PipelineYmlName: strconv.Itoa(int(scene.ID)),
		PipelineSource:  apistructs.PipelineSourceAutoTest,
		AutoRun:         true,
		ForceRun:        true,
		ClusterName:     req.ClusterName,
		PipelineYml:     yml,
		Labels:          req.Labels,
		RunParams:       params,
		IdentityInfo:    req.IdentityInfo,
		NormalLabels:    map[string]string{apistructs.LabelOrgName: org.Name, apistructs.LabelOrgID: strconv.FormatUint(org.ID, 10)},
	}

	if req.ConfigManageNamespaces != "" {
		reqPipeline.ConfigManageNamespaces = append(reqPipeline.ConfigManageNamespaces, req.ConfigManageNamespaces)
	}

	if reqPipeline.ClusterName == "" {
		testClusterName, err := svc.GetTestClusterNameBySpaceID(scene.SpaceID)
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

func (svc *Service) ExecuteDiceAutotestSceneStep(req apistructs.AutotestExecuteSceneStepRequest) (*apistructs.AutotestExecuteSceneStepRespData, error) {
	step, err := svc.db.GetAutoTestSceneStep(req.SceneStepID)
	if err != nil {
		return nil, err
	}
	if step.Type != apistructs.StepTypeAPI {
		return nil, fmt.Errorf("only supports api type execution")
	}
	if step.Value == "" {
		return nil, fmt.Errorf("no api is referenced")
	}

	var pipelineCmsGetConfigsRequest cmspb.CmsNsConfigsGetRequest
	pipelineCmsGetConfigsRequest.PipelineSource = apistructs.PipelineSourceAutoTest.String()
	pipelineCmsGetConfigsRequest.GlobalDecrypt = true
	pipelineCmsGetConfigsRequest.Ns = req.ConfigManageNamespaces
	configs, _ := svc.cms.GetCmsNsConfigs(utils.WithInternalClientContext(context.Background()), &pipelineCmsGetConfigsRequest)

	caseParams := make(map[string]*apistructs.CaseParams)
	apiTestEnvData := &apistructs.APITestEnvData{}
	for _, conf := range configs.Data {
		switch conf.Key {
		case autotest.CmsCfgKeyAPIGlobalConfig:
			var apiConfig apistructs.AutoTestAPIConfig
			if err := json.Unmarshal([]byte(conf.Value), &apiConfig); err != nil {
				return nil, fmt.Errorf("failed to unmarshal apiConfig, err: %v", err)
			}
			apiTestEnvData.Domain = apiConfig.Domain
			apiTestEnvData.Header = apiConfig.Header
			apiTestEnvData.Global = make(map[string]*apistructs.APITestEnvVariable)
			for name, item := range apiConfig.Global {
				apiTestEnvData.Global[name] = &apistructs.APITestEnvVariable{
					Value: item.Value,
					Type:  item.Type,
				}
				caseParams[name] = &apistructs.CaseParams{
					Key:   name,
					Type:  item.Type,
					Value: item.Value,
				}
			}
		}
	}

	var jsonMap = map[string]interface{}{}
	err = json.Unmarshal([]byte(step.Value), &jsonMap)
	if err != nil {
		return nil, err
	}

	sceneInputs, err := svc.ListAutoTestSceneInput(step.SceneID)
	if err != nil {
		return nil, err
	}

	specJson, err := json.Marshal(jsonMap["apiSpec"])
	if err != nil {
		return nil, err
	}

	var apiTestStr = string(specJson)
	for _, param := range sceneInputs {
		param.Temp = convertJsonParam(param.Temp)
		apiTestStr = strings.ReplaceAll(apiTestStr, expression.LeftPlaceholder+" "+expression.Params+"."+param.Name+" "+expression.RightPlaceholder, expression.ReplaceRandomParams(param.Temp))
		apiTestStr = strings.ReplaceAll(apiTestStr, expression.OldLeftPlaceholder+expression.Params+"."+param.Name+expression.OldRightPlaceholder, expression.ReplaceRandomParams(param.Temp))
	}

	apiTestStr = svc.renderPreSceneStepsOutput(step.SceneID, apiTestStr)

	for _, conf := range configs.Data {
		switch conf.Key {
		case autotest.CmsCfgKeyAPIGlobalConfig:
			var apiConfig apistructs.AutoTestAPIConfig
			if err := json.Unmarshal([]byte(conf.Value), &apiConfig); err != nil {
				return nil, fmt.Errorf("failed to unmarshal apiConfig, err: %v", err)
			}
			for _, item := range apiConfig.Global {
				item.Value = convertJsonParam(item.Value)
				apiTestStr = strings.ReplaceAll(apiTestStr, expression.LeftPlaceholder+" "+expression.Configs+"."+apistructs.PipelineSourceAutoTest.String()+"."+item.Name+" "+expression.RightPlaceholder, expression.ReplaceRandomParams(item.Value))
			}
		}
	}

	apiTestStr = expression.ReplaceRandomParams(apiTestStr)

	var apiInfoV2 apistructs.APIInfoV2
	err = json.Unmarshal([]byte(apiTestStr), &apiInfoV2)
	if err != nil {
		return nil, err
	}

	clusterName, err := svc.getClusterNameBySpaceID(step.SpaceID)
	if err != nil {
		return nil, err
	}

	apiTest := apitestsv2.New(&apistructs.APIInfo{
		ID:        apiInfoV2.ID,
		Name:      apiInfoV2.Name,
		URL:       apiInfoV2.URL,
		Method:    apiInfoV2.Method,
		Headers:   apiInfoV2.Headers,
		Params:    apiInfoV2.Params,
		Body:      apiInfoV2.Body,
		OutParams: apiInfoV2.OutParams,
		Asserts:   [][]apistructs.APIAssert{apiInfoV2.Asserts},
	}, apitestsv2.WithNetportalConfigs(customhttp.GetNetPortalUrl(clusterName)))

	var respData apistructs.AutotestExecuteSceneStepRespData
	cookieJar, err := cookiejar.New(&cookiejar.Options{PublicSuffixList: publicsuffix.List})
	if err != nil {
		return nil, err
	}
	hc := http.Client{Jar: cookieJar}
	apiReq, apiResp, err := apiTest.Invoke(&hc, apiTestEnvData, caseParams)
	if err != nil {
		// 单个 API 执行失败，不返回失败，继续执行下一个
		logrus.Warningf("invoke api error, apiInfo:%+v, (%+v)", apiTest.API, err)
		respData.Resp = &apistructs.APIResp{
			BodyStr: err.Error(),
		}
		respData.Info = apiReq
		respData.Asserts = &apistructs.APITestsAssertResult{
			Success: false,
			Result:  []*apistructs.APITestsAssertData{},
		}
		return &respData, nil
	}

	respData.Resp = apiResp
	respData.Info = apiReq
	outParams := apiTest.ParseOutParams(apiTest.API.OutParams, apiResp, caseParams)
	if len(apiTest.API.Asserts) > 0 {
		asserts := apiTest.API.Asserts[0]
		succ, assertResult := apiTest.JudgeAsserts(outParams, asserts)
		logrus.Infof("judge assert result: %v", succ)

		respData.Asserts = &apistructs.APITestsAssertResult{
			Success: succ,
			Result:  assertResult,
		}
	}

	return &respData, nil
}

func (svc *Service) getClusterNameBySpaceID(spaceID uint64) (string, error) {
	space, err := svc.GetSpace(spaceID)
	if err != nil {
		return "", fmt.Errorf("not find step space %v", spaceID)
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

func (svc *Service) renderPreSceneStepsOutput(sceneID uint64, replaceYml string) string {
	var pipelinePageListRequest = apistructs.PipelinePageListRequest{
		PageNum:  1,
		PageSize: 1,
		Sources: []apistructs.PipelineSource{
			apistructs.PipelineSourceAutoTest,
		},
		YmlNames: []string{
			strconv.Itoa(int(sceneID)),
		},
		Statuses: []string{apistructs.PipelineStatusSuccess.String(), apistructs.PipelineStatusFailed.String()},
		DescCols: []string{"id"},
	}
	pagePipeline, err := svc.bdl.PageListPipeline(pipelinePageListRequest)
	if err != nil {
		logrus.Errorf("renderPreSceneStepsOutput, pageListPipeline error %v", err)
		return replaceYml
	}

	if pagePipeline == nil || len(pagePipeline.Pipelines) <= 0 {
		return replaceYml
	}

	pipeline, err := svc.bdl.GetPipeline(pagePipeline.Pipelines[0].ID)
	if err != nil {
		logrus.Errorf("renderPreSceneStepsOutput, GetPipeline error %v", err)
		return replaceYml
	}

	if pipeline == nil {
		return replaceYml
	}

	for _, stage := range pipeline.PipelineStages {
		for _, task := range stage.PipelineTasks {
			for _, result := range task.Result.Metadata {
				replaceValue := convertJsonParam(result.Value)
				replaceYml = strings.ReplaceAll(replaceYml, expression.GenOutputRef(task.Name, result.Name), replaceValue)
			}
		}
	}

	return replaceYml
}

func (svc *Service) SceneToYml(scene uint64, req apistructs.SnippetConfig) (string, error) {
	sceneInputs, err := svc.ListAutoTestSceneInput(scene)
	if err != nil {
		return "", err
	}

	sceneOutputs, err := svc.ListAutoTestSceneOutput(scene)
	if err != nil {
		return "", err
	}

	sceneSteps, err := svc.ListAutoTestSceneStep(scene)
	if err != nil {
		return "", err
	}

	return svc.DoSceneToYml(sceneSteps, sceneInputs, sceneOutputs, req)
}

func (svc *Service) DoSceneToYml(sceneSteps []apistructs.AutoTestSceneStep, sceneInputs []apistructs.AutoTestSceneInput, sceneOutputs []apistructs.AutoTestSceneOutput, req apistructs.SnippetConfig) (string, error) {
	sceneStages := StepToStages(sceneSteps)

	yml, err := SceneToPipelineYml(sceneInputs, sceneOutputs, sceneStages, req)
	if err != nil {
		return "", err
	}
	return yml, err
}

func SceneToPipelineYml(inputs []apistructs.AutoTestSceneInput, outputs []apistructs.AutoTestSceneOutput, stages [][]apistructs.AutoTestSceneStep, req apistructs.SnippetConfig) (string, error) {
	var spec pipelineyml.Spec
	spec.Params = make([]*pipelineyml.PipelineParam, len(inputs))
	spec.Outputs = make([]*pipelineyml.PipelineOutput, len(outputs))
	for index, input := range inputs {
		spec.Params[index] = &pipelineyml.PipelineParam{
			Name:     input.Name,
			Desc:     input.Description,
			Required: true,
		}
	}
	for index, output := range outputs {
		spec.Outputs[index] = &pipelineyml.PipelineOutput{
			Name: output.Name,
			Desc: output.Description,
			Ref:  output.Value,
		}
	}
	var stagesValue []*pipelineyml.Stage
	for _, stage := range stages {
		var specStage pipelineyml.Stage
		var index = 0
		for _, step := range stage {
			// 跳过空的执行
			if step.Value == "" {
				continue
			}
			action, err := StepToAction(step, req)
			if err != nil {
				return "", err
			}
			specStage.Actions = append(specStage.Actions, action)
			index++
		}
		if index > 0 {
			stagesValue = append(stagesValue, &specStage)
		}
	}
	spec.Stages = stagesValue

	spec.Version = "1.1"
	yml, err := pipelineyml.GenerateYml(&spec)
	if err != nil {
		return "", err
	}

	return string(yml), nil
}

func StepToAction(step apistructs.AutoTestSceneStep, req apistructs.SnippetConfig) (map[pipelineyml.ActionType]*pipelineyml.Action, error) {
	var action pipelineyml.Action
	stepJson, err := json.Marshal(step)
	if err != nil {
		return nil, err
	}
	action.Labels = map[string]string{}
	action.Labels[apistructs.AutotestSceneStep] = base64.StdEncoding.EncodeToString(stepJson)
	action.Labels[apistructs.AutotestType] = apistructs.AutotestSceneStep
	action.Labels[apistructs.LabelIsRefSet] = req.Labels[apistructs.LabelIsRefSet]
	action.Alias = pipelineyml.ActionAlias(strconv.Itoa(int(step.ID)))
	action.If = expression.LeftPlaceholder + " 1 == 1 " + expression.RightPlaceholder
	action.Disable = step.IsDisabled

	switch step.Type {
	case apistructs.StepTypeCustomScript:
		var value apistructs.AutoTestRunCustom
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return nil, err
		}

		action.Type = "custom-script"
		action.Version = "1.0"
		action.Commands = value.Commands
		action.Image = value.Image
	case apistructs.StepTypeScene:
		var value apistructs.AutoTestRunScene
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return nil, err
		}

		action.Params = make(map[string]interface{})
		for key, value := range value.RunParams {
			action.Params[key] = value
		}

		action.Type = pipelineyml.Snippet
		action.SnippetConfig = &pipelineyml.SnippetConfig{
			Name:   strconv.Itoa(int(value.SceneID)),
			Source: apistructs.PipelineSourceAutoTest.String(),
			Labels: map[string]string{
				apistructs.LabelAutotestExecType: apistructs.SceneAutotestExecType,
				apistructs.LabelSceneID:          strconv.Itoa(int(value.SceneID)),
				apistructs.LabelSpaceID:          strconv.Itoa(int(step.SpaceID)),
				apistructs.LabelIterationID:      req.Labels[apistructs.LabelIterationID],
				apistructs.LabelTestPlanID:       req.Labels[apistructs.LabelTestPlanID],
			},
		}
	case apistructs.StepTypeAPI:
		var value apistructs.AutoTestRunStep
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return nil, err
		}

		action.Type = "api-test"
		action.Version = "2.0"
		action.Params = value.ApiSpec
		if value.Loop != nil && value.Loop.Strategy != nil && value.Loop.Strategy.MaxTimes > 0 {
			action.Loop = value.Loop
		}
	case apistructs.StepTypeWait:
		var value apistructs.AutoTestRunWait
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return nil, err
		}
		if value.WaitTime > 0 {
			value.WaitTimeSec = value.WaitTime
		}
		if value.WaitTimeSec <= 0 {
			return nil, errors.Errorf("Invalid wait time sec: %d", value.WaitTimeSec)
		}

		action.Type = "wait"
		action.Version = "1.0"
		action.Params = map[string]interface{}{
			"wait_time_sec": value.WaitTimeSec,
		}
	case apistructs.StepTypeConfigSheet:
		var value apistructs.AutoTestRunConfigSheet
		err := json.Unmarshal([]byte(step.Value), &value)
		if err != nil {
			return nil, err
		}
		action.Params = make(map[string]interface{})
		for key, value := range value.RunParams {
			action.Params[key] = value
		}

		action.Type = apistructs.ActionTypeSnippet
		action.SnippetConfig = &pipelineyml.SnippetConfig{
			Name:   value.ConfigSheetID,
			Source: apistructs.PipelineSourceAutoTest.String(),
			Labels: map[string]string{
				apistructs.LabelSnippetScope: apistructs.FileTreeScopeAutoTestConfigSheet,
			},
		}
	}

	return map[pipelineyml.ActionType]*pipelineyml.Action{
		action.Type: &action,
	}, nil
}

func StepToStages(steps []apistructs.AutoTestSceneStep) [][]apistructs.AutoTestSceneStep {
	var stages = make([][]apistructs.AutoTestSceneStep, len(steps))
	for index, step := range steps {
		stages[index] = append(stages[index], step)
		for _, childStep := range step.Children {
			stages[index] = append(stages[index], childStep)
		}
	}
	return stages
}

func (svc *Service) CancelDiceAutotestScene(req apistructs.AutotestCancelSceneRequest) error {
	var autotestSceneRequest apistructs.AutotestSceneRequest
	autotestSceneRequest.SceneID = req.AutoTestScene.ID
	scene, err := svc.GetAutotestScene(autotestSceneRequest)
	if err != nil {
		return err
	}
	var pipelinePageListRequest = apistructs.PipelinePageListRequest{
		PageNum:  1,
		PageSize: 1,
		Sources: []apistructs.PipelineSource{
			apistructs.PipelineSourceAutoTest,
		},
		YmlNames: []string{
			strconv.Itoa(int(scene.ID)),
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

// CopyAutotestScene 复制场景
func (svc *Service) CopyAutotestScene(req apistructs.AutotestSceneCopyRequest, isSpaceCopy bool, preSceneIdMap map[uint64]uint64) (uint64, error) {
	set, err := svc.GetSceneSet(req.SetID)
	if err != nil {
		return 0, err
	}
	sp, err := svc.GetSpace(set.SpaceID)
	if err != nil {
		return 0, err
	}

	// 鉴权
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.IdentityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return 0, err
		}
		if !access.Access {
			return 0, apierrors.ErrCreateAutoTestScene.AccessDenied()
		}
	}
	// 一个场景集下500个场景
	total, err := svc.db.CountSceneBySetID(req.SetID)
	if err != nil {
		return 0, err
	}
	if int(total) >= maxSize {
		return 0, fmt.Errorf("一个场景集合下，限制500个测试场景")
	}

	// 一个测试空间下50000个场景
	total, err = svc.db.CountSceneBySpaceID(req.SpaceID)
	if err != nil {
		return 0, err
	}
	if int(total) >= spaceMaxSize {
		return 0, fmt.Errorf("一个空间下，限制五万个场景")
	}

	// 获取原场景
	oldScene, err := svc.db.GetAutotestScene(req.SceneID)
	if err != nil {
		return 0, err
	}

	if req.SetID == 0 {
		req.SetID = oldScene.SetID
	}
	// 校验目标场景集是否存在
	checkSet, err := svc.db.GetSceneSet(req.SetID)
	if err != nil {
		return 0, err
	}
	if checkSet.SpaceID != req.SpaceID {
		return 0, apierrors.ErrCopyAutoTestScene.InvalidState("目标场景集不属于目标测试空间")
	}
	// 校验目标测试空间
	checkSpace, err := svc.db.GetAutoTestSpace(req.SpaceID)
	if err != nil {
		return 0, err
	}
	if checkSpace.Status != apistructs.TestSpaceOpen && !isSpaceCopy {
		return 0, apierrors.ErrCopyAutoTestScene.InvalidState("目标测试空间已锁定")
	}
	// 校验pre
	if req.PreID != 0 {
		checkScene, err := svc.db.GetAutotestScene(req.PreID)
		if err != nil {
			return 0, err
		}
		if checkScene.SetID != req.SetID {
			return 0, apierrors.ErrCopyAutoTestScene.InvalidState("目标场景不属于目标场景集")
		}
	}

	// 复制到指定位置
	newSceneName, err := svc.GenerateSceneName(oldScene.Name, req.SetID)
	if err != nil {
		return 0, err
	}
	newScene := &dao.AutoTestScene{
		Name:        newSceneName,
		Description: oldScene.Description,
		SpaceID:     req.SpaceID,
		SetID:       req.SetID,
		PreID:       req.PreID,
		CreatorID:   req.UserID,
		Status:      apistructs.DefaultSceneStatus,
		RefSetID:    oldScene.RefSetID,
	}

	if err = svc.db.Copy(newScene, newScene.SetID != oldScene.SetID); err != nil {
		return 0, err
	}

	newId := newScene.ID

	// 依次复制场景入参
	oldInput, err := svc.ListAutoTestSceneInput(req.SceneID)
	for _, v := range oldInput {
		v.Value = replacePreSceneValue(v.Value, preSceneIdMap)
		newInput := &dao.AutoTestSceneInput{
			Name:        v.Name,
			Value:       v.Value,
			Temp:        v.Temp,
			Description: v.Description,
			SceneID:     newId,
			SpaceID:     req.SpaceID,
			CreatorID:   req.UserID,
		}
		if err := svc.db.CreateAutoTestSceneInput(newInput); err != nil {
			return newScene.ID, err
		}
	}
	// 依次复制场景步骤
	step, err := svc.ListAutoTestSceneStep(req.SceneID)
	if err != nil {
		return newScene.ID, err
	}
	var head uint64
	var replaceIdMap = map[uint64]uint64{}
	for _, v := range step {
		v.Value = replacePreStepValue(v.Value, replaceIdMap)

		newStep := &dao.AutoTestSceneStep{
			Type:       v.Type,
			Value:      v.Value,
			Name:       v.Name,
			PreID:      head,
			PreType:    v.PreType,
			SceneID:    newId,
			SpaceID:    req.SpaceID,
			APISpecID:  v.APISpecID,
			CreatorID:  req.UserID,
			IsDisabled: v.IsDisabled,
		}
		if err := svc.db.CreateAutoTestSceneStep(newStep); err != nil {
			return newScene.ID, err
		}
		head = newStep.ID
		pHead := newStep.ID

		var childStepIdMap = map[uint64]uint64{}
		for _, pv := range v.Children {
			pv.Value = replacePreStepValue(pv.Value, replaceIdMap)

			newPStep := &dao.AutoTestSceneStep{
				Type:       pv.Type,
				Value:      pv.Value,
				Name:       pv.Name,
				PreID:      pHead,
				PreType:    pv.PreType,
				SceneID:    newId,
				SpaceID:    req.SpaceID,
				APISpecID:  pv.APISpecID,
				CreatorID:  req.UserID,
				IsDisabled: pv.IsDisabled,
			}

			if err := svc.db.CreateAutoTestSceneStep(newPStep); err != nil {
				return newScene.ID, err
			}
			pHead = newPStep.ID

			childStepIdMap[pv.ID] = newPStep.ID
		}

		replaceIdMap[v.ID] = newStep.ID
		for key, v := range childStepIdMap {
			replaceIdMap[key] = v
		}
	}
	// 依次复制场景出参
	oldOutput, err := svc.ListAutoTestSceneOutput(req.SceneID)
	for _, v := range oldOutput {
		v.Value = replacePreStepValue(v.Value, replaceIdMap)
		newOutput := &dao.AutoTestSceneOutput{
			Name:        v.Name,
			Value:       v.Value,
			Description: v.Description,
			SceneID:     newId,
			SpaceID:     req.SpaceID,
			CreatorID:   req.UserID,
		}
		if err := svc.db.CreateAutoTestSceneOutput(newOutput); err != nil {
			return newScene.ID, err
		}
	}

	go svc.db.AfterUpdateAutoTestSpaceElements(sp.ID)
	return newScene.ID, nil
}

func replacePreSceneValue(value string, replaceIdMap map[uint64]uint64) string {
	return replacePreStepValue(value, replaceIdMap)
}

func convertJsonParam(value string) string {
	if (strings.HasPrefix(value, "[") && strings.HasSuffix(value, "]")) ||
		(strings.HasPrefix(value, "{") && strings.HasSuffix(value, "}")) {
		return strings.ReplaceAll(value, "\"", "\\\"")
	}
	return value
}

func replacePreStepValue(value string, replaceIdMap map[uint64]uint64) string {
	if len(replaceIdMap) <= 0 {
		return value
	}

	return strutil.ReplaceAllStringSubmatchFunc(pexpr.PhRe, value, func(subs []string) string {
		phData := subs[0]
		inner := subs[1] // configs.key
		// 去除两边的空格
		inner = strings.Trim(inner, " ")
		ss := strings.SplitN(inner, ".", 3)
		if len(ss) < 2 {
			return phData
		}

		switch ss[0] {
		case expression.Outputs:
			if len(ss) > 2 {
				preIdInt, err := strconv.Atoi(ss[1])
				if err == nil {
					value, ok := replaceIdMap[uint64(preIdInt)]
					if ok {
						return strings.Replace(subs[0], ss[1], strconv.Itoa(int(value)), 1)
					}
				} else {
					logrus.Errorf("atoi name error: %v", err)
				}
			}
			return phData
		default: // case 3
			return phData
		}
	})
}

// GenerateSceneName 生成场景名，追加 (N)
func (svc *Service) GenerateSceneName(name string, setID uint64) (string, error) {
	// finalName, err := getTitleName(name)
	// if err != nil {
	// 	return "", err
	// }

	for {
		// find by name
		exist, err := svc.db.GetAutotestSceneByName(name, setID)
		if err != nil {
			return "", err
		}
		// not exist
		if exist == nil {
			return name, nil
		}
		// exist and is others, generate (N) and query again
		name, err = getTitleName(name)
		if err != nil {
			return "", err
		}
	}
}

// getTitleName 获取正确的name的后缀编号
func getTitleName(requestName string) (string, error) {
	pivot := strings.LastIndex(requestName, "_")
	if pivot < 0 {
		return fmt.Sprintf("%s%s", requestName, "_1"), nil
	}

	num, err := strconv.Atoi(requestName[pivot+1:])
	if err != nil {
		return fmt.Sprintf("%s%s", requestName, "_1"), nil
	}
	num += 1
	return fmt.Sprintf("%s%s%d", requestName[0:pivot], "_", num), nil
}

// getList 获取分页后的list
func (svc *Service) sortAutoTestSceneList(list []apistructs.AutoTestScene, pageNo, pageSize uint64) []apistructs.AutoTestScene {
	mp := make(map[uint64]apistructs.AutoTestScene)
	var rsp []apistructs.AutoTestScene
	for _, v := range list {
		mp[v.PreID] = v
	}
	l := (pageNo - 1) * pageSize
	r := l + pageSize
	index := uint64(0)
	for head := uint64(0); ; {
		s, ok := mp[head]
		if !ok {
			break
		}
		head = s.ID
		index++
		if index > l && index <= r {
			sc := s
			sc.StepCount = 0
			rsp = append(rsp, sc)
		}
	}
	return rsp
}

func getList(list []dao.AutoTestScene, pageNo, pageSize uint64) []apistructs.AutoTestScene {
	mp := make(map[uint64]dao.AutoTestScene)
	var rsp []apistructs.AutoTestScene
	for _, v := range list {
		mp[v.PreID] = v
	}
	l := (pageNo - 1) * pageSize
	r := l + pageSize
	index := uint64(0)
	for head := uint64(0); ; {
		s, ok := mp[head]
		if !ok {
			break
		}
		head = s.ID
		index++
		if index > l && index <= r {
			sc := s.Convert()
			sc.StepCount = 0
			rsp = append(rsp, sc)
		}
	}
	return rsp
}

func (svc *Service) GetAutotestScenesByIDs(sceneIDs []uint64) (map[uint64]apistructs.AutoTestScene, error) {
	// init
	mp := make(map[uint64]apistructs.AutoTestScene)
	for _, v := range sceneIDs {
		mp[v] = apistructs.AutoTestScene{}
	}
	// input
	inputs, err := svc.db.ListAutoTestSceneInputByScenes(sceneIDs)
	if err != nil {
		return nil, err
	}
	for _, v := range inputs {
		s := mp[v.SceneID]
		s.Inputs = append(s.Inputs, apistructs.AutoTestSceneInput{
			AutoTestSceneParams: apistructs.AutoTestSceneParams{
				ID:        v.ID,
				SpaceID:   v.SpaceID,
				CreatorID: v.CreatorID,
				UpdaterID: v.UpdaterID,
			},
			Name:        v.Name,
			Description: v.Description,
			Value:       v.Value,
			Temp:        v.Temp,
			SceneID:     v.SceneID,
		})
		mp[v.SceneID] = s
	}
	// step
	steps, err := svc.db.ListAutoTestSceneSteps(sceneIDs)
	if err != nil {
		return nil, err
	}
	type idType struct {
		PreID   uint64
		PreType apistructs.PreType
	}
	type stepStruct struct {
		SceneID uint64
		IdType  idType
	}

	stepMap := make(map[stepStruct]*apistructs.AutoTestSceneStep)
	for _, v := range steps {
		stepMap[stepStruct{IdType: idType{v.PreID, v.PreType}, SceneID: v.SceneID}] = v.Convert()
	}
	for _, v := range sceneIDs {
		scene := mp[v]
		// 获取串行节点列表
		for head := uint64(0); ; {
			s, ok := stepMap[stepStruct{IdType: idType{head, apistructs.PreTypeSerial}, SceneID: v}]
			if !ok {
				break
			}
			head = s.ID
			// 获取并行节点列表
			for head2 := s.ID; ; {
				s2, ok := stepMap[stepStruct{IdType: idType{head2, apistructs.PreTypeParallel}, SceneID: v}]
				if !ok {
					break
				}
				head2 = s2.ID
				s.Children = append(s.Children, *s2)
			}
			scene.Steps = append(scene.Steps, *s)
		}
		mp[v] = scene
	}
	// output
	outputs, err := svc.db.ListAutoTestSceneOutputByScenes(sceneIDs)
	if err != nil {
		return nil, err
	}
	for _, v := range outputs {
		s := mp[v.SceneID]
		s.Output = append(s.Output, apistructs.AutoTestSceneOutput{
			AutoTestSceneParams: apistructs.AutoTestSceneParams{
				ID:        v.ID,
				SpaceID:   v.SpaceID,
				CreatorID: v.CreatorID,
				UpdaterID: v.UpdaterID,
			},
			Name:        v.Name,
			Description: v.Description,
			Value:       v.Value,
			SceneID:     v.SceneID,
		})
		mp[v.SceneID] = s
	}

	mpRsp := map[uint64]apistructs.AutoTestScene{}
	for k, v := range mp {
		mpRsp[k] = v
	}
	return mpRsp, nil
}

// ListSceneBySceneSetID .
func (svc *Service) ListSceneBySceneSetID(setIDs ...uint64) (scenes []apistructs.AutoTestScene, total int64, err error) {
	list, total, err := svc.db.ListSceneBySceneSetID(setIDs...)
	if err != nil {
		return nil, 0, err
	}
	for _, v := range list {
		// not include ref set
		if v.RefSetID == 0 {
			scenes = append(scenes, v.Convert())
		}
	}
	return
}

// CountSceneByPlanIDs .
func (svc *Service) CountSceneByPlanIDs(planIDs ...uint64) (count apistructs.AutoTestSceneCount, err error) {
	return svc.db.CountSceneByPlanIDs(planIDs...)
}

// ListAutoTestSceneSteps .
func (svc *Service) ListAutoTestSceneSteps(sceneIDs []uint64) (sceneSteps []apistructs.AutoTestSceneStep, err error) {
	list, err := svc.db.ListAutoTestSceneSteps(sceneIDs)
	if err != nil {
		return nil, err
	}
	for _, v := range list {
		sceneSteps = append(sceneSteps, *v.Convert())
	}
	return
}

// CountAutoTestSceneSteps .
func (svc *Service) CountAutoTestSceneSteps(planIDs []uint64) (stepCount apistructs.AutoTestSceneStepCount, err error) {
	return svc.db.CountAutoTestSceneSteps(planIDs)
}

// ListAutotestSceneByGroupID .
func (svc *Service) ListAutotestSceneByGroupID(setID, groupID uint64) (scenes []apistructs.AutoTestScene, err error) {
	list, err := svc.db.ListAutotestSceneByGroupID(setID, groupID)
	if err != nil {
		return nil, err
	}
	for _, v := range list {
		scenes = append(scenes, v.Convert())
	}
	return
}
