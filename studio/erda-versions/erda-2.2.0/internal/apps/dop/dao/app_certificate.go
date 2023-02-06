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
)

// QuoteCertificate 创建应用Certificate
func (client *DBClient) QuoteCertificate(certificate *model.AppCertificate) error {
	return client.Create(certificate).Error
}

// CancelQuoteCertificate 取消引用Certificate
func (client *DBClient) CancelQuoteCertificate(quoteCertificateID int64) error {
	return client.Where("id = ?", quoteCertificateID).Delete(&model.AppCertificate{}).Error
}

// UpdateQuoteCertificate 更新Certificate
func (client *DBClient) UpdateQuoteCertificate(certificate *model.AppCertificate) error {
	return client.Save(certificate).Error
}

// GetAppCertificateByID 根据certificateID获取应用Certificate信息
func (client *DBClient) GetAppCertificateByID(quoteCertificateID uint64) (model.AppCertificate, error) {
	var certificate model.AppCertificate
	if err := client.Where("id = ?", quoteCertificateID).Find(&certificate).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return certificate, ErrNotFoundCertificate
		}
		return certificate, err
	}
	return certificate, nil
}

// GetAppCertificateByAppIDAndCertificateID 根据certificateID获取应用Certificate信息
func (client *DBClient) GetAppCertificateByAppIDAndCertificateID(appID, quoteCertificateID uint64) (*model.AppCertificate, error) {
	var certificate model.AppCertificate
	if err := client.Where("app_id = ?", appID).
		Where("certificate_id = ?", quoteCertificateID).
		Find(&certificate).Error; err != nil {
		return nil, err
	}
	return &certificate, nil
}

// GetAppCertificateByApprovalID 根据certificateID获取应用Certificate信息
func (client *DBClient) GetAppCertificateByApprovalID(approvalID int64) (*model.AppCertificate, error) {
	var certificate model.AppCertificate
	if err := client.Where("approval_id = ?", approvalID).
		Find(&certificate).Error; err != nil {
		return nil, err
	}
	return &certificate, nil
}

// GetAppCertificatesByOrgIDAndName 根据appID和状态获取Certificate列表
func (client *DBClient) GetAppCertificatesByOrgIDAndName(params *apistructs.AppCertificateListRequest) (
	int, []model.AppCertificate, error) {
	var (
		certificates []model.AppCertificate
		total        int
	)
	db := client.Where("app_id = ?", params.AppID)
	if params.Status != "" {
		db = db.Where("status = ?", params.Status)
	}
	db = db.Order("updated_at DESC")
	if err := db.Offset((params.PageNo - 1) * params.PageSize).Limit(params.PageSize).
		Find(&certificates).Error; err != nil {
		return 0, nil, err
	}

	// 获取总量
	db = client.Model(&model.AppCertificate{}).Where("app_id = ?", params.AppID)
	if params.Status != "" {
		db = db.Where("status = ?", params.Status)
	}
	if err := db.Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, certificates, nil
}

// GetCountByCertificateID 根据certificateID获取引用总数
func (client *DBClient) GetCountByCertificateID(certificateID int64) (int64, error) {
	var total int64
	db := client.Model(&model.AppCertificate{}).Where("certificate_id = ?", certificateID)
	if err := db.Count(&total).Error; err != nil {
		return 0, err
	}

	return total, nil
}
