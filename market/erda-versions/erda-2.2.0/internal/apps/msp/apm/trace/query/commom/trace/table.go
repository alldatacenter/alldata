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

package trace

import (
	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

const (
	ColumnTraceId   table.ColumnKey = "traceId"
	ColumnDuration  table.ColumnKey = "traceDuration"
	ColumnStartTime table.ColumnKey = "traceStartTime"
	ColumnSpanCount table.ColumnKey = "traceSpanCount"
	ColumnServices  table.ColumnKey = "traceServices"
)

const (
	StateKeyTracePaging = "trace_paging"
	StateKeyTraceSort   = "trace_sort"
)

func InitTable(lang i18n.LanguageCodes, i18n i18n.Translator) table.Table {
	return table.Table{
		Columns: table.ColumnsInfo{
			Orders: []table.ColumnKey{ColumnTraceId, ColumnDuration, ColumnStartTime, ColumnSpanCount, ColumnServices},
			ColumnsMap: map[table.ColumnKey]table.Column{
				ColumnTraceId:   {Title: i18n.Text(lang, string(ColumnTraceId)), EnableSort: false},
				ColumnDuration:  {Title: i18n.Text(lang, string(ColumnDuration)), EnableSort: true, FieldBindToOrder: string(ColumnDuration)},
				ColumnStartTime: {Title: i18n.Text(lang, string(ColumnStartTime)), EnableSort: true, FieldBindToOrder: string(ColumnStartTime)},
				ColumnSpanCount: {Title: i18n.Text(lang, string(ColumnSpanCount)), EnableSort: true, FieldBindToOrder: string(ColumnSpanCount)},
				ColumnServices:  {Title: i18n.Text(lang, string(ColumnServices)), EnableSort: false},
			},
		},
	}
}

func GetPagingFromGlobalState(globalState cptype.GlobalStateData) (pageNo int, pageSize int) {
	pageNo = 1
	pageSize = common.DefaultPageSize
	if paging, ok := globalState[StateKeyTracePaging]; ok && paging != nil {
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

func GetSortsFromGlobalState(globalState cptype.GlobalStateData) common.Sort {
	var sort common.Sort
	if sortCol, ok := globalState[StateKeyTraceSort]; ok && sortCol != nil {
		var clientSort table.OpTableChangeSortClientData
		clientSort, ok = sortCol.(table.OpTableChangeSortClientData)
		if !ok {
			ok = mapstructure.Decode(sortCol, &clientSort) == nil
		}
		if ok {
			col := clientSort.DataRef
			if col != nil && col.AscOrder != nil {
				return common.Sort{FieldKey: col.FieldBindToOrder, Ascending: *col.AscOrder}
			}
		}
	}
	return sort
}
