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

package db

import (
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/dicehub/extension/pb"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type Extension struct {
	dbengine.BaseModel
	Type        string `json:"type" gorm:"type:varchar(128)"` // type addon|action
	Name        string `json:"name" grom:"type:varchar(128);unique_index"`
	Category    string `json:"category" grom:"type:varchar(128)"`
	DisplayName string `json:"displayName" grom:"type:varchar(128)"`
	LogoUrl     string `json:"logoUrl" grom:"type:varchar(128)"`
	Desc        string `json:"desc" grom:"type:text"`
	Labels      string `json:"labels" grom:"type:labels"`
	Public      bool   `json:"public" ` // Whether to display in the service market
}

func (Extension) TableName() string {
	return "dice_extension"
}

func (ext *Extension) ToApiData() *pb.Extension {
	return &pb.Extension{
		Id:          ext.ID,
		Name:        ext.Name,
		Desc:        ext.Desc,
		Type:        ext.Type,
		DisplayName: ext.DisplayName,
		Category:    ext.Category,
		LogoUrl:     ext.LogoUrl,
		Public:      ext.Public,
		CreatedAt:   timestamppb.New(ext.CreatedAt),
		UpdatedAt:   timestamppb.New(ext.UpdatedAt),
	}
}

type ExtensionVersion struct {
	dbengine.BaseModel
	ExtensionId uint64 `json:"extensionId"`
	Name        string `gorm:"type:varchar(128);index:idx_name"`
	Version     string `json:"version" gorm:"type:varchar(128);index:idx_version"`
	Spec        string `gorm:"type:text"`
	Dice        string `gorm:"type:text"`
	Swagger     string `gorm:"type:longtext"`
	Readme      string `gorm:"type:longtext"`
	Public      bool   `json:"public"`
	IsDefault   bool   `json:"is_default"`
}

func (ExtensionVersion) TableName() string {
	return "dice_extension_version"
}
