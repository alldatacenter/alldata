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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"sync"

	"github.com/pkg/errors"

	librds "github.com/aliyun/alibaba-cloud-sdk-go/services/rds"
	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/rds"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) DeleteMysql(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceOnsResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
				Data: apistructs.CloudResourceOnsData{List: []apistructs.CloudResourceOnsBasicData{}},
			})
		}
	}()

	var req apistructs.DeleteCloudResourceMysqlRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
			err = fmt.Errorf("failed to unmarshal request: %+v", err)
			return
		}
	}

	// get identity info
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, req.ProjectID, apistructs.DeleteAction)
	if err != nil {
		return
	}

	if req.Source == apistructs.CloudResourceSourceAddon && req.RecordID != "" {
		records, er := e.dbclient.RecordsReader().ByIDs(req.RecordID).Do()
		if er != nil {
			err = fmt.Errorf("get record failed, request:%+v, error:%v", req, er)
			return
		}
		if len(records) == 0 {
			return mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
				Header: apistructs.Header{Success: true},
			})
		}
		r := records[0]

		// create addon failed, create instance success
		if r.Status == dbclient.StatusTypeFailed && r.RecordType == dbclient.RecordTypeCreateAliCloudMysql {
			var detail apistructs.CreateCloudResourceRecord
			er := json.Unmarshal([]byte(r.Detail), &detail)
			if er != nil {
				err = fmt.Errorf("unmarshal record detail info failed, error:%v", er)
				return
			}
			if detail.InstanceID != "" {
				err = fmt.Errorf("create addon failed, but related cloud resource have been created successfully")
				return
			}
		}
	}

	return mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) DeleteMysqlDatabase(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		} else {
			resp, err = mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
				Header: apistructs.Header{
					Success: true,
				},
			})
		}
	}()

	var req apistructs.DeleteCloudResourceMysqlDBRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal request: %+v", err)
		return
	}

	// get identity info
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, req.ProjectID, apistructs.DeleteAction)
	if err != nil {
		return
	}

	if req.Source == apistructs.CloudResourceSourceAddon && req.RecordID != "" {
		id, er := strconv.Atoi(req.RecordID)
		if er != nil {
			logrus.Errorf("delete failed, error:%+v", er)
			return
		}
		if id < 1 {
			logrus.Errorf("delete failed, invalid record id:%v", id)
			return
		}

		records, er := e.dbclient.RecordsReader().ByIDs(req.RecordID).Do()
		if er != nil {
			err = fmt.Errorf("get record failed, request:%+v, error:%v", req, er)
			return
		}
		if len(records) == 0 {
			return mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
				Header: apistructs.Header{Success: true},
			})
		}
		r := records[0]
		if r.Status == dbclient.StatusTypeFailed && r.RecordType == dbclient.RecordTypeCreateAliCloudMysql {
			list, er := e.dbclient.ResourceRoutingReader().ByRecordIDs(req.RecordID).
				ByResourceTypes(dbclient.ResourceTypeMysql.String()).Do()
			if er != nil {
				err = fmt.Errorf("check resource routing failed, error:%v", er)
				return
			}
			if len(list) != 0 {
				err = fmt.Errorf("create addon failed, but related cloud resource have been created successfully")
				return
			}
		}
	}

	return mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) ListMysql(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceMysqlResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
				Data: apistructs.CloudResourceMysqlData{List: []apistructs.CloudResourceMysqlBasicData{}},
			})
		}
	}()

	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	projid := r.URL.Query().Get("projectID")
	workspace := r.URL.Query().Get("workspace")

	// get identity info
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, projid, apistructs.GetAction)
	if err != nil {
		return
	}

	// get ak/sk info
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}

	var resultList []rds.DBInstanceInDescribeDBInstancesWithTag

	if projid == "" {
		// request come from cloud resource
		regionids := e.getAvailableRegions(ak_ctx, r)
		mysqlList, _, er := rds.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
		if er != nil {
			err = fmt.Errorf("failed to get mysql list: %v", err)
			return
		}
		resultList = mysqlList
	} else {
		// request come from addon
		clusterName, projName, er := aliyun_resources.GetProjectClusterName(ak_ctx, projid, workspace)
		if er != nil {
			err = fmt.Errorf("get project cluster name failed")
			return
		}

		v, er := vpc.GetVpcByCluster(ak_ctx, clusterName)
		if er != nil {
			err = fmt.Errorf("get vpc by cluster name failed, error:%+v", er)
			return
		}

		ak_ctx.Region = v.RegionId

		var instsWithProjID []librds.DBInstance
		var instsWithClusterName []librds.DBInstance

		var ge error
		var wg sync.WaitGroup
		wg.Add(2) // get intersection (proj id, cluster name)

		// get instance with project id tag
		go func() {
			defer func() { wg.Done() }()

			rsp, err := rds.DescribeResource(ak_ctx, aliyun_resources.DefaultPageOption, "", projName)
			if err != nil {
				e := fmt.Errorf("list mysql instance failed, error:%v", err)
				logrus.Errorf(e.Error())
				ge = e
			}
			instsWithProjID = rsp.DBInstances
		}()

		// get instance with cluster name tag
		go func() {
			defer func() { wg.Done() }()

			rsp, err := rds.DescribeResource(ak_ctx, aliyun_resources.DefaultPageOption, clusterName, "")
			if err != nil {
				e := fmt.Errorf("list mysql instance failed, error:%v", err)
				logrus.Errorf(e.Error())
				ge = e
			}
			instsWithClusterName = rsp.DBInstances
		}()

		wg.Wait()
		if ge != nil {
			err = fmt.Errorf("get instance by project and cluster name tag faield, error:%+v", ge)
			return
		}

		// get instance with both clusterName & projectId tag
		instsWithProjSet := set.New()
		for _, i := range instsWithProjID {
			instsWithProjSet.Insert(i.DBInstanceId)
		}
		for i, j := range instsWithClusterName {
			if instsWithProjSet.Has(j.DBInstanceId) {
				resultList = append(resultList, rds.DBInstanceInDescribeDBInstancesWithTag{
					DBInstance: instsWithClusterName[i],
					Tag:        nil,
				})
			}
		}
	}

	result := []apistructs.CloudResourceMysqlBasicData{}
	for _, ins := range resultList {
		result = append(result, apistructs.CloudResourceMysqlBasicData{
			ID:         ins.DBInstanceId,
			Name:       ins.DBInstanceDescription,
			Region:     ins.RegionId,
			Category:   ins.Category, // empty result from alicloud api
			Spec:       i18n.Sprintf(ins.DBInstanceClass),
			Version:    ins.EngineVersion,
			Status:     i18n.Sprintf(ins.DBInstanceStatus),
			ChargeType: ins.PayType,
			CreateTime: ins.CreateTime,
			ExpireTime: ins.ExpireTime,
			Tag:        ins.Tag,
		})
	}
	return mkResponse(apistructs.ListCloudResourceMysqlResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.CloudResourceMysqlData{
			Total: len(result),
			List:  result,
		},
	})
}

