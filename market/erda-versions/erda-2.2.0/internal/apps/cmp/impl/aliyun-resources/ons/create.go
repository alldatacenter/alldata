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

package ons

import (
	"encoding/json"
	"fmt"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/ons"
	libvpc "github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
)

func CreateInstanceWithRecord(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceOnsRequest,
	record *dbclient.Record) {
	// create instance
	var detail apistructs.CreateCloudResourceRecord
	createInstanceStep := apistructs.CreateCloudResourceStep{
		Step:   string(dbclient.RecordTypeCreateAliCloudOns),
		Status: string(dbclient.StatusTypeSuccess)}
	detail.Steps = append(detail.Steps, createInstanceStep)
	detail.Steps[len(detail.Steps)-1].Name = req.Name
	detail.ClientToken = req.ClientToken
	detail.InstanceName = req.Name

	// Duplicate name check
	regionids := aliyun_resources.ActiveRegionIDs(ctx)
	list, err := List(ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
	if err != nil {
		err := fmt.Errorf("list ons failed, error:%v", err)
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
		return
	}
	for _, m := range list {
		if req.Name == m.InstanceName {
			err := fmt.Errorf("ons instance already exist, name:%s", m.InstanceName)
			logrus.Errorf("%s, request:%+v", err.Error(), req)
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}
	}

	// get region info by cluster name if not provide
	if req.Region == "" {
		v, err := vpc.GetVpcByCluster(ctx, req.ClusterName)
		if err != nil {
			err := fmt.Errorf("get region info failed, error:%v", err)
			logrus.Errorf("%s, cluster:%+v", err.Error(), req.ClusterName)
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}
		req.Region = v.RegionId
	}
	ctx.Region = req.Region

	logrus.Debugf("create ons instance request:%v", req)
	r, err := CreateInstance(ctx, req)
	if err != nil {
		err := fmt.Errorf("create instance failed, error:%v", err)
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
		return
	}
	detail.InstanceID = r.InstanceId

	// create topic
	topicReq := apistructs.CreateCloudResourceOnsTopicRequest{
		CreateCloudResourceBaseInfo: apistructs.CreateCloudResourceBaseInfo{
			Vendor:      req.Vendor,
			Region:      req.Region,
			VpcID:       req.VpcID,
			VSwitchID:   req.VSwitchID,
			ZoneID:      req.ZoneID,
			OrgID:       req.OrgID,
			UserID:      req.UserID,
			ClusterName: req.ClusterName,
			ProjectID:   req.ProjectID,
			Source:      req.Source,
			ClientToken: req.ClientToken,
		},
		InstanceID: r.InstanceId,
		Topics:     req.Topics,
	}
	_ = CreateTopicWithRecord(ctx, topicReq, record, &detail)
}

func CreateTopicWithRecord(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceOnsTopicRequest,
	record *dbclient.Record, detail *apistructs.CreateCloudResourceRecord) (err error) {

	defer func() {
		if err != nil {
			logrus.Errorf("create ons topic failed, request:%+v, error:%+v", req, err)
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, detail, err)
		}
	}()

	if detail == nil {
		detail = &apistructs.CreateCloudResourceRecord{InstanceID: req.InstanceID}
		detail.InstanceID = req.InstanceID
	}
	// create ons topic
	createTopicStep := apistructs.CreateCloudResourceStep{
		Step:   string(dbclient.RecordTypeCreateAliCloudOnsTopic),
		Status: string(dbclient.StatusTypeSuccess)}
	detail.Steps = append(detail.Steps, createTopicStep)

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

	onsInfo, err := GetInstanceDetailInfo(ctx, req.InstanceID)
	if err != nil {
		err = fmt.Errorf("get ons detail info failed, error:%v", err)
		return
	}

	logrus.Debugf("create ons topic request: %+v", req)
	err = CreateTopic(ctx, req)
	if err != nil {
		err = fmt.Errorf("create ons topic failed, error:%v", err)
		return
	}

	// create ons group
	for _, t := range req.Topics {
		// only create topic
		if t.GroupId == "" {
			continue
		}
		// create both topic & group
		detail.Steps[len(detail.Steps)-1].Name = t.TopicName
		groupReq := apistructs.CreateCloudResourceOnsGroupRequest{
			InstanceID: req.InstanceID,
			Groups: []apistructs.CloudResourceOnsGroupBaseInfo{
				{
					GroupType: t.GroupType,
					GroupId:   t.GroupId,
				},
			},
		}

		logrus.Debugf("create ons group request:%+v", groupReq)
		err = CreateGroup(ctx, groupReq)
		if err != nil {
			err = fmt.Errorf("create ons group failed, error:%v", err)
			return
		}
		if req.Source == apistructs.CloudResourceSourceAddon {
			cb := apistructs.AddonConfigCallBackResponse{
				Config: []apistructs.AddonConfigCallBackItemResponse{
					{
						Name:  "ONS_ACCESSKEY",
						Value: ctx.AccessKeyID,
					},
					{
						Name:  "ONS_SECRETKEY",
						Value: ctx.AccessSecret,
					},
					{
						Name: "ONS_NAMESERVER",
						// default, use tcp endpoint
						Value: onsInfo.Endpoints.TcpEndpoint,
					},
					{
						Name:  "ONS_PRODUCERID",
						Value: t.GroupId,
					},
					{
						Name:  "ONS_TOPIC",
						Value: t.TopicName,
					},
				},
			}

			// TODO: only support one addon in a request
			if t.AddonID == "" {
				t.AddonID = req.ClientToken
			}

			logrus.Debugf("start addon config callback, addonid: %s", t.AddonID)
			_, err = ctx.Bdl.AddonConfigCallback(t.AddonID, cb)
			if err != nil {
				err = fmt.Errorf("addon config call back failed, error: %v", err)
				return
			}

			_, err = ctx.Bdl.AddonConfigCallbackProvison(t.AddonID, apistructs.AddonCreateCallBackResponse{IsSuccess: true})
			if err != nil {
				err = fmt.Errorf("add call back provision failed, error:%v", err)
				return
			}

			// create resource routing record
			_, err = ctx.DB.ResourceRoutingWriter().Create(&dbclient.ResourceRouting{
				ResourceID:   req.InstanceID,
				ResourceName: t.TopicName,
				ResourceType: dbclient.ResourceTypeOnsTopic,
				Vendor:       req.Vendor,
				OrgID:        req.OrgID,
				ClusterName:  req.ClusterName,
				ProjectID:    req.ProjectID,
				AddonID:      t.AddonID,
				Status:       dbclient.ResourceStatusAttached,
				RecordID:     record.ID,
				Detail:       "",
			})
			if err != nil {
				err = fmt.Errorf("write resource routing to db failed, error:%v", err)
				return
			}
		}
	}

	// success, update ops record
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

func CreateInstance(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceOnsRequest) (data ons.Data, err error) {
	client, err := ons.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return ons.Data{}, err
	}

	request := ons.CreateOnsInstanceCreateRequest()
	request.Scheme = "https"
	request.InstanceName = req.Name
	if req.Remark != "" {
		request.Remark = req.Remark
	}

	response, err := client.OnsInstanceCreate(request)
	if err != nil {
		return ons.Data{}, err
	}
	return response.Data, nil
}

