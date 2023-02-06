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

package overview

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/sirupsen/logrus"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ecs"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ons"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/oss"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/rds"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/redis"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/vpc"
)

type CachedCloudResourceOverview struct {
	ECS   apistructs.CloudResourceOverviewDetailData
	VPC   apistructs.CloudResourceOverviewDetailData
	OSS   apistructs.CloudResourceOverviewDetailData
	RDS   apistructs.CloudResourceOverviewDetailData
	MQ    apistructs.CloudResourceOverviewDetailData
	REDIS apistructs.CloudResourceOverviewDetailData

	LastUpdateTimestamp int64
}

func timeCost(part string, start time.Time) {
	tc := time.Since(start)
	fmt.Printf("part %s, time cost = %v\n", part, tc)
}

func InitCloudResourceOverview() map[string]*apistructs.CloudResourceTypeOverview {
	computeResource := apistructs.CloudResourceTypeOverview{ResourceTypeData: map[string]*apistructs.CloudResourceOverviewDetailData{
		"ECS": {
			CloudResourceBasicData: apistructs.CloudResourceBasicData{
				TotalCount:  0,
				DisplayName: "",
			},
		},
	}}
	networkResource := apistructs.CloudResourceTypeOverview{ResourceTypeData: map[string]*apistructs.CloudResourceOverviewDetailData{
		"VPC": {
			CloudResourceBasicData: apistructs.CloudResourceBasicData{
				TotalCount:  0,
				DisplayName: "",
			},
		},
	}}
	storageResource := apistructs.CloudResourceTypeOverview{ResourceTypeData: map[string]*apistructs.CloudResourceOverviewDetailData{
		"OSS_BUCKET": {
			CloudResourceBasicData: apistructs.CloudResourceBasicData{
				TotalCount:  0,
				DisplayName: "",
			},
		},
	}}
	cloudserviceResource := apistructs.CloudResourceTypeOverview{ResourceTypeData: map[string]*apistructs.CloudResourceOverviewDetailData{
		"RDS": {
			CloudResourceBasicData: apistructs.CloudResourceBasicData{
				TotalCount:  0,
				DisplayName: "",
			},
		},
		"ROCKET_MQ": {
			CloudResourceBasicData: apistructs.CloudResourceBasicData{
				TotalCount:  0,
				DisplayName: "",
			},
		},
		"REDIS": {
			CloudResourceBasicData: apistructs.CloudResourceBasicData{
				TotalCount:  0,
				DisplayName: "",
			},
		},
	}}

	allResource := map[string]*apistructs.CloudResourceTypeOverview{
		"COMPUTE":       &computeResource,
		"NETWORK":       &networkResource,
		"STORAGE":       &storageResource,
		"CLOUD_SERVICE": &cloudserviceResource,
	}
	return allResource
}

