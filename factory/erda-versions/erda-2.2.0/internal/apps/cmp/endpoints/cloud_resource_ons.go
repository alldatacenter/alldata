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
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ons"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

// TODO, now support process create addon failed but related cloud resource create successfully
func (e *Endpoints) DeleteOns(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
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

	var req apistructs.DeleteCloudResourceOnsRequest
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
		if r.Status == dbclient.StatusTypeFailed && r.RecordType == dbclient.RecordTypeCreateAliCloudOns {
			var detail apistructs.CreateCloudResourceRecord
			er := json.Unmarshal([]byte(r.Detail), &detail)
			if er != nil {
				err = fmt.Errorf("unmarshal record detail info failed, error:%v", err)
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

func (e *Endpoints) DeleteOnsTopic(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
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

	var req apistructs.DeleteCloudResourceOnsTopicRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal create ons instance request: %+v", err)
		return
	}

	logrus.Debugf("ons topic delete request:%+v", req)
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
			return
		}
		r := records[0]
		if r.Status == dbclient.StatusTypeFailed && r.RecordType == dbclient.RecordTypeCreateAliCloudOns {
			list, er := e.dbclient.ResourceRoutingReader().ByRecordIDs(req.RecordID).
				ByResourceTypes(dbclient.ResourceTypeOns.String()).Do()
			if er != nil {
				err = fmt.Errorf("check resource routing failed, error:%v", er)
				return
			}
			// Failed to create addon, but cloud resource create succeed
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

func (e *Endpoints) ListOns(ctx context.Context, r *http.Request, vars map[string]string) (
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

	var regionids []string
	if projid == "" {
		// request come from cloud resource
		regions := e.getAvailableRegions(ak_ctx, r)
		regionids = append(regionids, regions.ECS...)
	} else {
		// request come from addon
		clusterName, _, er := aliyun_resources.GetProjectClusterName(ak_ctx, projid, workspace)
		if er != nil {
			err = er
			return
		}

		v, er := vpc.GetVpcByCluster(ak_ctx, clusterName)
		if er != nil {
			err = er
			return
		}
		regionids = append(regionids, v.RegionId)
	}

	onsList, err := ons.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids, "")
	if err != nil {
		err = fmt.Errorf("failed to get ons list: %v", err)
		return
	}
	resultList := []apistructs.CloudResourceOnsBasicData{}
	for _, ins := range onsList {
		tags := map[string]string{}
		for _, tag := range ins.Tags.Tag {
			if strings.HasPrefix(tag.Key, aliyun_resources.TagPrefixProject) {
				tags[tag.Key] = tag.Value
			}
		}
		resultList = append(resultList, apistructs.CloudResourceOnsBasicData{
			Region:       ins.Region,
			ID:           ins.InstanceId,
			Name:         ins.InstanceName,
			InstanceType: i18n.Sprintf(ons.GetInstanceType(ins.InstanceType)),
			Status:       i18n.Sprintf(ons.GetInstanceStatus(ins.InstanceStatus)),
			Tags:         tags,
		})
	}
	return mkResponse(apistructs.ListCloudResourceOnsResponse{
		Header: apistructs.Header{Success: true},
		Data: apistructs.CloudResourceOnsData{
			Total: len(resultList),
			List:  resultList,
		},
	})
}

func (e *Endpoints) ListOnsGroup(ctx context.Context, r *http.Request, vars map[string]string) (
	resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.CloudResourceOnsGroupInfoResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
				Data: apistructs.CloudResourceOnsGroupInfoData{List: []apistructs.CloudResourceOnsGroupBasicData{}},
			})
		}
	}()

	var req apistructs.CloudResourceOnsGroupInfoRequest

	_ = ctx.Value("i18nPrinter").(*message.Printer)

	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	region := r.URL.Query().Get("region")
	instanceID := r.URL.Query().Get("instanceID")
	groupID := r.URL.Query().Get("groupID")
	groupType := r.URL.Query().Get("groupType")
	req.Region = region
	req.InstanceID = instanceID
	req.GroupType = groupType
	req.GroupID = groupID

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

	// get ak/sk info
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	rsp, er := ons.DescribeGroup(ak_ctx, req)
	if er != nil {
		err = fmt.Errorf("describe group failed, error:%v", e)
		return
	}
	resultList := []apistructs.CloudResourceOnsGroupBasicData{}
	for _, g := range rsp {
		tags := map[string]string{}
		for _, tag := range g.Tags.Tag {
			if strings.HasPrefix(tag.Key, aliyun_resources.TagPrefixProject) {
				tags[tag.Key] = tag.Value
			}
		}
		resultList = append(resultList, apistructs.CloudResourceOnsGroupBasicData{
			GroupId:    g.GroupId,
			Remark:     g.Remark,
			InstanceId: g.InstanceId,
			GroupType:  g.GroupType,
			Tags:       tags,
			// ms
			CreateTime: time.Unix(g.CreateTime/1000, 0).UTC().Format("2006-01-02T15:04:05Z"),
		})
	}
	resp, err = mkResponse(apistructs.CloudResourceOnsGroupInfoResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CloudResourceOnsGroupInfoData{
			Total: len(rsp),
			List:  resultList,
		},
	})
	return
}

