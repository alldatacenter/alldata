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

package bundle

import (
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

func (b *Bundle) RunSoldierScript(scriptName string, params map[string]string) error {
	host, err := b.urls.Soldier()
	if err != nil {
		return err
	}
	hc := b.hc

	var runResp apistructs.RunScriptResponse
	req := hc.Post(host).Path(strutil.Concat("/autoop/run/", scriptName))
	for k, v := range params {
		req = req.Param(strutil.Concat("ENV_", k), v)
	}
	resp, err := req.Do().JSON(&runResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !runResp.Success {
		return toAPIError(resp.StatusCode(), runResp.Error)
	}

	if runResp.Data == false {
		return errors.Errorf("failed to run colony script, script: %s, soldier: %s", scriptName, host)
	}

	return nil
}

// MySQLInit 主从初始化
func (b *Bundle) MySQLInit(mysqlExecList *[]apistructs.MysqlExec, soldierUrl string) error {
	hc := b.hc
	logrus.Infof("MySQLInit body:%+v", *mysqlExecList)
	resp, err := hc.Post(soldierUrl).Path(apistructs.AddonMysqlInitURL).
		Header("Content-Type", "application/json").
		Header("Accept", "application/json").
		JSONBody(mysqlExecList).
		Do().DiscardBody()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	logrus.Infof("MySQLInit response :%+v", resp.StatusCode())
	if !resp.IsOK() {
		return errors.New("mysql初始化报错。")
	}
	return nil
}

// MySQLCheck 主从初始化
func (b *Bundle) MySQLCheck(mysqlExec *apistructs.MysqlExec, soldierUrl string) error {
	hc := b.hc
	logrus.Infof("MySQLCheck body:%+v", *mysqlExec)
	var checkResp apistructs.GetMysqlCheckResponse
	resp, err := hc.Post(soldierUrl).Path(apistructs.AddonMysqlcheckStatusURL).
		Header("Content-Type", "application/json").
		Header("Accept", "application/json").
		JSONBody(mysqlExec).
		Do().JSON(&checkResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	logrus.Infof("MySQLCheck response :%+v", *resp)
	if !resp.IsOK() {
		return errors.New("mysql主从状态同步检测报错。")
	}
	logrus.Infof("MySQLCheck checkResp :%+v", checkResp)
	if len(checkResp.Data) == 0 {
		return errors.New("mysql主从同步状态异常。")
	}
	for _, value := range checkResp.Data {
		if strings.ToLower(value) != "yes" && strings.ToLower(value) != "connecting" {
			return errors.New("mysql主从同步状态异常。")
		}
	}

	return nil
}

// MySQLExec 初始化mysql数据库
func (b *Bundle) MySQLExec(mysqlExec *apistructs.MysqlExec, soldierUrl string) error {
	hc := b.hc
	logrus.Infof("MySQLExec body: %+v", *mysqlExec)

	logrus.Infof("MySQLExec url: %s", soldierUrl)
	resp, err := hc.Post(soldierUrl).Path(apistructs.AddonMysqlExecURL).
		Header("Content-Type", "application/json").
		Header("Accept", "application/json").
		JSONBody(mysqlExec).
		Do().DiscardBody()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	logrus.Infof("MySQLExec response: %+v", *resp)
	if !resp.IsOK() {
		return errors.New("mysql初始化数据库报错。")
	}

	return nil
}

// MySQLExecFile 初始化mysql init.sql
func (b *Bundle) MySQLExecFile(mysqlExec *apistructs.MysqlExec, soldierUrl string) error {
	hc := b.hc
	logrus.Infof("executing init.sql, request body: %+v", *mysqlExec)
	logrus.Infof("executing init.sql url: %s", soldierUrl)
	resp, err := hc.Post(soldierUrl).Path(apistructs.AddonMysqlExecFileURL).
		Header("Content-Type", "application/json").
		Header("Accept", "application/json").
		JSONBody(mysqlExec).
		Do().DiscardBody()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	logrus.Infof("executed init.sql, response: %+v", *resp)
	if !resp.IsOK() {
		return errors.New("mysql init sql 报错。")
	}

	return nil
}
