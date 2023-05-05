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

package vpc

import (
	"fmt"

	libvpc "github.com/aliyun/alibaba-cloud-sdk-go/services/vpc"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	"github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources/ecs"
)

func GetVpcByCluster(ak_ctx aliyun_resources.Context, cluster string) (libvpc.Vpc, error) {
	regionids := aliyun_resources.ActiveRegionIDs(ak_ctx)
	vpcs, _, err := List(ak_ctx, aliyun_resources.DefaultPageOption, regionids.ECS, cluster)
	if err != nil || len(vpcs) != 1 {
		var e error
		if err != nil {
			e = fmt.Errorf("failed to get vpclist: %v", err)
		} else if len(vpcs) == 0 {
			e = fmt.Errorf("cannot get vpc info by cluserName, please tag vpc with tag [dice-cluster/%s: true] first", cluster)
		} else {
			e = fmt.Errorf("vpc number in cluster[%s] is more than 1,  num is: %d", cluster, len(vpcs))
		}
		logrus.Errorf("get region info by failed, cluster: %s, regions:%v, error:%v", cluster, regionids, e)
		return libvpc.Vpc{}, e
	}
	return vpcs[0], nil
}

func GetVpcBaseInfo(ak_ctx aliyun_resources.Context, cluster string, vpcID string) (apistructs.CloudResourceVpcBaseInfo, error) {
	if vpcID == "" && cluster == "" {
		logrus.Info("empty vpcID and cluster name")
		return apistructs.CloudResourceVpcBaseInfo{}, nil
	}

	var vpcInfo libvpc.Vpc
	pageSize := 1
	pageNum := 1
	defaultPage := aliyun_resources.PageOption{PageSize: &pageSize, PageNumber: &pageNum}

	// get vpc base info by vpcID or cluster name
	// try to get vpc info: vpc id, vpc cidr
	if vpcID != "" {
		ak_ctx.VpcID = vpcID
		v, err := DescribeVPCs(ak_ctx, defaultPage, "", vpcID)
		if err != nil || v == nil || v.TotalCount == 0 {
			var e error
			if err != nil {
				e = err
			} else {
				e = fmt.Errorf("cannot get vpc info by vpc id:%s", vpcID)
			}
			return apistructs.CloudResourceVpcBaseInfo{}, e
		}
		vpcInfo = v.Vpcs.Vpc[0]
	} else if cluster != "" {
		v, err := GetVpcByCluster(ak_ctx, cluster)
		if err != nil {
			return apistructs.CloudResourceVpcBaseInfo{}, err
		}
		vpcInfo = v
		ak_ctx.VpcID = v.VpcId
		ak_ctx.Region = v.RegionId
	}

	logrus.Infof("get vpc info:%+v", vpcInfo)

	// try to get vsw info: vsw id, zone id
	if vpcInfo.VpcId == "" {
		err := fmt.Errorf("failed to get vpc id, empty vpc id, cluster:%s", cluster)
		logrus.Errorf(err.Error())
		return apistructs.CloudResourceVpcBaseInfo{}, err
	}
	rsp, err := ecs.DescribeResource(ak_ctx, defaultPage, "", "", nil)
	if err != nil || rsp == nil || rsp.TotalCount == 0 {
		var e error
		if rsp == nil {
			e = fmt.Errorf("cannot get target zone info, empty response")
		} else if rsp.TotalCount == 0 {
			e = fmt.Errorf("cannot get target zone info, no ecs in this vpc")
		} else {
			e = fmt.Errorf("cannot get target zone info: %v", err)
		}
		logrus.Errorf(e.Error())
		return apistructs.CloudResourceVpcBaseInfo{}, e
	}
	return apistructs.CloudResourceVpcBaseInfo{
		Region:    ak_ctx.Region,
		VpcID:     vpcInfo.VpcId,
		VpcCIDR:   vpcInfo.CidrBlock,
		VSwitchID: rsp.Instances.Instance[0].VpcAttributes.VSwitchId,
		ZoneID:    rsp.Instances.Instance[0].ZoneId,
	}, nil
}
