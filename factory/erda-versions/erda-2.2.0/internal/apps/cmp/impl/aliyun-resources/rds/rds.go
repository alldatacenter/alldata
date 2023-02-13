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

package rds

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	strlib "strings"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/rds"
	"github.com/appscode/go/strings"
	"github.com/golang-collections/collections/set"
	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/pkg/strutil"
)

type DescribeDBInstancesResponse struct {
	aliyun_resources.ResponsePager
	DBInstances []rds.DBInstance
}

type DBInstanceInDescribeDBInstancesWithTag struct {
	rds.DBInstance
	Tag map[string]string `json:"tag"`
}

func GetChargeType(payType string) string {
	if strlib.ToLower(payType) == aliyun_resources.ChargeTypePrepaid {
		return "PrePaid"
	} else if strlib.ToLower(payType) == "postpaid" {
		return "PostPaid"
	} else {
		return ""
	}
}

func List(ctx aliyun_resources.Context, page aliyun_resources.PageOption,
	regions []string,
	cluster string) ([]DBInstanceInDescribeDBInstancesWithTag, int, error) {
	rdslist := []DBInstanceInDescribeDBInstancesWithTag{}
	total := 0
	for _, region := range regions {
		ctx.Region = region
		dbs, err := DescribeResource(ctx, page, cluster, "")
		if err != nil {
			return nil, 0, err
		}
		for _, db := range dbs.DBInstances {
			tags, err := DescribeTags(ctx, db.DBInstanceId)
			if err != nil {
				return nil, 0, err
			}
			tagResult := map[string]string{}
			for _, tag := range tags.Items.TagInfos {
				if strlib.HasPrefix(tag.TagKey, aliyun_resources.TagPrefixProject) {
					tagResult[tag.TagKey] = tag.TagValue
				}
			}
			rdslist = append(rdslist, DBInstanceInDescribeDBInstancesWithTag{
				DBInstance: db,
				Tag:        tagResult,
			})
		}
		total += dbs.TotalCount
	}
	return rdslist, total, nil
}

func ListByCluster(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string) (DescribeDBInstancesResponse, error) {
	if strings.IsEmpty(&cluster) {
		err := fmt.Errorf("empty cluster name")
		logrus.Errorf(err.Error())
		return DescribeDBInstancesResponse{}, err
	}
	return DescribeResource(ctx, page, cluster, "")
}

func DescribeResource(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string, projName string) (DescribeDBInstancesResponse, error) {
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return DescribeDBInstancesResponse{}, err
	}

	tags := make(map[string]string)

	request := rds.CreateDescribeDBInstancesRequest()
	request.Scheme = "https"

	if page.PageSize == nil {
		pagesize := 30
		page.PageSize = &pagesize
	}
	request.PageSize = requests.NewInteger(*page.PageSize)
	if page.PageNumber != nil {
		request.PageNumber = requests.NewInteger(*page.PageNumber)
	}
	if cluster != "" {
		tagKey, tagValue := aliyun_resources.GenClusterTag(cluster)
		tags[tagKey] = tagValue
	}
	if projName != "" {
		tagKey, tagValue := aliyun_resources.GenProjectTag(projName)
		tags[tagKey] = tagValue
	}
	if cluster != "" || projName != "" {
		tagReq, err := json.Marshal(tags)
		if err != nil {
			logrus.Errorf("generate resource tag failed, cluster: %s, error: %v", cluster, err)
			return DescribeDBInstancesResponse{}, err
		}
		request.Tags = string(tagReq)
		//request.Tags = fmt.Sprintf("{\"%s\": \"%s\"}", tagKey, tagValue)
	}
	// status:
	//	Creating
	//  Running
	//  Deleting
	//  Rebooting
	//  ...
	response, err := client.DescribeDBInstances(request)
	if err != nil {
		logrus.Errorf("describe db instance failed, error:%v", err)
		return DescribeDBInstancesResponse{}, err
	}

	return DescribeDBInstancesResponse{
		ResponsePager: aliyun_resources.ResponsePager{
			TotalCount: response.TotalRecordCount,
			PageSize:   *page.PageSize, // empty pageSize from response
			PageNumber: response.PageNumber,
		},
		DBInstances: response.Items.DBInstance,
	}, nil
}

func GetInstanceDetailInfo(ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlDetailInfoRequest) (*apistructs.CloudResourceMysqlDetailInfoData, error) {
	r, err := DescribeInstanceAttribute(ctx, req)
	if err != nil {
		return nil, err
	}
	if len(r.Items.DBInstanceAttribute) == 0 {
		err := fmt.Errorf("get instance detail info failed, empty response")
		logrus.Errorf("%s, response:%+v", err.Error(), r)
		return nil, err
	}
	content := r.Items.DBInstanceAttribute[0]

	return &apistructs.CloudResourceMysqlDetailInfoData{
		ID:          content.DBInstanceId,
		Name:        content.DBInstanceDescription,
		Category:    content.Category,
		RegionId:    content.RegionId,
		VpcId:       content.VpcId,
		VSwitchId:   content.VSwitchId,
		ZoneId:      content.ZoneId,
		Host:        content.ConnectionString,
		Port:        content.Port,
		Memory:      strconv.FormatInt(content.DBInstanceMemory, 10),
		StorageSize: strconv.Itoa(content.DBInstanceStorage),
		StorageType: content.DBInstanceStorageType,
		Status:      content.DBInstanceStatus,
	}, nil
}

