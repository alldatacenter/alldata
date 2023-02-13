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

package addon

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/mysqlhelper"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	CREATE_DB        = "CREATE DATABASE IF NOT EXISTS `%s`;"
	CREATE_USER      = "CREATE USER '%s'@'%%' IDENTIFIED by '%s';"
	GRANT_USER_TO_DB = "GRANT ALL ON %s.* TO '%s'@'%%';"
	FLUSH            = "flush privileges;"
)

// CreateAddonTenant
func (a *Addon) CreateAddonTenant(name string, addonInstanceRoutingID string, config map[string]string) (string, error) {
	addoninsRouting, err := a.db.GetInstanceRouting(addonInstanceRoutingID)
	if err != nil {
		return "", err
	}
	if addoninsRouting == nil {
		return "", fmt.Errorf("未找到 addon(%s)", addonInstanceRoutingID)
	}
	if addoninsRouting.Status != string(apistructs.AddonAttached) {
		return "", fmt.Errorf("addon(%s) not ready, cannot create tenant", addoninsRouting.ID)
	}

	addonins, err := a.db.GetAddonInstance(addoninsRouting.RealInstance)
	if err != nil {
		return "", err
	}
	projectid, err := strconv.ParseUint(addoninsRouting.ProjectID, 10, 64)
	if err != nil {
		return "", fmt.Errorf("failed to parse project id(%s), %v", addoninsRouting.ProjectID, err)
	}
	existTenants, err := a.db.ListAddonInstanceTenantByProjectIDs([]uint64{projectid})
	if err != nil {
		return "", err
	}
	for _, e := range existTenants {
		if e.Name == name {
			return "", fmt.Errorf("已存在同名addon租户")
		}
	}

	switch addoninsRouting.AddonName {
	case "mysql":
		return a.CreateMysqlTenant(name, addoninsRouting, addonins, config)
	default:
		return "", fmt.Errorf("addon(%s) not support creating tenant yet", addoninsRouting.AddonName)
	}
}

func (a *Addon) CreateMysqlTenant(name string, addoninsRouting *dbclient.AddonInstanceRouting, addonins *dbclient.AddonInstance, config map[string]string) (string, error) {
	username := name + "-" + uuid.UUID()[:12]

	dbs_s := config["database"]
	dbs := strutil.Split(dbs_s, ",", true)
	if len(dbs) == 0 {
		dbs = []string{username}
	}

	kmskey, err := a.bdl.KMSCreateKey(apistructs.KMSCreateKeyRequest{
		CreateKeyRequest: kmstypes.CreateKeyRequest{
			PluginKind: kmstypes.PluginKind_DICE_KMS,
		},
	})
	if err != nil {
		return "", err
	}

	passwd := uuid.UUID()
	r, err := a.bdl.KMSEncrypt(apistructs.KMSEncryptRequest{
		EncryptRequest: kmstypes.EncryptRequest{
			KeyID:           kmskey.KeyMetadata.KeyID,
			PlaintextBase64: base64.StdEncoding.EncodeToString([]byte(passwd)),
		},
	})
	if err != nil {
		return "", err
	}
	passwd_encrypted := r.CiphertextBase64

	// 创建 user 及 db
	sqls := []string{}
	for _, db := range dbs {
		sqls = append(sqls, fmt.Sprintf(CREATE_DB, db))
	}
	sqls = append(sqls, fmt.Sprintf(CREATE_USER, username, passwd))
	for _, db := range dbs {
		sqls = append(sqls, fmt.Sprintf(GRANT_USER_TO_DB, db, username))
	}
	sqls = append(sqls, FLUSH)

	addonconfig, err := a.getAddonConfig(addonins.ID, nil)
	if err != nil {
		return "", err
	}
	host, ok1 := addonconfig.Config["MYSQL_HOST"]
	rootpasswd, ok2 := addonconfig.Config["MYSQL_PASSWORD"]
	port, ok3 := addonconfig.Config["MYSQL_PORT"]
	rootname, ok4 := addonconfig.Config["MYSQL_USERNAME"]

	if !ok1 || !ok2 || !ok3 || !ok4 {
		return "", fmt.Errorf("addon 配置中 MYSQL_HOST | MYSQL_PASSWORD | MYSQL_PORT | MYSQL_USERNAME 不存在: %+v", addonconfig.Config)
	}

	var execSqlDto mysqlhelper.Request
	execSqlDto.Url = strutil.Join([]string{apistructs.AddonMysqlJdbcPrefix, host.(string), ":", port.(string)}, "")
	execSqlDto.User = rootname.(string)
	execSqlDto.Password = rootpasswd.(string)
	execSqlDto.Sqls = sqls
	execSqlDto.ClusterKey = addonins.Cluster
	if err := execSqlDto.Exec(); err != nil {
		return "", err
	}

	// 创建 tenant 记录
	tenantConfig := map[string]string{
		"MYSQL_HOST":        host.(string),
		"MYSQL_PASSSWORD":   passwd_encrypted,
		"MYSQL_PORT":        port.(string),
		"MYSQL_USERNAME":    username,
		"MYSQL_DATABASE":    strutil.Join(dbs, ",", true),
		"ADDON_HAS_ENCRIPY": "YES",
	}
	tenantConfig_s, err := json.Marshal(tenantConfig)
	if err != nil {
		return "", err
	}
	id := a.getRandomId()
	if err := a.db.CreateAddonInstanceTenant(&dbclient.AddonInstanceTenant{
		ID:                     id,
		Name:                   name,
		AddonInstanceID:        addonins.ID,
		AddonInstanceRoutingID: addoninsRouting.ID,
		Config:                 string(tenantConfig_s),
		OrgID:                  addonins.OrgID,
		ProjectID:              addonins.ProjectID,
		AppID:                  addonins.ApplicationID,
		Workspace:              addonins.Workspace,
		Deleted:                "N",
		KmsKey:                 kmskey.KeyMetadata.KeyID,
		CreatedAt:              time.Now(),
		UpdatedAt:              time.Now(),
	}); err != nil {
		return "", err
	}
	return id, nil
}
