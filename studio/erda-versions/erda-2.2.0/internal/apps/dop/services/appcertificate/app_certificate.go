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

// Package appcertificate 封装AppCertificate资源相关操作
package appcertificate

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/internal/apps/dop/services/certificate"
	"github.com/erda-project/erda/internal/apps/dop/utils"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms"
)

// AppCertificate 资源对象操作封装
type AppCertificate struct {
	db          *dao.DBClient
	bdl         *bundle.Bundle
	certificate *certificate.Certificate
	cms         cmspb.CmsServiceServer
}

// Option 定义 AppCertificate 对象的配置选项
type Option func(*AppCertificate)

// New 新建 AppCertificate 实例，通过 AppCertificate 实例操作企业资源
func New(options ...Option) *AppCertificate {
	p := &AppCertificate{}
	for _, op := range options {
		op(p)
	}
	return p
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(a *AppCertificate) {
		a.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(a *AppCertificate) {
		a.bdl = bdl
	}
}

// WithCertificate 配置 证书
func WithCertificate(cer *certificate.Certificate) Option {
	return func(a *AppCertificate) {
		a.certificate = cer
	}
}

func WithPipelineCms(cms cmspb.CmsServiceServer) Option {
	return func(a *AppCertificate) {
		a.cms = cms
	}
}

// Create 引用AppCertificate
func (c *AppCertificate) Create(userID string, orgID uint64, createReq *apistructs.CertificateQuoteRequest) error {
	certificate, err := c.db.GetAppCertificateByAppIDAndCertificateID(createReq.AppID, createReq.CertificateID)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return err
		}
	}
	if certificate != nil {
		return errors.Errorf("failed to create certificate(name already exists)")
	}

	pushConfig, err := json.Marshal(apistructs.PushCertificateConfigs{Enable: false})
	if err != nil {
		return errors.Errorf("failed to marshal push app certificate config")
	}

	// 获取应用信息
	appInfo, err := c.bdl.GetApp(createReq.AppID)
	if err != nil {
		return errors.Errorf("failed to get application info")
	}

	if appInfo == nil {
		return errors.Errorf("failed to get application info, nil app info")
	}

	// 获取证书信息
	cerInfo, err := c.db.GetCertificateByID(int64(createReq.CertificateID))
	if err != nil {
		return errors.Errorf("failed to get certificate info, certificateID:%d, :(%v)",
			createReq.CertificateID, err)
	}

	// 创建审批流
	// 插入 uuid
	var extra = make(map[string]string)
	extra["android"] = cerInfo.Android
	extra["ios"] = cerInfo.Ios

	approvalCreateReq := apistructs.ApproveCreateRequest{
		OrgID:      orgID,
		Title:      fmt.Sprintf("应用(%s)引用证书:%s", appInfo.Name, cerInfo.Name),
		Type:       apistructs.ApproveCeritficate,
		Priority:   "middle",
		TargetID:   createReq.AppID,
		TargetName: appInfo.Name,
		EntityID:   createReq.CertificateID,
		Extra:      extra,
		Desc:       fmt.Sprintf("application:%s quote certificate", appInfo.Name),
	}

	newApproval, err := c.bdl.CreateApprove(&approvalCreateReq, userID)
	if err != nil {
		return err
	}

	// 添加AppCertificate至DB
	certificate = &model.AppCertificate{
		AppID:         int64(createReq.AppID),
		ApprovalID:    int64(newApproval.ID),
		CertificateID: int64(createReq.CertificateID),
		Operator:      userID,
		Status:        string(apistructs.ApprovalStatusPending),
		PushConfig:    string(pushConfig),
	}
	if err = c.db.QuoteCertificate(certificate); err != nil {
		return errors.Errorf("failed to quote certificate to db, (%+v)", err)
	}

	return nil
}

// ModifyApprovalStatus 修改AppCertificate审批转态
func (c *AppCertificate) ModifyApprovalStatus(approvalID int64, status apistructs.ApprovalStatus) error {
	// 检查待更新的certificate是否存在
	certificate, err := c.db.GetAppCertificateByApprovalID(approvalID)
	if err != nil {
		return errors.Wrap(err, "not exist certificate")
	}

	certificate.Status = string(status)
	if err = c.db.UpdateQuoteCertificate(certificate); err != nil {
		logrus.Errorf("failed to update app certificate, (%v)", err)
		return errors.Errorf("failed to update app certificate")
	}

	return nil
}

