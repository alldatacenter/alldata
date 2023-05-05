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

package zone

import (
	"sync"

	"github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/sirupsen/logrus"

	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

func List(ctx aliyun_resources.Context, regions []string) ([]vpc.Zone, error) {
	allzones := []vpc.Zone{}
	listSch := make(chan listS, 20)
	var wg sync.WaitGroup
	wg.Add(len(regions))
	for _, region := range regions {
		ctx.Region = region
		go func(ctx aliyun_resources.Context) {
			defer func() { wg.Done() }()
			listF(ctx, listSch)
		}(ctx)
	}
	wg.Wait()
	close(listSch)
	for s := range listSch {
		allzones = append(allzones, s.zones...)
	}
	return allzones, nil
}

type listS struct {
	zones []vpc.Zone
}

func listF(ctx aliyun_resources.Context, ch chan listS) {
	zones, err := describeZones(ctx)
	if err != nil {
		logrus.Errorf("failed to describezones: %v", err)
		ch <- listS{}
		return
	}
	ch <- listS{zones: zones}
}

func describeZones(ctx aliyun_resources.Context) ([]vpc.Zone, error) {
	client, err := vpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return nil, err
	}
	request := vpc.CreateDescribeZonesRequest()
	request.Scheme = "https"

	response, err := client.DescribeZones(request)
	if err != nil {
		return nil, err
	}
	return response.Zones.Zone, nil
}
