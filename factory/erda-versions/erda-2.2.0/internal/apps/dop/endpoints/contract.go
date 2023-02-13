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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

// 创建一个合约
func (e *Endpoints) CreateContract(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.CreateContract.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.CreateContract.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var body apistructs.CreateContractBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		return apierrors.CreateContract.InvalidParameter(err).ToResp(), nil
	}

	var req = apistructs.CreateContractReq{
		OrgID:     orgID,
		Identity:  &identityInfo,
		URIParams: &apistructs.CreateContractURIParams{ClientID: vars[urlPathClientID]},
		Body:      &body,
	}

	client, sk, contract, apiError := e.assetSvc.CreateContract(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(map[string]interface{}{
		"client":   client,
		"sk":       sk,
		"contract": contract,
	})
}

// 查询客户端下的合约列表
func (e *Endpoints) ListContract(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ListContracts.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.ListContracts.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var queryParams apistructs.ListContractQueryParams
	if err = e.queryStringDecoder.Decode(&queryParams, r.URL.Query()); err != nil {
		return apierrors.ListContracts.MissingParameter("invalid query parameters").ToResp(), nil
	}

	var req = apistructs.ListContractsReq{
		OrgID:       orgID,
		Identity:    &identity,
		URIParams:   &apistructs.ListContractsURIParams{ClientID: vars[urlPathClientID]},
		QueryParams: &queryParams,
	}

	data, apiError := e.assetSvc.ListContracts(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	var userIDs []string
	for _, v := range data.List {
		userIDs = append(userIDs, v.CreatorID, v.UpdaterID)
	}

	return httpserver.OkResp(data, strutil.DedupSlice(userIDs))
}

func (e *Endpoints) GetContract(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetContract.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetContract.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.GetContractReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.GetContractURIParams{
			ClientID:   vars[urlPathClientID],
			ContractID: vars[urlPathContractID],
		},
	}

	client, sk, contact, apiError := e.assetSvc.GetContract(&req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(map[string]interface{}{
		"client":   client,
		"sk":       sk,
		"contract": contact,
	})
}

// 查询合约操作记录
func (e *Endpoints) ListContractRecords(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ListContractRecords.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.ListContractRecords.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.ListContractRecordsReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.ListContractRecordsURIParams{
			ClientID:   vars[urlPathClientID],
			ContractID: vars[urlPathContractID],
		},
	}

	data, apiError := e.assetSvc.ListContractRecords(&req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	var userIDs []string
	for _, v := range data.List {
		userIDs = append(userIDs, v.CreatorID)
	}

	return httpserver.OkResp(data, strutil.DedupSlice(userIDs))
}

// 更新合约状态
func (e *Endpoints) UpdateContract(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.UpdateContract.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.UpdateContract.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.UpdateContractReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.UpdateContractURIParams{
			ClientID:   vars[urlPathClientID],
			ContractID: vars[urlPathContractID],
		},
		Body: new(apistructs.UpdateContractBody),
	}

	if err = json.NewDecoder(r.Body).Decode(req.Body); err != nil {
		return apierrors.UpdateContract.ToResp(), nil
	}

	client, contract, apiError := e.assetSvc.UpdateContract(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(map[string]interface{}{"client": client, "contract": contract})
}

func (e *Endpoints) DeleteContract(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetContract.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetContract.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.GetContractReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.GetContractURIParams{
			ClientID:   vars[urlPathClientID],
			ContractID: vars[urlPathContractID],
		},
	}

	if apiError := e.assetSvc.DeleteContract(ctx, &req); apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(nil)
}
