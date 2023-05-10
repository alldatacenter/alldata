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

package testcase

import (
	"fmt"
	"os"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) createOrUpdateAPIs(caseID, projectID uint64, apis []*apistructs.ApiTestInfo) error {
	// handle apis order firstly
	for i, api := range apis {
		api.UsecaseOrder = int64(i + 1) // UsecaseOrder 从 1 开始，因为 0 会被 orm 忽略，导致会有两个 1，因此排序错误
	}
	for _, api := range apis {
		api := api
		api.UsecaseID = int64(caseID)
		api.ProjectID = int64(projectID)
		if api.ApiID == 0 {
			if _, err := svc.CreateAPI(apistructs.ApiTestsCreateRequest{ApiTestInfo: *api}); err != nil {
				return err
			}
		} else {
			if _, err := svc.UpdateAPI(apistructs.ApiTestsUpdateRequest{ApiTestInfo: *api, IsResult: false}); err != nil {
				return err
			}
		}
	}

	return nil
}

// ExecuteAPIs return created pipelien id
func (svc *Service) ExecuteAPIs(req apistructs.ApiTestsActionRequest) (uint64, error) {
	if req.ProjectID == 0 {
		return 0, apierrors.ErrExecuteAPITest.MissingParameter("projectID")
	}

	if req.TestPlanID == 0 {
		return 0, apierrors.ErrExecuteAPITest.MissingParameter("testPlanID")
	}

	if len(req.UsecaseIDs) == 0 {
		return 0, apierrors.ErrExecuteAPITest.MissingParameter("usecaseIDs")
	}

	apiMapList := make(map[int64][]int64)
	// 根据usecaseID列表，获取apiID列表
	for _, ucID := range req.UsecaseIDs {
		// 根据usecaseID获取apiID列表
		ats, err := dbclient.GetApiTestListByUsecaseID(int64(ucID))
		if err != nil {
			logrus.Warningf("failed to get apiID list, usecaseID:%d, (%+v)", ucID, err)
			continue
		}

		if len(ats) == 0 {
			continue
		}

		apiMapList[int64(ucID)] = []int64{}

		for _, at := range ats {
			apiMapList[int64(ucID)] = append(apiMapList[int64(ucID)], at.ID)
		}
	}

	if len(apiMapList) == 0 {
		return 0, apierrors.ErrExecuteAPITest.InternalError(fmt.Errorf("not exist api test"))
	}

	// 创建qa.yml，做api测试任务
	ymlContent, err := generatePipelineYml(apiMapList, req.ProjectTestEnvID)
	if err != nil {
		return 0, apierrors.ErrExecuteAPITest.InternalError(err)
	}

	// 执行pipeline流程
	// 根据projectID，获取集群名称
	clusterName, orgName, err := svc.getClusterNameAndOrgNameFromCmdb(req.ProjectID)
	if err != nil {
		return 0, apierrors.ErrExecuteAPITest.InternalError(err)
	}

	// insert labels
	labels := make(map[string]string)
	labels[apistructs.LabelProjectID] = strconv.FormatInt(req.ProjectID, 10)
	labels[apistructs.LabelOrgName] = orgName
	labels[apistructs.LabelDiceWorkspace] = string(apistructs.TestWorkspace)
	labels[apistructs.LabelTestPlanID] = strconv.FormatInt(req.TestPlanID, 10)

	reqPipeline := &apistructs.PipelineCreateRequestV2{
		PipelineSource:  apistructs.PipelineSourceAPITest,
		PipelineYmlName: fmt.Sprintf("api-test-%s.yml", strconv.FormatInt(req.ProjectID, 10)),
		PipelineYml:     ymlContent,
		Labels:          labels,
		ClusterName:     clusterName,
		AutoRun:         true,
		ForceRun:        true,
	}

	resp, err := svc.createPipeline(reqPipeline)
	if err != nil {
		logrus.Errorf("failed to create pipeline, resp: %+v, (%+v)", resp, err)
		return 0, apierrors.ErrExecuteAPITest.InternalError(err)
	}

	if resp.ID != 0 {
		for _, apiList := range apiMapList {
			for _, api := range apiList {
				// Get api info, if error, continue
				apiTest, err := dbclient.GetApiTest(api)
				if err != nil {
					logrus.Warningf("failed to get api test info, apiID: %d, (%+v)", api, err)
					continue
				}

				apiTest.PipelineID = int64(resp.ID)

				// Update pipelineID
				dbclient.UpdateApiTest(apiTest)
			}
		}
	}

	return resp.ID, nil
}

