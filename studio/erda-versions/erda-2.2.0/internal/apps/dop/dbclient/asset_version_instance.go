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
	"github.com/erda-project/erda/apistructs"
)

func FirstOrCreateInstantiation(model *apistructs.InstantiationModel, params map[string]interface{}) error {
	return DB.FirstOrCreate(model, params).Error
}

func OneInstantiation(model *apistructs.InstantiationModel, params map[string]interface{}) error {
	return DB.First(model, params).Error
}

func UpdateInstantiation(req *apistructs.UpdateInstantiationReq) error {
	where := map[string]interface{}{
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
		"id":              req.URIParams.InstantiationID,
		"org_id":          req.OrgID,
	}
	updates := map[string]interface{}{
		"type":         req.Body.Type,
		"url":          req.Body.URL,
		"project_id":   req.Body.ProjectID,
		"app_id":       req.Body.AppID,
		"runtime_id":   req.Body.RuntimeID,
		"service_name": req.Body.ServiceName,
		"workspace":    req.Body.Workspace,
	}

	return Sq().Model(new(apistructs.InstantiationModel)).
		Where(where).
		Updates(updates).
		Error
}
