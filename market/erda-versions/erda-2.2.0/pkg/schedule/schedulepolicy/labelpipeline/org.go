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

package labelpipeline

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

// Tenant label
func OrgLabelFilter(
	r *labelconfig.RawLabelRuleResult, r2 *labelconfig.RawLabelRuleResult2, li *labelconfig.LabelInfo) {
	// Two ways to get orgName
	// 1. Labelconfig.ORG_KEY in diceyml.meta (li.Label)
	// 2. diceyml.deployments.selectors.org, as long as a certain service exists, take the first one
	// Priority 2> 1
	orgName := li.Label[labelconfig.ORG_KEY]
	for _, selectors := range li.Selectors {
		if v, ok := selectors["org"]; ok && len(v.Values) > 0 {
			orgName = v.Values[0]
			break
		}
	}
	if orgName == "" {
		r2.HasOrg = false
		logrus.Infof("obj(%s) have no orgName", li.ObjName)
		return
	}
	// Get whether to enable org scheduling from the cluster fine configuration
	if li.OptionsPlus != nil {
		for _, orgCfg := range li.OptionsPlus.Orgs {
			if orgCfg.Name == orgName {
				if v, orgOK := orgCfg.Options[labelconfig.ENABLE_ORG]; orgOK && v == "true" {
					r.ExclusiveLikes = append(r.ExclusiveLikes, labelconfig.ORG_VALUE_PREFIX+orgName)
					r2.HasOrg = true
					r2.Org = orgName
					logrus.Infof("obj(%s) got refined org configuration", li.ObjName)
					return
				}
				break
			}
		}
	}

	// Get whether to enable org scheduling from the basic configuration
	// It is generally recommended that whether org is enabled or not is placed in the fine configuration, and org is not configured in the basic configuration, that is, org is not enabled by default
	enableOrgScheduler := li.ExecutorConfig.EnableOrgLabelSchedule()
	// Org scheduling is not turned on
	if !enableOrgScheduler {
		r.UnLikePrefixs = append(r.UnLikePrefixs, labelconfig.ORG_VALUE_PREFIX)
		r2.HasOrg = false
		return
	}

	r.ExclusiveLikes = append(r.ExclusiveLikes, labelconfig.ORG_VALUE_PREFIX+orgName)
	r2.HasOrg = true
	r2.Org = orgName
}
