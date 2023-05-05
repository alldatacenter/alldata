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

package clusters

import (
	"errors"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

func (c *Clusters) ClusterPreview(req apistructs.CloudClusterRequest) ([]apistructs.CloudResource, error) {

	if req.CloudVendor == apistructs.CloudVendorAliEcs {
		return c.alicloudEcsPreview(req)
	} else if req.CloudVendor == apistructs.CloudVendorAliAck ||
		req.CloudVendor == apistructs.CloudVendorAliCS ||
		req.CloudVendor == apistructs.CloudVendorAliCSManaged {
		return c.alicloudAckPreview(req)
	} else {
		errstr := fmt.Sprintf("cloud vendor:%v is not valid", req.CloudVendor)
		logrus.Errorf(errstr)
		return nil, errors.New(errstr)
	}
}

func (c *Clusters) alicloudEcsPreview(req apistructs.CloudClusterRequest) ([]apistructs.CloudResource, error) {
	var resourceSummary []apistructs.CloudResource
	var ecsResource, slbResource, natResource, nasResource apistructs.CloudResource

	ecsResource.Resource = apistructs.ResourceEcs
	ecsResource.ResourceProfile = apistructs.ResourceEcs.GetResSpec()
	ecsResource.ResourceNum = req.ClusterSpec.GetSpecNum()
	ecsResource.ChargeType = req.ChargeType
	ecsResource.ChargePeriod = req.ChargePeriod

	slbResource.Resource = apistructs.ResourceSlb
	slbResource.ResourceProfile = apistructs.ResourceSlb.GetResSpec()
	slbResource.ResourceNum = 1
	slbResource.ChargeType = req.ChargeType
	slbResource.ChargePeriod = req.ChargePeriod

	natResource.Resource = apistructs.ResourceNat
	natResource.ResourceProfile = apistructs.ResourceNat.GetResSpec()
	natResource.ResourceNum = 1
	natResource.ChargeType = req.ChargeType
	natResource.ChargePeriod = req.ChargePeriod

	nasResource.Resource = apistructs.ResourceNAS
	nasResource.ResourceProfile = apistructs.ResourceNAS.GetResSpec()
	nasResource.ResourceNum = 1
	nasResource.ChargeType = req.ChargeType
	nasResource.ChargePeriod = req.ChargePeriod

	resourceSummary = append(resourceSummary, ecsResource, slbResource, natResource, nasResource)

	return resourceSummary, nil
}

func (c *Clusters) alicloudAckPreview(req apistructs.CloudClusterRequest) ([]apistructs.CloudResource, error) {
	var resourceSummary []apistructs.CloudResource
	var ecsResource, slbResource, natResource, nasResource apistructs.CloudResource

	ecsResource.Resource = apistructs.ResourceEcs
	ecsResource.ResourceProfile = []string{"ecs.n2.xlarge", "System Disk: cloud_ssd, 200G"}
	ecsResource.ResourceNum = 3
	ecsResource.ChargeType = req.ChargeType
	ecsResource.ChargePeriod = req.ChargePeriod

	slbResource.Resource = apistructs.ResourceSlb
	slbResource.ResourceProfile = []string{"slb.s1.small"}
	slbResource.ResourceNum = 1
	slbResource.ChargeType = req.ChargeType
	slbResource.ChargePeriod = req.ChargePeriod

	natResource.Resource = apistructs.ResourceNat
	natResource.ResourceProfile = apistructs.ResourceNat.GetResSpec()
	natResource.ResourceNum = 1
	natResource.ChargeType = req.ChargeType
	natResource.ChargePeriod = req.ChargePeriod

	nasResource.Resource = apistructs.ResourceNAS
	nasResource.ResourceProfile = apistructs.ResourceNAS.GetResSpec()
	nasResource.ResourceNum = 1
	nasResource.ChargeType = req.ChargeType
	nasResource.ChargePeriod = req.ChargePeriod

	resourceSummary = append(resourceSummary, ecsResource, slbResource, natResource, nasResource)

	return resourceSummary, nil
}
