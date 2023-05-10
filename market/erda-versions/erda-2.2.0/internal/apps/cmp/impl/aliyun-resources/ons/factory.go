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
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
)

type OnsFactory struct {
	*resource_factory.BaseResourceFactory
}

func creator(ctx aliyun_resources.Context, m resource_factory.BaseResourceMaterial, r *dbclient.Record, d *apistructs.CreateCloudResourceRecord, v apistructs.CloudResourceVpcBaseInfo) (*apistructs.AddonConfigCallBackResponse, *dbclient.ResourceRouting, error) {
	var err error

	req, ok := m.(apistructs.CreateCloudResourceOnsRequest)
	if !ok {
		return nil, nil, errors.Errorf("convert material failed, material: %+v", m)
	}
	regionids := aliyun_resources.ActiveRegionIDs(ctx)
	list, err := List(ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
	if err != nil {
		err = errors.Wrap(err, "list redis failed")
		return nil, nil, err
	}
	for _, item := range list {
		if req.Name == item.InstanceName {
			err := errors.Errorf("ons instance already exist, name:%s", item.InstanceName)
			return nil, nil, err
		}
	}

	logrus.Infof("start to create ons instance, request: %+v", req)
	resp, err := CreateInstance(ctx, req)
	if err != nil {
		return nil, nil, errors.Wrap(err, "create ons instance failed")
	}
	d.InstanceID = resp.InstanceId

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
		InstanceID: resp.InstanceId,
		Topics:     req.Topics,
	}

	createTopicStep := apistructs.CreateCloudResourceStep{
		Step:   string(dbclient.RecordTypeCreateAliCloudOnsTopic),
		Status: string(dbclient.StatusTypeSuccess)}
	d.Steps = append(d.Steps, createTopicStep)

	onsInfo, err := GetInstanceDetailInfo(ctx, topicReq.InstanceID)
	if err != nil {
		return nil, nil, errors.Wrap(err, "get ons detail info failed")
	}
	logrus.Infof("create ons topic request: %+v", req)
	err = CreateTopic(ctx, topicReq)
	if err != nil {
		return nil, nil, errors.Wrap(err, "create ons topic failed")
	}

	if len(topicReq.Topics) == 0 {
		return nil, nil, nil
	}

	// create ons group
	t := topicReq.Topics[0]

	d.Steps[len(d.Steps)-1].Name = t.TopicName
	groupReq := apistructs.CreateCloudResourceOnsGroupRequest{
		InstanceID: topicReq.InstanceID,
		Groups: []apistructs.CloudResourceOnsGroupBaseInfo{{
			GroupType: t.GroupType,
			GroupId:   t.GroupId,
		}},
	}

	logrus.Infof("create ons group request:%+v", groupReq)
	err = CreateGroup(ctx, groupReq)
	if err != nil {
		return nil, nil, errors.Wrap(err, "create ons group failed")
	}
	if topicReq.Source != apistructs.CloudResourceSourceAddon {
		return nil, nil, nil
	}
	cbResp := &apistructs.AddonConfigCallBackResponse{
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
	routing := &dbclient.ResourceRouting{
		ResourceID:   topicReq.InstanceID,
		ResourceName: t.TopicName,
		ResourceType: dbclient.ResourceTypeOnsTopic,
		Vendor:       req.Vendor,
		OrgID:        req.OrgID,
		ClusterName:  req.ClusterName,
		ProjectID:    req.ProjectID,
		AddonID:      t.AddonID,
		Status:       dbclient.ResourceStatusAttached,
		RecordID:     r.ID,
	}
	return cbResp, routing, nil
}

func init() {
	factory := OnsFactory{BaseResourceFactory: &resource_factory.BaseResourceFactory{}}
	factory.Creator = creator
	factory.RecordType = dbclient.RecordTypeCreateAliCloudOns
	err := resource_factory.Register(dbclient.ResourceTypeOns, factory)
	if err != nil {
		panic(err)
	}
}
