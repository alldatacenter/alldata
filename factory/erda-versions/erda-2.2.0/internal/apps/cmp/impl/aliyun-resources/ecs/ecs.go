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

package ecs

import (
	"fmt"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/ecs"
	"github.com/appscode/go/strings"
	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/pkg/strutil"
)

func List(ctx aliyun_resources.Context, page aliyun_resources.PageOption,
	regions []string,
	cluster string,
	IPs []string) ([]ecs.Instance, int, error) {
	instances := []ecs.Instance{}
	pagesize := 100
	for _, region := range regions {
		ctx.Region = region
		pagenum := 1
		max := 1000
		for ((pagenum - 1) * pagesize) < max {
			rsp, err := DescribeResource(ctx,
				aliyun_resources.PageOption{
					PageSize:   &pagesize,
					PageNumber: &pagenum,
				}, cluster, "", IPs)
			if err != nil {
				return nil, 0, err
			}
			instances = append(instances, rsp.Instances.Instance...)
			if len(instances) >= ((*page.PageNumber) * (*page.PageSize)) {
				break
			}
			pagenum += 1
			max = rsp.TotalCount
		}
	}
	start := (*page.PageNumber - 1) * (*page.PageSize)
	end := start + (*page.PageSize)
	if start >= len(instances) {
		return []ecs.Instance{}, 0, nil
	}
	if end > len(instances) {
		end = len(instances)
	}
	total, err := totalEcs(ctx, regions, cluster)
	if err != nil {
		logrus.Errorf("get total ecs number failed, error: %v", err)
		return nil, 0, err
	}

	instances = instances[start:end]

	return instances, total, nil
}

func totalEcs(ctx aliyun_resources.Context, regions []string, cluster string) (int, error) {
	pagesize := 10
	pagenum := 1
	total := 0
	for _, region := range regions {
		ctx.Region = region
		resp, err := DescribeResource(ctx,
			aliyun_resources.PageOption{
				PageSize:   &pagesize,
				PageNumber: &pagenum,
			}, cluster, "", []string{})
		if err != nil {
			return 0, err
		}
		total += resp.TotalCount
	}
	return total, nil
}

func Stop(ctx aliyun_resources.Context, IDs []string) (*ecs.StopInstancesResponse, error) {

	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ecs client error: %+v", err)
		return nil, err
	}

	// create request
	request := ecs.CreateStopInstancesRequest()
	request.Scheme = "https"

	request.InstanceId = &IDs
	request.ForceStop = requests.NewBoolean(true)
	response, err := client.StopInstances(request)
	if err != nil {
		fmt.Print(err.Error())
		return nil, err
	}
	return response, nil
}

func Start(ctx aliyun_resources.Context, IDs []string) (*ecs.StartInstancesResponse, error) {

	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ecs client error: %+v", err)
		return nil, err
	}

	// create request
	request := ecs.CreateStartInstancesRequest()
	request.Scheme = "https"
	request.InstanceId = &IDs
	response, err := client.StartInstances(request)
	if err != nil {
		fmt.Print(err.Error())
		return nil, err
	}
	return response, nil
}

func Restart(ctx aliyun_resources.Context, IDs []string) (*ecs.RebootInstancesResponse, error) {

	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ecs client error: %+v", err)
		return nil, err
	}

	// create request
	request := ecs.CreateRebootInstancesRequest()
	request.Scheme = "https"

	request.InstanceId = &IDs
	request.ForceReboot = requests.NewBoolean(true)
	response, err := client.RebootInstances(request)
	if err != nil {
		fmt.Print(err.Error())
		return nil, err
	}
	return response, nil
}

func AutoRenew(ctx aliyun_resources.Context, id string, duration int, s bool) (*ecs.ModifyInstanceAutoRenewAttributeResponse, error) {

	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ecs client error: %+v", err)
		return nil, err
	}

	// create request
	request := ecs.CreateModifyInstanceAutoRenewAttributeRequest()
	request.Scheme = "https"

	request.InstanceId = id
	request.Duration = requests.NewInteger(duration)
	if !s {
		request.RenewalStatus = "NotRenewal"
	}
	request.AutoRenew = requests.NewBoolean(true)
	response, err := client.ModifyInstanceAutoRenewAttribute(request)
	if err != nil {
		fmt.Print(err.Error())
		return nil, err
	}
	return response, nil
}