func (e *Endpoints) GetMysqlDetailInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceOnsResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
				Data: apistructs.CloudResourceOnsData{List: []apistructs.CloudResourceOnsBasicData{}},
			})
		}
	}()

	var req apistructs.CloudResourceMysqlDetailInfoRequest
	id := vars["instanceID"]
	region := r.URL.Query().Get("region")
	req.InstanceID = id
	req.Region = region

	// get identity info
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}

	if req.Region == "" {
		err = fmt.Errorf("get mysql detail info faild, miss parameter region")
		return
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	res, err := rds.GetInstanceFullDetailInfo(ctx, ak_ctx, req)
	if err != nil {
		return
	}

	return mkResponse(apistructs.CloudResourceMysqlFullDetailInfoResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: res,
	})
}

func (e *Endpoints) CreateMysqlInstance(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceOnsResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
				Data: apistructs.CloudResourceOnsData{List: []apistructs.CloudResourceOnsBasicData{}},
			})
		}
	}()

	req := apistructs.CreateCloudResourceMysqlRequest{CreateCloudResourceBaseRequest: &apistructs.CreateCloudResourceBaseRequest{
		CreateCloudResourceBaseInfo:   &apistructs.CreateCloudResourceBaseInfo{},
		CreateCloudResourceChargeInfo: apistructs.CreateCloudResourceChargeInfo{},
		InstanceName:                  "",
	}}

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal request: %+v", err)
		return
	}
	if req.Vendor == "" {
		req.Vendor = aliyun_resources.CloudVendorAliCloud.String()
	}

	// get identity info: user/org id
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, req.ProjectID, apistructs.CreateAction)
	if err != nil {
		return
	}
	req.UserID = i.UserID
	req.OrgID = i.OrgID

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}

	factory, err := resource_factory.GetResourceFactory(e.dbclient, dbclient.ResourceTypeMysql)
	if err != nil {
		return
	}
	record, err := factory.CreateResource(ak_ctx, req)
	if err != nil {
		return
	}
	return mkResponse(apistructs.CreateCloudResourceMysqlResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CreateCloudResourceBaseResponseData{RecordID: record.ID},
	})
}

