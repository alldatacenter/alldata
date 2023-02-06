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
	"io/ioutil"
	"net/http"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/redis"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) ListRedis(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {

	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceRedisResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
				Data: apistructs.ListCloudResourceRedisData{List: []apistructs.CloudResourceRedisBasicData{}},
			})
		}
	}()

	i18n := ctx.Value("i18nPrinter").(*message.Printer)
	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.Wrapf(err, "failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = errors.Wrapf(err, "failed to get ak of org: %s", i.OrgID)
		return
	}

	// come from cloud resource, get from cloud online
	regionids := e.getAvailableRegions(ak_ctx, r)
	list, err := redis.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
	if err != nil {
		err = errors.Wrapf(err, "failed to get redis list")
		return
	}
	resultList := []apistructs.CloudResourceRedisBasicData{}
	for _, ins := range list {
		tags := map[string]string{}
		for _, tag := range ins.Tags.Tag {
			if strings.HasPrefix(tag.Key, aliyun_resources.TagPrefixProject) {
				tags[tag.Key] = tag.Value
			}
		}
		resultList = append(resultList, apistructs.CloudResourceRedisBasicData{
			ID:         ins.InstanceId,
			Name:       ins.InstanceName,
			Region:     ins.RegionId,
			Spec:       i18n.Sprintf(ins.InstanceClass),
			Version:    ins.EngineVersion,
			Capacity:   strconv.FormatInt(ins.Capacity, 10) + " MB",
			Status:     i18n.Sprintf(ins.InstanceStatus),
			Tags:       tags,
			ChargeType: ins.ChargeType,
			ExpireTime: ins.EndTime,
			CreateTime: ins.CreateTime,
		})
	}
	resp, err = mkResponse(apistructs.ListCloudResourceRedisResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.ListCloudResourceRedisData{
			Total: len(resultList),
			List:  resultList,
		},
	})
	return
}

func (e *Endpoints) CetRedisDetailInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.ListCloudResourceRedisResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: errors.Cause(err).Error()},
				},
				Data: apistructs.ListCloudResourceRedisData{List: []apistructs.CloudResourceRedisBasicData{}},
			})
		}
	}()

	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = errors.Wrapf(err, "failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}
	region := r.URL.Query().Get("region")
	instanceID := vars["instanceID"]

	if region == "" {
		err = fmt.Errorf("get redis detail info faild, empty region")
		return
	}
	if instanceID == "" {
		err = fmt.Errorf("get redis detail info faild, empty ")
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get access key from org: %v", i.OrgID)
		return
	}
	ak_ctx.Region = region

	res, err := redis.GetInstanceFullDetailInfo(ctx, ak_ctx, instanceID)
	if err != nil {
		err = errors.Wrapf(err, "failed to describe resource detail info")
		return
	}

	resp, err = mkResponse(apistructs.CloudResourceRedisDetailInfoResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: res,
	})
	return
}

func (e *Endpoints) CreateRedis(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	req := apistructs.CreateCloudResourceRedisRequest{
		CreateCloudResourceBaseRequest: &apistructs.CreateCloudResourceBaseRequest{
			CreateCloudResourceBaseInfo: &apistructs.CreateCloudResourceBaseInfo{},
		},
	}
	if req.Vendor == "" {
		req.Vendor = aliyun_resources.CloudVendorAliCloud.String()
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		err := fmt.Errorf("failed to unmarshal create redis request: %v", err)
		content, _ := ioutil.ReadAll(r.Body)
		logrus.Errorf("%s, request:%v", err.Error(), content)
		return mkResponse(apistructs.CreateCloudResourceRedisResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return resp, nil
	}
	// permission check
	err := e.PermissionCheck(i.UserID, i.OrgID, req.ProjectID, apistructs.CreateAction)
	if err != nil {
		return mkResponse(apistructs.CreateCloudResourceRedisResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}
	req.UserID = i.UserID
	req.OrgID = i.OrgID

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		return resp, nil
	}

	factory, err := resource_factory.GetResourceFactory(e.dbclient, dbclient.ResourceTypeRedis)
	if err != nil {
		return mkResponse(apistructs.CreateCloudResourceMysqlResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}
	record, err := factory.CreateResource(ak_ctx, req)
	if err != nil {
		return mkResponse(apistructs.CreateCloudResourceMysqlResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	return mkResponse(apistructs.CreateCloudResourceRedisResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CreateCloudResourceBaseResponseData{RecordID: record.ID},
	})
}

func (e *Endpoints) DeleteRedisResource(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
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

	var req apistructs.DeleteCloudResourceRedisRequest
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
		if r.Status == dbclient.StatusTypeFailed && r.RecordType == dbclient.RecordTypeCreateAliCloudRedis {
			var detail apistructs.CreateCloudResourceRecord
			er := json.Unmarshal([]byte(r.Detail), &detail)
			if er != nil {
				err = fmt.Errorf("unmarshl record detail info failed, error:%v", er)
				return
			}
			// Failed to create addon, but cloud resource create succeed
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