//get rds full detail info
func GetInstanceFullDetailInfo(c context.Context, ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlDetailInfoRequest) ([]apistructs.CloudResourceDetailInfo, error) {
	i18n := c.Value("i18nPrinter").(*message.Printer)
	r1, err := DescribeInstanceAttribute(ctx, req)
	if err != nil {
		return nil, err
	}
	if len(r1.Items.DBInstanceAttribute) == 0 {
		err := fmt.Errorf("get instance detail info failed, empty response")
		logrus.Errorf("%s, response:%+v", err.Error(), r1)
		return nil, err
	}
	content := r1.Items.DBInstanceAttribute[0]

	r2, err := DescribeDBInstanceNetInfo(ctx, req)
	if err != nil {
		return nil, err
	}

	var internalEndpoint string
	var internalPort string
	var publicEndpoint string
	var publicPort string

	for _, netInfo := range r2.DBInstanceNetInfos.DBInstanceNetInfo {
		if netInfo.IPType == "Private" || netInfo.IPType == "Inner" {
			internalEndpoint = netInfo.ConnectionString
			internalPort = netInfo.Port
		}
		if netInfo.IPType == "Public" {
			internalEndpoint = netInfo.ConnectionString
			internalPort = netInfo.Port
		}
	}

	switch r2.InstanceNetworkType {
	case "Classic":

	case "VPC":

	}

	r3, err := DescribeDBInstanceResourceUsage(ctx, req)
	if err != nil {
		return nil, err
	}
	dbUsageSize := fmt.Sprintf("%.2f", float64(r3.DiskUsed)/1024/1024/1024)

	var basicInfo []apistructs.CloudResourceDetailItem
	basicInfo = append(basicInfo,
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Instance ID"),
			Value: content.DBInstanceId,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Name"),
			Value: content.DBInstanceDescription,
		},
		apistructs.CloudResourceDetailItem{
			//TODO format
			Name:  i18n.Sprintf("Region and Zone"),
			Value: content.RegionId + " " + strlib.ToLower(content.ZoneId[len(content.ZoneId)-1:]),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Instance Role & Edition"),
			Value: i18n.Sprintf(content.Category),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Storage Type"),
			Value: i18n.Sprintf(content.DBInstanceStorageType),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Internal Endpoint"),
			Value: internalEndpoint,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Internal Port"),
			Value: internalPort,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Public Endpoint"),
			Value: publicEndpoint,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Public Port"),
			Value: publicPort,
		},
	)

	var configInfo []apistructs.CloudResourceDetailItem
	configInfo = append(configInfo,
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Instance Family"),
			Value: i18n.Sprintf(content.DBInstanceClassType),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Database Engine"),
			Value: content.Engine + " " + content.EngineVersion,
		},
		apistructs.CloudResourceDetailItem{
			Name:  "CPU",
			Value: content.DBInstanceCPU,
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Memory"),
			Value: strconv.FormatInt(content.DBInstanceMemory, 10) + "MB",
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Maximum Connections"),
			Value: strconv.Itoa(content.MaxConnections),
		},
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Maximum Iops"),
			Value: strconv.Itoa(content.MaxIOPS),
		},
	)
	var usageInfo []apistructs.CloudResourceDetailItem
	usageInfo = append(usageInfo,
		apistructs.CloudResourceDetailItem{
			Name:  i18n.Sprintf("Storage Capacity"),
			Value: i18n.Sprintf("Used") + " " + dbUsageSize + "G (" + i18n.Sprintf("Total") + ": " + strconv.Itoa(content.DBInstanceStorage) + "G)",
		},
	)

	var res []apistructs.CloudResourceDetailInfo
	res = append(res,
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Basic Information"),
			Items: basicInfo,
		},
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Configuration Information"),
			Items: configInfo,
		},
		apistructs.CloudResourceDetailInfo{
			Label: i18n.Sprintf("Usage Statistics"),
			Items: usageInfo,
		},
	)
	return res, nil
}

func DescribeInstanceAttribute(ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlDetailInfoRequest) (*rds.DescribeDBInstanceAttributeResponse, error) {
	if strings.IsEmpty(&req.InstanceID) {
		err := fmt.Errorf("get instance attribute failed, empty instance id")
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		return nil, err
	}

	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return nil, err
	}

	request := rds.CreateDescribeDBInstanceAttributeRequest()
	request.Scheme = "https"

	request.DBInstanceId = req.InstanceID

	response, err := client.DescribeDBInstanceAttribute(request)
	if err != nil {
		logrus.Errorf("describe mysql instance attribute failed, error: %+v", err)
		return nil, err
	}
	return response, nil
}