func (e *Endpoints) ListOnsTopic(ctx context.Context, r *http.Request, vars map[string]string) (
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

	var req apistructs.CloudResourceOnsTopicInfoRequest
	i18n := ctx.Value("i18nPrinter").(*message.Printer)

	_ = strutil.Split(r.URL.Query().Get("vendor"), ",", true)
	region := r.URL.Query().Get("region")
	instanceID := r.URL.Query().Get("instanceID")
	topicName := r.URL.Query().Get("topicName")
	req.Region = region
	req.InstanceID = instanceID
	req.TopicName = topicName

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

	// get ak/sk info
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}
	ak_ctx.Region = req.Region

	rsp, er := ons.DescribeTopic(ak_ctx, req)
	if er != nil {
		err = fmt.Errorf("describe topic failed, error:%v", er)
		return
	}
	resultList := []apistructs.OnsTopic{}
	for _, t := range rsp {
		tags := map[string]string{}
		for _, tag := range t.Tags.Tag {
			if strings.HasPrefix(tag.Key, aliyun_resources.TagPrefixProject) {
				tags[tag.Key] = tag.Value
			}
		}
		msgType := ons.GetMsgType(t.MessageType)
		resultList = append(resultList, apistructs.OnsTopic{
			TopicName:    t.Topic,
			MessageType:  i18n.Sprintf(msgType),
			Relation:     t.Relation,
			RelationName: i18n.Sprintf(strings.ToLower(t.RelationName)),
			Remark:       t.Remark,
			Tags:         tags,
			// ms
			CreateTime: time.Unix(t.CreateTime/1000, 0).UTC().Format("2006-01-02T15:04:05Z"),
		})
	}
	resp, err = mkResponse(apistructs.CloudResourceOnsTopicInfoResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CloudResourceOnsTopicInfo{
			Total: len(rsp),
			List:  resultList,
		},
	})
	return
}

func (e *Endpoints) CreateOnsInstance(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
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

	req := apistructs.CreateCloudResourceOnsRequest{
		CreateCloudResourceBaseInfo: &apistructs.CreateCloudResourceBaseInfo{},
	}
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal create ons instance request: %+v", err)
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

	factory, err := resource_factory.GetResourceFactory(e.dbclient, dbclient.ResourceTypeOns)
	if err != nil {
		return
	}
	record, err := factory.CreateResource(ak_ctx, req)
	if err != nil {
		return
	}

	return mkResponse(apistructs.CreateCloudResourceOnsResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CreateCloudResourceBaseResponseData{RecordID: record.ID},
	})
}

func (e *Endpoints) CreateOnsTopic(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
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

	var req apistructs.CreateCloudResourceOnsTopicRequest
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

	// get cloud resource context: ak/sk...
	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get org ak, org id:%v", i.OrgID)
		return
	}

	req.UserID = i.UserID
	req.OrgID = i.OrgID

	// request check
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
		err = fmt.Errorf("create ons topic request invalid, error:%+v", err)
		return
	}

	record, rsp := e.InitRecord(dbclient.Record{
		RecordType:  dbclient.RecordTypeCreateAliCloudOnsTopic,
		UserID:      req.UserID,
		OrgID:       req.OrgID,
		ClusterName: req.ClusterName,
		Status:      dbclient.StatusTypeProcessing,
		Detail:      "",
		PipelineID:  0,
	})
	if rsp != nil {
		err = fmt.Errorf("init ops record failed, error:%+v", err)
		return
	}

	err = ons.CreateTopicWithRecord(ak_ctx, req, record, nil)
	if err != nil {
		return
	}

	resp, err = mkResponse(apistructs.CreateCloudResourceOnsTopicResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: apistructs.CreateCloudResourceBaseResponseData{RecordID: record.ID},
	})
	return
}

func (e *Endpoints) CreateOnsGroup(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
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

	var req apistructs.CreateCloudResourceOnsGroupRequest

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

	er := ons.CreateGroup(ak_ctx, req)
	if er != nil {
		err = fmt.Errorf("create ons group failed, error:%v", err)
		return
	}

	resp, err = mkResponse(apistructs.CreateCloudResourceOnsGroupResponse{
		Header: apistructs.Header{
			Success: true,
		},
	})
	return
}

func (e *Endpoints) CetOnsDetailInfo(ctx context.Context, r *http.Request, vars map[string]string) (
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
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return
	}

	region := r.URL.Query().Get("region")
	instanceID := vars["instanceID"]

	if region == "" {
		err = fmt.Errorf("get ons detail info faild, empty region")
		return
	}
	if instanceID == "" {
		err = fmt.Errorf("get ons detail info faild, empty instance id")
	}

	ak_ctx, resp := e.mkCtx(ctx, i.OrgID)
	if resp != nil {
		err = fmt.Errorf("failed to get access key from org: %v", i.OrgID)
		return
	}
	ak_ctx.Region = region

	res, err := ons.GetInstanceFullDetailInfo(ctx, ak_ctx, instanceID)
	if err != nil {
		err = errors.Wrapf(err, "failed to describe resource detail info")
		return
	}

	resp, err = mkResponse(apistructs.CloudResourceOnsDetailInfoResponse{
		Header: apistructs.Header{
			Success: true,
		},
		Data: res,
	})
	return
}
