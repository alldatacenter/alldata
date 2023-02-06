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
	"fmt"
	"strings"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
)

// APIAssetVersionSpec is dice_api_asset_version_specs model
type APIAssetVersionSpecsModel apistructs.APIAssetVersionSpecsModel

func (m APIAssetVersionSpecsModel) TableName() string {
	return "dice_api_asset_version_specs"
}

func CreateOrUpdateAPIAssetVersionSpec(spec *APIAssetVersionSpecsModel) error {
	if spec.VersionID == 0 {
		return fmt.Errorf("missing versionID")
	}

	exist, err := GetAPIAssetVersionSpec(&apistructs.GetAPIAssetVersionReq{
		OrgID:    spec.OrgID,
		Identity: nil,
		URIParams: &apistructs.AssetVersionDetailURI{
			AssetID:   spec.AssetID,
			VersionID: spec.VersionID,
		},
		QueryParams: &apistructs.GetAPIAssetVersionQueryParams{
			Asset: false,
			Spec:  false,
		},
	})
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return err
		}
		// not exist, do create
		spec.UpdaterID = ""
		return DB.Create(spec).Error
	}
	// do update
	spec.CreatorID = exist.CreatorID
	spec.ID = exist.ID
	return DB.Save(spec).Error
}

func GetAPIAssetVersionSpec(req *apistructs.GetAPIAssetVersionReq) (*APIAssetVersionSpecsModel, error) {
	var spec APIAssetVersionSpecsModel
	if err := Sq().Where(map[string]interface{}{
		"org_id":     req.OrgID,
		"asset_id":   req.URIParams.AssetID,
		"version_id": req.URIParams.VersionID,
	}).First(&spec).Error; err != nil {
		return nil, err
	}
	return &spec, nil
}

func QuerySpecsFromVersions(orgID uint64, assetID string, versionIDs []string, m map[uint64]*apistructs.PagingAPIAssetVersionRspObj) error {
	var models []APIAssetVersionSpecsModel
	if err := Sq().
		Where(map[string]interface{}{
			"org_id":   orgID,
			"asset_id": assetID,
		}).Where("version_id in (?)", strings.Join(versionIDs, ", ")).
		Find(&models).
		Error; err != nil {
		return err
	}
	for _, v := range models {
		if obj, ok := m[v.VersionID]; ok {
			spec := apistructs.APIAssetVersionSpecsModel(v)
			obj.Spec = &spec
		}
	}
	return nil
}