//describe rds net info
func DescribeDBInstanceNetInfo(ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlDetailInfoRequest) (*rds.DescribeDBInstanceNetInfoResponse, error) {
	if strings.IsEmpty(&req.InstanceID) {
		err := fmt.Errorf("get instance attribute failed, empty instance id")
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		return nil, err
	}

	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return nil, err
	}

	request := rds.CreateDescribeDBInstanceNetInfoRequest()
	request.Scheme = "https"

	request.DBInstanceId = req.InstanceID

	response, err := client.DescribeDBInstanceNetInfo(request)
	if err != nil {
		logrus.Errorf("describe mysql instance netinfo failed, error: %+v", err)
		return nil, err
	}
	return response, nil
}

//describe rds resource usage
func DescribeDBInstanceResourceUsage(ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlDetailInfoRequest) (*rds.DescribeResourceUsageResponse, error) {
	if strings.IsEmpty(&req.InstanceID) {
		err := fmt.Errorf("get instance attribute failed, empty instance id")
		logrus.Errorf("%s, request:%+v", err.Error(), req)
		return nil, err
	}

	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %+v", err)
		return nil, err
	}

	request := rds.CreateDescribeResourceUsageRequest()
	request.Scheme = "https"

	request.DBInstanceId = req.InstanceID

	response, err := client.DescribeResourceUsage(request)
	if err != nil {
		logrus.Errorf("describe mysql instance netinfo failed, error: %+v", err)
		return nil, err
	}
	return response, nil
}

// describe database info
func DescribeDBInfo(ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlDetailInfoRequest) (*rds.DescribeDatabasesResponse, error) {
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %v", err)
		return nil, err
	}

	request := rds.CreateDescribeDatabasesRequest()
	request.Scheme = "https"
	request.DBInstanceId = req.InstanceID
	request.PageSize = requests.NewInteger(10)

	response, err := client.DescribeDatabases(request)
	if err != nil {
		logrus.Errorf("describe mysql database failed, error:%v", err)
		return nil, err
	}
	return response, nil
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

	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("rds, create client failed: %+v", err)
		return err
	}

	request := rds.CreateTagResourcesRequest()
	request.Scheme = "https"
	request.ResourceType = "INSTANCE"
	request.ResourceId = &instanceIds

	var tagKV []rds.TagResourcesTag
	for _, v := range tags {
		tagKV = append(tagKV, rds.TagResourcesTag{
			Value: "true",
			Key:   v,
		})
	}
	request.Tag = &tagKV

	_, err = client.TagResources(request)
	if err != nil {
		logrus.Errorf("rds, tag resource failed, error:%v", err)
		return err
	}
	return nil
}

func UnTag(ctx aliyun_resources.Context, instanceIds []string, tags []string) error {
	if len(instanceIds) == 0 || len(tags) == 0 {
		return nil
	}

	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("rds, create client failed: %+v", err)
		return err
	}

	request := rds.CreateUntagResourcesRequest()
	request.Scheme = "https"

	request.ResourceType = "instance"
	request.ResourceId = &instanceIds
	if len(tags) > 0 {
		request.TagKey = &tags
	}

	_, err = client.UntagResources(request)
	if err != nil {
		logrus.Errorf("rds, failed to untag resource, instances ids:%v, tags:%v, error:%v", instanceIds, tags, err)
		return err
	}
	return nil
}

func DescribeTags(ctx aliyun_resources.Context, instanceId string) (*rds.DescribeTagsResponse, error) {

	var resp *rds.DescribeTagsResponse
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("rds, create client failed: %+v", err)
		return nil, err
	}

	request := rds.CreateDescribeTagsRequest()
	request.Scheme = "https"
	request.DBInstanceId = instanceId

	resp, err = client.DescribeTags(request)
	if err != nil {
		logrus.Errorf("rds, tag resource failed, error:%v", err)
		return nil, err
	}
	return resp, nil
}

func Classify(ins []DBInstanceInDescribeDBInstancesWithTag) (runningCount, gonnaExpiredCount, expiredCount, stoppedCount,
	postpaidCount, prepaidCount int, err error) {
	now := time.Now()
	for _, i := range ins {
		if strutil.ToLower(i.PayType) == "postpaid" {
			postpaidCount += 1
		} else {
			prepaidCount += 1
		}

		// stopped status
		if strutil.ToLower(i.DBInstanceStatus) == "deleting" {
			stoppedCount += 1
			continue
		}
		// postpaid running status
		if strutil.ToLower(i.PayType) == "postpaid" {
			runningCount += 1
			continue
		}

		var t time.Time
		t, err = time.Parse("2006-01-02T15:04:05Z", i.ExpireTime)
		if err != nil {
			logrus.Errorf("rds, failed to parse expiredtime: %v, %s", err, i.ExpireTime)
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
	return
}