// Delete 取消引用Certificate
func (c *AppCertificate) Delete(appID, certificateID int64) error {
	// 判断证书是否存在
	certificate, err := c.db.GetAppCertificateByAppIDAndCertificateID(uint64(appID), uint64(certificateID))
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return err
		}
	}

	if certificate == nil {
		return errors.Errorf("not exist quote certificate, (%v)", err)
	}

	if err := c.db.CancelQuoteCertificate(int64(certificate.ID)); err != nil {
		return errors.Errorf("failed to delete certificate, (%v)", err)
	}
	logrus.Infof("cancel quote certificate %d success", certificateID)

	// 删除环境配置的 key 信息
	if certificate.PushConfig == "" {
		return nil
	}
	var oriPushConfig apistructs.PushCertificateConfigs
	err = json.Unmarshal([]byte(certificate.PushConfig), &oriPushConfig)
	if err != nil {
		return errors.Errorf("failed to unmarshal certificate push config, (%v)", err)
	}

	var deleteCmsReq = cmspb.CmsNsConfigsDeleteRequest{
		PipelineSource: apistructs.PipelineSourceDice.String(),
		DeleteForce:    true,
	}
	for _, env := range oriPushConfig.Envs {
		switch oriPushConfig.CertificateType {
		case apistructs.IOSCertificateType:
			deleteCmsReq.DeleteKeys = []string{oriPushConfig.IOSKey.KeyChainP12File,
				oriPushConfig.IOSKey.DebugMobileProvision,
				oriPushConfig.IOSKey.ReleaseMobileProvision,
				oriPushConfig.IOSKey.KeyChainP12Password,
			}
		case apistructs.AndroidCertificateType:
			deleteCmsReq.DeleteKeys = []string{
				oriPushConfig.AndroidKey.DebugKeyStoreFile,
				oriPushConfig.AndroidKey.DebugKeyPassword,
				oriPushConfig.AndroidKey.DebugStorePassword,
				oriPushConfig.AndroidKey.DebugKeyStoreAlias,
				oriPushConfig.AndroidKey.ReleaseKeyStoreFile,
				oriPushConfig.AndroidKey.ReleaseKeyPassword,
				oriPushConfig.AndroidKey.ReleaseStorePassword,
				oriPushConfig.AndroidKey.ReleaseKeyStoreAlias,
			}
		}
		deleteCmsReq.Ns = fmt.Sprintf("app-%d-%s", certificate.AppID, strings.ToLower(string(env)))
		if _, err = c.cms.DeleteCmsNsConfigs(utils.WithInternalClientContext(context.Background()), &deleteCmsReq); err != nil {
			return err
		}
	}

	return nil
}

// ListAllAppCertificates 列出所有应用下的引用证书
func (c *AppCertificate) ListAllAppCertificates(params *apistructs.AppCertificateListRequest) (
	*apistructs.PagingAppCertificateDTO, error) {
	total, certificates, err := c.db.GetAppCertificatesByOrgIDAndName(params)
	if err != nil {
		return nil, errors.Errorf("failed to get app certificates, (%v)", err)
	}

	// 转换成所需格式
	certificateDTOs := make([]apistructs.ApplicationCertificateDTO, 0, len(certificates))
	for i := range certificates {
		// 获取企业证书信息
		cerInfo, err := c.db.GetCertificateByID(certificates[i].CertificateID)
		if err != nil {
			return nil, errors.Errorf("failed to get certificate info, certificateID:%d, :(%v)",
				certificates[i].CertificateID, err)
		}

		certificateDTOs = append(certificateDTOs, *(c.convertToAppCertificateDTO(&cerInfo, &certificates[i])))
	}

	return &apistructs.PagingAppCertificateDTO{Total: total, List: certificateDTOs}, nil
}

