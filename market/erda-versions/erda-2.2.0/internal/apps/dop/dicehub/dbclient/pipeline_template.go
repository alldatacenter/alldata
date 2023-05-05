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
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type DicePipelineTemplate struct {
	dbengine.BaseModel
	Name           string `json:"name" gorm:"type:varchar(255)"`
	LogoUrl        string `json:"logoUrl" gorm:"type:varchar(255)"`
	Desc           string `json:"desc" gorm:"type:varchar(255)"`
	ScopeType      string `json:"scope_type" gorm:"type:varchar(255)"`
	ScopeId        string `json:"scope_id" gorm:"type:bigint(20)"`
	DefaultVersion string `json:"default_version" gorm:"type:varchar(255)"`
}

func (ext *DicePipelineTemplate) ToApiData() *apistructs.PipelineTemplate {
	return &apistructs.PipelineTemplate{
		ID:        ext.ID,
		Name:      ext.Name,
		Desc:      ext.Desc,
		LogoUrl:   ext.LogoUrl,
		CreatedAt: ext.CreatedAt,
		UpdatedAt: ext.UpdatedAt,
		ScopeID:   ext.ScopeId,
		ScopeType: ext.ScopeType,
		Version:   ext.DefaultVersion,
	}
}

type DicePipelineTemplateVersion struct {
	dbengine.BaseModel
	TemplateId uint64 `json:"template_id"`
	Name       string `json:"name" gorm:"type:varchar(255);"`
	Version    string `json:"version" gorm:"type:varchar(128);"`
	Spec       string `json:"spec" gorm:"type:text"`
	Readme     string `json:"readme" gorm:"type:longtext"`
}

func (ext *DicePipelineTemplateVersion) ToApiData() *apistructs.PipelineTemplateVersion {
	return &apistructs.PipelineTemplateVersion{
		ID:         ext.ID,
		Name:       ext.Name,
		TemplateId: ext.TemplateId,
		Version:    ext.Version,
		Spec:       ext.Spec,
		Readme:     ext.Readme,
		CreatedAt:  ext.CreatedAt,
		UpdatedAt:  ext.UpdatedAt,
	}
}

func (client *DBClient) CreatePipelineTemplate(template *DicePipelineTemplate) error {
	err := client.Create(template).Error
	return err
}

func (client *DBClient) UpdatePipelineTemplate(template *DicePipelineTemplate) error {
	err := client.Model(template).Save(template).Error
	return err
}

func (client *DBClient) GetPipelineTemplate(name string, scopeType string, scopeId string) (*DicePipelineTemplate, error) {
	var result DicePipelineTemplate
	err := client.Model(&DicePipelineTemplate{}).Where("name = ? and scope_type = ? and scope_id = ?", name, scopeType, scopeId).Find(&result).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &result, err
}

func (client *DBClient) GetPipelineTemplateVersion(version string, templateId uint64) (*DicePipelineTemplateVersion, error) {
	var result DicePipelineTemplateVersion
	err := client.Model(&DicePipelineTemplate{}).Where(" template_id = ? and version = ? ", templateId, version).Find(&result).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return &result, err
}

func (client *DBClient) CreatePipelineTemplateVersion(version *DicePipelineTemplateVersion) error {
	err := client.Create(version).Error
	return err
}

func (client *DBClient) UpdatePipelineTemplateVersion(version *DicePipelineTemplateVersion) error {
	err := client.Model(version).Save(version).Error
	return err
}

func (client *DBClient) QueryByPipelineTemplates(template *DicePipelineTemplate, pageSize int, pageNo int) ([]DicePipelineTemplate, int, error) {

	var result []DicePipelineTemplate
	var total int

	query := client.Model(&DicePipelineTemplate{})
	if template != nil {
		if template.Name != "" {
			query = query.Where(" name like ? ", "%"+template.Name+"%")
		}
		if template.ScopeType != "" {
			query = query.Where(" scope_type = ? ", template.ScopeType)
		}
		if template.ScopeId != "" {
			query = query.Where(" scope_id = ? ", template.ScopeId)
		}
	}

	if pageNo > 0 && pageSize > 0 {

		if query.Count(&total).Error == gorm.ErrRecordNotFound {
			return nil, total, nil
		}

		err := query.Offset((pageNo - 1) * pageSize).Limit(pageSize).Find(&result).Error

		if err == gorm.ErrRecordNotFound {
			return nil, 0, nil
		}

		return result, total, nil
	} else {

		err := query.Find(&result).Error
		if err == gorm.ErrRecordNotFound {
			return nil, 0, nil
		}

		return result, 0, err
	}
}

func (client *DBClient) QueryPipelineTemplateVersions(version *DicePipelineTemplateVersion) ([]DicePipelineTemplateVersion, error) {
	query := client.Model(&DicePipelineTemplateVersion{})
	if version != nil {
		if version.TemplateId > 0 {
			query = query.Where(" template_id = ? ", version.TemplateId)
		}

		if version.Name != "" {
			query = query.Where(" name = ? ", version.Name)
		}
	}

	var result []DicePipelineTemplateVersion
	err := query.Find(&result).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	return result, err
}
