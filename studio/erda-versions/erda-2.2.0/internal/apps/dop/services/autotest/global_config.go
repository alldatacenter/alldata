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

package autotest

import (
	"context"
	"encoding/json"
	"fmt"
	"sort"
	"time"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/utils"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

const (
	CmsCfgKeyScope           = "AUTOTEST_SCOPE"
	CmsCfgKeyScopeID         = "AUTOTEST_SCOPE_ID"
	CmsCfgKeyDisplayName     = "AUTOTEST_DISPLAY_NAME"
	CmsCfgKeyDesc            = "AUTOTEST_DESC"
	CmsCfgKeyCreatorID       = "AUTOTEST_CREATOR_ID"
	CmsCfgKeyUpdaterID       = "AUTOTEST_UPDATER_ID"
	CmsCfgKeyCreatedAt       = "AUTOTEST_CREATED_AT"
	CmsCfgKeyUpdatedAt       = "AUTOTEST_UPDATED_AT"
	CmsCfgKeyAPIGlobalConfig = "AUTOTEST_API_GLOBAL_CONFIG"
	CmsCfgKeyUIGlobalConfig  = "AUTOTEST_UI_GLOBAL_CONFIG"
)

func (svc *Service) CreateGlobalConfig(req apistructs.AutoTestGlobalConfigCreateRequest) (*apistructs.AutoTestGlobalConfig, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrCreateAutoTestGlobalConfig.InvalidParameter(err)
	}
	// req -> globalConfig
	globalConfig := apistructs.AutoTestGlobalConfig{
		Scope:       req.Scope,
		ScopeID:     req.ScopeID,
		DisplayName: req.DisplayName,
		Desc:        req.Desc,
		CreatorID:   req.IdentityInfo.UserID,
		UpdaterID:   req.IdentityInfo.UserID,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
		APIConfig:   req.APIConfig,
		UIConfig:    req.UIConfig,
	}
	if err := svc.createOrUpdatePipelineCmsGlobalConfigs(&globalConfig); err != nil {
		return nil, apierrors.ErrCreateAutoTestGlobalConfig.InternalError(err)
	}
	return &globalConfig, nil
}

func (svc *Service) UpdateGlobalConfig(req apistructs.AutoTestGlobalConfigUpdateRequest) (*apistructs.AutoTestGlobalConfig, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrUpdateAutoTestGlobalConfig.InvalidParameter(err)
	}

	// 查询
	globalConfig, err := svc.parseGlobalConfigFromCmsNs(req.PipelineCmsNs)
	if err != nil {
		return nil, apierrors.ErrUpdateAutoTestGlobalConfig.InternalError(err)
	}

	// 基础信息
	globalConfig.DisplayName = req.DisplayName
	globalConfig.Desc = req.Desc
	globalConfig.UpdaterID = req.IdentityInfo.UserID
	globalConfig.UpdatedAt = time.Now()

	// 更新 globalConfig
	if req.APIConfig != nil {
		globalConfig.APIConfig = req.APIConfig
	}
	if req.UIConfig != nil {
		globalConfig.UIConfig = req.UIConfig
	}

	// 更新
	if err := svc.createOrUpdatePipelineCmsGlobalConfigs(globalConfig); err != nil {
		return nil, apierrors.ErrCreateAutoTestGlobalConfig.InternalError(err)
	}

	return globalConfig, nil
}

