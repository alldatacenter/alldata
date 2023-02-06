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

package transaction

import (
	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

const (
	ColumnTransactionName table.ColumnKey = "transactionName"
	ColumnReqCount        table.ColumnKey = "reqCount"
	ColumnErrorCount      table.ColumnKey = "errorCount"
	ColumnSlowCount       table.ColumnKey = "slowCount"
	ColumnAvgDuration     table.ColumnKey = "avgDuration"
)

const (
	StateKeyTransactionLayerPathFilter = "transaction_layer_path"
	StateKeyTransactionPaging          = "transaction_paging"
	StateKeyTransactionSort            = "transaction_sort"
)

func InitTable(lang i18n.LanguageCodes, i18n i18n.Translator) table.Table {
	return table.Table{
		Columns: table.ColumnsInfo{
			Orders: []table.ColumnKey{ColumnTransactionName, ColumnReqCount, ColumnErrorCount, ColumnSlowCount, ColumnAvgDuration},
			ColumnsMap: map[table.ColumnKey]table.Column{
				ColumnTransactionName: {Title: i18n.Text(lang, string(ColumnTransactionName)), EnableSort: false},
				ColumnReqCount:        {Title: i18n.Text(lang, string(ColumnReqCount)), EnableSort: true, FieldBindToOrder: string(ColumnReqCount)},
				ColumnErrorCount:      {Title: i18n.Text(lang, string(ColumnErrorCount)), EnableSort: true, FieldBindToOrder: string(ColumnErrorCount)},
				ColumnSlowCount:       {Title: i18n.Text(lang, string(ColumnSlowCount)), EnableSort: true, FieldBindToOrder: string(ColumnSlowCount)},
				ColumnAvgDuration:     {Title: i18n.Text(lang, string(ColumnAvgDuration)), EnableSort: true, FieldBindToOrder: string(ColumnAvgDuration)},
			},
		},
	}
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
