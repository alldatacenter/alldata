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

package common

const ScenarioKey = "msp-alert-event-detail"

const (
	ComponentNameMoreOperations    = "moreOperations"
	ComponentNamePauseFormModal    = "pauseFormModal"
	ComponentNameEventOverviewInfo = "eventOverviewInfo"
	ComponentNameEventStatusInfo   = "eventStatusInfo"
	ComponentNameMonitorDataChart  = "monitorDataChart"
	ComponentNameEventHistoryTable = "eventHistoryTable"
)

const (
	StateKeyAlertEvent = "alertEvent"
	StateKeyPageTitle  = "pageTitle"
)

const (
	GlobalStateKeyConfigurableFilterOptionsKey = "gsConfigurableFilterOptions"
	GlobalStateKeyPaging                       = "table_paging"
	GlobalStateKeySort                         = "table_sort"
)

const (
	AlertEventStateAlert   = "alert"
	AlertEventStateRecover = "recover"
	AlertEventStatePause   = "pause"
	AlertEventStateStop    = "stop"
)

const TimeFormatLayout = "2006/01/02 15:04:05"
const DefaultPageSize = 10
