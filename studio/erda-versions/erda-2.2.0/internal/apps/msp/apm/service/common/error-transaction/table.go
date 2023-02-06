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

package error_transaction

import (
	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

const (
	ColumnOccurTime table.ColumnKey = "occurTime"
	ColumnDuration  table.ColumnKey = "duration"
	ColumnTraceId   table.ColumnKey = "traceId"
)

const (
	StateKeyTransactionDurationFilter = "error_transaction_filter_duration"
	StateKeyTransactionPaging         = "error_transaction_paging"
	StateKeyTransactionSort           = "error_transaction_sort"
)

type ErrorTransactionFilter struct {
	MinDuration float64 `json:"minDuration"`
	MaxDuration float64 `json:"maxDuration"`
}

func SetFilterToGlobalState(globalState cptype.GlobalStateData, opData ErrorTransactionFilter) {
	globalState[StateKeyTransactionDurationFilter] = opData
}

func GetFilterFromGlobalState(globalState cptype.GlobalStateData) ErrorTransactionFilter {
	filter, ok := globalState[StateKeyTransactionDurationFilter]
	if !ok {
		return ErrorTransactionFilter{}
	}
	typed, ok := filter.(ErrorTransactionFilter)
	if ok {
		return typed
	}
	_ = mapstructure.Decode(filter, &typed)
	return typed
}

func SetPagingToGlobalState(globalState cptype.GlobalStateData, opData table.OpTableChangePageClientData) {
	globalState[StateKeyTransactionPaging] = opData
}

func GetPagingFromGlobalState(globalState cptype.GlobalStateData) (pageNo int, pageSize int) {
	pageNo = 1
	pageSize = common.DefaultPageSize
	if paging, ok := globalState[StateKeyTransactionPaging]; ok && paging != nil {
		var clientPaging table.OpTableChangePageClientData
		clientPaging, ok = paging.(table.OpTableChangePageClientData)
		if !ok {
			ok = mapstructure.Decode(paging, &clientPaging) == nil
		}
		if ok {
			pageNo = int(clientPaging.PageNo)
			pageSize = int(clientPaging.PageSize)
		}
	}
	return pageNo, pageSize
}

func SetSortsToGlobalState(globalState cptype.GlobalStateData, opData table.OpTableChangeSortClientData) {
	globalState[StateKeyTransactionSort] = opData
}

func GetSortsFromGlobalState(globalState cptype.GlobalStateData) []*common.Sort {
	var sorts []*common.Sort
	if sortCol, ok := globalState[StateKeyTransactionSort]; ok && sortCol != nil {
		var clientSort table.OpTableChangeSortClientData
		clientSort, ok = sortCol.(table.OpTableChangeSortClientData)
		if !ok {
			ok = mapstructure.Decode(sortCol, &clientSort) == nil
		}
		if ok {
			col := clientSort.DataRef
			if col != nil && col.AscOrder != nil {
				sorts = append(sorts, &common.Sort{
					FieldKey:  col.FieldBindToOrder,
					Ascending: *col.AscOrder,
				})
			}
		}
	}
	return sorts
}
