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

package eip

import (
	"fmt"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/appscode/go/strings"
	"github.com/sirupsen/logrus"

	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

func ListByCluster(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string) (*vpc.DescribeEipAddressesResponse, error) {
	if strings.IsEmpty(&cluster) {
		err := fmt.Errorf("empty cluster name")
		logrus.Errorf(err.Error())
		return nil, err
	}

	response, err := DescribeResource(ctx, page, cluster, "", "")
	if err != nil {
		logrus.Errorf("describe ecs resource failed, cluster: %s, error: %+v", cluster, err)
	}

	return response, nil
}

func DescribeResource(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string,
	associatedInstanceType string, associatedInstanceID string) (*vpc.DescribeEipAddressesResponse, error) {
	// create client
	client, err := vpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create eip client error: %+v", err)
		return nil, err
	}

	// create request
	request := vpc.CreateDescribeEipAddressesRequest()
	request.Scheme = "https"
	if page.PageNumber == nil || page.PageSize == nil || *page.PageSize <= 0 || *page.PageNumber <= 0 {
		err := fmt.Errorf("invalid page parameters: %+v", page)
		logrus.Errorf(err.Error())
		return nil, err
	}
	if page.PageSize != nil {
		request.PageSize = requests.NewInteger(*page.PageSize)
	}
	if page.PageNumber != nil {
		request.PageNumber = requests.NewInteger(*page.PageNumber)
	}
	request.RegionId = ctx.Region
	if !strings.IsEmpty(&cluster) {
		tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
		request.Tag = &[]vpc.DescribeEipAddressesTag{{Key: tagKey, Value: tagValue}}
	}
	if !strings.IsEmpty(&associatedInstanceType) && !strings.IsEmpty(&associatedInstanceID) {
		request.AssociatedInstanceType = associatedInstanceType
		request.AssociatedInstanceId = associatedInstanceID
	}

	// describe resource
	// status:
	//	Associating
	//	Unassociating
	//	InUse
	//	Available
	response, err := client.DescribeEipAddresses(request)
	if err != nil {
		logrus.Errorf("describe eip error: %+v", err)
		return nil, err
	}
	return response, nil
}

func GetEipIDByNat(ctx aliyun_resources.Context, page aliyun_resources.PageOption, natGatewayIds []string) ([]string, error) {
	if len(natGatewayIds) == 0 {
		err := fmt.Errorf("get eip id by nat failed, empty nat gateway ids")
		logrus.Errorf(err.Error())
		return nil, err
	}
	var eipIDs []string
	for _, id := range natGatewayIds {
		if id == "" {
			continue
		}
		response, err := DescribeResource(ctx, page, "", "Nat", id)
		if err != nil {
			logrus.Errorf("describe nat failed, id: %s, error: %+v", id, err)
		} else {
			for _, e := range response.EipAddresses.EipAddress {
				eipIDs = append(eipIDs, e.AllocationId)
			}
		}
	}
	return eipIDs, nil
}

func GetEipIDBySlb(ctx aliyun_resources.Context, page aliyun_resources.PageOption, slbIds []string) ([]string, error) {
	if len(slbIds) == 0 {
		err := fmt.Errorf("get eip id by nat failed, empty nat gateway ids")
		logrus.Errorf(err.Error())
		return nil, err
	}
	var eipIDs []string
	for _, id := range slbIds {
		if id == "" {
			continue
		}
		response, err := DescribeResource(ctx, page, "", "SlbInstance", id)
		if err != nil {
			logrus.Errorf("describe nat failed, id: %s, error: %+v", id, err)
		} else {
			for _, e := range response.EipAddresses.EipAddress {
				eipIDs = append(eipIDs, e.AllocationId)
			}
		}
	}
	return eipIDs, nil
}

func TagResource(ctx aliyun_resources.Context, cluster string, resourceIDs []string) error {
	if len(resourceIDs) == 0 {
		return nil
	}

	client, err := vpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create eip client error: %+v", err)
		return err
	}

	request := vpc.CreateTagResourcesRequest()
	request.Scheme = "https"

	request.RegionId = ctx.Region
	// resource id (eip)
	request.ResourceId = &resourceIDs
	request.ResourceType = "EIP"
	tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
	request.Tag = &[]vpc.TagResourcesTag{{Key: tagKey, Value: tagValue}}

	logrus.Debugf("eip tag request: %+v", request)

	_, err = client.TagResources(request)
	if err != nil {
		logrus.Errorf("tag eip resource failed, cluster: %s, resource ids: %+v, error: %+v", cluster, resourceIDs, err)
		return err
	}
	return nil
}