func Classify(ins []ecs.Instance) (runningCount, gonnaExpiredCount, expiredCount, stoppedCount,
	postpaidCount, prepaidCount, diceManagedCount int, err error) {
	now := time.Now()
	for _, i := range ins {
		if strutil.ToLower(i.InstanceChargeType) == "postpaid" {
			postpaidCount += 1
		} else {
			prepaidCount += 1
		}

		// stopped status
		if strutil.ToLower(i.Status) == "stopped" {
			stoppedCount += 1
			continue
		}
		// postpaid running status
		if strutil.ToLower(i.InstanceChargeType) == "postpaid" {
			runningCount += 1
			continue
		}

		var t time.Time
		t, err = time.Parse("2006-01-02T15:04Z07:00", i.ExpiredTime)
		if err != nil {
			logrus.Errorf("ecs, failed to parse expiredtime: %v, %s", err, i.ExpiredTime)
			continue
		}
		if t.Before(now) {
			expiredCount += 1
		} else if t.Before(now.Add(24 * 10 * time.Hour)) {
			gonnaExpiredCount += 1
		} else {
			runningCount += 1
		}

	}
	managedIns := instancesByCluster(ins)
	diceManagedCount = len(managedIns)
	return
}

func instancesByCluster(ins []ecs.Instance) []ecs.Instance {
	r := []ecs.Instance{}
	for _, i := range ins {
		for _, t := range i.Tags.Tag {
			if strutil.HasPrefixes(t.TagKey, "dice-cluster/") {
				r = append(r, i)
				break
			}
		}
	}
	return r
}

type MonthAddTrendData struct {
	AxisIndex int    `json:"axisIndex"`
	ChartType string `json:"chartType"`
	UnitType  string `json:"unitType"`
	Unit      string `json:"unit"`
	Name      string `json:"name"`
	Tag       string `json:"tag"`
	Data      []int  `json:"data"`
}
type MonthAddTrendData_0 struct {
	Data []struct {
		MonthAdd MonthAddTrendData `json:"monthadd"`
	} `json:"data"`
}
type MonthAddTrend struct {
	Time    []int64               `json:"time"`
	Results []MonthAddTrendData_0 `json:"results"`
}

func Trend(ins []ecs.Instance) (*apistructs.MonthAddTrend, error) {
	currentMonth, err := time.Parse("2006-01", time.Now().Format("2006-01"))
	if err != nil {
		return nil, err
	}
	m0 := currentMonth.Unix()
	m1 := currentMonth.AddDate(0, -1, 0).Unix()
	m2 := currentMonth.AddDate(0, -2, 0).Unix()
	m3 := currentMonth.AddDate(0, -3, 0).Unix()
	m4 := currentMonth.AddDate(0, -4, 0).Unix()
	m5 := currentMonth.AddDate(0, -5, 0).Unix()

	monthList := map[int64]int{
		m0: 0,
		m1: 0,
		m2: 0,
		m3: 0,
		m4: 0,
		m5: 0,
	}
	for _, i := range ins {
		t, err := time.Parse("2006-01-02T15:04Z07:00", i.CreationTime)
		if err != nil {
			return nil, err
		}
		tunix := t.Unix()
		if tunix >= m0 {
			monthList[m0] += 1
		} else if tunix < m0 && tunix >= m1 {
			monthList[m1] += 1
		} else if tunix < m1 && tunix >= m2 {
			monthList[m2] += 1
		} else if tunix < m2 && tunix >= m3 {
			monthList[m3] += 1
		} else if tunix < m3 && tunix >= m4 {
			monthList[m4] += 1
		} else if tunix < m4 && tunix >= m5 {
			monthList[m5] += 1
		}
	}
	return &apistructs.MonthAddTrend{
		Time: []int64{
			m5 * 1000, m4 * 1000, m3 * 1000, m2 * 1000, m1 * 1000, m0 * 1000, // ms
		},
		Total: 0,
		Title: "ECS trending",
		Results: []apistructs.MonthAddTrendData_0{
			{
				Data: []struct {
					MonthAdd apistructs.MonthAddTrendData `json:"monthadd"`
				}{
					{MonthAdd: apistructs.MonthAddTrendData{
						Data: []int{
							monthList[m5],
							monthList[m4],
							monthList[m3],
							monthList[m2],
							monthList[m1],
							monthList[m0],
						},
						Name: "ecs monthly add",
					}},
				},
			},
		},
	}, nil
}

func ListInstanceTypes(ctx aliyun_resources.Context) ([]ecs.InstanceType, error) {
	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return nil, err
	}
	request := ecs.CreateDescribeInstanceTypesRequest()
	request.Scheme = "https"

	response, err := client.DescribeInstanceTypes(request)
	if err != nil {
		return nil, err
	}
	return response.InstanceTypes.InstanceType, nil
}

func ListByCluster(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string) (*ecs.DescribeInstancesResponse, error) {
	if strings.IsEmpty(&cluster) {
		err := fmt.Errorf("empty cluster name")
		logrus.Errorf(err.Error())
		return nil, err
	}

	response, err := DescribeResource(ctx, page, cluster, "", nil)
	if err != nil {
		logrus.Errorf("describe ecs resource failed, cluster: %s, error: %+v", cluster, err)
	}

	return response, nil
}

