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

package apiEditor

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/pkg/autotest/step"
)

// Save 保存
func (ae *ApiEditor) Save(c *cptype.Component) error {
	var data map[string]interface{}
	dataBytes, err := json.Marshal(c.State["data"])
	if err != nil {
		return err
	}
	if err := json.Unmarshal(dataBytes, &data); err != nil {
		return err
	}
	tmpAPISpecID, ok := data["apiSpecId"]
	if !ok {
		return errors.New("apiSpecID is empty")
	}
	// TODO: 类型要改
	var apiSpecID uint64
	if tmpAPISpecID == nil {
		apiSpecID = 0
	} else {
		apiSpecID, err = strconv.ParseUint(fmt.Sprintf("%v", tmpAPISpecID), 10, 64)
		if err != nil {
			return err
		}
	}
	if _, ok1 := data["apiSpec"]; ok1 {
		delete(data, "apiSpecId")
		apiInfoBytes, err := json.Marshal(data)
		if err != nil {
			return err
		}
		var apiSpec step.APISpec
		if err := json.Unmarshal(apiInfoBytes, &apiSpec); err != nil {
			return err
		}
		updateReq := apistructs.AutotestSceneRequest{
			SceneID:   ae.State.SceneId,
			Value:     string(apiInfoBytes),
			APISpecID: apiSpecID,
			Name:      apiSpec.APIInfo.Name,
		}
		updateReq.UserID = ae.sdk.Identity.UserID
		updateReq.ID = ae.State.StepId
		if _, err := ae.bdl.UpdateAutoTestSceneStep(updateReq); err != nil {
			return err
		}
	}
	return nil
}

// ExecuteApi 小试一把
func (ae *ApiEditor) ExecuteApi(ops interface{}) error {
	meta, err := GetOpsInfo(ops)
	if err != nil {
		return err
	}
	ae.State.AttemptTest.Visible = true
	req := apistructs.AutotestExecuteSceneStepRequest{
		SceneStepID:            ae.State.StepId,
		UserID:                 ae.sdk.Identity.UserID,
		ConfigManageNamespaces: meta.ConfigEnv,
	}
	req.UserID = ae.sdk.Identity.UserID

	rsp, err := ae.bdl.ExecuteDiceAutotestSceneStep(req)
	if err != nil {
		return err
	}
	if rsp.Data.Resp.Status == 200 {
		ae.State.AttemptTest.Status = "Passed"
	} else {
		ae.State.AttemptTest.Status = "Failed"
	}
	// 如果Asserts为nil传给前端页面会崩
	if rsp.Data.Asserts.Result == nil {
		rsp.Data.Asserts.Result = []*apistructs.APITestsAssertData{}
	}
	for i, v := range rsp.Data.Asserts.Result {
		rsp.Data.Asserts.Result[i].ActualValue = jsonOneLine(v.ActualValue)
	}
	ae.State.AttemptTest.Data = map[string]interface{}{
		"asserts":  rsp.Data.Asserts,
		"response": rsp.Data.Resp,
		"request":  rsp.Data.Info,
	}
	return nil
}

func (ae *ApiEditor) GenExecuteButton(ctx context.Context) (string, error) {
	projectID := int(cputil.GetInParamByKey(ctx, "projectId").(float64))
	project, err := ae.bdl.GetProject(uint64(projectID))
	if err != nil {
		return "", err
	}
	testClusterName, ok := project.ClusterConfig[string(apistructs.TestWorkspace)]
	if !ok {
		return "", fmt.Errorf("not found cluster")
	}
	var autoTestGlobalConfigListRequest apistructs.AutoTestGlobalConfigListRequest
	autoTestGlobalConfigListRequest.ScopeID = strconv.Itoa(int(project.ID))
	autoTestGlobalConfigListRequest.Scope = "project-autotest-testcase"
	autoTestGlobalConfigListRequest.UserID = ae.sdk.Identity.UserID
	configs, err := ae.bdl.ListAutoTestGlobalConfig(autoTestGlobalConfigListRequest)
	if err != nil {
		return "", err
	}
	list := []Menu{
		{
			Text: ae.sdk.I18n("empty"),
			Key:  "无",
			Operations: map[string]interface{}{
				apistructs.ClickOperation.String(): ClickOperation{
					Key:    "execute",
					Reload: true,
					Meta: Meta{
						Env:       testClusterName,
						ScenesID:  ae.State.SceneId,
						ConfigEnv: "",
					},
				},
			},
		},
	}
	for _, v := range configs {
		list = append(list, Menu{
			Text: v.DisplayName,
			Key:  v.Ns,
			Operations: map[string]interface{}{
				apistructs.ClickOperation.String(): ClickOperation{
					Key:    "execute",
					Reload: true,
					Meta: Meta{
						Env:       testClusterName,
						ScenesID:  ae.State.SceneId,
						ConfigEnv: v.Ns,
					},
				},
			},
		})
	}
	mp := map[string]interface{}{
		"menu":      list,
		"text":      cputil.I18n(ctx, "saveExec"),
		"type":      "primary",
		"disabled":  true,
		"allowSave": true,
	}
	str, err := json.Marshal(mp)
	if err != nil {
		return "", err
	}
	return string(str), nil
}

func jsonOneLine(o interface{}) string {
	defer func() {
		if r := recover(); r != nil {
			logrus.Errorf("recover from jsonOneLine: %v", r)
		}
	}()
	if o == nil {
		return ""
	}
	switch o.(type) {
	case string: // 去除引号
		return o.(string)
	case []byte: // 去除引号
		return string(o.([]byte))
	default:
		var buffer bytes.Buffer
		enc := json.NewEncoder(&buffer)
		enc.SetEscapeHTML(false)
		if err := enc.Encode(o); err != nil {
			panic(err)
		}
		return strings.TrimSuffix(buffer.String(), "\n")
	}
}
