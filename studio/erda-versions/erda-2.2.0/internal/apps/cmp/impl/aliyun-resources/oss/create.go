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
	"encoding/json"
	"fmt"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
)

func CreateBucketWithRecord(ctx aliyun_resources.Context, req apistructs.CreateCloudResourceOssRequest, record *dbclient.Record) {
	if len(req.Buckets) == 0 {
		return
	}
	// now only support create one bucket a time
	b := req.Buckets[0]

	var detail apistructs.CreateCloudResourceRecord
	createInstanceStep := apistructs.CreateCloudResourceStep{
		Step:   string(dbclient.RecordTypeCreateAliCloudOss),
		Status: string(dbclient.StatusTypeSuccess)}
	detail.Steps = append(detail.Steps, createInstanceStep)
	detail.ClientToken = req.ClientToken
	detail.InstanceName = b.Name
	detail.Steps[len(detail.Steps)-1].Name = b.Name

	// get region info by cluster name if not provide
	if req.Region == "" {
		v, err := vpc.GetVpcByCluster(ctx, req.ClusterName)
		if err != nil {
			err := fmt.Errorf("get region info failed, error:%v", err)
			logrus.Errorf("%s, cluster:%+v", err.Error(), req.ClusterName)
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}
		req.Region = v.RegionId
	}
	ctx.Region = req.Region

	logrus.Debugf("start to create bucket, request:%+v", req)
	err := CreateBucket(ctx, b)
	if err != nil {
		e := fmt.Errorf("create oss bucket failed, error:%v", err)
		logrus.Errorf(e.Error())
		aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
		return
	}
	if req.Source == apistructs.CloudResourceSourceAddon {
		cb := apistructs.AddonConfigCallBackResponse{
			Config: []apistructs.AddonConfigCallBackItemResponse{
				{
					Name:  "OSS_ENDPOINT",
					Value: fmt.Sprintf("http://oss-%s.aliyuncs.com", ctx.Region),
				},
				{
					Name:  "OSS_BUCKET",
					Value: b.Name,
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

		// TODO: only support one addon in a request
		if b.AddonID == "" {
			b.AddonID = req.ClientToken
		}

		logrus.Debugf("start addon config callback, request: %+v", b.AddonID)
		_, err := ctx.Bdl.AddonConfigCallback(b.AddonID, cb)
		if err != nil {
			e := fmt.Errorf("oss addon callback config failed, error:%v", err)
			logrus.Errorf(e.Error())
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}

		_, err = ctx.Bdl.AddonConfigCallbackProvison(b.AddonID, apistructs.AddonCreateCallBackResponse{IsSuccess: true})
		if err != nil {
			err := fmt.Errorf("add call back provision failed, error:%v", err)
			logrus.Errorf(err.Error())
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}

		// update resource routing record
		_, err = ctx.DB.ResourceRoutingWriter().Create(&dbclient.ResourceRouting{
			ResourceID:   b.Name,
			ResourceName: b.Name,
			ResourceType: dbclient.ResourceTypeOss,
			Vendor:       req.Vendor,
			OrgID:        req.OrgID,
			ClusterName:  req.ClusterName,
			ProjectID:    req.ProjectID,
			// TODO bound addonID with addon in request
			AddonID:  b.AddonID,
			Status:   "ATTACHED",
			RecordID: record.ID,
			Detail:   "",
		})
		if err != nil {
			e := fmt.Errorf("update resource routing failed, error:%v", err)
			logrus.Errorf(e.Error())
			aliyun_resources.ProcessFailedRecord(ctx, req.Source, req.ClientToken, record, &detail, err)
			return
		}
	}

	// success, update ops_record
	content, err := json.Marshal(detail)
	if err != nil {
		logrus.Errorf("marshal record detail failed, error:%+v", err)
	}
	record.Status = dbclient.StatusTypeSuccess
	record.Detail = string(content)
	if err := ctx.DB.RecordsWriter().Update(*record); err != nil {
		logrus.Errorf("failed to update record: %v", err)
	}
}

func CreateBucket(ctx aliyun_resources.Context, bucket apistructs.OssBucketInfo) error {
	buckets, err := List(ctx, aliyun_resources.DefaultPageOption, aliyun_resources.ActiveRegionIDs(ctx).VPC, "", []string{}, "")
	if err != nil {
		return err
	}
	for _, b := range buckets {
		if b.Name == bucket.Name {
			return fmt.Errorf("oss bucket name: [%s] already exists", b.Name)
		}
	}
	endpoint := fmt.Sprintf("http://oss-%s.aliyuncs.com", ctx.Region)
	accessKeyId := ctx.AccessKeyID
	accessKeySecret := ctx.AccessSecret

	opts := []oss.Option{}
	// storage type standard
	opts = append(opts, oss.ObjectStorageClass(oss.StorageStandard))
	opts = append(opts, oss.AcceptEncoding(bucket.Acl))

	// init
	client, err := oss.New(endpoint, accessKeyId, accessKeySecret)
	if err != nil {
		return err
	}
	// request
	err = client.CreateBucket(bucket.Name)
	return err
}

func ProcessFailedRecord(ctx aliyun_resources.Context, record *dbclient.Record, detail *apistructs.CreateCloudResourceRecord) {
	content, err := json.Marshal(detail)
	if err != nil {
		logrus.Errorf("marshal record detail failed, error:%+v", err)
	}
	record.Status = dbclient.StatusTypeFailed
	record.Detail = string(content)
	if err := ctx.DB.RecordsWriter().Update(*record); err != nil {
		logrus.Errorf("failed to update record: %v", err)
	}
}
