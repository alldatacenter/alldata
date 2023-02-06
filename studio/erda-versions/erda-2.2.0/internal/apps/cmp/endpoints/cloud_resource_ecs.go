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
	"strings"

	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ecs"
	libregion "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/region"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) ECSTrending(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return resp, nil
	}
	pagesize := 99999
	pageno := 1
	page := aliyun_resources.PageOption{PageSize: &pagesize, PageNumber: &pageno}
	regionids := e.getAvailableRegions(ak_ctx, r)
	ecsList, _, err := ecs.List(ak_ctx, page, regionids.ECS, "", nil)
	if err != nil {
		errstr := fmt.Sprintf("failed to get ecs trend: %v", err)
		return mkResponse(apistructs.GetCloudResourceECSTrendResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	trend, err := ecs.Trend(ecsList)
	if err != nil {
		errstr := fmt.Sprintf("failed to get ecs trend: %v", err)
		return mkResponse(apistructs.GetCloudResourceECSTrendResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.GetCloudResourceECSTrendResponse{
		Header: apistructs.Header{Success: true},
		Data:   *trend,
	})
}

func (e *Endpoints) ListECS(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	lang := r.Header.Get("Lang")
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	pageno, err := strconv.Atoi(r.URL.Query().Get("pageNo"))
	if err != nil {
		errstr := "illegal pageNo arg"
		return mkResponse(apistructs.ListCloudResourceECSResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
			Data: apistructs.ListCloudResourceECSData{List: []apistructs.ListCloudResourceECS{}},
		})
	}
	pagesize, err := strconv.Atoi(r.URL.Query().Get("pageSize"))
	if err != nil {
		errstr := "illegal pageSize arg"
		return mkResponse(apistructs.ListCloudResourceECSResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
			Data: apistructs.ListCloudResourceECSData{List: []apistructs.ListCloudResourceECS{}},
		})
	}
	page := aliyun_resources.PageOption{PageSize: &pagesize, PageNumber: &pageno}

	cluster := r.URL.Query().Get("cluster")
	IPs := strutil.Split(r.URL.Query().Get("innerIpAddress"), ",", true)
	orgid := r.Header.Get("Org-ID")
	ak_ctx, resp := e.mkCtx(ctx, orgid)
	if resp != nil {
		return resp, nil
	}
	regions, err := libregion.List(ak_ctx)
	if err != nil {
		errstr := fmt.Sprintf("failed to get regionlist: %v", err)
		return mkResponse(apistructs.ListCloudResourceECSResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
			Data: apistructs.ListCloudResourceECSData{List: []apistructs.ListCloudResourceECS{}},
		})
	}
	regionmap := map[string]string{}
	for _, r := range regions {
		regionmap[r.RegionId] = r.LocalName
	}

	regionids := e.getAvailableRegions(ak_ctx, r)
	ecsList, total, err := ecs.List(ak_ctx, page, regionids.ECS, cluster, IPs)
	if err != nil {
		errstr := fmt.Sprintf("failed to get ecslist: %v", err)
		return mkResponse(apistructs.ListCloudResourceECSResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
			Data: apistructs.ListCloudResourceECSData{List: []apistructs.ListCloudResourceECS{}},
		})
	}
	resultList := []apistructs.ListCloudResourceECS{}
	for _, ins := range ecsList {
		innerIP := ""
		if len(ins.VpcAttributes.PrivateIpAddress.IpAddress) > 0 {
			innerIP = ins.VpcAttributes.PrivateIpAddress.IpAddress[0]
		}
		tags := map[string]string{}
		for _, tag := range ins.Tags.Tag {
			if strings.HasPrefix(tag.TagKey, aliyun_resources.TagPrefixCluster) {
				tags[tag.TagKey] = tag.TagValue
			}

		}
		var osName string
		if strutil.Contains(lang, "zh") {
			osName = ins.OSName
		} else {
			osName = ins.OSNameEn
		}
		resultList = append(resultList, apistructs.ListCloudResourceECS{
			ID:             ins.InstanceId,
			StartTime:      ins.StartTime,
			RegionID:       ins.RegionId,
			RegionName:     regionmap[ins.RegionId],
			ChargeType:     ins.InstanceChargeType,
			Vendor:         "aliyun",
			InnerIpAddress: innerIP,
			HostName:       ins.HostName,
			Memory:         ins.Memory,
			ExpireTime:     ins.ExpiredTime,
			OsName:         osName,
			CPU:            ins.Cpu,
			Status:         i18n.Sprintf(ins.Status),
			Tag:            tags,
		})
	}
	return mkResponse(apistructs.ListCloudResourceECSResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.ListCloudResourceECSData{
			Total: total,
			List:  resultList,
		},
	})
}