func (svc *Service) parseGlobalConfigFromCmsNs(ns string) (*apistructs.AutoTestGlobalConfig, error) {
	// 查询
	configs, err := svc.cms.GetCmsNsConfigs(utils.WithInternalClientContext(context.Background()),
		&cmspb.CmsNsConfigsGetRequest{
			Ns:             ns,
			PipelineSource: apistructs.PipelineSourceAutoTest.String(),
			Keys:           nil,
			GlobalDecrypt:  true,
		})
	if err != nil {
		return nil, err
	}

	result, err := svc.parseCmsConfigsToAutoTestGlobalConfig(configs.Data, &apistructs.AutoTestGlobalConfig{Ns: ns})
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (svc *Service) parseCmsConfigsToAutoTestGlobalConfig(configs []*cmspb.PipelineCmsConfig, result *apistructs.AutoTestGlobalConfig) (*apistructs.AutoTestGlobalConfig, error) {
	for _, cfg := range configs {
		switch cfg.Key {
		case CmsCfgKeyScope:
			result.Scope = cfg.Value
		case CmsCfgKeyScopeID:
			result.ScopeID = cfg.Value
		case CmsCfgKeyDisplayName:
			result.DisplayName = cfg.Value
		case CmsCfgKeyDesc:
			result.Desc = cfg.Value
		case CmsCfgKeyCreatorID:
			result.CreatorID = cfg.Value
		case CmsCfgKeyUpdaterID:
			result.CreatorID = cfg.Value
		case CmsCfgKeyCreatedAt:
			var createdAt time.Time
			if err := json.Unmarshal([]byte(cfg.Value), &createdAt); err == nil {
				result.CreatedAt = createdAt
			}
		case CmsCfgKeyUpdatedAt:
			var updatedAt time.Time
			if err := json.Unmarshal([]byte(cfg.Value), &updatedAt); err == nil {
				result.UpdatedAt = updatedAt
			}
		case CmsCfgKeyAPIGlobalConfig:
			var apiConfig apistructs.AutoTestAPIConfig
			if err := json.Unmarshal([]byte(cfg.Value), &apiConfig); err != nil {
				return nil, fmt.Errorf("failed to unmarshal apiConfig, err: %v", err)
			}
			result.APIConfig = &apiConfig
		case CmsCfgKeyUIGlobalConfig:
			var uiConfig apistructs.AutoTestUIConfig
			if err := json.Unmarshal([]byte(cfg.Value), &uiConfig); err != nil {
				return nil, fmt.Errorf("failed to unmarshal uiConfig, err: %v", err)
			}
			result.UIConfig = &uiConfig
		}
	}

	if result.Scope == "" {
		return nil, fmt.Errorf("invalid scope")
	}
	if result.ScopeID == "" {
		return nil, fmt.Errorf("invalid scopeID")
	}
	return result, nil
}

// createOrUpdatePipelineCmsGlobalConfigs
func (svc *Service) createOrUpdatePipelineCmsGlobalConfigs(cfg *apistructs.AutoTestGlobalConfig) error {
	// ns 不存在则默认赋值，相当于创建；已存在的话则为更新
	if cfg.Ns == "" {
		cfg.Ns = generateGlobalConfigPipelineCmsNs(cfg.Scope, cfg.ScopeID)
	}
	if cfg.Scope == "" {
		return fmt.Errorf("invalid scope")
	}
	if cfg.ScopeID == "" {
		return fmt.Errorf("invalid scopeID")
	}
	if cfg.DisplayName == "" {
		return fmt.Errorf("invalid displayName")
	}

	kvs := make(map[string]*cmspb.PipelineCmsConfigValue)
	// 默认注入 scope & scopeID
	kvs[CmsCfgKeyScope] = &cmspb.PipelineCmsConfigValue{
		Value:       cfg.Scope,
		EncryptInDB: false,
	}
	kvs[CmsCfgKeyScopeID] = &cmspb.PipelineCmsConfigValue{
		Value:       cfg.ScopeID,
		EncryptInDB: false,
	}
	if cfg.DisplayName != "" {
		kvs[CmsCfgKeyDisplayName] = &cmspb.PipelineCmsConfigValue{
			Value:       cfg.DisplayName,
			EncryptInDB: false,
		}
	}
	// desc 可以更新为空
	kvs[CmsCfgKeyDesc] = &cmspb.PipelineCmsConfigValue{
		Value:       cfg.Desc,
		EncryptInDB: false,
	}
	if cfg.CreatorID != "" {
		kvs[CmsCfgKeyCreatorID] = &cmspb.PipelineCmsConfigValue{
			Value:       cfg.CreatorID,
			EncryptInDB: false,
		}
	}
	if cfg.UpdaterID != "" {
		kvs[CmsCfgKeyUpdaterID] = &cmspb.PipelineCmsConfigValue{
			Value:       cfg.UpdaterID,
			EncryptInDB: false,
		}
	}
	if !cfg.CreatedAt.IsZero() {
		createdAtJSON, _ := cfg.CreatedAt.MarshalJSON()
		kvs[CmsCfgKeyCreatedAt] = &cmspb.PipelineCmsConfigValue{
			Value:       string(createdAtJSON),
			EncryptInDB: false,
		}
	}
	if !cfg.UpdatedAt.IsZero() {
		updatedAtJSON, _ := cfg.UpdatedAt.MarshalJSON()
		kvs[CmsCfgKeyUpdatedAt] = &cmspb.PipelineCmsConfigValue{
			Value:       string(updatedAtJSON),
			EncryptInDB: false,
		}
	}
	if cfg.APIConfig != nil {
		// polish global
		// 插入时保证 item.name = name，取出时无需再重新赋值保证
		for name, value := range cfg.APIConfig.Global {
			ensure := value
			ensure.Name = name
			cfg.APIConfig.Global[name] = ensure
		}
		b, err := json.Marshal(cfg.APIConfig)
		if err != nil {
			return fmt.Errorf("invalid apiConfig, err: %v", err)
		}
		kvs[CmsCfgKeyAPIGlobalConfig] = &cmspb.PipelineCmsConfigValue{
			Value:       string(b),
			EncryptInDB: false,
			Type:        cms.ConfigTypeKV,
			Operations:  &cms.DefaultOperationsForKV,
			Comment:     "auto test api global config",
			From:        apistructs.PipelineSourceAutoTest.String(),
		}

		// 转成 config.autotest.xx 语法由 pipeline 渲染
		for _, item := range cfg.APIConfig.Global {
			kvs[apistructs.PipelineSourceAutoTest.String()+"."+item.Name] = &cmspb.PipelineCmsConfigValue{
				Value:       item.Value,
				EncryptInDB: false,
				Type:        cms.ConfigTypeKV,
				Operations:  &cms.DefaultOperationsForKV,
				Comment:     "auto test api global config",
				From:        apistructs.PipelineSourceAutoTest.String(),
			}
		}
	}
	if cfg.UIConfig != nil {
		b, err := json.Marshal(cfg.UIConfig)
		if err != nil {
			return fmt.Errorf("invalid uiConfig, err: %v", err)
		}
		kvs[CmsCfgKeyUIGlobalConfig] = &cmspb.PipelineCmsConfigValue{
			Value:       string(b),
			EncryptInDB: false,
			Type:        cms.ConfigTypeKV,
			Operations:  &cms.DefaultOperationsForKV,
			Comment:     "auto test ui global config",
			From:        apistructs.PipelineSourceAutoTest.String(),
		}
	}
	if _, err := svc.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(context.Background()), &cmspb.CmsNsConfigsUpdateRequest{
		Ns:             cfg.Ns,
		PipelineSource: apistructs.PipelineSourceAutoTest.String(),
		KVs:            kvs,
	}); err != nil {
		return err
	}
	return nil
}

