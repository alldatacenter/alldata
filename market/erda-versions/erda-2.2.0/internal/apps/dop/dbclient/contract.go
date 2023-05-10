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

package dbclient

import (
	"context"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
)

func ListContracts(ctx context.Context, trans i18n.Translator, req *apistructs.ListContractsReq) (uint64, []*apistructs.ContractModelAdvance, error) {
	var (
		models []*apistructs.ContractModel
		total  uint64
	)

	DB.Where(map[string]interface{}{
		"org_id":    req.OrgID,
		"client_id": req.URIParams.ClientID,
	}).
		Where("status in (?)", req.QueryParams.Status).
		Order("updated_at DESC").
		Offset((req.QueryParams.PageNo - 1) * req.QueryParams.PageNo).Limit(req.QueryParams.PageSize).
		Find(&models).
		Offset(0).Limit(-1).
		Count(&total)

	advances := make([]*apistructs.ContractModelAdvance, len(models))
	names := make(map[uint64]string)
	for i, v := range models {
		var client apistructs.ClientModel
		DB.First(&client, map[string]interface{}{"id": v.ClientID})

		advances[i] = &apistructs.ContractModelAdvance{
			ContractModel:     *v,
			ClientName:        client.Name,
			ClientDisplayName: client.DisplayName,
			CurSLAName:        getSLAName(ctx, trans, v.CurSLAID, names),
			RequestSLAName:    getSLAName(ctx, trans, v.RequestSLAID, names),
			EndpointName:      "",
			ProjectID:         0,
			Workspace:         "",
		}
	}

	return total, advances, nil
}

func GetContract(req *apistructs.GetContractReq) (*apistructs.ContractModel, error) {
	var (
		model apistructs.ContractModel
	)

	if err := Sq().Where(map[string]interface{}{
		"org_id":    req.OrgID,
		"client_id": req.URIParams.ClientID,
		"id":        req.URIParams.ContractID,
	}).First(&model).Error; err != nil {
		return nil, err
	}

	return &model, nil
}

func ListContractRecords(req *apistructs.ListContractRecordsReq) ([]*apistructs.ContractRecordModel, error) {
	var (
		models []*apistructs.ContractRecordModel
	)

	if err := Sq().Where(map[string]interface{}{
		"org_id":      req.OrgID,
		"contract_id": req.URIParams.ContractID,
	}).Order("created_at DESC").Find(&models).Error; err != nil {
		return nil, err
	}

	return models, nil
}
