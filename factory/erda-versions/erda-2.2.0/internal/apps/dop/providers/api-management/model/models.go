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

package model

import (
	"time"

	"github.com/erda-project/erda-infra/providers/mysql/v2/plugins/fields"
)

type Model struct {
	ID        fields.UUID      `gorm:"id"`
	CreatedAt time.Time        `gorm:"created_at"`
	UpdatedAt time.Time        `gorm:"updated_at"`
	DeletedAt fields.DeletedAt `gorm:"deleted_at"`
}

type Common struct {
	OrgID     uint32 `gorm:"org_id"`
	OrgName   string `gorm:"org_name"`
	CreatorID string `gorm:"creator_id"`
	UpdaterID string `gorm:"updater_id"`
}

type APIMExportRecord struct {
	Model
	Common

	AssetID        string `gorm:"asset_id"`
	AssetName      string `gorm:"asset_name"`
	VersionID      uint32 `gorm:"version_id"`
	SwaggerVersion string `json:"swagger_version"`
	Major          uint32 `gorm:"major"`
	Minor          uint32 `gorm:"minor"`
	Patch          uint32 `gorm:"patch"`
	SpecProtocol   string `gorm:"spec_protocol"`
}

func (APIMExportRecord) TableName() string {
	return "erda_apim_export_record"
}

type APIAssetVersion struct {
	ID             uint64 `gorm:"id"`
	OrgID          uint64 `gorm:"org_id"`
	AssetID        string `gorm:"asset_id"`
	Major          uint32 `gorm:"major"`
	Minor          uint32 `gorm:"minor"`
	Patch          uint32 `gorm:"patch"`
	Desc           string `gorm:"desc"`
	SpecProtocol   string `gorm:"specProtocol"`
	CreatorID      string `gorm:"creator_id"`
	UpdaterID      string `gorm:"updater_id"`
	CreatedAt      string `gorm:"created_at"`
	UpdatedAt      string `gorm:"updated_at"`
	SwaggerVersion string `gorm:"swagger_version"`
	AssetName      string `gorm:"asset_name"`
	Deprecated     bool   `gorm:"deprecated"`
	Source         string `gorm:"source"`
	AppID          uint32 `gorm:"app_id"`
	Branch         string `gorm:"branch"`
	ServiceName    string `gorm:"service_name"`
}

func (APIAssetVersion) TableName() string {
	return "dice_api_asset_versions"
}