func convert2ReqStruct(at *dbclient.ApiTest) *apistructs.ApiTestInfo {
	return &apistructs.ApiTestInfo{
		ApiID:        at.ID,
		UsecaseID:    at.UsecaseID,
		UsecaseOrder: at.UsecaseOrder,
		ProjectID:    at.ProjectID,
		Status:       apistructs.ApiTestStatus(at.Status),
		ApiInfo:      at.ApiInfo,
		ApiRequest:   at.ApiRequest,
		ApiResponse:  at.ApiResponse,
		AssertResult: at.AssertResult,
	}
}

// API 返回对应的错误类型
const (
	PipelineYmlVersion = "1.1"
	ApiTestType        = "api-test"
	ApiTestIDs         = "api_ids"
	UsecaseID          = "usecase_id"
	PipelineStageLen   = 10
)

func generatePipelineYml(apiMapList map[int64][]int64, projectTestEnvID int64) (string, error) {
	pipelineYml := &apistructs.PipelineYml{
		Version: PipelineYmlVersion,
	}

	// pipeline插入env: PROJECT_TEST_ENV_ID
	if projectTestEnvID != 0 {
		envs := make(map[string]string)
		envs["PROJECT_TEST_ENV_ID"] = strconv.FormatInt(projectTestEnvID, 10)
		pipelineYml.Envs = envs
	}

	// 获取环境变量的api test action对应版本号
	apiVersion := os.Getenv("API_TEST_ACTION_VERSION")
	if apiVersion == "" {
		apiVersion = "1.0"
	}

	stages := make([][]*apistructs.PipelineYmlAction, 0, len(apiMapList))
	stageActions := make([]*apistructs.PipelineYmlAction, 0, PipelineStageLen)
	for caseID, apiList := range apiMapList {
		// 将 APIID 列表改成以逗号分隔的字符串
		var apiIDs string
		for i, apiID := range apiList {
			if i == 0 {
				apiIDs = strconv.FormatInt(apiID, 10)
				continue
			}
			apiIDs = fmt.Sprintf("%s,%s", apiIDs, strconv.FormatInt(apiID, 10))
		}

		params := make(map[string]interface{})
		params[ApiTestIDs] = apiIDs
		params[UsecaseID] = caseID
		action := &apistructs.PipelineYmlAction{
			Type:    ApiTestType,
			Alias:   strconv.FormatInt(caseID, 10),
			Params:  params,
			Version: apiVersion,
		}
		stageActions = append(stageActions, action)

		// 每 10 个 case 为一个 stage
		if len(stageActions) == PipelineStageLen {
			stages = append(stages, stageActions)
			stageActions = make([]*apistructs.PipelineYmlAction, 0)
		}
	}

	if len(stageActions) > 0 {
		stages = append(stages, stageActions)
	}

	pipelineYml.Stages = append(pipelineYml.Stages, stages...)

	byteContent, err := yaml.Marshal(pipelineYml)
	if err != nil {
		return "", errors.Errorf("failed to marshal pipeline yaml, pipelineYml:%+v, (%+v)", pipelineYml, err)
	}

	logrus.Debugf("[PipelineYml]: %s", string(byteContent))

	return string(byteContent), nil
}

// 根据projectID向CMDB获取集群管理名称
func (svc *Service) getClusterNameAndOrgNameFromCmdb(projectID int64) (string, string, error) {
	projectInfo, err := svc.bdl.GetProject(uint64(projectID))
	if err != nil {
		return "", "", err
	}

	// get orgName by orgID
	orgInfo, err := svc.bdl.GetOrg(projectInfo.OrgID)
	if err != nil {
		return "", "", err
	}

	// get cluster name
	if v, ok := projectInfo.ClusterConfig[string(apistructs.TestWorkspace)]; ok {
		return v, orgInfo.Name, nil
	}

	return "", "", errors.Errorf("failed to get cluster name with TEST env.")
}

// createPipeline 创建pipeline流程
func (svc *Service) createPipeline(reqPipeline *apistructs.PipelineCreateRequestV2) (*apistructs.PipelineDTO, error) {
	resp, err := svc.bdl.CreatePipeline(reqPipeline)
	if err != nil {
		return nil, err
	}
	return resp, nil
}
