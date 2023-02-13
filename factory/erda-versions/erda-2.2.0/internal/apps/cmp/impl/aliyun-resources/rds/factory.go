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

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

type RdsFactory struct {
	*resource_factory.BaseResourceFactory
}

func creator(ctx aliyun_resources.Context, m resource_factory.BaseResourceMaterial, r *dbclient.Record, d *apistructs.CreateCloudResourceRecord, v apistructs.CloudResourceVpcBaseInfo) (*apistructs.AddonConfigCallBackResponse, *dbclient.ResourceRouting, error) {
	var err error

	req, ok := m.(apistructs.CreateCloudResourceMysqlRequest)
	if !ok {
		return nil, nil, errors.Errorf("convert material failed, material: %+v", m)
	}
	if v.VpcCIDR != "" {
		req.SecurityIPList = v.VpcCIDR
	}
	regionids := aliyun_resources.ActiveRegionIDs(ctx)
	list, _, err := List(ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
	if err != nil {
		err = errors.Wrap(err, "list mysql failed")
		return nil, nil, err
	}
	for _, item := range list {
		if req.InstanceName == item.DBInstanceDescription {
			err := errors.Errorf("mysql instance already exist, region:%s, name:%s", item.RegionId, item.DBInstanceDescription)
			return nil, nil, err
		}
	}
	logrus.Infof("start to create rds instance, request: %+v", req)

	resp, err := CreateInstance(ctx, req)
	if err != nil {
		return nil, nil, errors.Wrap(err, "create mysql instance failed")
	}
	d.InstanceID = resp.DBInstanceId
	// not come from addon, only create instance
	if req.Source != apistructs.CloudResourceSourceAddon {
		return nil, nil, nil
	}
	// tag resource with [cluster name, project name]
	clusterTag, _ := aliyun_resources.GenClusterTag(req.ClusterName)
	// get project info
	_, projName, err := aliyun_resources.GetProjectClusterName(ctx, req.ProjectID, "")
	if err != nil {
		err = fmt.Errorf("get project name from project id failed, error:%+v", err)
		return nil, nil, err
	}
	projectTag, _ := aliyun_resources.GenProjectTag(projName)
	err = TagResource(ctx, []string{resp.DBInstanceId}, []string{clusterTag, projectTag})
	if err != nil {
		return nil, nil, errors.Wrap(err, "tag resource failed")
	}
	// if no databases, create finish
	if len(req.Databases) == 0 {
		return nil, nil, nil
	}

	// create mysql database
	dbReq := apistructs.CreateCloudResourceMysqlDBRequest{
		CreateCloudResourceBaseInfo: apistructs.CreateCloudResourceBaseInfo{
			Vendor:      req.Vendor,
			Region:      req.Region,
			OrgID:       req.OrgID,
			UserID:      req.UserID,
			ClusterName: req.ClusterName,
			ProjectID:   req.ProjectID,
			Source:      req.Source,
			ClientToken: req.ClientToken,
		},
		InstanceID: resp.DBInstanceId,
		Databases:  req.Databases,
	}

	// create mysql database step
	createDatabaseStep := apistructs.CreateCloudResourceStep{
		Step:   string(dbclient.RecordTypeCreateAliCloudMysqlDB),
		Status: string(dbclient.StatusTypeSuccess)}
	d.Steps = append(d.Steps, createDatabaseStep)

	err = CreateDatabasesWithWait(ctx, dbReq)
	if err != nil {
		return nil, nil, errors.Wrap(err, "create mysql database failed")
	}

	// create mysql database account
	for i, db := range dbReq.Databases {
		d.Steps[len(d.Steps)-1].Name = db.DBName
		reqAccount := apistructs.CreateCloudResourceMysqlDBAccountsRequest{
			InstanceID: dbReq.InstanceID,
			MysqlDataBaseInfo: apistructs.MysqlDataBaseInfo{
				DBName: db.DBName,
				CloudResourceMysqlAccount: apistructs.CloudResourceMysqlAccount{
					Account:          db.Account,
					Password:         db.Password,
					AccountPrivilege: db.AccountPrivilege,
				},
			},
		}
		// request come from addon
		if dbReq.Source == apistructs.CloudResourceSourceAddon && db.Account == "" {
			// Unique name: Length 2~16 characters. Must start with letter, end with letter or number,
			// consists of lowercase letters, numbers, or underscores.
			account := apistructs.CloudResourceMysqlAccount{
				Account:          "ac" + uuid.UUID()[:12],
				Password:         uuid.UUID()[:8] + "0@x" + uuid.UUID()[:8],
				AccountPrivilege: "ReadWrite",
			}
			reqAccount.Account = account.Account
			reqAccount.Password = account.Password
			reqAccount.AccountPrivilege = account.AccountPrivilege
			dbReq.Databases[i].Account = account.Account
			dbReq.Databases[i].Password = account.Password
			dbReq.Databases[i].AccountPrivilege = account.AccountPrivilege
		}
		err = CreateDatabaseAccount(ctx, reqAccount)
		if err != nil {
			return nil, nil, errors.Wrap(err, "create mysql db account failed")
		}
	}

	if req.Source != apistructs.CloudResourceSourceAddon || len(req.Databases) == 0 {
		return nil, nil, nil
	}
	db := req.Databases[0]

	cbResp := &apistructs.AddonConfigCallBackResponse{
		Config: []apistructs.AddonConfigCallBackItemResponse{
			{
				Name: "MYSQL_HOST",
				// mysql intranet endpoint
				Value: fmt.Sprintf("%s.mysql.rds.aliyuncs.com", dbReq.InstanceID),
			},
			{
				Name:  "MYSQL_PORT",
				Value: "3306",
			},
			{
				Name:  "MYSQL_DATABASE",
				Value: db.DBName,
			},
			{
				Name:  "MYSQL_USERNAME",
				Value: db.Account,
			},
			{
				Name:  "MYSQL_PASSWORD",
				Value: db.Password,
			},
		},
	}

	routing := &dbclient.ResourceRouting{
		ResourceID:   dbReq.InstanceID,
		ResourceName: db.DBName,
		ResourceType: dbclient.ResourceTypeMysqlDB,
		Vendor:       req.Vendor,
		OrgID:        req.OrgID,
		ClusterName:  req.ClusterName,
		ProjectID:    req.ProjectID,
		AddonID:      db.AddonID,
		Status:       dbclient.ResourceStatusAttached,
		RecordID:     r.ID,
	}
	return cbResp, routing, nil
}

func init() {
	factory := RdsFactory{BaseResourceFactory: &resource_factory.BaseResourceFactory{}}
	factory.Creator = creator
	factory.RecordType = dbclient.RecordTypeCreateAliCloudMysql
	err := resource_factory.Register(dbclient.ResourceTypeMysql, factory)
	if err != nil {
		panic(err)
	}
}
