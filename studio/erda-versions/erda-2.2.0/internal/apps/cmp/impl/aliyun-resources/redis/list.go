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
	"fmt"
	"strings"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	kvstore "github.com/aliyun/alibaba-cloud-sdk-go/services/r-kvstore"
	"github.com/sirupsen/logrus"

	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/pkg/strutil"
)

// list instance
func List(ctx aliyun_resources.Context, page aliyun_resources.PageOption,
	regions []string,
	cluster string) ([]kvstore.KVStoreInstance, error) {
	instances := []kvstore.KVStoreInstance{}
	for _, region := range regions {
		ctx.Region = region
		response, err := DescribeResource(ctx, page, cluster, []string{})
		if err != nil {
			return nil, err
		}
		instances = append(instances, response.Instances.KVStoreInstance...)
	}
	return instances, nil
}

// describe instance
func DescribeResource(ctx aliyun_resources.Context, page aliyun_resources.PageOption, cluster string, instanceIDs []string) (*kvstore.DescribeInstancesResponse, error) {
	// create client
	client, err := kvstore.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create ecs client error: %+v", err)
		return nil, err
	}

	// create request
	request := kvstore.CreateDescribeInstancesRequest()
	request.Scheme = "https"
	// Query multi instances, using (",") to separate the IDs
	// If ID is empty, query all instances in this user.
	request.InstanceIds = strings.Join(instanceIDs, ",")
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
		request.Tag = &[]kvstore.DescribeInstancesTag{{Key: tagKey, Value: tagValue}}
	}

	response, err := client.DescribeInstances(request)
	if err != nil {
		e := fmt.Errorf("describe redis instances failed, error:%v", err)
		logrus.Errorf(e.Error())
		return nil, e
	}
	return response, nil
}

func Classify(ins []kvstore.KVStoreInstance) (runningCount, gonnaExpiredCount, expiredCount, stoppedCount,
	postpaidCount, prepaidCount int, err error) {
	now := time.Now()
	for _, i := range ins {
		if strutil.ToLower(i.ChargeType) == "postpaid" {
			postpaidCount += 1
		} else {
			prepaidCount += 1
		}

		// stopped status
		if strutil.ToLower(i.InstanceStatus) == "released" {
			stoppedCount += 1
			continue
		}
		// postpaid running status
		if strutil.ToLower(i.ChargeType) == "postpaid" {
			runningCount += 1
			continue
		}

		var t time.Time
		t, err = time.Parse("2006-01-02T15:04:05Z", i.EndTime)
		if err != nil {
			logrus.Errorf("redis, failed to parse expiredtime: %v, %s", err, i.EndTime)
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