func (e *Endpoints) StopECS(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	req := apistructs.HandleCloudResourceEcsRequest{}
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to decode StopCloudResourceEcsRequest: %v", err)
		return
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		return resp, nil
	}

	ak_ctx.Region = req.Region
	var failedInstance []apistructs.HandleCloudResourceECSDataResult
	response, err := ecs.Stop(ak_ctx, req.InstanceIds)
	if err != nil {
		err = fmt.Errorf("failed to stop instance: %v", err)
		return
	}

	for _, ins := range response.InstanceResponses.InstanceResponse {
		if ins.Code != "200" {
			failedInstance = append(failedInstance, apistructs.HandleCloudResourceECSDataResult{
				Message:    ins.Message,
				InstanceId: ins.InstanceId,
			})
		}
	}

	if len(failedInstance) != 0 {
		return mkResponse(apistructs.HandleCloudResourceECSResponse{
			Header: apistructs.Header{Success: false},
			Data: apistructs.HandleCloudResourceECSData{
				FailedInstances: failedInstance,
			},
		})
	}
	return mkResponse(apistructs.HandleCloudResourceECSResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) StartECS(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	req := apistructs.HandleCloudResourceEcsRequest{}
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to decode StartCloudResourceEcsRequest: %v", err)
		return
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		return resp, nil
	}

	ak_ctx.Region = req.Region
	var failedInstance []apistructs.HandleCloudResourceECSDataResult
	response, err := ecs.Start(ak_ctx, req.InstanceIds)
	if err != nil {
		errstr := fmt.Sprintf("failed to start instance: %v", err)
		return mkResponse(apistructs.HandleCloudResourceECSResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	for _, ins := range response.InstanceResponses.InstanceResponse {
		if ins.Code != "200" {
			failedInstance = append(failedInstance, apistructs.HandleCloudResourceECSDataResult{
				Message:    ins.Message,
				InstanceId: ins.InstanceId,
			})
		}
	}

	if len(failedInstance) != 0 {
		return mkResponse(apistructs.HandleCloudResourceECSResponse{
			Header: apistructs.Header{Success: false},
			Data: apistructs.HandleCloudResourceECSData{
				FailedInstances: failedInstance,
			},
		})
	}
	return mkResponse(apistructs.HandleCloudResourceECSResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) RestartECS(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	req := apistructs.HandleCloudResourceEcsRequest{}
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to decode RestartCloudResourceEcsRequest: %v", err)
		return
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		return resp, nil
	}

	ak_ctx.Region = req.Region
	var failedInstance []apistructs.HandleCloudResourceECSDataResult
	response, err := ecs.Restart(ak_ctx, req.InstanceIds)
	if err != nil {
		errstr := fmt.Sprintf("failed to restart instance: %v", err)
		return mkResponse(apistructs.HandleCloudResourceECSResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	for _, ins := range response.InstanceResponses.InstanceResponse {
		if ins.Code != "200" {
			failedInstance = append(failedInstance, apistructs.HandleCloudResourceECSDataResult{
				Message:    ins.Message,
				InstanceId: ins.InstanceId,
			})
		}
	}

	if len(failedInstance) != 0 {
		return mkResponse(apistructs.HandleCloudResourceECSResponse{
			Header: apistructs.Header{Success: false},
			Data: apistructs.HandleCloudResourceECSData{
				FailedInstances: failedInstance,
			},
		})
	}
	return mkResponse(apistructs.HandleCloudResourceECSResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) AutoRenewECS(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	req := apistructs.AutoRenewCloudResourceEcsRequest{}
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to decode StopCloudResourceEcsRequest: %v", err)
		return
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		return resp, nil
	}

	ak_ctx.Region = req.Region
	var failedInstance []apistructs.HandleCloudResourceECSDataResult
	for _, ins := range req.InstanceIds {
		response, err := ecs.AutoRenew(ak_ctx, ins, req.Duration, req.Switch)
		if response.GetHttpStatus() != 200 {
			errstr := fmt.Sprintf("failed to renew instance: %v", err)
			failedInstance = append(failedInstance, apistructs.HandleCloudResourceECSDataResult{
				Message:    errstr,
				InstanceId: ins,
			})
		}
	}

	if len(failedInstance) != 0 {
		return mkResponse(apistructs.HandleCloudResourceECSResponse{
			Header: apistructs.Header{Success: false},
			Data: apistructs.HandleCloudResourceECSData{
				FailedInstances: failedInstance,
			},
		})
	}
	return mkResponse(apistructs.HandleCloudResourceECSResponse{
		Header: apistructs.Header{Success: true},
	})
}
