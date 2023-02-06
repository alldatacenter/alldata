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

package rds

import (
	"fmt"

	"github.com/aliyun/alibaba-cloud-sdk-go/services/rds"
	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

// describe database info
func DescribeAccounts(ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlListAccountRequest) (*rds.DescribeAccountsResponse, error) {
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %v", err)
		return nil, err
	}

	request := rds.CreateDescribeAccountsRequest()
	request.Scheme = "https"
	request.DBInstanceId = req.InstanceID

	response, err := client.DescribeAccounts(request)
	if err != nil {
		logrus.Errorf("describe mysql acount failed, error:%v", err)
		return nil, err
	}
	return response, nil
}

// create accounts for a database
// instance --> database --> accounts
func CreateDatabaseAccount(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlDBAccountsRequest) error {
	// ignore if no account
	if req.Account == "" {
		return nil
	}
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return err
	}

	// create account if password given
	if req.Password != "" {
		reqAccount := rds.CreateCreateAccountRequest()
		reqAccount.Scheme = "https"
		reqAccount.DBInstanceId = req.InstanceID
		reqAccount.AccountName = req.Account
		reqAccount.AccountPassword = req.Password

		_, err := client.CreateAccount(reqAccount)
		if err != nil {
			err := fmt.Errorf("create mysql database account failed, request:%+v, error:%v", req, err)
			logrus.Error(err.Error())
			return err
		}
	}

	// grant account privilege & bound to database
	if req.DBName != "" && req.AccountPrivilege != "" {
		reqPrivilege := rds.CreateGrantAccountPrivilegeRequest()
		reqPrivilege.Scheme = "https"
		reqPrivilege.DBInstanceId = req.InstanceID
		reqPrivilege.AccountName = req.Account
		reqPrivilege.DBName = req.DBName
		reqPrivilege.AccountPrivilege = req.AccountPrivilege

		_, err = client.GrantAccountPrivilege(reqPrivilege)
		if err != nil {
			err := fmt.Errorf("grant account privilege failed, request:%+v, error:%v", req, err)
			logrus.Error(err.Error())
			return err
		}
	}
	return nil
}

func CreateAccount(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlAccountRequest) error {
	// ignore if no account
	if req.Account == "" {
		return nil
	}
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return err
	}
	reqAccount := rds.CreateCreateAccountRequest()
	reqAccount.Scheme = "https"
	reqAccount.DBInstanceId = req.InstanceID
	reqAccount.AccountName = req.Account
	reqAccount.AccountPassword = req.Password
	reqAccount.AccountType = "Normal"
	reqAccount.AccountDescription = req.Description

	_, err = client.CreateAccount(reqAccount)
	if err != nil {
		err := fmt.Errorf("create mysql account failed, request:%+v, error:%v", req, err)
		logrus.Error(err.Error())
		return err
	}
	return nil
}

func ResetAccountPassword(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlAccountRequest) error {
	// ignore if no account
	if req.Account == "" {
		return nil
	}
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return err
	}
	request := rds.CreateResetAccountPasswordRequest()
	request.Scheme = "https"

	request.DBInstanceId = req.InstanceID
	request.AccountName = req.Account
	request.AccountPassword = req.Password

	_, err = client.ResetAccountPassword(request)
	if err != nil {
		err := fmt.Errorf("reset mysql account password failed, request:%+v, error:%v", req, err)
		logrus.Error(err.Error())
		return err
	}
	return nil
}

func ChangeAccountPrivilege(ctx aliyun_resources.Context, req apistructs.ChangeMysqlAccountPrivilegeRequest) error {
	var revokeDbs []apistructs.MysqlAccountPrivilege
	newDbSet := set.New()
	for _, v := range req.AccountPrivileges {
		newDbSet.Insert(v.DBName)
	}
	for _, v := range req.OldAccountPrivileges {
		if !newDbSet.Has(v.DBName) {
			revokeDbs = append(revokeDbs, apistructs.MysqlAccountPrivilege{
				DBName: v.DBName,
			})
		}
	}
	gr := apistructs.GrantMysqlAccountPrivilegeRequest{
		Vendor:            req.Vendor,
		Region:            req.Region,
		InstanceID:        req.InstanceID,
		Account:           req.Account,
		AccountPrivileges: req.AccountPrivileges,
	}
	err := GrantAccountPrivilege(ctx, gr)
	if err != nil {
		return err
	}

	rr := apistructs.GrantMysqlAccountPrivilegeRequest{
		Vendor:            req.Vendor,
		Region:            req.Region,
		InstanceID:        req.InstanceID,
		Account:           req.Account,
		AccountPrivileges: revokeDbs,
	}
	err = RevokeAccountPrivilege(ctx, rr)
	if err != nil {
		return err
	}
	return nil
}

func GrantAccountPrivilege(ctx aliyun_resources.Context, req apistructs.GrantMysqlAccountPrivilegeRequest) error {
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return err
	}

	reqPrivilege := rds.CreateGrantAccountPrivilegeRequest()
	reqPrivilege.Scheme = "https"
	reqPrivilege.DBInstanceId = req.InstanceID
	reqPrivilege.AccountName = req.Account

	var success []string

	for _, v := range req.AccountPrivileges {
		reqPrivilege.DBName = v.DBName
		reqPrivilege.AccountPrivilege = v.AccountPrivilege
		_, err = client.GrantAccountPrivilege(reqPrivilege)
		if err != nil {
			err := fmt.Errorf("grant account privilege failed, failed one:%+v, success:%v, request:%+v, error:%v", v.DBName, success, req, err)
			logrus.Error(err.Error())
			return err
		}
		success = append(success, v.DBName)
	}
	return nil
}

func RevokeAccountPrivilege(ctx aliyun_resources.Context, req apistructs.GrantMysqlAccountPrivilegeRequest) error {
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return err
	}
	request := rds.CreateRevokeAccountPrivilegeRequest()
	request.Scheme = "https"

	request.DBInstanceId = req.InstanceID
	request.AccountName = req.Account

	var success []string

	for _, v := range req.AccountPrivileges {
		request.DBName = v.DBName
		_, err := client.RevokeAccountPrivilege(request)
		if err != nil {
			err := fmt.Errorf("revoke account privilege failed, failed one:%+v, success:%v, request:%+v, error:%v", v.DBName, success, req, err)
			logrus.Error(err.Error())
			return err
		}
		success = append(success, v.DBName)
	}
	return nil
}
