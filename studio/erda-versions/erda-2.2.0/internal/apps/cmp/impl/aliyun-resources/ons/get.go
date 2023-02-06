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
	"context"
	"strconv"

	"github.com/aliyun/alibaba-cloud-sdk-go/services/ons"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

func GetInstanceDetailInfo(ctx aliyun_resources.Context, instanceID string) (ons.InstanceBaseInfo, error) {
	client, err := ons.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ons client error:%v", err)
		return ons.InstanceBaseInfo{}, err
	}

	request := ons.CreateOnsInstanceBaseInfoRequest()
	request.Scheme = "https"

	request.InstanceId = instanceID

	response, err := client.OnsInstanceBaseInfo(request)
	if err != nil {
		logrus.Errorf("get ons base info failed, error: %v", err)
		return ons.InstanceBaseInfo{}, err
	}
	return response.InstanceBaseInfo, nil
}

func GetInstanceFullDetailInfo(c context.Context, ctx aliyun_resources.Context, instanceID string) ([]apistructs.CloudResourceDetailInfo, error) {
	i18n := c.Value("i18nPrinter").(*message.Printer)
	content, err := GetInstanceDetailInfo(ctx, instanceID)
	if err != nil {
		return nil, err
	}

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
			Name:  i18n.Sprintf("IndependentNaming"),
			Value: i18n.Sprintf(strconv.FormatBool(content.IndependentNaming)),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Spec"),
			Value: i18n.Sprintf(GetInstanceType(content.InstanceType)),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Description"),
			Value: content.Remark,
		},
	)

	var tcpEndpoint []apistructs.CloudResourceDetailItem
	var httpEndpoint []apistructs.CloudResourceDetailItem

	tcpEndpoint = append(tcpEndpoint,
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Private Host"),
			Value: i18n.Sprintf(content.Endpoints.TcpEndpoint),
		},
	)

	httpEndpoint = append(httpEndpoint,
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Private Host"),
			Value: i18n.Sprintf(content.Endpoints.HttpInternalEndpoint),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Public Host"),
			Value: i18n.Sprintf(content.Endpoints.HttpInternetEndpoint),
		},
	)

	var res []apistructs.CloudResourceDetailInfo
	res = append(res,
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Basic Information"),
			Items: basicInfo,
		},
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Tcp Endpoint"),
			Items: tcpEndpoint,
		},
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Http Endpoint"),
			Items: httpEndpoint,
		},
	)
	return res, nil
}