func (e *Endpoints) CreateMysqlDatabase(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.CreateCloudResourceOnsTopicResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()

	var req apistructs.CreateCloudResourceMysqlDBRequest
	if req.Vendor == "" {
		req.Vendor = aliyun_resources.CloudVendorAliCloud.String()
	}

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal create ons topic request: %+v", err)
		return
	}

	// get identity info: user/org id
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, req.ProjectID, apistructs.CreateAction)
	if err != nil {
		return
	}
	req.UserID = i.UserID
	req.OrgID = i.OrgID

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}

	err = e.CreateAddonCheck(apistructs.CreateCloudResourceBaseInfo{
		Vendor:      req.Vendor,
		Region:      req.Region,
		VpcID:       req.VpcID,
		ZoneID:      req.ZoneID,
		OrgID:       req.OrgID,
		UserID:      req.UserID,
		ClusterName: req.ClusterName,
		ProjectID:   req.ProjectID,
		Source:      req.Source,
		ClientToken: req.ClientToken,
	})
	if err != nil {
		return
	}

	record, rsp := e.InitRecord(dbclient.Record{
		RecordType:  dbclient.RecordTypeCreateAliCloudMysqlDB,
		UserID:      req.UserID,
		OrgID:       req.OrgID,
		ClusterName: req.ClusterName,
		Status:      dbclient.StatusTypeProcessing,
		Detail:      "",
		PipelineID:  0,
	})
	if rsp != nil {
		err = fmt.Errorf("init cmp record failed, error:%+v", err)
		return
	}

	err = rds.CreateDBWithRecord(ak_ctx, req, record, nil)
	if err != nil {
		return
	}

	return mkResponse(apistructs.CreateCloudResourceMysqlDBResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CreateCloudResourceBaseResponseData{RecordID: record.ID},
	})
}

func (e *Endpoints) ListMysqlAccount(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.CreateCloudResourceGatewayResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()

	var req apistructs.CloudResourceMysqlListAccountRequest
	id := vars["instanceID"]
	region := r.URL.Query().Get("region")
	req.InstanceID = id
	req.Region = region

	// get identity info: user/org id
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}

	if req.Region == "" {
		err = fmt.Errorf("get mysql detail info faild, miss parameter region")
		return
	}

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	res, er := rds.DescribeAccounts(ak_ctx, req)
	if er != nil {
		err = fmt.Errorf("describe account failed, request:%+v error:%+v", req, er)
		return
	}

	result := []apistructs.CloudResourceMysqlListAccountItem{}
	for _, ins := range res.Accounts.DBInstanceAccount {
		var dbPrivileges []apistructs.CloudResourceMysqlAccountPrivileges
		for _, p := range ins.DatabasePrivileges.DatabasePrivilege {
			dbPrivileges = append(dbPrivileges, apistructs.CloudResourceMysqlAccountPrivileges{
				DBName:           p.DBName,
				AccountPrivilege: p.AccountPrivilege,
			})
		}
		result = append(result, apistructs.CloudResourceMysqlListAccountItem{
			AccountName:        ins.AccountName,
			AccountStatus:      ins.AccountStatus,
			AccountType:        ins.AccountType,
			AccountDescription: ins.AccountDescription,
			DatabasePrivileges: dbPrivileges,
		})
	}

	resp, err = mkResponse(apistructs.CloudResourceMysqlListAccountResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CloudResourceMysqlListAccountData{
			List: result,
		},
	})
	return
}

