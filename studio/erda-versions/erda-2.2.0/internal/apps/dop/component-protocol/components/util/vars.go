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

package util

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
)

const ColumnPipelineStatus = "pipelineStatus"

var PipelineDefinitionStatus = []apistructs.PipelineStatus{
	apistructs.PipelineStatusAnalyzed,
	apistructs.PipelineStatusRunning,
	apistructs.PipelineStatusSuccess,
	apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusStopByUser,
}

var PipelineDefinitionStatusMap = map[apistructs.PipelineStatus]apistructs.PipelineStatus{
	apistructs.PipelineStatusAnalyzed:        apistructs.PipelineStatusAnalyzed,
	apistructs.PipelineStatusBorn:            apistructs.PipelineStatusAnalyzed,
	apistructs.PipelineStatusCreated:         apistructs.PipelineStatusAnalyzed,
	apistructs.PipelineStatusMark:            apistructs.PipelineStatusAnalyzed,
	apistructs.PipelineStatusQueue:           apistructs.PipelineStatusAnalyzed,
	apistructs.PipelineStatusInitializing:    apistructs.PipelineStatusAnalyzed,
	apistructs.PipelineStatusRunning:         apistructs.PipelineStatusRunning,
	apistructs.PipelineStatusSuccess:         apistructs.PipelineStatusSuccess,
	apistructs.PipelineStatusFailed:          apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusAnalyzeFailed:   apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusPaused:          apistructs.PipelineStatusRunning,
	apistructs.PipelineStatusCreateError:     apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusStartError:      apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusTimeout:         apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusStopByUser:      apistructs.PipelineStatusStopByUser,
	apistructs.PipelineStatusNoNeedBySystem:  apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusCancelByRemote:  apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusError:           apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusUnknown:         apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusDBError:         apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusLostConn:        apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusDisabled:        apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusWaitApproval:    apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusApprovalSuccess: apistructs.PipelineStatusFailed,
	apistructs.PipelineStatusApprovalFail:    apistructs.PipelineStatusFailed,
}

var PipelineDefinitionStatusMapList = func() map[apistructs.PipelineStatus][]apistructs.PipelineStatus {
	var mapList = map[apistructs.PipelineStatus][]apistructs.PipelineStatus{}
	for k, v := range PipelineDefinitionStatusMap {
		if mapList[v] == nil {
			mapList[v] = []apistructs.PipelineStatus{}
		}
		mapList[v] = append(mapList[v], k)
	}
	return mapList
}()

func TransferStatus(status string) []string {
	var statusString []string
	for _, v := range PipelineDefinitionStatusMapList[apistructs.PipelineStatus(status)] {
		statusString = append(statusString, v.String())
	}
	return statusString
}

func DisplayStatusText(ctx context.Context, status string) string {
	if status == "" {
		return "-"
	}
	if _, ok := PipelineDefinitionStatusMap[apistructs.PipelineStatus(status)]; !ok {
		return "-"
	}
	return cputil.I18n(ctx, ColumnPipelineStatus+PipelineDefinitionStatusMap[apistructs.PipelineStatus(status)].String())
}
