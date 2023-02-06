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

package i18n

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/i18n"
)

func TransferTaskStatus(status apistructs.PipelineStatus, i18nLocale *i18n.LocaleResource) string {
	switch status {
	case apistructs.PipelineStatusAnalyzed, apistructs.PipelineStatusBorn:
		return i18nLocale.Get(I18nKeyStatusInitialized)
	case apistructs.PipelineStatusCreated:
		return i18nLocale.Get(I18nKeyStatusCreated)
	case apistructs.PipelineStatusMark, apistructs.PipelineStatusQueue:
		return i18nLocale.Get(I18nKeyStatusQueue)
	case apistructs.PipelineStatusRunning:
		return i18nLocale.Get(I18nKeyStatusExecuting)
	case apistructs.PipelineStatusSuccess:
		return i18nLocale.Get(I18nKeyStatusExecuteSuccess)
	case apistructs.PipelineStatusFailed:
		return i18nLocale.Get(I18nKeyStatusExecuteFailed)
	case apistructs.PipelineStatusAnalyzeFailed:
		return i18nLocale.Get(I18nKeyStatusInitializeFailed)
	case apistructs.PipelineStatusPaused:
		return i18nLocale.Get(I18nKeyStatusPause)
	case apistructs.PipelineStatusCreateError:
		return i18nLocale.Get(I18nKeyStatusCreateFailed)
	case apistructs.PipelineStatusStartError:
		return i18nLocale.Get(I18nKeyStatusStartFailed)
	case apistructs.PipelineStatusTimeout:
		return i18nLocale.Get(I18nKeyStatusTimeout)
	case apistructs.PipelineStatusStopByUser, apistructs.PipelineStatusCancelByRemote:
		return i18nLocale.Get(I18nKeyStatusCanceled)
	case apistructs.PipelineStatusNoNeedBySystem:
		return i18nLocale.Get(I18nKeyStatusNoNeedBySystem)
	case apistructs.PipelineStatusInitializing:
		return i18nLocale.Get(I18nKeyStatusInitializing)
	case apistructs.PipelineStatusError, apistructs.PipelineStatusUnknown, apistructs.PipelineStatusDBError, apistructs.PipelineStatusLostConn:
		return i18nLocale.Get(I18nKeyStatusAbnormal)
	case apistructs.PipelineStatusDisabled:
		return i18nLocale.Get(I18nKeyStatusDisable)
	case apistructs.PipelineStatusWaitApproval:
		return i18nLocale.Get(I18nKeyStatusApprovalWait)
	case apistructs.PipelineStatusApprovalSuccess:
		return i18nLocale.Get(I18nKeyStatusApprovalSuccess)
	case apistructs.PipelineStatusApprovalFail:
		return i18nLocale.Get(I18nKeyStatusApprovalFailed)
	default:
		return ""
	}
}
