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

package redis

import (
	"context"
	"fmt"
	"strconv"
	native_strings "strings"

	kvstore "github.com/aliyun/alibaba-cloud-sdk-go/services/r-kvstore"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vswitch"
)

func GetInstanceFullDetailInfo(c context.Context, ctx aliyun_resources.Context, instanceID string) ([]apistructs.CloudResourceDetailInfo, error) {
	i18n := c.Value("i18nPrinter").(*message.Printer)
	r1, err := DescribeResourceDetailInfo(ctx, instanceID)
	if err != nil {
		return nil, err
	}
	if len(r1.Instances.DBInstanceAttribute) == 0 {
		err := fmt.Errorf("get instance detail info failed, empty response")
		logrus.Errorf("%s, response:%+v", err.Error(), r1)
		return nil, err
	}
	content := r1.Instances.DBInstanceAttribute[0]

	var basicInfo []apistructs.CloudResourceDetailItem
	basicInfo = append(basicInfo,
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Instance ID"),
			Value: content.InstanceId,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Name"),
			Value: content.InstanceName,
		},
		apistructs.CloudResourceDetailItem{
			//TODO format
			Name:  i18n.Sprintf("Region and Zone"),
			Value: content.RegionId + " " + native_strings.ToLower(content.ZoneId[len(content.ZoneId)-1:]),
		},
		apistructs.CloudResourceDetailItem{
			//CLASSIC（classic network）
			//VPC（vpc network）
			Name:  i18n.Sprintf("Network Type"),
			Value: i18n.Sprintf(content.NetworkType) + " " + fmt.Sprintf("(%s)", content.VpcId),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Spec"),
			Value: i18n.Sprintf(content.InstanceClass),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("VSwitch"),
			Value: content.VSwitchId,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Version"),
			Value: content.EngineVersion,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("MaxConnections"),
			Value: strconv.Itoa(int(content.Connections)),
		},
	)

	var connectionInfo []apistructs.CloudResourceDetailItem
	publicHost := "--"
	publicPort := "--"
	netInfo, err := NetInfo(ctx, instanceID)
	// ignore netInfo error, partial error
	if err == nil {
		if netInfo.DBInstanceNetType == "0" {
			publicHost = netInfo.ConnectionString
			publicPort = netInfo.Port
		}
	}

	connectionInfo = append(connectionInfo,
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Private Host"),
			Value: i18n.Sprintf(content.ConnectionDomain),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Port"),
			Value: strconv.Itoa(int(content.Port)),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Public Host"),
			Value: publicHost,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Port"),
			Value: publicPort,
		},
	)

	var res []apistructs.CloudResourceDetailInfo
	res = append(res,
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Basic Information"),
			Items: basicInfo,
		},
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Connection Information"),
			Items: connectionInfo,
		},
	)
	return res, nil
}

func DescribeResourceDetailInfo(ctx aliyun_resources.Context, instanceID string) (*kvstore.DescribeInstanceAttributeResponse, error) {
	// create client
	client, err := kvstore.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create redis client error: %+v", err)
		return nil, err
	}

	// describe redis instance detail info
	request := kvstore.CreateDescribeInstanceAttributeRequest()
	request.Scheme = "https"

	request.InstanceId = instanceID

	response, err := client.DescribeInstanceAttribute(request)
	if err != nil {
		e := fmt.Errorf("describe redis instance attribute failed, error:%v", err)
		logrus.Errorf(e.Error())
		return nil, e
	}
	return response, nil
}

func NetInfo(ctx aliyun_resources.Context, instanceID string) (kvstore.InstanceNetInfo, error) {
	// create client
	client, err := kvstore.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create redis client error: %+v", err)
		return kvstore.InstanceNetInfo{}, err
	}
	request := kvstore.CreateDescribeDBInstanceNetInfoRequest()
	request.Scheme = "https"

	request.InstanceId = instanceID

	//DBInstanceNetType	 (The type of network to which the network information belongs)：
	//0（public network）
	//1（classic network）
	//2（vpc network)

	response, err := client.DescribeDBInstanceNetInfo(request)
	if err != nil {
		logrus.Errorf("describe instance net info failed, request:%+v, error:%v", instanceID, err)
		return kvstore.InstanceNetInfo{}, err
	}
	if response == nil || len(response.NetInfoItems.InstanceNetInfo) == 0 {
		err := fmt.Errorf("describe instance net info failed, empty response")
		logrus.Errorf("describe instance net info failed, request:%+v, error:%v", instanceID, err)
		return kvstore.InstanceNetInfo{}, err

	}
	return response.NetInfoItems.InstanceNetInfo[0], nil
}

func SupportZones(ctx aliyun_resources.Context) ([]string, error) {
	rsp, err := DescribeZones(ctx)
	if err != nil {
		logrus.Errorf("describe redis zone failed, error:%v", err)
		return nil, err
	}
	if rsp == nil {
		logrus.Warnf("describe redis zone returned empty response")
		return nil, err
	}
	var zones []string
	for _, i := range rsp.Zones.KVStoreZone {
		zones = append(zones, i.ZoneId)
	}
	return zones, nil
}

func DescribeZones(ctx aliyun_resources.Context) (*kvstore.DescribeZonesResponse, error) {
	// create client
	client, err := kvstore.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return nil, err
	}

	request := kvstore.CreateDescribeZonesRequest()
	request.Scheme = "https"

	response, err := client.DescribeZones(request)
	if err != nil {
		return nil, err
	}
	return response, nil
}

func GetAvailableVsw(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceBaseInfo) (*vpc.VSwitch, error) {
	zones, err := SupportZones(ctx)
	if err != nil {
		logrus.Errorf("get support zones failed, err:%v", err)
		return nil, err
	}

	s := set.New()
	for _, z := range zones {
		s.Insert(z)
	}
	// request zone is available
	if s.Has(req.ZoneID) {
		return &vpc.VSwitch{VSwitchId: req.VSwitchID, ZoneId: req.ZoneID}, nil
	}

	// request zone ont available,  try to find another
	vsws, _, err := vswitch.List(ctx, []string{ctx.Region})
	if err != nil {
		logrus.Errorf("list vsw failed, err:%v", err)
		return nil, err
	}
	for _, v := range vsws {
		if s.Has(v.ZoneId) {
			return &vpc.VSwitch{VSwitchId: v.VSwitchId, ZoneId: v.ZoneId}, nil
		}
	}
	err = fmt.Errorf("no available vswitch in zone %v under region %s, please crate related zone first", zones, ctx.Region)
	return nil, err
}
