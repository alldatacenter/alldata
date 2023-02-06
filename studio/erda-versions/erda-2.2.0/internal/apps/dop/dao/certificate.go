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

package dao

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateCertificate 创建Certificate
func (client *DBClient) CreateCertificate(certificate *model.Certificate) error {
	return client.Create(certificate).Error
}

// UpdateCertificate 更新Certificate
func (client *DBClient) UpdateCertificate(certificate *model.Certificate) error {
	return client.Save(certificate).Error
}

// DeleteCertificate 删除Certificate
func (client *DBClient) DeleteCertificate(certificateID int64) error {
	return client.Where("id = ?", certificateID).Delete(&model.Certificate{}).Error
}

// GetCertificateByID 根据certificateID获取Certificate信息
func (client *DBClient) GetCertificateByID(certificateID int64) (model.Certificate, error) {
	var certificate model.Certificate
	if err := client.Where("id = ?", certificateID).Find(&certificate).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return certificate, ErrNotFoundCertificate
		}
		return certificate, err
	}
	return certificate, nil
}

// GetCertificatesByOrgIDAndName 根据orgID与名称获取Certificate列表
func (client *DBClient) GetCertificatesByOrgIDAndName(orgID int64, params *apistructs.CertificateListRequest) (
	int, []model.Certificate, error) {
	var (
		certificates []model.Certificate
		total        int
	)
	db := client.Where("org_id = ?", orgID)
	if params.Name != "" {
		db = db.Where("name = ?", params.Name)
	}
	if params.Type != "" {
		db = db.Where("type = ?", params.Type)
	}
	if params.Query != "" {
		db = db.Where("name LIKE ?", strutil.Concat("%", params.Query, "%"))
	}
	db = db.Order("updated_at DESC")
	if err := db.Offset((params.PageNo - 1) * params.PageSize).Limit(params.PageSize).
		Find(&certificates).Error; err != nil {
		return 0, nil, err
	}

	// 获取总量
	db = client.Model(&model.Certificate{}).Where("org_id = ?", orgID)
	if params.Name != "" {
		db = db.Where("name = ?", params.Name)
	}
	if params.Type != "" {
		db = db.Where("type = ?", params.Type)
	}
	if params.Query != "" {
		db = db.Where("name LIKE ?", strutil.Concat("%", params.Query, "%"))
	}
	if err := db.Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, certificates, nil
}

// GetCertificatesByIDs 根据certificateIDs获取Certificate列表
func (client *DBClient) GetCertificatesByIDs(certificateIDs []int64, params *apistructs.CertificateListRequest) (
	int, []model.Certificate, error) {
	var (
		total        int
		certificates []model.Certificate
	)
	db := client.Where("id in (?)", certificateIDs)
	if params.Name != "" {
		db = db.Where("name = ?", params.Name)
	}
	if params.Query != "" {
		db = db.Where("name LIKE ?", strutil.Concat("%", params.Query, "%"))
	}
	db = db.Order("updated_at DESC")
	if err := db.Offset((params.PageNo - 1) * params.PageSize).Limit(params.PageSize).
		Find(&certificates).Error; err != nil {
		return 0, nil, err
	}

	// 获取总量
	db = client.Model(&model.Certificate{}).Where("id in (?)", certificateIDs)
	if params.Name != "" {
		db = db.Where("name = ?", params.Name)
	}
	if params.Query != "" {
		db = db.Where("name LIKE ?", strutil.Concat("%", params.Query, "%"))
	}
	if err := db.Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, certificates, nil
}

// GetCertificateByOrgAndName 根据orgID & Certificate名称 获取证书信息
func (client *DBClient) GetCertificateByOrgAndName(orgID int64, name string) (*model.Certificate, error) {
	var certificate model.Certificate
	if err := client.Where("org_id = ?", orgID).
		Where("name = ?", name).Find(&certificate).Error; err != nil {
		return nil, err
	}
	return &certificate, nil
}
