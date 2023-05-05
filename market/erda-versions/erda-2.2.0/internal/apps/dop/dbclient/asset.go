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

// APIAsset is dice_api_assets model
type APIAssetsModel apistructs.APIAssetsModel

func (APIAssetsModel) TableName() string {
	return "dice_api_assets"
}

func GetAPIAsset(req *apistructs.GetAPIAssetReq) (*APIAssetsModel, error) {
	var asset APIAssetsModel
	if err := Sq().Where("`org_id` = ?", req.OrgID).Where("BINARY `asset_id` = ?", req.URIParams.AssetID).
		First(&asset).Error; err != nil {
		return nil, err
	}
	return &asset, nil
}

func QueryAPILatestVersion(orgID uint64, assetID string) (*apistructs.APIAssetVersionsModel, error) {
	var model apistructs.APIAssetVersionsModel
	if err := Sq().Where(map[string]interface{}{
		"org_id":   orgID,
		"asset_id": assetID,
	}).Order("major DESC, minor DESC, patch DESC").
		First(&model).Error; err != nil {
		return nil, err
	}

	return &model, nil
}

func QueryVersionLatestSpec(orgID, versionID uint64) (*apistructs.APIAssetVersionSpecsModel, error) {
	var model apistructs.APIAssetVersionSpecsModel
	if err := Sq().Where(map[string]interface{}{
		"org_id":     orgID,
		"version_id": versionID,
	}).First(&model).Error; err != nil {
		return nil, err
	}
	return &model, nil
}

// DeleteAPIAssetByOrgAssetID deletes the APIAsset and APIAssetVersion record for giving assetID
func DeleteAPIAssetByOrgAssetID(orgID uint64, assetID string, cascade bool) error {
	tx := Tx()
	defer tx.RollbackUnlessCommitted()

	params := map[string]interface{}{
		"org_id":   orgID,
		"asset_id": assetID,
	}

	// if cascading delete, delete instance, spce text, version records
	if cascade {
		if err := tx.Delete(new(apistructs.InstantiationModel), params).Error; err != nil {
			return err
		}

		if err := tx.Delete(new(apistructs.APIAssetVersionSpecsModel), params).Error; err != nil {
			return err
		}

		if err := tx.Delete(new(apistructs.APIAssetVersionsModel), params).Error; err != nil {
			return err
		}
	}

	if err := tx.Delete(new(apistructs.APIAssetsModel), params).Error; err != nil {
		return err
	}

	tx.Commit()

	return nil
}
