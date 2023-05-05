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
	"time"

	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/common"
)

func SetAlertEventToGlobalState(gs cptype.GlobalStateData, alertEvent *pb.AlertEventItem) {
	gs[StateKeyAlertEvent] = alertEvent
	gs[StateKeyPageTitle] = alertEvent.Name
}

func GetAlertEventFromGlobalState(gs cptype.GlobalStateData) *pb.AlertEventItem {
	item, ok := gs[StateKeyAlertEvent]
	if !ok {
		return nil
	}

	typedItem, ok := item.(*pb.AlertEventItem)
	if !ok {
		return nil
	}

	return typedItem
}

func FormatTimeMs(timestamp int64) string {
	if timestamp == 0 {
		return "-"
	}

	return time.Unix(timestamp/1e3, 0).Format(TimeFormatLayout)
}

func GetInterval(startTimeMs, endTimeMs int64, minInterval time.Duration, preferredPoints int64) string {
	interval := time.Duration((endTimeMs - startTimeMs) / preferredPoints / 1e3 * 1e9)
	if interval < minInterval {
		interval = minInterval
	}
	return interval.String()
}

func SetPagingToGlobalState(globalState cptype.GlobalStateData, opData table.OpTableChangePageClientData) {
	globalState[GlobalStateKeyPaging] = opData
}

func GetPagingFromGlobalState(globalState cptype.GlobalStateData) (pageNo int64, pageSize int64) {
	pageNo = 1
	pageSize = DefaultPageSize
	if paging, ok := globalState[GlobalStateKeyPaging]; ok && paging != nil {
		var clientPaging table.OpTableChangePageClientData
		clientPaging, ok = paging.(table.OpTableChangePageClientData)
		if !ok {
			ok = mapstructure.Decode(paging, &clientPaging) == nil
		}
		if ok {
			pageNo = int64(clientPaging.PageNo)
			pageSize = int64(clientPaging.PageSize)
		}
	}
	return pageNo, pageSize
}

func SetSortsToGlobalState(globalState cptype.GlobalStateData, opData table.OpTableChangeSortClientData) {
	globalState[GlobalStateKeySort] = opData
}

func GetSortsFromGlobalState(globalState cptype.GlobalStateData) []*Sort {
	var sorts []*Sort
	if sortCol, ok := globalState[GlobalStateKeySort]; ok && sortCol != nil {
		var clientSort table.OpTableChangeSortClientData
		clientSort, ok = sortCol.(table.OpTableChangeSortClientData)
		if !ok {
			ok = mapstructure.Decode(sortCol, &clientSort) == nil
		}
		if ok {
			col := clientSort.DataRef
			if col != nil && col.AscOrder != nil {
				sorts = append(sorts, &Sort{
					FieldKey:  col.FieldBindToOrder,
					Ascending: *col.AscOrder,
				})
			}
		}
	}
	return sorts
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