// PushConfigs 推送Certificate配置到配置管理
func (c *AppCertificate) PushConfigs(certificatePushReq *apistructs.PushCertificateConfigsRequest) error {
	var (
		pushReq = &cmspb.CmsNsConfigsUpdateRequest{
			PipelineSource: apistructs.PipelineSourceDice.String(),
		}
		valueMap = make(map[string]*cmspb.PipelineCmsConfigValue)
	)

	// 先删除四大环境的 key
	// 检查待更新的certificate是否存在
	appCertificateInfo, err := c.db.GetAppCertificateByAppIDAndCertificateID(certificatePushReq.AppID,
		certificatePushReq.CertificateID)
	if err != nil {
		return errors.Wrap(err, "not exist app certificate")
	}

	if appCertificateInfo == nil {
		return errors.New("not exist app certificate")
	}

	var oriPushConfig apistructs.PushCertificateConfigs
	err = json.Unmarshal([]byte(appCertificateInfo.PushConfig), &oriPushConfig)
	if err != nil {
		return errors.Errorf("failed to unmarshal certificate push config, (%v)", err)
	}

	// 删除四大环境旧的配置
	var deleteCmsReq = cmspb.CmsNsConfigsDeleteRequest{
		PipelineSource: apistructs.PipelineSourceDice.String(),
		DeleteForce:    true,
	}
	for _, env := range oriPushConfig.Envs {
		switch certificatePushReq.CertificateType {
		case apistructs.IOSCertificateType:
			deleteCmsReq.DeleteKeys = []string{oriPushConfig.IOSKey.KeyChainP12File,
				oriPushConfig.IOSKey.DebugMobileProvision,
				oriPushConfig.IOSKey.ReleaseMobileProvision,
				oriPushConfig.IOSKey.KeyChainP12Password,
			}
		case apistructs.AndroidCertificateType:
			deleteCmsReq.DeleteKeys = []string{
				oriPushConfig.AndroidKey.DebugKeyStoreFile,
				oriPushConfig.AndroidKey.DebugKeyPassword,
				oriPushConfig.AndroidKey.DebugStorePassword,
				oriPushConfig.AndroidKey.DebugKeyStoreAlias,
				oriPushConfig.AndroidKey.ReleaseKeyStoreFile,
				oriPushConfig.AndroidKey.ReleaseKeyPassword,
				oriPushConfig.AndroidKey.ReleaseStorePassword,
				oriPushConfig.AndroidKey.ReleaseKeyStoreAlias,
			}
		}
		deleteCmsReq.Ns = fmt.Sprintf("app-%d-%s", certificatePushReq.AppID, strings.ToLower(string(env)))
		if _, err := c.cms.DeleteCmsNsConfigs(utils.WithInternalClientContext(context.Background()), &deleteCmsReq); err != nil {
			return err
		}
	}

	// 创建四大环境的配置
	// 获取证书信息
	cerInfo, err := c.certificate.Get(int64(certificatePushReq.CertificateID))
	if err != nil {
		return errors.Errorf("failed to get certificate info, certificateID:%d, :(%v)",
			certificatePushReq.CertificateID, err)
	}

	fileConfigOperations := &cmspb.PipelineCmsConfigOperations{
		CanDelete:   false,
		CanDownload: true,
		CanEdit:     false,
	}

	kvConfigOperations := &cmspb.PipelineCmsConfigOperations{
		CanDelete:   false,
		CanDownload: false,
		CanEdit:     false,
	}

	switch certificatePushReq.CertificateType {
	case apistructs.IOSCertificateType:
		// KeyChainP12
		valueMap[certificatePushReq.IOSKey.KeyChainP12File] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.IOSInfo.KeyChainP12.UUID,
			EncryptInDB: false,
			Type:        cms.ConfigTypeDiceFile,
			From:        "certificate",
			Operations:  fileConfigOperations,
		}

		// KeyChainP12Password
		valueMap[certificatePushReq.IOSKey.KeyChainP12Password] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.IOSInfo.KeyChainP12.Password,
			EncryptInDB: true,
			Type:        cms.ConfigTypeKV,
			From:        "certificate",
			Operations:  kvConfigOperations,
		}

		// ReleaseMobileProvision
		valueMap[certificatePushReq.IOSKey.ReleaseMobileProvision] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.IOSInfo.ReleaseProvisionFile.UUID,
			EncryptInDB: false,
			Type:        cms.ConfigTypeDiceFile,
			From:        "certificate",
			Operations:  fileConfigOperations,
		}

		// DebugMobileProvision
		valueMap[certificatePushReq.IOSKey.DebugMobileProvision] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.IOSInfo.DebugProvisionFile.UUID,
			EncryptInDB: false,
			Type:        cms.ConfigTypeDiceFile,
			From:        "certificate",
			Operations:  fileConfigOperations,
		}
	case apistructs.AndroidCertificateType:
		// DebugKeyStoreFile
		valueMap[certificatePushReq.AndroidKey.DebugKeyStoreFile] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.DebugKeyStore.UUID,
			EncryptInDB: false,
			Type:        cms.ConfigTypeDiceFile,
			From:        "certificate",
			Operations:  fileConfigOperations,
		}

		// DebugKeyPassword
		valueMap[certificatePushReq.AndroidKey.DebugKeyPassword] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.DebugKeyStore.KeyPassword,
			EncryptInDB: true,
			Type:        cms.ConfigTypeKV,
			From:        "certificate",
			Operations:  kvConfigOperations,
		}

		// DebugStorePassword
		valueMap[certificatePushReq.AndroidKey.DebugStorePassword] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.DebugKeyStore.StorePassword,
			EncryptInDB: true,
			Type:        cms.ConfigTypeKV,
			From:        "certificate",
			Operations:  kvConfigOperations,
		}

		// DebugStoreAlias
		valueMap[certificatePushReq.AndroidKey.DebugKeyStoreAlias] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.DebugKeyStore.Alias,
			EncryptInDB: true,
			Type:        cms.ConfigTypeKV,
			From:        "certificate",
			Operations:  kvConfigOperations,
		}

		// ReleaseKeyStoreFile
		valueMap[certificatePushReq.AndroidKey.ReleaseKeyStoreFile] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.ReleaseKeyStore.UUID,
			EncryptInDB: false,
			Type:        cms.ConfigTypeDiceFile,
			From:        "certificate",
			Operations:  fileConfigOperations,
		}

		// ReleaseKeyPassword
		valueMap[certificatePushReq.AndroidKey.ReleaseKeyPassword] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.ReleaseKeyStore.KeyPassword,
			EncryptInDB: true,
			Type:        cms.ConfigTypeKV,
			From:        "certificate",
			Operations:  kvConfigOperations,
		}

		// ReleaseStorePassword
		valueMap[certificatePushReq.AndroidKey.ReleaseStorePassword] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.ReleaseKeyStore.StorePassword,
			EncryptInDB: true,
			Type:        cms.ConfigTypeKV,
			From:        "certificate",
			Operations:  kvConfigOperations,
		}

		// ReleaseStoreAlias
		valueMap[certificatePushReq.AndroidKey.ReleaseKeyStoreAlias] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.AndroidInfo.ManualInfo.ReleaseKeyStore.Alias,
			EncryptInDB: true,
			Type:        cms.ConfigTypeKV,
			From:        "certificate",
			Operations:  kvConfigOperations,
		}
	case apistructs.MessageCertificateType:
		valueMap[certificatePushReq.MessageKey.Key] = &cmspb.PipelineCmsConfigValue{
			Value:       cerInfo.MessageInfo.UUID,
			EncryptInDB: false,
			Type:        cms.ConfigTypeDiceFile,
			From:        "certificate",
			Operations:  fileConfigOperations,
		}
	}

	pushReq.KVs = valueMap

	for _, env := range certificatePushReq.Envs {
		pushReq.Ns = fmt.Sprintf("app-%d-%s", certificatePushReq.AppID, strings.ToLower(string(env)))
		if _, err := c.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(context.Background()), pushReq); err != nil {
			return err
		}
	}

	// update app certificate pushConfig
	pushConfig, err := json.Marshal(&apistructs.PushCertificateConfigs{
		Enable:          certificatePushReq.Enable,
		CertificateType: certificatePushReq.CertificateType,
		Envs:            certificatePushReq.Envs,
		IOSKey:          certificatePushReq.IOSKey,
		AndroidKey:      certificatePushReq.AndroidKey,
		MessageKey:      certificatePushReq.MessageKey,
	})
	if err != nil {
		return errors.Wrap(err, "failed to marshal app certificate config")
	}

	appCertificateInfo.PushConfig = string(pushConfig)
	if err = c.db.UpdateQuoteCertificate(appCertificateInfo); err != nil {
		logrus.Errorf("failed to update app certificate config, (%v)", err)
		return errors.Errorf("failed to update app certificate config")
	}

	return nil
}

