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

package vpc

import (
	"fmt"
	"strings"
	"sync"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	libvpc "github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

func List(ctx aliyun_resources.Context, page aliyun_resources.PageOption,
	regions []string,
	cluster string) ([]libvpc.Vpc, int, error) {
	vpcs := []libvpc.Vpc{}
	total := 0
	listSch := make(chan listS, 20)
	var wg sync.WaitGroup

	wg.Add(len(regions))
	for _, region := range regions {
		ctx.Region = region
		go func(ctx aliyun_resources.Context) {
			defer func() { wg.Done() }()
			listF(ctx, page, cluster, listSch)
		}(ctx)
	}
	wg.Wait()
	close(listSch)
	for s := range listSch {
		vpcs = append(vpcs, s.vpcs...)
		total += s.total
	}
	return vpcs, total, nil
}

type listS struct {
	vpcs  []libvpc.Vpc
	total int
}

func listF(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string, ch chan listS) {
	vpclist, err := ListByCluster(ctx, page, cluster)
	if err != nil {
		ch <- listS{}
		return
	}
	ch <- listS{vpcs: vpclist.Vpcs.Vpc, total: vpclist.TotalCount}
}

type VPCCreateRequest struct {
	CidrBlock   string
	Name        string
	Description string
}

func ListByCluster(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string) (
	*libvpc.DescribeVpcsResponse, error) {
	return DescribeVPCs(ctx, page, cluster, "")
}

func DescribeVPCs(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string, id string) (
	*libvpc.DescribeVpcsResponse, error) {
	// create client
	client, err := libvpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create vpc client error: %+v", err)
		return nil, err
	}

	// create request
	request := libvpc.CreateDescribeVpcsRequest()
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
	if cluster != "" {
		tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
		request.Tag = &[]libvpc.DescribeVpcsTag{{Key: tagKey, Value: tagValue}}
	}
	if id != "" {
		request.VpcId = id
	}

	// describe resource
	// status:
	//	Available
	//	Pending：Configuring
	response, err := client.DescribeVpcs(request)
	if err != nil {
		logrus.Errorf("describe vpc error: %+v", err)
		return nil, err
	}
	return response, nil
}

func Classify(vpcs []libvpc.Vpc) []apistructs.CloudResourceLabelCount {
	if len(vpcs) == 0 {
		return nil
	}
	var result []apistructs.CloudResourceLabelCount
	ls := set.New()
	clusterTags := make(map[string]int)
	// travel vpc and get each dice-cluster tag count
	for _, vpc := range vpcs {
		for _, v := range vpc.Tags.Tag {
			if strings.HasPrefix(v.Key, aliyun_resources.TagPrefixCluster) {
				if ls.Has(v.Key) {
					clusterTags[v.Key] += 1
				} else {
					ls.Insert(v.Key)
					clusterTags[v.Key] = 1
				}
			}
		}
	}
	for k, v := range clusterTags {
		result = append(result, apistructs.CloudResourceLabelCount{
			Label: k,
			Count: v,
		})
	}
	return result
}

func OverwriteTags(ctx aliyun_resources.Context, items []apistructs.CloudResourceTagItem, tags []string, resourceType aliyun_resources.TagResourceType) error {
	// remove old keys prefix with dice-cluster
	for _, v := range items {
		if err := UntagResource(ctx, v.ResourceID, v.OldTags, resourceType); err != nil {
			logrus.Errorf("failed to untag vpc, request: %+v, err: %v", v, err)
		}
	}

	// set new tags
	var instanceIDs []string
	for i := range items {
		instanceIDs = append(instanceIDs, items[i].ResourceID)
	}
	err := TagResource(ctx, instanceIDs, tags, resourceType)
	return err
}

func TagResource(ctx aliyun_resources.Context, resourceIDs []string, tags []string, resourceType aliyun_resources.TagResourceType) error {
	if len(resourceIDs) == 0 || len(tags) == 0 {
		logrus.Infof("empty resourceIDs or tags, ignore vpc tag")
		return nil
	}

	switch resourceType {
	case aliyun_resources.TagResourceTypeVpc, aliyun_resources.TagResourceTypeVsw, aliyun_resources.TagResourceTypeEip:
	default:
		err := fmt.Errorf("tag vpc related resource failed, support types:%v invalide resource type: %s",
			[]aliyun_resources.TagResourceType{aliyun_resources.TagResourceTypeVpc, aliyun_resources.TagResourceTypeVsw, aliyun_resources.TagResourceTypeEip}, resourceType)
		logrus.Errorf(err.Error())
		return err
	}

	client, err := libvpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return err
	}
	request := libvpc.CreateTagResourcesRequest()
	request.Scheme = "https"
	request.RegionId = ctx.Region
	request.ResourceType = resourceType.String()
	request.ResourceId = &resourceIDs

	var tagKV []libvpc.TagResourcesTag
	for _, tagkey := range tags {
		tagKV = append(tagKV, libvpc.TagResourcesTag{Key: tagkey, Value: "true"})
	}
	request.Tag = &tagKV

	if _, err := client.TagResources(request); err != nil {
		logrus.Errorf("tag vpc resource failed, resource ids: %+v, error: %+v", resourceIDs, err)
		return err
	}
	return nil
}

func UntagResource(ctx aliyun_resources.Context, resourceIDs string, keys []string, resourceType aliyun_resources.TagResourceType) error {
	if resourceIDs == "" || len(keys) == 0 {
		logrus.Infof("ignore vpc untag action, empty vpcid or untag keys")
		return nil
	}

	switch resourceType {
	case aliyun_resources.TagResourceTypeVpc, aliyun_resources.TagResourceTypeVsw, aliyun_resources.TagResourceTypeEip:
	default:
		err := fmt.Errorf("untag vpc related resource failed, support types:%v invalide resource type: %s",
			[]aliyun_resources.TagResourceType{aliyun_resources.TagResourceTypeVpc, aliyun_resources.TagResourceTypeVsw, aliyun_resources.TagResourceTypeEip}, resourceType)
		logrus.Errorf(err.Error())
		return err
	}

	client, err := libvpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return err
	}
	request := libvpc.CreateUnTagResourcesRequest()
	request.Scheme = "https"

	request.ResourceType = resourceType.String()
	request.ResourceId = &[]string{resourceIDs}
	request.TagKey = &keys

	if _, err := client.UnTagResources(request); err != nil {
		return err
	}
	return nil
}
