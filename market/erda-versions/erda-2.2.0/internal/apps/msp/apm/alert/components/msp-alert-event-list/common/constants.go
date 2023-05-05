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

const ScenarioKey = "msp-alert-event-list"

const (
	ComponentNameConfigurableFilter = "configurableFilter"
	ComponentNameSearchFilter       = "searchFilter"
	ComponentNameTable              = "table"
)

const (
	GlobalStateKeyConfigurableFilterOptionsKey = "gsConfigurableFilterOptions"
	GlobalStateKeySearchFilterOptionsKey       = "gsSearchFilterOptions"
	GlobalStateKeyPaging                       = "table_paging"
	GlobalStateKeySort                         = "table_sort"
)

const DefaultPageSize = 20

const (
	Fatal    string = "FATAL"
	Critical string = "CRITICAL"
	Warning  string = "WARNING"
	Notice   string = "NOTICE"

	Alert   string = "alert"
	Recover string = "recover"
	Pause   string = "pause"
	Stop    string = "stop"

	System string = "System"
	Custom string = "Custom"

	ColorProcessing string = "processing"
	ColorDefault    string = "default"
	ColorWarning    string = "warning"
	ColorError      string = "error"
)

var LevelColors = map[string]string{
	Fatal:    ColorError,
	Critical: ColorWarning,
	Warning:  ColorDefault,
	Notice:   ColorProcessing,
}

var StateColors = map[string]string{
	Alert:   ColorError,
	Recover: ColorProcessing,
	Pause:   ColorDefault,
	Stop:    ColorWarning,
}