func generateGlobalConfigPipelineCmsNsPrefix(scope, scopeID string) string {
	return fmt.Sprintf("autotest^scope-%s^scopeid-%s^", scope, scopeID)
}

func generateGlobalConfigPipelineCmsNs(scope, scopeID string) string {
	return generateGlobalConfigPipelineCmsNsPrefix(scope, scopeID) + uuid.SnowFlakeID()
}

func (svc *Service) DeleteGlobalConfig(req apistructs.AutoTestGlobalConfigDeleteRequest) (*apistructs.AutoTestGlobalConfig, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrDeleteAutoTestGlobalConfig.InvalidParameter(err)
	}

	// 查询
	globalConfig, err := svc.parseGlobalConfigFromCmsNs(req.PipelineCmsNs)
	if err != nil {
		return nil, apierrors.ErrDeleteAutoTestGlobalConfig.InternalError(err)
	}

	// 删除
	if _, err := svc.cms.DeleteCmsNsConfigs(utils.WithInternalClientContext(context.Background()), &cmspb.CmsNsConfigsDeleteRequest{
		Ns:             req.PipelineCmsNs,
		PipelineSource: apistructs.PipelineSourceAutoTest.String(),
		DeleteNs:       true,
		DeleteForce:    true,
		DeleteKeys:     nil,
	}); err != nil {
		return nil, apierrors.ErrDeleteAutoTestGlobalConfig.InternalError(err)
	}

	return globalConfig, nil
}

func (svc *Service) ListGlobalConfigs(req apistructs.AutoTestGlobalConfigListRequest) ([]apistructs.AutoTestGlobalConfig, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrListAutoTestGlobalConfigs.InvalidParameter(err)
	}

	// 获取 cms ns 列表
	nsPrefix := generateGlobalConfigPipelineCmsNsPrefix(req.Scope, req.ScopeID)
	namespaces, err := svc.cms.ListCmsNs(utils.WithInternalClientContext(context.Background()), &cmspb.CmsListNsRequest{
		PipelineSource: apistructs.PipelineSourceAutoTest.String(),
		NsPrefix:       nsPrefix,
	})
	if err != nil {
		return nil, apierrors.ErrListAutoTestGlobalConfigs.InternalError(err)
	}

	var batchGetRequest = cmspb.CmsNsConfigsBatchGetRequest{}
	for _, ns := range namespaces.Data {
		batchGetRequest.Namespaces = append(batchGetRequest.Namespaces, ns.Ns)
	}
	batchGetRequest.PipelineSource = apistructs.PipelineSourceAutoTest.String()
	batchGetRequest.GlobalDecrypt = true

	resp, err := svc.cms.BatchGetCmsNsConfigs(utils.WithInternalClientContext(context.Background()), &batchGetRequest)
	if err != nil {
		return nil, err
	}

	var nsCfgMap = map[string][]*cmspb.PipelineCmsConfig{}
	for _, cfg := range resp.Configs {
		config := cfg
		if config.Ns == nil {
			continue
		}
		nsCfgMap[config.Ns.Ns] = append(nsCfgMap[config.Ns.Ns], config)
	}

	var sortResult apistructs.SortByUpdateTimeAutoTestGlobalConfigs
	for ns, configs := range nsCfgMap {
		result, err := svc.parseCmsConfigsToAutoTestGlobalConfig(configs, &apistructs.AutoTestGlobalConfig{Ns: ns})
		if err != nil {
			return nil, err
		}
		sortResult = append(sortResult, *result)
	}

	// sort by update time
	sort.Sort(sortResult)

	return sortResult, nil
}