func CloudResourceOverview(ak_ctx aliyun_resources.Context) (map[string]*apistructs.CloudResourceTypeOverview, error) {

	allResource := InitCloudResourceOverview()

	regionids := aliyun_resources.ActiveRegionIDs(ak_ctx)
	var wg sync.WaitGroup
	wg.Add(6) // ecs, vpc, oss, rds, mq, redis
	// ecs
	go func() {
		start := time.Now()
		defer func() {
			wg.Done()
			timeCost("ECS", start)
		}()
		pagesizeall := 99999
		pagenum := 1
		all, total, err := ecs.List(ak_ctx, aliyun_resources.PageOption{
			PageSize:   &pagesizeall,
			PageNumber: &pagenum,
		}, regionids.ECS, "", nil)
		if err != nil {
			logrus.Errorf("ecs overview failed, error:%v", err)
			return
		}

		runningCount, gonnaExpiredCount, expiredCount, stoppedCount, postpaidCount, prepaidCount, diceManagedCount, err := ecs.Classify(all)
		if err != nil {
			logrus.Errorf("ecs classify failed, error: %v", err)
			return
		}
		allResource["COMPUTE"].ResourceTypeData["ECS"].TotalCount = total
		allResource["COMPUTE"].ResourceTypeData["ECS"].ExpireDays = aliyun_resources.CloudSourceExpireDays
		allResource["COMPUTE"].ResourceTypeData["ECS"].StatusCount = []apistructs.CloudResourceStatusCount{
			{Label: aliyun_resources.CloudSourceRunning, Status: aliyun_resources.CloudSourceRunning, Count: runningCount},
			{Label: aliyun_resources.CloudSourceBeforeExpired, Status: aliyun_resources.CloudSourceBeforeExpiredInTenDays, Count: gonnaExpiredCount},
			{Label: aliyun_resources.CloudSourceExpired, Status: aliyun_resources.CloudSourceExpired, Count: expiredCount},
			{Label: aliyun_resources.CloudSourceStopped, Status: aliyun_resources.CloudSourceStopped, Count: stoppedCount},
		}
		allResource["COMPUTE"].ResourceTypeData["ECS"].ChargeTypeCount = []apistructs.CloudResourceChargeTypeCount{
			{ChargeType: "PostPaid", Count: postpaidCount},
			{ChargeType: "PrePaid", Count: prepaidCount},
		}
		allResource["COMPUTE"].ResourceTypeData["ECS"].ManagedCount = &diceManagedCount
	}()

	// vpc
	go func() {
		start := time.Now()
		defer func() {
			wg.Done()
			timeCost("VPC", start)
		}()
		vpcs, total, err := vpc.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.VPC, "")
		if err != nil {
			logrus.Errorf("vpc overview failed, error:%v", err)
			return
		}
		labelCount := vpc.Classify(vpcs)
		allResource["NETWORK"].ResourceTypeData["VPC"].LabelCount = labelCount
		allResource["NETWORK"].ResourceTypeData["VPC"].TotalCount = total
	}()

	// oss
	go func() {
		start := time.Now()
		defer func() {
			wg.Done()
			timeCost("OSS", start)
		}()
		ossBuckets, err := oss.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.VPC, "", []string{}, "")
		if err != nil {
			logrus.Errorf("oss overview failed, error:%v", err)
			return
		}
		allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"].TotalCount = len(ossBuckets)
	}()

	// rds
	go func() {
		start := time.Now()
		defer func() {
			wg.Done()
			timeCost("RDS", start)
		}()
		ins, total, err := rds.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
		if err != nil {
			logrus.Errorf("rds overview failed, error:%v", err)
			return
		}
		// https://www.alibabacloud.com/help/zh/doc-detail/26315.htm#reference-nyz-nnn-12b
		// rds instance status list
		runningCount, gonnaExpiredCount, expiredCount, stoppedCount, postpaidCount, prepaidCount, err := rds.Classify(ins)
		if err != nil {
			logrus.Errorf("rds classify failed, error: %v", err)
			return
		}

		allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].TotalCount = total
		allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].ExpireDays = aliyun_resources.CloudSourceExpireDays
		allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].StatusCount = []apistructs.CloudResourceStatusCount{
			{Label: aliyun_resources.CloudSourceRunning, Status: aliyun_resources.CloudSourceRunning, Count: runningCount},
			{Label: aliyun_resources.CloudSourceBeforeExpired, Status: aliyun_resources.CloudSourceBeforeExpiredInTenDays, Count: gonnaExpiredCount},
			{Label: aliyun_resources.CloudSourceExpired, Status: aliyun_resources.CloudSourceExpired, Count: expiredCount},
			{Label: aliyun_resources.CloudSourceStopped, Status: aliyun_resources.CloudSourceStopped, Count: stoppedCount},
		}
		allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].ChargeTypeCount = []apistructs.CloudResourceChargeTypeCount{
			{ChargeType: "PostPaid", Count: postpaidCount},
			{ChargeType: "PrePaid", Count: prepaidCount},
		}
	}()

	// redis
	go func() {
		start := time.Now()
		defer func() {
			wg.Done()
			timeCost("REDIS", start)
		}()
		ins, err := redis.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
		if err != nil {
			logrus.Errorf("redis overview failed, error:%v", err)
			return
		}
		// redis instance status list
		// https://help.aliyun.com/document_detail/26315.html?spm=a2c4g.11186623.2.16.7aa024daPIAv9D
		runningCount, gonnaExpiredCount, expiredCount, stoppedCount, postpaidCount, prepaidCount, err := redis.Classify(ins)
		if err != nil {
			logrus.Errorf("redis classify failed, error: %v", err)
			return
		}
		allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].TotalCount = len(ins)
		allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].ExpireDays = aliyun_resources.CloudSourceExpireDays
		allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].StatusCount = []apistructs.CloudResourceStatusCount{
			{Label: aliyun_resources.CloudSourceRunning, Status: aliyun_resources.CloudSourceRunning, Count: runningCount},
			{Label: aliyun_resources.CloudSourceBeforeExpired, Status: aliyun_resources.CloudSourceBeforeExpiredInTenDays, Count: gonnaExpiredCount},
			{Label: aliyun_resources.CloudSourceExpired, Status: aliyun_resources.CloudSourceExpired, Count: expiredCount},
			{Label: aliyun_resources.CloudSourceStopped, Status: aliyun_resources.CloudSourceStopped, Count: stoppedCount},
		}
		allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].ChargeTypeCount = []apistructs.CloudResourceChargeTypeCount{
			{ChargeType: "PostPaid", Count: postpaidCount},
			{ChargeType: "PrePaid", Count: prepaidCount},
		}
	}()

	// ons
	go func() {
		start := time.Now()
		defer func() {
			wg.Done()
			timeCost("ONS", start)
		}()
		ins, err := ons.List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.ECS, "")
		if err != nil {
			logrus.Errorf("ons overview failed, error:%v", err)
			return
		}

		// ons instance status list
		// https://help.aliyun.com/document_detail/106351.html?spm=a2c4g.11186623.6.693.7bf55d78YdVqGe
		runningCount, gonnaExpiredCount, expiredCount, stoppedCount, postpaidCount, prepaidCount, err := ons.Classify(ins)
		if err != nil {
			logrus.Errorf("ons classify failed, error: %v", err)
			return
		}
		allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].TotalCount = len(ins)
		allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].ExpireDays = aliyun_resources.CloudSourceExpireDays
		allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].StatusCount = []apistructs.CloudResourceStatusCount{
			{Label: aliyun_resources.CloudSourceRunning, Status: aliyun_resources.CloudSourceRunning, Count: runningCount},
			{Label: aliyun_resources.CloudSourceBeforeExpired, Status: aliyun_resources.CloudSourceBeforeExpiredInTenDays, Count: gonnaExpiredCount},
			{Label: aliyun_resources.CloudSourceExpired, Status: aliyun_resources.CloudSourceExpired, Count: expiredCount},
			{Label: aliyun_resources.CloudSourceStopped, Status: aliyun_resources.CloudSourceStopped, Count: stoppedCount},
		}
		allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].ChargeTypeCount = []apistructs.CloudResourceChargeTypeCount{
			{ChargeType: "PostPaid", Count: postpaidCount},
			{ChargeType: "PrePaid", Count: prepaidCount},
		}
	}()

	// wait until finish
	wg.Wait()
	logrus.Infof("cloud resource overview:%+v", allResource)

	return allResource, nil
}

