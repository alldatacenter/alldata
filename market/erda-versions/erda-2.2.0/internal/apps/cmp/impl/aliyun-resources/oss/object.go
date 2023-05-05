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
	"sync"
	"time"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/sirupsen/logrus"

	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
)

type BucketInfo struct {
	AllObjectTotalSize int64
}

func GetBucketInfo(ctx aliyun_resources.Context, bucketname string, location string) (*BucketInfo, error) {
	endpoint := fmt.Sprintf("http://oss-%s.aliyuncs.com", location)
	accessKeyId := ctx.AccessKeyID
	accessKeySecret := ctx.AccessSecret
	// init
	client, err := oss.New(endpoint, accessKeyId, accessKeySecret)
	if err != nil {
		logrus.Errorf("create oss client error: %+v", err)
		return nil, err
	}
	b, err := client.Bucket(bucketname)
	var totalsize int64

	mark := ""
	for {
		options := []oss.Option{}
		if mark != "" {
			options = append(options, oss.Marker(mark))
		}
		objs, err := b.ListObjects(options...)
		if err != nil {
			return nil, err
		}
		if !objs.IsTruncated {
			break
		}
		mark = objs.NextMarker

		for _, o := range objs.Objects {
			totalsize += o.Size
		}
		logrus.Debugf("bucket: [%s], size: %v G, object num: %v", bucketname, totalsize/1024.0/1024.0, len(objs.Objects))
	}
	return &BucketInfo{AllObjectTotalSize: totalsize}, nil
}

func GetBucketsSize(ctx aliyun_resources.Context, buckets []oss.BucketProperties) (int64, error) {
	var allsize int64
	m := new(sync.Map)
	ch := make(chan struct{}, 20)
	var wg sync.WaitGroup
	wg.Add(len(buckets))
	start := time.Now()
	logrus.Debugf("oss buckets num: %d, buckets: %v", len(buckets), buckets)
	for i, b := range buckets {
		ch <- struct{}{}
		logrus.Debugf("start to get bucket [%v] size for [%s]", i, b.Name)
		go func(index int, b oss.BucketProperties) {
			defer func() {
				<-ch
				wg.Done()
			}()
			info, err := GetBucketInfo(ctx, b.Name, b.Location)
			if err != nil {
				logrus.Errorf("get bucket info failed, bucket:%s, error:%v", b.Name, err)
				return
			}
			m.Store(info.AllObjectTotalSize, struct{}{})
			logrus.Debugf("finish get bucket[%d] [%s] size: %vG", index, b.Name, info.AllObjectTotalSize/1024.0/1024.0)
		}(i, b)
	}
	wg.Wait()
	end := time.Now()
	d := end.Sub(start).Minutes()
	m.Range(func(k interface{}, _ interface{}) bool {
		allsize += k.(int64)
		return true
	})
	logrus.Infof("finish calculate oss bucket size, start:%v, end:%v, spend: [%v min], size:%vG", start, end, d, allsize/1024.0/1024.0)
	return allsize, nil
}
