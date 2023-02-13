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

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func ListMyClients(req *apistructs.ListMyClientsReq, orgManager bool) (total uint64, models []*apistructs.ClientModel, err error) {
	var (
		keyword = strutil.Concat("%", req.QueryParams.Keyword, "%")
	)

	sq := DB.Where("org_id = ?", req.OrgID).
		Where("? = true OR ? = creator_id", orgManager, req.Identity.UserID)
	if req.QueryParams.Keyword != "" {
		sq = sq.Where("name LIKE ? OR client_id LIKE ?", keyword, keyword)
	}
	sq = sq.Order("updated_at DESC")

	if req.QueryParams.Paging {
		if err := sq.Limit(req.QueryParams.PageSize).Offset((req.QueryParams.PageNo - 1) * req.QueryParams.PageSize).Find(&models).
			Limit(-1).Offset(0).Count(&total).
			Error; err != nil {
			return 0, nil, err
		}
	} else {
		if err := sq.Find(&models).Count(&total).Error; err != nil {
			return 0, nil, err
		}
	}

	return total, models, nil
}

// GetMyClient select a Client details
func GetMyClient(req *apistructs.GetClientReq, orgManager bool) (*apistructs.ClientModel, error) {
	var (
		model apistructs.ClientModel
	)
	if err := DB.Where("org_id = ?", req.OrgID).
		Where("? in (client_id, id)", req.URIParams.ClientID).
		Where("? = creator_id OR ? = true", req.Identity.UserID, orgManager).
		First(&model).
		Error; err != nil {
		return nil, err
	}
	return &model, nil
}

func ListSwaggerVersionClients(ctx context.Context, trans i18n.Translator, req *apistructs.ListSwaggerVersionClientsReq) ([]*apistructs.ListSwaggerVersionClientOjb, error) {
	var (
		list      []*apistructs.ListSwaggerVersionClientOjb
		contracts []*apistructs.ContractModel
	)
	if err := DB.Find(&contracts, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
	}).
		Error; err != nil {
		return nil, errors.Wrapf(err, "failed to Find contracts, asset_id: %s, swagger_version: %s", req.URIParams.AssetID, req.URIParams.SwaggerVersion)
	}

	var slaNames = make(map[uint64]string)
	for _, contract := range contracts {
		var (
			client apistructs.ClientModel
		)
		if err := DB.First(&client, map[string]interface{}{
			"org_id": req.OrgID,
			"id":     contract.ClientID,
		}).Error; err != nil {
			continue
		}

		obj := apistructs.ListSwaggerVersionClientOjb{
			Client: &client,
			Contract: &apistructs.ContractModelAdvance{
				ContractModel:     *contract,
				ClientName:        client.Name,
				ClientDisplayName: client.DisplayName,
				CurSLAName:        getSLAName(ctx, trans, contract.CurSLAID, slaNames),
				RequestSLAName:    getSLAName(ctx, trans, contract.RequestSLAID, slaNames),
			},
			Permission: map[string]bool{"edit": false},
		}
		list = append(list, &obj)
	}

	return list, nil
}

// if salID is nil, returns "";
// if slaID is in names, return the value;
// otherwise, select out the slaName from database and records it in names.
func getSLAName(ctx context.Context, trans i18n.Translator, slaID *uint64, names map[uint64]string) string {
	codes := httpserver.UnwrapI18nCodes(ctx)

	if slaID == nil {
		return ""
	}

	if *slaID == 0 {
		return trans.Text(codes, "UnlimitedSLAName")
	}

	name, ok := names[*slaID]
	if ok {
		return name
	}

	var sla apistructs.SLAModel
	if err := DB.First(&sla, map[string]interface{}{"id": *slaID}).
		Error; err != nil {
		return ""
	}

	names[*slaID] = sla.Name

	return sla.Name
}