func GetCloudResourceOverView(ak_ctx aliyun_resources.Context, i18n *message.Printer) (map[string]*apistructs.CloudResourceTypeOverview, error) {
	allResourceRaw, err := GetCloudResourceOverViewRaw(ak_ctx)
	if err != nil {
		return InitCloudResourceOverview(), err
	}
	allResource := make(map[string]*apistructs.CloudResourceTypeOverview)
	// deep copy
	raw, _ := json.Marshal(allResourceRaw)
	err = json.Unmarshal(raw, &allResource)
	if err != nil {
		return InitCloudResourceOverview(), err
	}
	// i18n
	// COMPUTE:ECS
	for i, v := range allResource["COMPUTE"].ResourceTypeData["ECS"].StatusCount {
		allResource["COMPUTE"].ResourceTypeData["ECS"].StatusCount[i].Status = i18n.Sprintf(v.Status)
	}
	for i, v := range allResource["COMPUTE"].ResourceTypeData["ECS"].ChargeTypeCount {
		allResource["COMPUTE"].ResourceTypeData["ECS"].ChargeTypeCount[i].ChargeType = i18n.Sprintf(v.ChargeType)
	}
	// CLOUD_SERVICE:RDS
	for i, v := range allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].StatusCount {
		allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].StatusCount[i].Status = i18n.Sprintf(v.Status)
	}
	for i, v := range allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].ChargeTypeCount {
		allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"].ChargeTypeCount[i].ChargeType = i18n.Sprintf(v.ChargeType)
	}
	// CLOUD_SERVICE:REDIS
	for i, v := range allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].StatusCount {
		allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].StatusCount[i].Status = i18n.Sprintf(v.Status)
	}
	for i, v := range allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].ChargeTypeCount {
		allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"].ChargeTypeCount[i].ChargeType = i18n.Sprintf(v.ChargeType)
	}
	// CLOUD_SERVICE:ROCKET_MQ
	for i, v := range allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].StatusCount {
		allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].StatusCount[i].Status = i18n.Sprintf(v.Status)
	}
	for i, v := range allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].ChargeTypeCount {
		allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"].ChargeTypeCount[i].ChargeType = i18n.Sprintf(v.ChargeType)
	}
	return allResource, nil
}