func (c *AppCertificate) convertToAppCertificateDTO(certificate *model.Certificate, appCertificate *model.AppCertificate) *apistructs.ApplicationCertificateDTO {
	var (
		androidInfo apistructs.AndroidCertificateDTO
		iosInfo     apistructs.IOSCertificateDTO
	)
	_ = json.Unmarshal([]byte(certificate.Android), &androidInfo)
	_ = json.Unmarshal([]byte(certificate.Ios), &iosInfo)

	appCer := &apistructs.ApplicationCertificateDTO{
		ID:            uint64(appCertificate.ID),
		CertificateID: uint64(certificate.ID),
		ApprovalID:    uint64(appCertificate.ApprovalID),
		AppID:         uint64(appCertificate.AppID),
		Type:          certificate.Type,
		Name:          certificate.Name,
		AndroidInfo:   androidInfo,
		IOSInfo:       iosInfo,
		Desc:          certificate.Desc,
		Status:        appCertificate.Status,
		OrgID:         uint64(certificate.OrgID),
		Creator:       certificate.Creator,
		Operator:      appCertificate.Operator,
		CreatedAt:     appCertificate.CreatedAt,
	}

	json.Unmarshal([]byte(appCertificate.PushConfig), &appCer.PushConfig)
	return appCer
}
