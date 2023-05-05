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
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vswitch"
	libzone "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/zone"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) CreateVSW(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return resp, nil
	}
	req := apistructs.CreateCloudResourceVSWRequest{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("failed to decode CreateCloudResourceVSWRequest: %v", err)
		return mkResponse(apistructs.CreateCloudResourceVSWResponse{
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
	vswid, err := vswitch.Create(ak_ctx, vswitch.VSwitchCreateRequest{
		RegionID:  req.Region,
		CidrBlock: req.CidrBlock,
		VpcID:     req.VPCID,
		ZoneID:    req.ZoneID,
		Name:      req.VSWName,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to create vswitch: %v", err)
		return mkResponse(apistructs.CreateCloudResourceVSWResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.CreateCloudResourceVSWResponse{
		Header: apistructs.Header{Success: true},
		Data:   apistructs.CreateCloudResourceVSW{VSWID: vswid},
	})
}

func (e *Endpoints) ListVSW(ctx context.Context, r *http.Request, vars map[string]string) (
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
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	// query by regions
	queryRegions := strutil.Split(r.URL.Query().Get("region"), ",", true)
	// query by vpc id
	vpcId := r.URL.Query().Get("vpcID")
	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		err = fmt.Errorf("failed to get access key from org: %v", orgid)
		return
	}

	ak_ctx.VpcID = vpcId
	regionids := e.getAvailableRegions(ak_ctx, r)
	var vsw_regions []string
	if len(queryRegions) > 0 {
		vsw_regions = queryRegions
	} else {
		vsw_regions = regionids.VPC
	}
	vsws, _, err := vswitch.List(ak_ctx, vsw_regions)
	if err != nil {
		err = fmt.Errorf("failed to get vswlist: %v", err)
		return
	}

	zones, err := libzone.List(ak_ctx, regionids.VPC)
	if err != nil {
		err = fmt.Errorf("failed to get zonelist: %v", err)
		return
	}
	zonemap := map[string]string{}
	for _, z := range zones {
		zonemap[z.ZoneId] = z.LocalName
	}

	resultlist := []apistructs.ListCloudResourceVSW{}
	for _, vsw := range vsws {
		tags := map[string]string{}
		// only show tags with prefix dice-cluster
		for _, tag := range vsw.Tags.Tag {
			if strings.HasPrefix(tag.Key, aliyun_resources.TagPrefixCluster) {
				tags[tag.Key] = tag.Value
			}
		}
		resultlist = append(resultlist, apistructs.ListCloudResourceVSW{
			VswName:   vsw.VSwitchName,
			VSwitchID: vsw.VSwitchId,
			CidrBlock: vsw.CidrBlock,
			VpcID:     vsw.VpcId,
			Status:    i18n.Sprintf(vsw.Status),
			Region:    vsw.Region,
			ZoneID:    vsw.ZoneId,
			ZoneName:  zonemap[vsw.ZoneId],
			Tags:      tags,
		})
	}
	return mkResponse(apistructs.ListCloudResourceVSWResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.ListCloudResourceVSWData{
			Total: len(resultlist),
			List:  resultlist,
		},
	})
}
