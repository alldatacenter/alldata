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

package region

import (
	"github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"

	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

func List(ctx aliyun_resources.Context) ([]vpc.Region, error) {
	// regionid doesn't need in this interface, use "cn-hangzhou" fill it.
	client, err := vpc.NewClientWithAccessKey("cn-hangzhou", ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		return nil, err
	}

	request := vpc.CreateDescribeRegionsRequest()
	request.Scheme = "https"

	response, err := client.DescribeRegions(request)
	if err != nil {
		return nil, err
	}
	return response.Regions.Region, nil
}
