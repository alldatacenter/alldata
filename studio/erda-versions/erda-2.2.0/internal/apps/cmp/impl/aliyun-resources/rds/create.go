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
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/rds"
	libvpc "github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

// Deprecated
// create mysql instance with record
func CreateInstanceWithRecord(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlRequest, record *dbclient.Record) {
	// create mysql instance
	var detail apistructs.CreateCloudResourceRecord

	// create mysql instance step
	createInstanceStep := apistructs.CreateCloudResourceStep{
		Step:   string(dbclient.RecordTypeCreateAliCloudMysql),
		Status: string(dbclient.StatusTypeSuccess)}
	detail.Steps = append(detail.Steps, createInstanceStep)
	detail.Steps[len(detail.Steps)-1].Name = req.InstanceName
	detail.ClientToken = req.ClientToken
	detail.InstanceName = req.InstanceName

	// Duplicate name check
	regionids := aliyun_resources.ActiveRegionIDs(ctx)
	list, _, err := List(ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
	if err != nil {
		err := fmt.Errorf("list mysql failed, error:%v", err)
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
		return
	}
	for _, m := range list {
		if req.InstanceName == m.DBInstanceDescription {
			err := fmt.Errorf("mysql instance already exist, region:%s, name:%s", m.RegionId, m.DBInstanceDescription)
			logrus.Errorf("%s, request:%+v", err.Error(), req)
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}
	}

	// request from addon: none region, get region/cidr from vpc(select by cluster name)
	// request from cloud management:  has region and vpc id, use them to  get cidr/zoneID and more detail info
	if req.ZoneID == "" {
		ctx.Region = req.Region
		ctx.VpcID = req.VpcID
		v, err := vpc.GetVpcBaseInfo(ctx, req.ClusterName, req.VpcID)
		if err != nil {
			err := fmt.Errorf("get vpc info failed, error:%v", err)
			logrus.Errorf("%s, request:%+v", err.Error(), req)
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}
		req.Region = v.Region
		req.VpcID = v.VpcID
		req.SecurityIPList = v.VpcCIDR
		req.VSwitchID = v.VSwitchID
		req.ZoneID = v.ZoneID
	}
	ctx.Region = req.Region

	logrus.Debugf("start to create instance, request:%+v", req)
	r, err := CreateInstance(ctx, req)
	if err != nil {
		err := fmt.Errorf("create instance failed, error:%v", err)
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
		return
	}
	detail.InstanceID = r.DBInstanceId

	// tag resource with [cluster name, project id]
	clusterTag, _ := aliyun_resources.GenClusterTag(req.ClusterName)
	// get project info
	_, projName, err := aliyun_resources.GetProjectClusterName(ctx, req.ProjectID, "")
	if err != nil {
		err = fmt.Errorf("get project name from project id failed, error:%+v", err)
		aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
		return
	}
	projectTag, _ := aliyun_resources.GenProjectTag(projName)
	err = TagResource(ctx, []string{r.DBInstanceId}, []string{clusterTag, projectTag})
	if err != nil {
		err := fmt.Errorf("tag resource failed, error:%v", err)
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
		return
	}

	// if no databases, create finish
	if len(req.Databases) == 0 {
		content, err := json.Marshal(detail)
		if err != nil {
			logrus.Errorf("marshal record detail failed, error:%+v", err)
		}
		record.Status = dbclient.StatusTypeSuccess
		record.Detail = string(content)
		if err := ctx.DB.RecordsWriter().Update(*record); err != nil {
			logrus.Errorf("failed to update record: %v", err)
		}
		return
	}

	// create mysql database
	reqDB := apistructs.CreateCloudResourceMysqlDBRequest{
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
		InstanceID: r.DBInstanceId,
		Databases:  req.Databases,
	}
	_ = CreateDBWithRecord(ctx, reqDB, record, &detail)
}

// create mysql database with record
func CreateDBWithRecord(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlDBRequest,
	record *dbclient.Record, detail *apistructs.CreateCloudResourceRecord) (err error) {

	defer func() {
		if err != nil {
			logrus.Errorf("create mysql db failed, request:%+v, error:%+v", req, err)
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, detail, err)
		}
	}()

	// if record detail is nil, not come from create mysql instance, init it
	if detail == nil {
		detail = &apistructs.CreateCloudResourceRecord{InstanceID: req.InstanceID}
		detail.InstanceID = req.InstanceID
	}

	// create mysql database step
	createDatabaseStep := apistructs.CreateCloudResourceStep{
		Step:   string(dbclient.RecordTypeCreateAliCloudMysqlDB),
		Status: string(dbclient.StatusTypeSuccess)}
	detail.Steps = append(detail.Steps, createDatabaseStep)

	// get region info by cluster name if not provide
	if req.Region == "" {
		var v libvpc.Vpc
		v, err = vpc.GetVpcByCluster(ctx, req.ClusterName)
		if err != nil {
			err = fmt.Errorf("get region info failed, error:%v", err)
			return
		}
		req.Region = v.RegionId
	}
	ctx.Region = req.Region

	var reqDB apistructs.CreateCloudResourceMysqlDBRequest
	reqDB.InstanceID = req.InstanceID
	reqDB.Databases = req.Databases

	// Only one database can create in one time currently.
	logrus.Debugf("start to create database:%+v", reqDB)
	err = CreateDatabases(ctx, reqDB)
	if err != nil {
		err = fmt.Errorf("create mysql database failed, error:%v", err)
		return
	}

	// create mysql database account
	for i, db := range req.Databases {
		detail.Steps[len(detail.Steps)-1].Name = db.DBName
		reqAccount := apistructs.CreateCloudResourceMysqlDBAccountsRequest{
			InstanceID: req.InstanceID,
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
		if req.Source == apistructs.CloudResourceSourceAddon && db.Account == "" {
			// if request from addon and not account, auto generate on
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
			req.Databases[i].Account = account.Account
			req.Databases[i].Password = account.Password
			req.Databases[i].AccountPrivilege = account.AccountPrivilege
		}

		err = CreateDatabaseAccount(ctx, reqAccount)
		if err != nil {
			err = fmt.Errorf("create mysql db account failed, error:%v", err)
			return
		}
	}

	// post addon config step
	if req.Source == apistructs.CloudResourceSourceAddon && len(req.Databases) > 0 {
		db := req.Databases[0]
		// TODO construct addon call back request, Done
		cb := apistructs.AddonConfigCallBackResponse{
			Config: []apistructs.AddonConfigCallBackItemResponse{
				{
					Name: "MYSQL_HOST",
					// mysql intranet endpoint
					Value: fmt.Sprintf("%s.mysql.rds.aliyuncs.com", req.InstanceID),
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

		// TODO: only support one addon in a request
		if db.AddonID == "" {
			db.AddonID = req.ClientToken
		}

		logrus.Debugf("start addon config callback, addonid:%s", db.AddonID)
		_, err = ctx.Bdl.AddonConfigCallback(db.AddonID, cb)
		if err != nil {
			err = fmt.Errorf("mysql db addon call back failed, error: %v", err)
			return
		}
		_, err = ctx.Bdl.AddonConfigCallbackProvison(db.AddonID, apistructs.AddonCreateCallBackResponse{IsSuccess: true})
		if err != nil {
			err = fmt.Errorf("add call back provision failed, error:%v", err)
			return
		}

		// create resource routing record
		_, err = ctx.DB.ResourceRoutingWriter().Create(&dbclient.ResourceRouting{
			ResourceID:   req.InstanceID,
			ResourceName: db.DBName,
			ResourceType: dbclient.ResourceTypeMysqlDB,
			Vendor:       req.Vendor,
			OrgID:        req.OrgID,
			ClusterName:  req.ClusterName,
			ProjectID:    req.ProjectID,
			AddonID:      db.AddonID,
			Status:       dbclient.ResourceStatusAttached,
			RecordID:     record.ID,
			Detail:       "",
		})
		if err != nil {
			err = fmt.Errorf("write resource routing to db failed, error:%v", err)
			return
		}
	}

	// create cloud resource successfully, update ops record
	content, err := json.Marshal(detail)
	if err != nil {
		logrus.Errorf("marshal record detail failed, error:%+v", err)
	}
	record.Status = dbclient.StatusTypeSuccess
	record.Detail = string(content)
	if err := ctx.DB.RecordsWriter().Update(*record); err != nil {
		logrus.Errorf("failed to update record: %v", err)
	}
	return
}

// create mysql instance
func CreateInstance(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlRequest) (*rds.CreateDBInstanceResponse, error) {
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return nil, err
	}
	client.SetReadTimeout(30 * time.Minute)

	// create mysql instance request
	request := rds.CreateCreateDBInstanceRequest()
	request.Scheme = "https"

	request.Engine = "MySQL"
	request.EngineVersion = "5.7"

	request.Category = req.SpecType
	request.DBInstanceClass = req.SpecSize
	// Basic edition: only use cloud_ssd
	// High-availability edition: use local_ssd
	if req.SpecType == "Basic" {
		request.DBInstanceStorageType = "cloud_ssd"
	} else if req.SpecType == "High-availability" {
		request.DBInstanceStorageType = "local_ssd"
	} else {
		err := fmt.Errorf("create mysql instance failed, invalid class type:%s, support class type:%s, request: %+v", req.SpecType, "[Basic, High-availability]", req)
		logrus.Error(err.Error())
		return nil, err
	}
	request.DBInstanceStorage = requests.NewInteger(req.StorageSize)

	request.SystemDBCharset = "utf8mb4"
	request.DBInstanceDescription = req.InstanceName
	request.ClientToken = req.ClientToken
	request.InstanceNetworkType = "VPC"
	request.ConnectionMode = "Standard"
	request.DBInstanceNetType = "Intranet"
	// TODO: get vpc id from request
	request.VPCId = req.VpcID
	request.VSwitchId = req.VSwitchID
	// TODO: choose zone id which contains more resource in vpc
	// Parameter must be supplied when VPC and switch are specified, to matching available area for switch
	request.ZoneId = req.ZoneID
	// TODO: get vpc cidr from vpc id
	request.SecurityIPList = req.SecurityIPList

	// Billed monthly，convert year to month
	if strings.ToLower(req.ChargeType) == aliyun_resources.ChargeTypePrepaid {
		req.ChargeType = "Prepaid"
	} else if strings.ToLower(req.ChargeType) == "postpaid" {
		req.ChargeType = "Postpaid"
	}
	request.PayType = req.ChargeType
	request.UsedTime = req.ChargePeriod
	if strings.ToLower(req.ChargePeriod) == aliyun_resources.ChargeTypePrepaid {
		request.Period = "Month"
		request.AutoRenew = strconv.FormatBool(req.AutoRenew)
	}

	response, err := client.CreateDBInstance(request)
	if err != nil {
		err := fmt.Errorf("create mysql instance failed, request: %+v, error:%v", req, err)
		return nil, err
	}
	return response, nil
}

// create mysql databases
// instance id, databases
func CreateDatabasesWithWait(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlDBRequest) error {
	if len(req.Databases) == 0 {
		return nil
	}

	// TODO: now only support create one database at a time
	req.Databases = req.Databases[:1]

	// wait mysql instance to be running status, [12 min]
	status := ""
	for i := 0; i < 12; i++ {
		rsp, err := GetInstanceDetailInfo(ctx, apistructs.CloudResourceMysqlDetailInfoRequest{InstanceID: req.InstanceID})
		if err != nil {
			logrus.Errorf("describe mysql instance attribute failed, instance id:%s, error:%v", req.InstanceID, err)
			return err
		}
		status = rsp.Status
		if strings.ToLower(status) == "running" {
			break
		}
		time.Sleep(1 * time.Minute)
	}
	if strings.ToLower(status) != "running" {
		err := fmt.Errorf("wait mysql instance to running status time out, time:%d(min), status:%s", 12, status)
		logrus.Errorf(err.Error())
		return err
	}

	return CreateDatabases(ctx, req)
}

func CreateDatabases(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceMysqlDBRequest) error {
	if len(req.Databases) == 0 {
		return nil
	}

	// TODO: now only support create one database at a time
	req.Databases = req.Databases[:1]

	// create mysql database
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return err
	}
	var (
		successDBs []string
	)

	for _, db := range req.Databases {
		request := rds.CreateCreateDatabaseRequest()
		request.Scheme = "https"

		request.DBInstanceId = req.InstanceID
		request.DBName = db.DBName
		if db.CharacterSetName == "" {
			request.CharacterSetName = "utf8mb4"
		} else {
			request.CharacterSetName = db.CharacterSetName
		}
		request.DBDescription = db.Description

		_, err := client.CreateDatabase(request)
		if err != nil {
			logrus.Errorf("create mysql database failed, failed:%s, success:%v request:%+v, error:%+v", db.DBName, successDBs, req, err)
			return err
		}
		successDBs = append(successDBs, db.DBName)
	}
	return nil
}
