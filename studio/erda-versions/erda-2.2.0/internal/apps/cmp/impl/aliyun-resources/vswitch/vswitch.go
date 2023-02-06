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

package vswitch

import (
	"fmt"
	"sync"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/sirupsen/logrus"

	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

type WithRegionVSwitch struct {
	Region string `json:"region"`
	vpc.VSwitch
}

func List(ctx aliyun_resources.Context, regions []string) ([]WithRegionVSwitch, int, error) {
	var vswlist []WithRegionVSwitch
	total := 0
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
		vswlist = append(vswlist, s.vsws...)
		total += s.total
	}

	return vswlist, total, nil
}

type listS struct {
	vsws  []WithRegionVSwitch
	total int
}

func listF(ctx aliyun_resources.Context, ch chan listS) {
	logrus.Infof("vsw list start(%s): %v", ctx.Region, time.Now())
	client, err := vpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("failed to NewClientWithAccessKey: %v", err)
		ch <- listS{}
		return
	}
	request := vpc.CreateDescribeVSwitchesRequest()
	request.Scheme = "https"
	request.PageSize = requests.NewInteger(50)
	if ctx.VpcID != "" {
		request.VpcId = ctx.VpcID
	}
	response, err := client.DescribeVSwitches(request)
	if err != nil {
		logrus.Errorf("failed to DescribeVSwitches: %v", err)
		ch <- listS{}
		return
	}

	var result []WithRegionVSwitch
	for i := range response.VSwitches.VSwitch {
		result = append(result, WithRegionVSwitch{
			Region:  ctx.Region,
			VSwitch: response.VSwitches.VSwitch[i]})
	}
	ch <- listS{vsws: result, total: response.TotalCount}
	logrus.Infof("vsw list finish(%s): %v", ctx.Region, time.Now())
}

type VSwitchCreateRequest struct {
	RegionID  string
	CidrBlock string
	VpcID     string
	ZoneID    string
	Name      string
}

func Create(ctx aliyun_resources.Context, req VSwitchCreateRequest) (string, error) {
	vswlist, _, err := List(ctx, aliyun_resources.ActiveRegionIDs(ctx).VPC)
	if err != nil {
		return "", err
	}
	for _, vsw := range vswlist {
		if vsw.VSwitchName == req.Name {
			return "", fmt.Errorf("vsw name:[%s] already exists", req.Name)
		}
	}

	client, err := vpc.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return "", err
	}
	request := vpc.CreateCreateVSwitchRequest()
	request.Scheme = "https"
	request.CidrBlock = req.CidrBlock
	request.VpcId = req.VpcID
	request.ZoneId = req.ZoneID
	request.VSwitchName = req.Name
	response, err := client.CreateVSwitch(request)
	if err != nil {
		return "", err
	}
	return response.VSwitchId, nil
}
