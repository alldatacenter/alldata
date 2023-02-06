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
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

func (a *Addon) toOverrideConfigFromMySQLAccount(config map[string]interface{}, mySQLAccountID string) error {
	account, err := a.db.GetMySQLAccountByID(mySQLAccountID)
	if err != nil {
		return err
	}
	return a._toOverrideConfigFromMySQLAccount(config, account)
}

func (a *Addon) _toOverrideConfigFromMySQLAccount(config map[string]interface{}, account *dbclient.MySQLAccount) error {
	password := account.Password
	if account.KMSKey != "" {
		dr, err := a.kms.Decrypt(account.Password, account.KMSKey)
		if err != nil {
			return err
		}
		rawSecret, err := base64.StdEncoding.DecodeString(dr.PlaintextBase64)
		if err != nil {
			return err
		}
		password = string(rawSecret)
	} else {
		// try to decrypt in old-style
		_password, err := a.encrypt.DecryptPassword(password)
		if err != nil {
			logrus.Errorf("failed to decrypt password for mysql account %s: %s", account.ID, err)
		} else {
			password = _password
		}
	}
	config["MYSQL_USERNAME"] = account.Username
	config["MYSQL_PASSWORD"] = password
	return nil
}

func (a *Addon) InitMySQLAccount(addonIns *dbclient.AddonInstance, addonInsRouting *dbclient.AddonInstanceRouting, operator string) error {
	if addonIns.AddonName != "mysql" {
		return nil
	}
	extra, err := a.db.GetByInstanceIDAndField(addonInsRouting.RealInstance, "password")
	if err != nil {
		return err
	}
	if extra == nil {
		return fmt.Errorf("not found extra for instance: %s", addonInsRouting.RealInstance)
	}
	account := buildMySQLAccount(addonIns, addonInsRouting, extra, operator)
	return a.db.CreateMySQLAccount(account)
}

func buildMySQLAccount(addonIns *dbclient.AddonInstance, addonInsRouting *dbclient.AddonInstanceRouting,
	extra *dbclient.AddonInstanceExtra, operator string) *dbclient.MySQLAccount {
	return &dbclient.MySQLAccount{
		Username:          "mysql",
		Password:          extra.Value,
		KMSKey:            addonIns.KmsKey,
		InstanceID:        addonIns.ID,
		RoutingInstanceID: addonInsRouting.ID,
		Creator:           operator,
	}
}

func (a *Addon) prepareAttachment(addonInsRouting *dbclient.AddonInstanceRouting, addonAttach *dbclient.AddonAttachment) bool {
	if addonInsRouting.AddonName != "mysql" {
		return false
	}
	accounts, err := a.db.GetMySQLAccountListByRoutingInstanceID(addonInsRouting.ID)
	if err != nil {
		logrus.Errorf("get account list failed, %+v", err)
	}
	return a._prepareAttachment(addonAttach, accounts)
}

func (a *Addon) _prepareAttachment(addonAttach *dbclient.AddonAttachment, accounts []dbclient.MySQLAccount) bool {
	if len(accounts) == 0 {
		return false
	}
	addonAttach.MySQLAccountID = accounts[0].ID
	addonAttach.MySQLAccountState = "CUR"
	return true
}
