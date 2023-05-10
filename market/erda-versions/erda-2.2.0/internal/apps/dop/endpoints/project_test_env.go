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

package endpoints

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

const APITestEnv = "API_TEST_ENV"

// CreateAPITestEnv 创建API测试环境变量
func (e *Endpoints) CreateAPITestEnv(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	if r.ContentLength == 0 {
		return apierrors.ErrCreateAPITestEnv.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var req apistructs.APITestEnvCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	testEnv, err := convert2TestEnvDB(&req.APITestEnvData)
	if err != nil {
		return apierrors.ErrCreateAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	retID, err := dbclient.CreateTestEnv(testEnv)
	if err != nil {
		return apierrors.ErrCreateAPITestEnv.InternalError(err).ToResp(), nil
	}

	respData := req.APITestEnvData
	respData.ID = retID

	return httpserver.OkResp(&respData)
}

// UpdateAPITestEnv 更新API测试环境变量
func (e *Endpoints) UpdateAPITestEnv(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	envID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrUpdateAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.ErrUpdateAPITestEnv.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var req apistructs.APITestEnvUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	testEnv, err := convert2TestEnvDB(&req.APITestEnvData)
	if err != nil {
		return apierrors.ErrUpdateAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	testEnv.ID = envID
	err = dbclient.UpdateTestEnv(testEnv)
	if err != nil {
		return apierrors.ErrUpdateAPITestEnv.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(envID)
}

// GetAPITestEnv 获取指定ID的环境变量信息
func (e *Endpoints) GetAPITestEnv(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	envID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrGetAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	env, err := dbclient.GetTestEnv(envID)
	if err != nil {
		return apierrors.ErrGetAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	data, err := convert2TestEnvResp(env)
	if err != nil {
		return apierrors.ErrGetAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	return httpserver.OkResp(data)
}

// ListPAPITestEnvs 获取环境变量信息列表
func (e *Endpoints) ListAPITestEnvs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	envVarIDStr := r.URL.Query().Get("envID")
	if envVarIDStr == "" {
		return apierrors.ErrListAPITestEnvs.MissingParameter("envID").ToResp(), nil
	}

	envVarTypeStr := r.URL.Query().Get("envType")
	if envVarTypeStr == "" {
		return apierrors.ErrListAPITestEnvs.MissingParameter("envType").ToResp(), nil
	}

	testEnvList := make([]*apistructs.APITestEnvData, 0)
	envVarID, err := strconv.ParseInt(envVarIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrListAPITestEnvs.InvalidParameter(err).ToResp(), nil
	}

	envs, err := dbclient.GetTestEnvListByEnvID(envVarID, envVarTypeStr)
	if err != nil {
		return apierrors.ErrListAPITestEnvs.InternalError(err).ToResp(), nil
	}

	for _, env := range envs {
		data, err := convert2TestEnvResp(&env)
		if err != nil {
			return apierrors.ErrListAPITestEnvs.InternalError(err).ToResp(), nil
		}

		testEnvList = append(testEnvList, data)
	}

	return httpserver.OkResp(testEnvList)
}

// DeleteAPITestEnvByEnvID 根据envID删除测试环境变量
func (e *Endpoints) DeleteAPITestEnvByEnvID(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	envID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrDeleteAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	// get test env for return
	env, err := dbclient.GetTestEnv(envID)
	if err != nil {
		return apierrors.ErrGetAPITestEnv.InvalidParameter(err).ToResp(), nil
	}
	data, err := convert2TestEnvResp(env)
	if err != nil {
		return apierrors.ErrGetAPITestEnv.InvalidParameter(err).ToResp(), nil
	}

	err = dbclient.DeleteTestEnv(envID)
	if err != nil {
		return apierrors.ErrDeleteAPITestEnv.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(data)
}

func convert2TestEnvDB(req *apistructs.APITestEnvData) (*dbclient.APITestEnv, error) {
	header, err := json.Marshal(req.Header)
	if err != nil {
		return nil, err
	}

	global, err := json.Marshal(req.Global)
	if err != nil {
		return nil, err
	}

	return &dbclient.APITestEnv{
		EnvID:   req.EnvID,
		EnvType: string(req.EnvType),
		Name:    req.Name,
		Domain:  req.Domain,
		Header:  string(header),
		Global:  string(global),
	}, nil
}

func convert2TestEnvResp(env *dbclient.APITestEnv) (*apistructs.APITestEnvData, error) {
	header := make(map[string]string)
	global := make(map[string]*apistructs.APITestEnvVariable)
	err := json.Unmarshal([]byte(env.Header), &header)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal([]byte(env.Global), &global)
	if err != nil {
		return nil, err
	}

	return &apistructs.APITestEnvData{
		ID:      env.ID,
		EnvID:   env.EnvID,
		EnvType: apistructs.APITestEnvType(env.EnvType),
		Name:    env.Name,
		Domain:  env.Domain,
		Header:  header,
		Global:  global,
	}, nil
}
