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

import (
	"context"
	stdtime "time"

	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/common"
)

func GetInterval(startTimeMs, endTimeMs int64, minInterval stdtime.Duration, preferredPoints int64) string {
	interval := stdtime.Duration((endTimeMs - startTimeMs) / preferredPoints / 1e3 * 1e9)
	if interval < minInterval {
		interval = minInterval
	}
	return interval.String()
}

func ToInterface(value []int64) []interface{} {
	arr := make([]interface{}, 0)
	for _, v := range value {
		arr = append(arr, v)
	}
	return arr
}

func StringToInterface(value []string) []interface{} {
	arr := make([]interface{}, 0)
	for _, v := range value {
		arr = append(arr, v)
	}
	return arr
}

func GetMonitorAlertServiceFromContext(ctx context.Context) monitorpb.AlertServiceServer {
	val := ctx.Value(common.ContextKeyServiceMonitorAlertService)
	if val == nil {
		return nil
	}

	typed, ok := val.(monitorpb.AlertServiceServer)
	if !ok {
		return nil
	}
	return typed
}

func GetMonitorMetricServiceFromContext(ctx context.Context) metricpb.MetricServiceServer {
	val := ctx.Value(common.ContextKeyServiceMonitorMetricService)
	if val == nil {
		return nil
	}

	typed, ok := val.(metricpb.MetricServiceServer)
	if !ok {
		return nil
	}
	return typed
}
