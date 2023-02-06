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

package apm

import (
	"strconv"
)

const (
	Tags                      = "tags"
	Fields                    = "fields"
	Timestamp                 = "timestamp"
	Columns                   = "columns"
	TagsTargetApplicationId   = Tags + Sep4 + "target_application_id"
	TagsTargetRuntimeName     = Tags + Sep4 + "target_runtime_name"
	TagsTargetServiceId       = Tags + Sep4 + "target_service_id"
	TagsTargetServiceName     = Tags + Sep4 + "target_service_name"
	TagsTargetApplicationName = Tags + Sep4 + "target_application_name"
	TagsTargetRuntimeId       = Tags + Sep4 + "target_runtime_id"
	TagsTargetTerminusKey     = Tags + Sep4 + "target_terminus_key"
	TagsServiceMesh           = Tags + Sep4 + "service_mesh"
	TagsSourceApplicationId   = Tags + Sep4 + "source_application_id"
	TagsSourceRuntimeName     = Tags + Sep4 + "source_runtime_name"
	TagsSourceServiceName     = Tags + Sep4 + "source_service_name"
	TagsSourceServiceId       = Tags + Sep4 + "source_service_id"
	TagsSourceApplicationName = Tags + Sep4 + "source_application_name"
	TagsTargetAddonType       = Tags + Sep4 + "target_addon_type"
	TagsSourceRuntimeId       = Tags + Sep4 + "source_runtime_id"
	TagsSourceAddonType       = Tags + Sep4 + "source_addon_type"
	TagsTargetAddonId         = Tags + Sep4 + "target_addon_id"
	TagsTargetAddonGroup      = Tags + Sep4 + "target_addon_group"
	TagsSourceAddonId         = Tags + Sep4 + "source_addon_id"
	TagsSourceAddonGroup      = Tags + Sep4 + "source_addon_group"
	TagsSourceTerminusKey     = Tags + Sep4 + "source_terminus_key"
	TagsComponent             = Tags + Sep4 + "component"
	TagsDBType                = Tags + Sep4 + "db_type"
	TagsHost                  = Tags + Sep4 + "host"
	TagsHttpUrl               = Tags + Sep4 + "http_url"
	TagsPeerServiceScope      = Tags + Sep4 + "peer_service_scope"
	TagsPeerAddress           = Tags + Sep4 + "peer_address"
	TagsDBSystem              = Tags + Sep4 + "db_system"
	TagsDBHost                = Tags + Sep4 + "db_host"
	TagsPeerService           = Tags + Sep4 + "peer_service"
	TagsApplicationId         = Tags + Sep4 + "application_id"
	TagsApplicationName       = Tags + Sep4 + "application_name"
	TagsRuntimeId             = Tags + Sep4 + "runtime_id"
	TagsRuntimeName           = Tags + Sep4 + "runtime_name"
	TagsTerminusKey           = Tags + Sep4 + "terminus_key"
	TagsServiceName           = Tags + Sep4 + "service_name"
	TagsServiceId             = Tags + Sep4 + "service_id"
	FieldsCountSum            = Fields + Sep4 + "count_sum"
	FieldElapsedSum           = Fields + Sep4 + "elapsed_sum"
	FieldsErrorsSum           = Fields + Sep4 + "errors_sum"
)

const (
	Sep1              = "-"
	Sep2              = "*"
	Sep3              = "_"
	Sep4              = "."
	EmptyIndex        = Spot + Sep1 + "empty"
	Spot              = "spot"
	TimeForSplitIndex = 24 * 60 * 60 * 1000
)

const (
	// permission resources
	Monitor             string = "Monitor"
	MonitorTopology     string = "monitor_topology"
	Report              string = "report"
	MonitorProjectAlert string = "monitor_project_alert"
	MicroService        string = "microservice_metric"
)

func CreateEsIndices(indexKey string, startTimeMs int64, endTimeMs int64) []string {
	var indices []string
	if startTimeMs > endTimeMs {
		indices = append(indices, EmptyIndex)
	}
	timestampMs := startTimeMs - startTimeMs%TimeForSplitIndex
	endTimeMs = endTimeMs - endTimeMs%TimeForSplitIndex

	for startTimestampMs := timestampMs; startTimestampMs <= endTimeMs; startTimestampMs += TimeForSplitIndex {
		index := Spot + Sep1 + indexKey + Sep1 + Sep2 + Sep1 + strconv.FormatInt(startTimestampMs, 10)
		indices = append(indices, index)
	}
	if len(indices) <= 0 {
		indices = append(indices, EmptyIndex)
	}
	return indices
}
