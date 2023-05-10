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

package oss

import (
	"fmt"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	resource_factory "github.com/erda-project/erda/internal/apps/cmp/impl/resource-factory"
)

type OssFactory struct {
	*resource_factory.BaseResourceFactory
}

func creator(ctx aliyun_resources.Context, m resource_factory.BaseResourceMaterial, r *dbclient.Record, d *apistructs.CreateCloudResourceRecord, v apistructs.CloudResourceVpcBaseInfo) (*apistructs.AddonConfigCallBackResponse, *dbclient.ResourceRouting, error) {
	var err error

	req, ok := m.(apistructs.CreateCloudResourceOssRequest)
	if !ok {
		return nil, nil, errors.Errorf("convert material failed, material: %+v", m)
	}
	if len(req.Buckets) == 0 {
		return nil, nil, errors.New("empty buckets is invalid")
	}

	bucket := req.Buckets[0]

	logrus.Infof("start to create oss bucket, request: %+v", req)
	err = CreateBucket(ctx, bucket)
	if err != nil {
		return nil, nil, errors.Wrap(err, "create oss bucket failed")
	}
	if req.Source != apistructs.CloudResourceSourceAddon {
		return nil, nil, nil
	}
	cbResp := &apistructs.AddonConfigCallBackResponse{
		Config: []apistructs.AddonConfigCallBackItemResponse{
			{
				Name:  "OSS_ENDPOINT",
				Value: fmt.Sprintf("http://oss-%s.aliyuncs.com", ctx.Region),
			},
			{
				Name:  "OSS_BUCKET",
				Value: bucket.Name,
			},
			{
				Name:  "OSS_ACCESSKEY",
				Value: ctx.AccessKeyID,
			},
			{
				Name:  "OSS_SECRET",
				Value: ctx.AccessSecret,
			},
		},
	}
	routing := &dbclient.ResourceRouting{
		ResourceID:   bucket.Name,
		ResourceName: bucket.Name,
		ResourceType: dbclient.ResourceTypeOss,
		Vendor:       req.Vendor,
		OrgID:        req.OrgID,
		ClusterName:  req.ClusterName,
		ProjectID:    req.ProjectID,
		AddonID:      bucket.AddonID,
		Status:       dbclient.ResourceStatusAttached,
		RecordID:     r.ID,
	}
	return cbResp, routing, nil
}

func init() {
	factory := OssFactory{BaseResourceFactory: &resource_factory.BaseResourceFactory{}}
	factory.Creator = creator
	factory.RecordType = dbclient.RecordTypeCreateAliCloudOss
	err := resource_factory.Register(dbclient.ResourceTypeOss, factory)
	if err != nil {
		panic(err)
	}
}
