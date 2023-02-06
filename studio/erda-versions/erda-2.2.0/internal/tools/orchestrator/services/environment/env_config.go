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

package environment

import (
	"regexp"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

// EnvConfig 命名空间参数
type EnvConfig struct {
	db *dbclient.DBClient
}

// Option 定义 EnvConfig 对象的配置选项
type Option func(*EnvConfig)

// New 新建 EnvConfig 实例
func New(options ...Option) *EnvConfig {
	o := &EnvConfig{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(o *EnvConfig) {
		o.db = db
	}
}

const (
	Web             = "WEB"
	DeployEnvFormat = "^[A-Za-z_][A-Za-z0-9_]*"
	NotDeleteValue  = "N"
)

// GetDeployConfigs 根据指定 namespace 获取部署环境变量配置
func (e *EnvConfig) GetDeployConfigs(namespace string) ([]apistructs.EnvConfig, error) {
	// check namespace if exist
	ns, err := e.db.GetNamespaceByName(namespace)
	if err != nil {
		return nil, err
	}

	if ns == nil {
		return nil, errors.Errorf("not exist namespace, namespace: %s", namespace)
	}

	// TODO 操作鉴权
	//appID, err := strconv.ParseUint(ns.ApplicationID, 10, 64)
	//if err != nil {
	//	return nil, err
	//}
	//req := apistructs.PermissionCheckRequest{
	//	UserID:   userID,
	//	Scope:    apistructs.AppScope,
	//	ScopeID:  appID,
	//	Resource: apistructs.AppResource,
	//	Action:   apistructs.GetAction,
	//}
	//
	//if access, err := permission.CheckPermission(&req); err != nil || !access {
	//	return nil, apierrors.ErrGetDeployEnvConfig.AccessDenied()
	//}

	configItems, err := e.db.GetEnvConfigsByNamespaceID(ns.ID)
	if err != nil {
		return nil, err
	}

	// merge default namespace configs
	if !ns.IsDefault {
		nsRelation, err := e.db.GetNamespaceRelationByName(ns.Name)
		if err != nil {
			return nil, err
		}

		if nsRelation != nil {
			defaultNs, err := e.db.GetNamespaceByName(nsRelation.DefaultNamespace)
			if err != nil {
				return nil, err
			}

			envCongigs, err := e.db.GetEnvConfigsByNamespaceID(defaultNs.ID)
			if err != nil {
				return nil, err
			}

			configItems = append(configItems, envCongigs...)
		}
	}

	newConfigsItem, err := filterDeployEnvFormat(configItems)
	if err != nil {
		return nil, err
	}

	return decryptAndEntitys2Res(newConfigsItem, true), nil
}

func filterDeployEnvFormat(configs []dbclient.ConfigItem) ([]dbclient.ConfigItem, error) {
	var newConfigsItem []dbclient.ConfigItem
	configMap := make(map[string]struct{}, 0)
	for _, env := range configs {
		m, err := regexp.MatchString(DeployEnvFormat, env.ItemKey)
		if err != nil {
			return nil, errors.Errorf("failed to match config key, namaspace: %s, parten: %s, (%+v)",
				env.ItemKey, DeployEnvFormat, err)
		}
		if m {
			if _, ok := configMap[env.ItemKey]; !ok {
				configMap[env.ItemKey] = struct{}{}
				newConfigsItem = append(newConfigsItem, env)
			}
		}
	}

	return newConfigsItem, nil
}

func decryptAndEntitys2Res(configItems []dbclient.ConfigItem, decrypt bool) []apistructs.EnvConfig {
	envConfigs := []apistructs.EnvConfig{}
	for _, config := range configItems {
		envConfig := apistructs.EnvConfig{
			Key:        config.ItemKey,
			Comment:    config.ItemComment,
			Status:     config.Status,
			Encrypt:    config.Encrypt,
			Source:     config.Source,
			CreateTime: config.CreatedAt,
			UpdateTime: config.UpdatedAt,
			ConfigType: config.ItemType,
		}

		if decrypt || !config.Encrypt {
			envConfig.Value = config.ItemValue
		}

		envConfigs = append(envConfigs, envConfig)
	}

	return envConfigs
}