func CreateTopic(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceOnsTopicRequest) error {
	if len(req.Topics) == 0 {
		logrus.Infof("no topic in request, request: %v", req)
		return nil
	}

	client, err := ons.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return err
	}

	request := ons.CreateOnsTopicCreateRequest()
	request.Scheme = "https"
	request.InstanceId = req.InstanceID

	var success []string

	for _, t := range req.Topics {
		request.MessageType = requests.NewInteger(t.MessageType)
		request.Topic = t.TopicName
		request.Remark = t.Remark
		_, err := client.OnsTopicCreate(request)
		if err != nil {
			logrus.Errorf("create topic failed, request: %+v, failed one:%s, success: %v, error:%v",
				req, t.TopicName, success, err)
			return err
		}
		success = append(success, t.TopicName)
	}
	return nil
}

func CreateGroup(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceOnsGroupRequest) error {
	client, err := ons.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ons client failed, error: %+v", err)
		return err
	}

	request := ons.CreateOnsGroupCreateRequest()
	request.Scheme = "https"

	request.InstanceId = req.InstanceID

	var success []string

	for _, g := range req.Groups {
		request.GroupId = g.GroupId
		request.Remark = g.Remark
		if g.GroupType == "" {
			request.GroupType = "tcp"
		} else {
			request.GroupType = g.GroupType
		}

		_, err = client.OnsGroupCreate(request)
		if err != nil {
			logrus.Errorf("create ons group failed, request:%+v, failed one:%s, success:%+v, error: %v",
				req, g.GroupId, success, err)
			return err
		}
		success = append(success, g.GroupId)
	}
	return nil
}
