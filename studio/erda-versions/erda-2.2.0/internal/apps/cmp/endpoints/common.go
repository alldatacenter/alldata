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
	"fmt"
	"net/http"
	"strconv"
	"strings"

	libvpc "github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// PermissionCheck Permission check
func (e *Endpoints) PermissionCheck(userID, orgID, projectID, action string) error {
	if orgID == "" {
		return e.IsManager(userID, apistructs.SysScope, "")
	}
	// org permission check
	err := e.OrgPermCheck(userID, orgID, action)
	if err != nil && strings.Contains(err.Error(), "access denied") && projectID != "" {
		// project permission check
		return e.IsManager(userID, apistructs.ProjectScope, projectID)
	}
	return err
}

func (e *Endpoints) OrgPermCheck(userID, orgID, action string) error {
	orgid, _ := strconv.Atoi(orgID)
	p := apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgid),
		Resource: apistructs.CloudResourceResource,
		Action:   action,
	}
	logrus.Infof("perm check request:%+v", p)
	rspData, err := e.bdl.CheckPermission(&p)
	if err != nil {
		err = fmt.Errorf("check permission error: %v", err)
		logrus.Errorf("permission check failed, request:%+v, error:%v", p, err)
		return err
	}
	if !rspData.Access {
		err = fmt.Errorf("access denied")
		logrus.Errorf("access denied, request:%v, error:%+v", p, err)
		return err
	}
	return nil
}

func (e *Endpoints) IsManager(userID string, scopeType apistructs.ScopeType, scopeID string) error {
	req := apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: scopeType,
			ID:   scopeID,
		},
	}
	scopeRole, err := e.bdl.ScopeRoleAccess(userID, &req)
	if err != nil {
		return err
	}
	logrus.Debugf("scopeRole: %+v", scopeRole)
	if scopeRole.Access {
		for _, role := range scopeRole.Roles {
			if e.bdl.CheckIfRoleIsManager(role) {
				return nil
			}
		}
	}
	err = fmt.Errorf("access denied")
	return err
}

// InitRecord Create cloud resource
func (e *Endpoints) InitRecord(r dbclient.Record) (*dbclient.Record, httpserver.Responser) {
	recordID, err := e.dbclient.RecordsWriter().Create(&dbclient.Record{
		RecordType:  r.RecordType,
		UserID:      r.UserID,
		OrgID:       r.OrgID,
		ClusterName: r.ClusterName,
		Status:      r.Status,
		Detail:      "",
		PipelineID:  0,
	})
	if err != nil {
		err := fmt.Errorf("failed to write record, error:%v", err)
		logrus.Errorf(err.Error())
		return nil,
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: err.Error()},
					},
				},
			}
	}
	records, err := e.dbclient.RecordsReader().ByIDs(strconv.FormatUint(recordID, 10)).Do()
	if err != nil || len(records) == 0 {
		var errStr string
		if len(records) == 0 {
			errStr = fmt.Sprintf("failed to query records, empty record, record id:%d", recordID)
		} else {
			errStr = fmt.Sprintf("failed to query records: %v", err)
		}
		logrus.Error(errStr)
		return nil,
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: errStr},
					},
				},
			}
	}
	return &records[0], nil
}

func (e *Endpoints) GetVpcInfoByCluster(ak_ctx aliyun_resources.Context, r *http.Request,
	cluster string) (libvpc.Vpc, httpserver.Responser) {
	regionids := e.getAvailableRegions(ak_ctx, r)
	vpcs, _, err := vpc.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.VPC, cluster)
	if err != nil || len(vpcs) != 1 {
		var e error
		if err != nil {
			e = fmt.Errorf("failed to get vpclist: %v", err)
		} else if len(vpcs) == 0 {
			e = fmt.Errorf("cannot get vpc info by cluserName, please tag vpc with clusterName tag [%s] first", cluster)
		} else {
			e = fmt.Errorf("vpc number in cluster[%s] is more than 1,  num is: %d", cluster, len(vpcs))
		}
		logrus.Errorf(e.Error())
		return libvpc.Vpc{},
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: e.Error()},
					},
				},
			}
	}
	return vpcs[0], nil
}

func (e *Endpoints) GetIdentity(r *http.Request) (apistructs.Identity, httpserver.Responser) {
	userid := r.Header.Get("User-ID")
	orgid := r.Header.Get("Org-ID")
	if userid == "" || orgid == "" {
		var e error
		if userid == "" {
			e = fmt.Errorf("failed to get user id in http header")
		} else {
			e = fmt.Errorf("failed to get org id in http header")
		}
		logrus.Errorf(e.Error())
		return apistructs.Identity{},
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: e.Error()},
					},
				},
			}
	}
	return apistructs.Identity{UserID: userid, OrgID: orgid}, nil
}

func (e *Endpoints) CreateAddonCheck(req apistructs.CreateCloudResourceBaseInfo) error {
	var err error
	if req.Source != apistructs.CloudResourceSourceResource && req.Source != apistructs.CloudResourceSourceAddon {
		err = fmt.Errorf("request failed, invalide param, source: %s, only support:[addon, resource)] ", req.Source)
	} else if req.Source == apistructs.CloudResourceSourceResource && req.Region == "" {
		err = fmt.Errorf("request come from [resource] failed, missing param: [region]")
	} else if req.Source == apistructs.CloudResourceSourceAddon && req.Region == "" && req.ClusterName == "" {
		err = fmt.Errorf("request come from [addon] failed, both region and clusterName is empty")
	}
	return err
}
