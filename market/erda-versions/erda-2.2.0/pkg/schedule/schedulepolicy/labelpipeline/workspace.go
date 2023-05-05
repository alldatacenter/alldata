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
	"sort"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
	"github.com/erda-project/erda/pkg/strutil"
)

// Workspace label (ie env label, distinguish dev, test, staging, prod)
func WorkspaceLabelFilter(
	r *labelconfig.RawLabelRuleResult, r2 *labelconfig.RawLabelRuleResult2, li *labelconfig.LabelInfo) {
	envName_ := li.Label[labelconfig.WORKSPACE_KEY]
	envName := strings.ToLower(envName_)
	for _, selectors := range li.Selectors {
		if v, ok := selectors["workspace"]; ok && len(v.Values) > 0 {
			envName = v.Values[0]
			break
		}
	}
	if envName == "" {
		// Applications that are not labeled with env should not be scheduled to any node that has this label
		r.UnLikePrefixs = append(r.UnLikePrefixs, labelconfig.WORKSPACE_VALUE_PREFIX)
		r2.HasWorkSpace = false
		logrus.Infof("obj(%s) have no workspace label", li.ObjName)
		return
	}

	var (
		workspaceopt *executorconfig.WorkspaceOpt
		orgopt       *executorconfig.OrgOpt
	)
	// Get whether workspace scheduling is enabled from the fine configuration of the cluster, and the workspace configuration is under org
	orgName, orgOK := li.Label[labelconfig.ORG_KEY]
	for _, selectors := range li.Selectors {
		if v, ok := selectors["org"]; ok && len(v.Values) > 0 {
			orgName = v.Values[0]
			orgOK = true
			break
		}
	}
	// For applications that are not marked with org, in the fine configuration, it is not possible to find whether to enable the workspace scheduling configuration
	// Go to the basic configuration of the cluster to read whether workspace scheduling is enabled
	if !orgOK || li.OptionsPlus == nil {
		goto basic
	}

	orgopt = li.ExecutorConfig.OrgOpt(orgName)
	if orgopt == nil {
		goto basic
	}

	workspaceopt = orgopt.WorkspaceOpt(envName)
	if workspaceopt == nil {
		goto basic
	}

	if workspaceopt.EnableWorkspaceLabelSchedule() {
		if fixJobDest(r, r2, li, orgName, envName, li.ExecutorConfig, false) {
			return
		}
		r.ExclusiveLikes = append(r.ExclusiveLikes, labelconfig.WORKSPACE_VALUE_PREFIX+envName)
		r2.HasWorkSpace = true
		r2.WorkSpaces = []string{envName}
		// We have already set up the `workspace' label, and we
		// don't have to perform the `basic' part of the process
		// below, so just return here.
		return
	}

basic:
	// Get whether to enable workspace scheduling from the basic configuration
	enableEnvScheduler := li.ExecutorConfig.EnableWorkspaceLabelSchedule()
	// Workspace scheduling is not enabled
	if !enableEnvScheduler {
		// Compatible with old WORKSPACETAGS tags
		if tag, tagOK := li.ExecutorConfig.WORKSPACETAGSForCompatibility(); tagOK {
			tags := strings.Split(tag, ",")
			sort.Strings(tags)
			idx := sort.SearchStrings(tags, envName)
			if idx < len(tags) && tags[idx] == envName {
				r.ExclusiveLikes = append(r.ExclusiveLikes, labelconfig.WORKSPACE_VALUE_PREFIX+envName)
				r2.HasWorkSpace = true
				r2.WorkSpaces = []string{envName}
			} else {
				r.UnLikePrefixs = append(r.UnLikePrefixs, labelconfig.WORKSPACE_VALUE_PREFIX)
				r2.HasWorkSpace = false
			}
			return
		}
		r.UnLikePrefixs = append(r.UnLikePrefixs, labelconfig.WORKSPACE_VALUE_PREFIX)
		r2.HasWorkSpace = false
		return
	}

	if fixJobDest(r, r2, li, orgName, envName, li.ExecutorConfig, true) {
		return
	}
	r.ExclusiveLikes = append(r.ExclusiveLikes, labelconfig.WORKSPACE_VALUE_PREFIX+envName)
	r2.HasWorkSpace = true
	r2.WorkSpaces = []string{envName}
}

// fixJobDest For Staging and Prod jobs, additional destination workspaces can be configured, and the corresponding configuration is as follows:
// "STAGING_JOB_DEST":"dev"
// "PROD_JOB_DEST":"dev,test"
func fixJobDest(r *labelconfig.RawLabelRuleResult, r2 *labelconfig.RawLabelRuleResult2, li *labelconfig.LabelInfo, org, workspace string, options *executorconfig.ExecutorWholeConfigs, basicconfig bool) bool {
	if (li.ExecutorKind != labelconfig.EXECUTOR_METRONOME) && (li.ExecutorKind != labelconfig.EXECUTOR_K8SJOB) {
		return false
	}
	// The caller guarantees that 'opt' ​​is non-nil
	var opt interface {
		StagingJobAvailDest() ([]string, bool)
		ProdJobAvailDest() ([]string, bool)
	} = options
	if !basicconfig {
		opt = options.OrgOpt(org).WorkspaceOpt(workspace)
	}
	var (
		dests  []string
		destOK bool
	)
	switch workspace {
	case labelconfig.WORKSPACE_STAGING:
		dests, destOK = opt.StagingJobAvailDest()
	case labelconfig.WORKSPACE_PROD:
		dests, destOK = opt.ProdJobAvailDest()
	default:
		return false
	}
	if !destOK {
		// Default behavior when 'STAGING_JOB_DEST' or 'PROD_JOB_DEST' is not set, but is this really appropriate?
		r.InclusiveLikes = append(r.InclusiveLikes,
			labelconfig.WORKSPACE_VALUE_PREFIX+labelconfig.WORKSPACE_DEV)
		r.InclusiveLikes = append(r.InclusiveLikes,
			labelconfig.WORKSPACE_VALUE_PREFIX+labelconfig.WORKSPACE_TEST)
		r2.HasWorkSpace = true
		// The k8s cluster is not configured with default values ​​and is scheduled according to the actual workspace delivered
		r2.WorkSpaces = append(r2.WorkSpaces, workspace)
		return true
	}
	r.InclusiveLikes = append(r.InclusiveLikes, strutil.Map(dests, func(s string) string {
		return strutil.Concat(labelconfig.WORKSPACE_VALUE_PREFIX, s)
	})...)
	r2.HasWorkSpace = true
	r2.WorkSpaces = append(r2.WorkSpaces, dests...)
	return true
}