func (e *Endpoints) ListMysqlDatabase(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.CloudResourceOnsTopicInfoResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
				Data: apistructs.CloudResourceOnsTopicInfo{List: []apistructs.OnsTopic{}},
			})
		}
	}()

	var req apistructs.CloudResourceMysqlListDatabaseRequest
	id := vars["instanceID"]
	region := r.URL.Query().Get("region")
	req.InstanceID = id
	req.Region = region

	// get identity info
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}

	if req.Region == "" {
		err = fmt.Errorf("get mysql detail info faild, miss parameter region")
		return
	}

	// get ak/sk info
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	res, err := rds.DescribeDatabases(ak_ctx, req)
	if err != nil {
		return
	}

	result := []apistructs.CloudResourceMysqlListDatabaseItem{}
	for _, ins := range res.Databases.Database {
		var accounts []apistructs.CloudResourceMysqlListDatabaseAccount
		for _, p := range ins.Accounts.AccountPrivilegeInfo {
			accounts = append(accounts, apistructs.CloudResourceMysqlListDatabaseAccount{
				Account: p.Account,
			})
		}
		result = append(result, apistructs.CloudResourceMysqlListDatabaseItem{
			DBName:           ins.DBName,
			DBStatus:         ins.DBStatus,
			CharacterSetName: ins.CharacterSetName,
			DBDescription:    ins.DBDescription,
			Accounts:         accounts,
		})
	}

	return mkResponse(apistructs.CloudResourceMysqlListDatabaseResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CloudResourceMysqlListDatabaseData{
			List: result,
		},
	})
}

func (e *Endpoints) CreateMysqlAccount(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.CreateCloudResourceMysqlAccountResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()

	var req apistructs.CreateCloudResourceMysqlAccountRequest

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal create ons group request: %+v", err)
		return
	}

	// get identity info: user/org id
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return
	}

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	er := rds.CreateAccount(ak_ctx, req)
	if er != nil {
		err = fmt.Errorf("create mysql account failed, error:%v", err)
		return
	}

	resp, err = mkResponse(apistructs.CreateCloudResourceMysqlAccountResponse{
		Header: apistructs.Header{
			Success: true,
		},
	})
	return
}

func (e *Endpoints) ResetMysqlAccountPassword(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.GrantMysqlAccountPrivilegeResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()

	var req apistructs.CreateCloudResourceMysqlAccountRequest

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal create ons group request: %+v", err)
		return
	}

	// get identity info: user/org id
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return
	}

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	er := rds.ResetAccountPassword(ak_ctx, req)
	if er != nil {
		err = fmt.Errorf("create mysql account failed, error:%v", err)
		return
	}

	resp, err = mkResponse(apistructs.GrantMysqlAccountPrivilegeResponse{
		Header: apistructs.Header{
			Success: true,
		},
	})
	return
}

func (e *Endpoints) GrantMysqlAccountPrivilege(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.GrantMysqlAccountPrivilegeResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()

	var req apistructs.ChangeMysqlAccountPrivilegeRequest

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal create ons group request: %+v", err)
		return
	}

	// get identity info: user/org id
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return
	}

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	er := rds.ChangeAccountPrivilege(ak_ctx, req)
	if er != nil {
		err = fmt.Errorf("create mysql account failed, error:%v", err)
		return
	}

	resp, err = mkResponse(apistructs.GrantMysqlAccountPrivilegeResponse{
		Header: apistructs.Header{
			Success: true,
		},
	})
	return
}
