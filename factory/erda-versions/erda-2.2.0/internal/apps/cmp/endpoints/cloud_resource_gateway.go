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
	"net/http"
	"net/url"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	_ "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/cloudapi"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (e *Endpoints) CreateGatewayVpcGrant(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happend: %+v", err)
			resp, err = mkResponse(apistructs.CreateCloudResourceGatewayResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()
	req := apistructs.ApiGatewayVpcGrantRequest{
		CreateCloudResourceBaseInfo: &apistructs.CreateCloudResourceBaseInfo{},
	}
	if req.Vendor == "" {
		req.Vendor = aliyun_resources.CloudVendorAliCloud.String()
	}
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		err = errors.Wrapf(err, "failed to unamshal create cloud gateway req: %s", r.Body)
		return
	}
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

	gatewayMap, slbMap, err := e.getGatewayNameMap(ctx, req.ClusterName)
	if err != nil {
		return
	}
	if req.ID == "" {
		if _, ok := gatewayMap[req.Name]; ok {
			err = errors.Errorf("gateway instance name %s already exists", req.Name)
			return
		}
	}
	if req.Slb.ID == "" {
		if _, ok := slbMap[req.Slb.Name]; ok && req.Slb.ID == "" {
			err = errors.Errorf("slb instance name %s already exists", req.Slb.Name)
			return
		}
	}
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		return
	}
	factory, err := resource_factory.GetResourceFactory(e.dbclient, dbclient.ResourceTypeGateway)
	if err != nil {
		return
	}
	record, err := factory.CreateResource(ak_ctx, req)
	if err != nil {
		return
	}
	resp, err = mkResponse(apistructs.CreateCloudResourceGatewayResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CreateCloudResourceBaseResponseData{RecordID: record.ID},
	})
	return
}

func (e *Endpoints) ListGatewayAndVpc(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	projectId := r.URL.Query().Get("projectID")
	workspace := r.URL.Query().Get("workspace")
	defer func() {
		if err != nil {
			logrus.Errorf("error happend: %+v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceGatewayResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
			})
		}
	}()
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.New("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, projectId, apistructs.GetAction)
	if err != nil {
		return
	}

	pId, err := strconv.ParseUint(projectId, 10, 64)
	if err != nil {
		err = errors.WithStack(err)
		return
	}
	projectInfo, err := e.bdl.GetProjectWithSetter(pId, httpclient.SetParams(url.Values{"withQuota": {"true"}}))
	if err != nil {
		err = errors.WithStack(err)
		return
	}
	clusterName, ok := projectInfo.ClusterConfig[workspace]
	if !ok {
		err = errors.Errorf("workspace can not found cluster, projectInfo:%+v", projectInfo)
		return
	}
	gatewayMap, slbMap, err := e.getGatewayNameMap(ctx, clusterName)
	if err != nil {
		return
	}

	var gateways []apistructs.ApiGatewayInfo
	var slbs []apistructs.PrivateSlbInfo

	for name, id := range gatewayMap {
		gateways = append(gateways, apistructs.ApiGatewayInfo{
			ID:   id,
			Name: name,
		})
	}
	for name, id := range slbMap {
		slbs = append(slbs, apistructs.PrivateSlbInfo{
			ID:   id,
			Name: name,
		})
	}

	resp, err = mkResponse(apistructs.ListCloudResourceGatewayResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.ListCloudGateway{
			Slbs:     slbs,
			Gateways: gateways,
		},
	})
	return
}

func (e *Endpoints) getGatewayNameMap(ctx context.Context, clusterName string) (gateway map[string]string, slb map[string]string, err error) {
	resources, err := e.dbclient.ResourceRoutingReader().ByResourceTypes(dbclient.ResourceTypeGateway.String()).ByClusterName(clusterName).Do()
	if err != nil {
		err = errors.WithStack(err)
		return
	}
	gateway, slb = map[string]string{}, map[string]string{}
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	gateway[i18n.Sprintf("Shared Instance")] = "api-shared-vpc-001"
	for _, resource := range resources {
		idPair := strings.Split(resource.ResourceID, ":")
		namePair := strings.Split(resource.ResourceName, ":")
		if len(idPair) != 2 || len(namePair) != 2 {
			continue
		}
		gateway[namePair[0]] = idPair[0]
		slb[namePair[1]] = idPair[1]
	}
	if i18n.Sprintf("Shared Instance") != "Shared Instance" {
		delete(gateway, "Shared Instance")
	}
	return
}

func (e *Endpoints) DeleteGateway(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	return mkResponse(apistructs.CloudAddonResourceDeleteRespnse{
		Header: apistructs.Header{Success: true},
	})
}