func DescribeResource(ctx aliyun_resources.Context, page aliyun_resources.PageOption,
	cluster string, id string, IPs []string) (*ecs.DescribeInstancesResponse, error) {
	// create client
	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ecs client error: %+v", err)
		return nil, err
	}

	// create request
	request := ecs.CreateDescribeInstancesRequest()
	request.Scheme = "https"
	if page.PageNumber == nil || page.PageSize == nil || *page.PageSize <= 0 || *page.PageNumber <= 0 || *page.PageSize > 100 {
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

	if !strings.IsEmpty(&ctx.VpcID) {
		request.VpcId = ctx.VpcID
	}

	if !strings.IsEmpty(&cluster) {
		tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
		request.Tag = &[]ecs.DescribeInstancesTag{{Key: tagKey, Value: tagValue}}
	}
	if len(IPs) > 0 {
		ips := strutil.Join(strutil.Map(IPs, func(s string) string { return "\"" + s + "\"" }), ",")
		request.PrivateIpAddresses = fmt.Sprintf("[%s]", ips)
	}
	if id != "" {
		request.InstanceIds = fmt.Sprintf("[\"%s\"]", id)
	}

	// describe resource
	response, err := client.DescribeInstances(request)
	if err != nil {
		logrus.Errorf("describe ecs failed, error: %+v", err)
		return nil, err
	}
	return response, nil
}

func GetAllResourceIDs(ctx aliyun_resources.Context) ([]string, error) {
	var (
		page        aliyun_resources.PageOption
		instanceIDs []string
	)
	if strings.IsEmpty(&ctx.VpcID) {
		err := fmt.Errorf("get ecs resource id failed, empty vpc id")
		logrus.Errorf(err.Error())
		return nil, err
	}

	pageNumber := 1
	pageSize := 15
	page.PageNumber = &pageNumber
	page.PageSize = &pageSize

	for {
		response, err := DescribeResource(ctx, page, "", "", nil)
		if err != nil {
			return nil, err
		}
		for _, i := range response.Instances.Instance {
			instanceIDs = append(instanceIDs, i.InstanceId)
		}
		if response.Instances.Instance == nil || pageNumber*pageSize >= response.TotalCount {
			break
		}
		pageNumber += 1
	}
	return instanceIDs, nil
}

func OverwriteTags(ctx aliyun_resources.Context, items []apistructs.CloudResourceTagItem, tags []string) error {
	var (
		oldTags     []string
		instanceIDs []string
	)

	tagSet := set.New()

	for i := range items {
		instanceIDs = append(instanceIDs, items[i].ResourceID)
		for k, v := range items[i].OldTags {
			if !tagSet.Has(v) {
				oldTags = append(oldTags, items[i].OldTags[k])
				tagSet.Insert(items[i].OldTags[k])
			}
		}
	}

	// unset old tags
	err := UnTag(ctx, instanceIDs, oldTags)
	if err != nil {
		return err
	}

	// set new tags
	err = TagResource(ctx, instanceIDs, tags)
	return err
}

func TagResource(ctx aliyun_resources.Context, instanceIds []string, tags []string) error {
	if len(instanceIds) == 0 || len(tags) == 0 {
		return nil
	}

	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("ecs, create client failed: %+v", err)
		return err
	}

	request := ecs.CreateTagResourcesRequest()
	request.Scheme = "https"
	request.ResourceType = "INSTANCE"
	request.ResourceId = &instanceIds

	var tagKV []ecs.TagResourcesTag
	for _, v := range tags {
		tagKV = append(tagKV, ecs.TagResourcesTag{
			Value: "true",
			Key:   v,
		})
	}
	request.Tag = &tagKV

	_, err = client.TagResources(request)
	if err != nil {
		logrus.Errorf("ecs, tag resource failed, error:%v", err)
		return err
	}
	return nil
}

func UnTag(ctx aliyun_resources.Context, instanceIds []string, tags []string) error {
	if len(instanceIds) == 0 || len(tags) == 0 {
		return nil
	}

	client, err := ecs.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("ecs, create client failed: %+v", err)
		return err
	}

	request := ecs.CreateUntagResourcesRequest()
	request.Scheme = "https"

	request.ResourceType = "instance"
	request.ResourceId = &instanceIds
	if len(tags) > 0 {
		request.TagKey = &tags
	}

	_, err = client.UntagResources(request)
	if err != nil {
		logrus.Errorf("ecs, failed to untag resource, instances ids:%v, tags:%v, error:%v", instanceIds, tags, err)
		return err
	}
	return nil
}
