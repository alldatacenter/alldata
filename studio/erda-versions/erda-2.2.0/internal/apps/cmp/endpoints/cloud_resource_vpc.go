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
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	libregion "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/region"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) CreateVPC(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	req := apistructs.CreateCloudResourceVPCRequest{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("failed to decode CreateCloudResourceVPCRequest: %v", err)
		return mkResponse(apistructs.CreateCloudResourceVPCResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err := fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return errorresp.ErrResp(err)
	}

	// permission check
	err := e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return resp, nil
	}
	ak_ctx.Region = req.Region
	vpcid, err := vpc.Create(ak_ctx, vpc.VPCCreateRequest{
		CidrBlock: req.CidrBlock,
		Name:      req.VPCName,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to create vpc: %v", err)
		return mkResponse(apistructs.CreateCloudResourceVPCResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.CreateCloudResourceVPCResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.CreateCloudResourceVPC{
			VPCID: vpcid,
		},
	})
}

func (e *Endpoints) ListVPC(ctx context.Context, r *http.Request, vars map[string]string) (
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

	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	// query vpc by vendor
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	// query vpc by region
	queryRegions := strutil.Split(r.URL.Query().Get("region"), ",", true)
	// query vpc by cluster
	cluster := r.URL.Query().Get("cluster")
	orgid := r.Header.Get("Org-ID")

	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		err = fmt.Errorf("failed to get access key from org: %v", orgid)
		return
	}

	regionids := e.getAvailableRegions(ak_ctx, r)

	var vpcRegions []string
	if len(queryRegions) > 0 {
		vpcRegions = queryRegions
	} else {
		vpcRegions = regionids.VPC
	}
	logrus.Infof("vpc regions: %v", vpcRegions)
	vpcs, _, err := vpc.List(ak_ctx, aliyun_resources.DefaultPageOption, vpcRegions, cluster)
	if err != nil {
		err = fmt.Errorf("failed to get vpclist: %v", err)
		return
	}
	regions, err := libregion.List(ak_ctx)
	if err != nil {
		err = fmt.Errorf("failed to get regionlist: %v", err)
		return
	}
	regionmap := map[string]string{}
	for _, r := range regions {
		regionmap[r.RegionId] = r.LocalName
	}
	resultlist := []apistructs.ListCloudResourceVPC{}
	// only set/unset/filter tag with dice-cluster or dice-project prefix
	for _, v := range vpcs {
		tags := map[string]string{}
		for _, tag := range v.Tags.Tag {
			if strings.HasPrefix(tag.Key, aliyun_resources.TagPrefixCluster) {
				tags[tag.Key] = tag.Value
			}
		}
		resultlist = append(resultlist, apistructs.ListCloudResourceVPC{
			Vendor:     "aliyun",
			RegionID:   v.RegionId,
			RegionName: regionmap[v.RegionId],
			VpcID:      v.VpcId,
			VpcName:    v.VpcName,
			CidrBlock:  v.CidrBlock,
			Status:     i18n.Sprintf(v.Status),
			VswNum:     len(v.VSwitchIds.VSwitchId),
			Tags:       tags,
		})
	}

	resp, err = mkResponse(apistructs.ListCloudResourceVPCResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.ListCloudResourceVPCData{
			Total: len(resultlist),
			List:  resultlist,
		},
	})
	return
}

func (e *Endpoints) VPCTagCluster(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return resp, nil
	}
	req := apistructs.TagCloudResourceVPCRequest{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("failed to decode TagCloudResourceVPCRequest: %v", err)
		return mkResponse(apistructs.TagCloudResourceVPCResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err := fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return errorresp.ErrResp(err)
	}

	// permission check
	err := e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	ak_ctx.Region = req.Region
	if err := vpc.TagResource(ak_ctx, req.Cluster, req.VPCIDs, aliyun_resources.TagResourceTypeVpc); err != nil {
		errstr := fmt.Sprintf("failed to tag cluster on vpc: %v", err)
		return mkResponse(apistructs.TagCloudResourceVPCResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.TagCloudResourceVPCResponse{
		Header: apistructs.Header{Success: true},
	})
}
