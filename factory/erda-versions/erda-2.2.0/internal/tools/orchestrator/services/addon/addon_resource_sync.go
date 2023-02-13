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

package addon

import (
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

func (a *Addon) SyncAddonResources() {
	orgidList := a.getAllOrgIDs()
	for _, orgid := range orgidList {
		addons, err := a.ListAddonInstanceByOrg(orgid)
		if err != nil {
			logrus.Errorf("ListAddonInstanceByOrg: %v", err)
			continue
		}
		for _, addon := range addons {
			podList, err := a.instanceinfoImpl.GetPodInfo(apistructs.PodInfoRequest{
				OrgID:   strconv.FormatUint(orgid, 10),
				AddonID: addon.ID,
			})
			if err != nil {
				logrus.Errorf("failed to getpodinfo(orgid:%d, addonid:%s): %v", orgid, addon.ID, err)
				break
			}
			if err := a.updateAddonInstanceResource(addon, podList); err != nil {
				logrus.Errorf("UpdateAddonInstanceResource: %v", err)
				break
			}
		}

	}
}

func (a *Addon) getAllOrgIDs() []uint64 {
	orgs, err := a.bdl.ListDopOrgs(&apistructs.OrgSearchRequest{PageSize: 99999})
	if err != nil {
		return nil
	}
	var orgids []uint64
	for _, org := range orgs.List {
		orgids = append(orgids, org.ID)
	}
	return orgids
}

func (a *Addon) updateAddonInstanceResource(addon dbclient.AddonInstance, pods apistructs.PodInfoDataList) error {
	var cpurequest, cpulimit float64
	var memrequest, memlimit int
	for _, pod := range pods {
		cpurequest += pod.CpuRequest
		cpulimit += pod.CpuLimit
		memrequest += pod.MemRequest
		memlimit += pod.MemLimit
	}
	return a.db.UpdateAddonInstanceResource(addon.ID, cpurequest, cpulimit, memrequest, memlimit)
}
