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
	"github.com/aliyun/alibaba-cloud-sdk-go/services/rds"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

// describe database info
func DescribeDatabases(ctx aliyun_resources.Context, req apistructs.CloudResourceMysqlListDatabaseRequest) (*rds.DescribeDatabasesResponse, error) {
	client, err := rds.NewClientWithAccessKey(ctx.Region, ctx.AccessKeyID, ctx.AccessSecret)
	if err != nil {
		logrus.Errorf("create rds client error: %v", err)
		return nil, err
	}

	request := rds.CreateDescribeDatabasesRequest()
	request.Scheme = "https"
	request.DBInstanceId = req.InstanceID

	response, err := client.DescribeDatabases(request)
	if err != nil {
		logrus.Errorf("describe mysql database failed, error:%v", err)
		return nil, err
	}
	return response, nil
}
