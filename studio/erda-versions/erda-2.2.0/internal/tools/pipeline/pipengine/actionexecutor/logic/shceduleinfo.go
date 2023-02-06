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

package logic

import (
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy"
	"github.com/erda-project/erda/pkg/strutil"
)

func GetScheduleInfo(cluster apistructs.ClusterInfo, executorName, executorKind string, jobFromUser apistructs.JobFromUser) (apistructs.ScheduleInfo2, apistructs.ScheduleInfo, error) {
	job := apistructs.Job{
		JobFromUser: jobFromUser,
	}

	enableTag := true
	if cluster.SchedConfig != nil {
		enableTag = cluster.SchedConfig.EnableTag
	}
	// TODO migrate enableWorkspace, cluster-manager should control this param
	enableWorkspace := true
	if cluster.SchedConfig != nil {
		enableWorkspace = cluster.SchedConfig.EnableWorkspace
	}
	configs := executorconfig.ExecutorWholeConfigs{
		BasicConfig: map[string]string{
			"ENABLETAG":        strconv.FormatBool(enableTag),
			"ENABLE_WORKSPACE": strconv.FormatBool(enableWorkspace),
		},
	}
	scheduleInfo2, scheduleInfo, _, err := schedulepolicy.LabelFilterChain(&configs, executorName, strutil.ToUpper(executorKind), job)
	if err != nil {
		return apistructs.ScheduleInfo2{}, apistructs.ScheduleInfo{}, fmt.Errorf("failed to get task scheduleInfo2, err: %v", err)
	}
	return scheduleInfo2, scheduleInfo, nil
}