func GetCloudResourceOverViewRaw(ak_ctx aliyun_resources.Context) (map[string]*apistructs.CloudResourceTypeOverview, error) {
	// try to get cached result
	cachedView := GetCachedCloudResourceOverview(ak_ctx)

	// get from cache & update async
	if cachedView != nil {
		allResource := InitCloudResourceOverview()
		allResource["COMPUTE"].ResourceTypeData["ECS"] = &cachedView.ECS
		allResource["NETWORK"].ResourceTypeData["VPC"] = &cachedView.VPC
		allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"] = &cachedView.OSS
		allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"] = &cachedView.RDS
		allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"] = &cachedView.REDIS
		allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"] = &cachedView.MQ
		now := time.Now()
		timestamp := now.Unix()
		// try to update if exceed 15min
		if timestamp-cachedView.LastUpdateTimestamp > 60*15 {
			go func() {
				// try to get cloud resource overview from alicloud
				newRsc, err := CloudResourceOverview(ak_ctx)
				if err != nil {
					logrus.Errorf("cloud resource overview failed, error:%v", err)
					return
				}
				var overview CachedCloudResourceOverview

				overview.ECS = *(newRsc["COMPUTE"].ResourceTypeData["ECS"])
				overview.VPC = *(newRsc["NETWORK"].ResourceTypeData["VPC"])
				overview.OSS = *(newRsc["STORAGE"].ResourceTypeData["OSS_BUCKET"])
				overview.RDS = *(newRsc["CLOUD_SERVICE"].ResourceTypeData["RDS"])
				overview.REDIS = *(newRsc["CLOUD_SERVICE"].ResourceTypeData["REDIS"])
				overview.MQ = *(newRsc["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"])
				go func() {
					start := time.Now()
					defer func() {
						timeCost("OSS SIZE", start)
					}()
					// get oss size need too much time, so update it individually
					ossBuckets, err := oss.List(ak_ctx, aliyun_resources.DefaultPageOption, []string{}, "", []string{}, "")
					if err != nil {
						logrus.Errorf("oss overview failed, error:%v", err)
						return
					}
					ossSize, err := oss.GetBucketsSize(ak_ctx, ossBuckets)
					if err != nil {
						logrus.Errorf("oss overview failed, error:%v", err)
						return
					}
					newRsc["STORAGE"].ResourceTypeData["OSS_BUCKET"].StorageUsage = cachedView.OSS.StorageUsage
					if ossSize > 0 {
						newRsc["STORAGE"].ResourceTypeData["OSS_BUCKET"].StorageUsage = &ossSize
					}
					newRsc["STORAGE"].ResourceTypeData["OSS_BUCKET"].TotalCount = len(ossBuckets)
					overview.OSS = *(allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"])
					PutCachedCloudResourceOverview(ak_ctx, overview)
				}()
				PutCachedCloudResourceOverview(ak_ctx, overview)
			}()
		}

		return allResource, nil
	}

	// get from alicloud & update async
	allResource, err := CloudResourceOverview(ak_ctx)
	if err != nil {
		logrus.Errorf("cloud resource overview failed, error:%v", err)
		return InitCloudResourceOverview(), err
	}
	go func() {
		var overview CachedCloudResourceOverview
		overview.ECS = *(allResource["COMPUTE"].ResourceTypeData["ECS"])
		overview.VPC = *(allResource["NETWORK"].ResourceTypeData["VPC"])
		overview.OSS = *(allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"])
		overview.RDS = *(allResource["CLOUD_SERVICE"].ResourceTypeData["RDS"])
		overview.REDIS = *(allResource["CLOUD_SERVICE"].ResourceTypeData["REDIS"])
		overview.MQ = *(allResource["CLOUD_SERVICE"].ResourceTypeData["ROCKET_MQ"])

		go func() {
			start := time.Now()
			defer func() {
				timeCost("OSS SIZE", start)
			}()
			// get oss size need too much time, so update it individually
			ossBuckets, err := oss.List(ak_ctx, aliyun_resources.DefaultPageOption, []string{}, "", []string{}, "")
			if err != nil {
				logrus.Errorf("oss overview failed, error:%v", err)
				allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"].TotalCount = 0
				return
			}
			ossSize, err := oss.GetBucketsSize(ak_ctx, ossBuckets)
			if err != nil {
				logrus.Errorf("oss overview failed, error:%v", err)
				return
			}
			allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"].StorageUsage = &ossSize
			allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"].TotalCount = len(ossBuckets)
			overview.OSS = *(allResource["STORAGE"].ResourceTypeData["OSS_BUCKET"])
			PutCachedCloudResourceOverview(ak_ctx, overview)
		}()

		PutCachedCloudResourceOverview(ak_ctx, overview)
	}()

	return allResource, nil
}

func GetCachedCloudResourceOverview(ak_ctx aliyun_resources.Context) *CachedCloudResourceOverview {
	var overview CachedCloudResourceOverview
	key := fmt.Sprintf("%s/%s/%s/%s", aliyun_resources.CloudResourcePrefix, ak_ctx.OrgID, ak_ctx.Vendor, aliyun_resources.ResourceOverview)
	logrus.Infof("cached overview key:%s", key)
	err := ak_ctx.CachedJs.Get(context.Background(), key, &overview)
	if err != nil {
		logrus.Errorf("get cached cloud resource overview failed, key:%s, error:%v", key, err)
		return nil
	}
	if overview.VPC.TotalCount == 0 {
		return nil
	}

	return &overview
}

func PutCachedCloudResourceOverview(ak_ctx aliyun_resources.Context, overview CachedCloudResourceOverview) {
	now := time.Now()
	timestamp := now.Unix()
	overview.LastUpdateTimestamp = timestamp
	key := fmt.Sprintf("%s/%s/%s/%s", aliyun_resources.CloudResourcePrefix, ak_ctx.OrgID, ak_ctx.Vendor, aliyun_resources.ResourceOverview)
	err := ak_ctx.CachedJs.Put(context.Background(), key, overview)
	if err != nil {
		logrus.Errorf("put cached cloud resource overview failed, key:%s, error:%v", key, err)
	}
}
