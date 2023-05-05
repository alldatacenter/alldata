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
	"github.com/mitchellh/mapstructure"

	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

func IdNameValuesToSelectOptions(values []*IdNameValue) []model.SelectOption {
	var options []model.SelectOption
	for _, value := range values {
		options = append(options, model.SelectOption{
			Label: value.Name,
			Value: value.Id,
		})
	}
	return options
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
