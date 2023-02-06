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

package ack

import (
	"encoding/json"
	"fmt"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/cs"
	"github.com/sirupsen/logrus"

	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

type DescribeACKInstancesResponse struct {
	aliyun_resources.ResponsePager
	Instances []Instance
}
type Instance struct {
	Name           string     `json:"name"`
	ClusterID      string     `json:"cluster_id"`
	Size           int        `json:"size"`
	RegionID       string     `json:"region_id"`
	ClusterType    string     `json:"cluster_type"`
	CurrentVersion string     `json:"current_version"` //k8s version
	State          string     `json:"state"`
	Parameters     Parameters `json:"parameters"`
	Tags           []Tag      `json:"tags"`
}

type Parameters struct {
	MasterInstanceChargeType string `json:"MasterInstanceChargeType"`
	MasterInstanceType       string `json:"MasterInstanceType"`
	WorkerInstanceChargeType string `json:"WorkerInstanceChargeType"`
	WorkerInstanceType       string `json:"WorkerInstanceType"`
}

type Tag struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

func ListByCluster(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string) (*DescribeACKInstancesResponse, error) {
	// create client
	client, err := cs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create vpc client error: %+v", err)
		return nil, err
	}

	// create request
	request := cs.CreateDescribeClustersRequest()
	request.Scheme = "https"
	if page.PageNumber == nil || page.PageSize == nil || *page.PageSize <= 0 || *page.PageNumber <= 0 {
		err := fmt.Errorf("invalid page parameters: %+v", page)
		logrus.Errorf(err.Error())
		return nil, err
	}

	// describe resource
	// status:
	// 	running
	// 	stoped
	response, err := client.DescribeClusters(request)
	if err != nil {
		if response != nil && response.BaseResponse != nil && response.BaseResponse.GetHttpStatus() != 200 {
			logrus.Errorf("describe vpc failed, error: %+v", err)
			return nil, err
		}
	}
	if response == nil {
		err := fmt.Errorf("describe vpc failed, empty response")
		logrus.Errorf(err.Error())
		return nil, err

	}
	var resourceList []Instance
	b := response.GetHttpContentBytes()
	if err := json.Unmarshal(b, &resourceList); err != nil {
		logrus.Errorf("unmarshal ack response failed, error: %+v", err)
		return nil, err
	}

	var instances []Instance
	tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
	for _, resource := range resourceList {
		for _, kv := range resource.Tags {
			if kv.Key == tagKey && kv.Value == tagValue {
				instances = append(instances, resource)
				break
			}
		}
	}
	//// test
	//instances = append(instances, Instance{
	//	Name:           "terminus-dev-fake-ack",
	//	ClusterID:      "123456",
	//	Size:           11,
	//	RegionID:       "cn-hangzhou",
	//	ClusterType:    "Kubernetes",
	//	CurrentVersion: "1.10.4",
	//	State:          "running",
	//	Parameters: Parameters{
	//		MasterInstanceChargeType: "PrePaid",
	//		MasterInstanceType:       "ecs.r5.xlarge",
	//	},
	//	Tags: nil},
	//)
	return &DescribeACKInstancesResponse{
		ResponsePager: aliyun_resources.ResponsePager{
			TotalCount: len(instances),
			PageSize:   *page.PageSize,
			PageNumber: *page.PageNumber,
		},
		Instances: instances,
	}, nil
}

func TagResource(ctx aliyun_resources.Context, cluster string, resourceIDs []string) error {
	if len(resourceIDs) == 0 {
		return nil
	}

	for _, id := range resourceIDs {
		_ = TagOneResource(ctx, cluster, id)
	}

	return nil
}

func TagOneResource(ctx aliyun_resources.Context, cluster string, resourceID string) error {
	client, err := cs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create vpc client error: %+v", err)
		return err
	}

	request := requests.NewCommonRequest()
	request.Method = "POST"
	request.Scheme = "https"
	request.Domain = "cs.aliyuncs.com"
	request.Version = "2015-12-15"
	request.PathPattern = fmt.Sprintf("/clusters/%s/tags", resourceID)
	request.Headers["Content-Type"] = "application/json"
	request.QueryParams["RegionId"] = ctx.Region
	tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
	body := fmt.Sprintf(`[{"key":"%s","value":"%s"}]`, tagKey, tagValue)
	request.Content = []byte(body)

	_, err = client.ProcessCommonRequest(request)
	if err != nil {
		logrus.Errorf("tag ack resource failed, cluster: %s, resource id: %s, error: %+v", cluster, resourceID, err)
		return err
	}
	return nil
}
