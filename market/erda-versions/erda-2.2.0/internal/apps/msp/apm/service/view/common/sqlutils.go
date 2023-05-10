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
	"bytes"
	"fmt"
	"strings"

	"github.com/erda-project/erda/pkg/math"
)

func GetDataSourceNames(layers ...TransactionLayerType) string {
	var list []string
	for _, layer := range layers {
		switch layer {
		case TransactionLayerHttp:
			list = append(list, "application_http")
		case TransactionLayerRpc:
			list = append(list, "application_rpc")
		case TransactionLayerCache:
			list = append(list, "application_cache")
		case TransactionLayerDb:
			list = append(list, "application_db")
		case TransactionLayerMq:
			list = append(list, "application_mq")
		}
	}
	return strings.Join(list, ",")
}

func GetLayerPathKeys(layers ...TransactionLayerType) []string {
	var list []string
	for _, layer := range layers {
		switch layer {
		case TransactionLayerHttp:
			list = append(list, "http_path::tag")
		case TransactionLayerRpc:
			list = append(list, "rpc_target::tag")
		case TransactionLayerCache:
			list = append(list, "db_statement::tag")
		case TransactionLayerDb:
			list = append(list, "db_statement::tag")
		case TransactionLayerMq:
			list = append(list, "message_bus_destination::tag")
		}
	}
	return list
}

func GetSlowThreshold(layer TransactionLayerType) float64 {
	switch layer {
	case TransactionLayerHttp:
		return 300000000
	case TransactionLayerRpc:
		return 300000000
	case TransactionLayerCache:
		return 50000000
	case TransactionLayerDb:
		return 100000000
	case TransactionLayerMq:
		return 300000000
	default:
		return 300000000
	}
}

func BuildLayerPathFilterSql(path string, paramName string, fuzzy bool, layers ...TransactionLayerType) string {
	if len(path) == 0 {
		return ""
	}

	param := paramName
	if len(param) == 0 {
		param = fmt.Sprintf("'%s'", path)
	}
	op := "="
	if fuzzy {
		op = "=~"
	}
	keys := GetLayerPathKeys(layers...)
	var tokens []string
	for _, key := range keys {
		tokens = append(tokens, fmt.Sprintf("%s%s%s", key, op, param))
	}

	return fmt.Sprintf("AND (%s) ", strings.Join(tokens, " OR "))
}

func GetServerSideServiceIdKeys(layers ...TransactionLayerType) []string {
	var list []string
	for _, layer := range layers {
		switch layer {
		case TransactionLayerHttp, TransactionLayerRpc:
			list = append(list, "target_service_id::tag")
		case TransactionLayerMq, TransactionLayerCache, TransactionLayerDb:
			list = append(list, "source_service_id::tag")
		}
	}
	return list
}

func BuildServerSideServiceIdFilterSql(paramName string, layers ...TransactionLayerType) string {
	var tokens []string
	keys := GetServerSideServiceIdKeys(layers...)
	for _, key := range keys {
		tokens = append(tokens, fmt.Sprintf("%s=%s", key, paramName))
	}
	return fmt.Sprintf("AND (%s)", strings.Join(tokens, " OR "))
}

func BuildDurationFilterSql(fieldName string, minDuration, maxDuration float64) string {
	var buf bytes.Buffer
	if minDuration > 0 {
		buf.WriteString(fmt.Sprintf("AND %s>=%f ", fieldName, minDuration))
	}
	if maxDuration > 0 {
		buf.WriteString(fmt.Sprintf("AND %s<=%f", fieldName, maxDuration))
	}
	return buf.String()
}

func FormatFloatWith2Digits(value float64) float64 {
	return math.DecimalPlacesWithDigitsNumber(value, 2)
}

func GetSortSql(fieldSqlMap map[string]string, defaultOrder string, sorts ...*Sort) string {
	var buf bytes.Buffer
	for _, sort := range sorts {
		if _, ok := fieldSqlMap[sort.FieldKey]; !ok {
			continue
		}
		if buf.Len() > 0 {
			buf.WriteString(",")
		}
		buf.WriteString(fieldSqlMap[sort.FieldKey])
		if sort.Ascending {
			buf.WriteString(" ASC ")
		} else {
			buf.WriteString(" DESC ")
		}
	}
	if buf.Len() == 0 {
		buf.WriteString(defaultOrder)
	}
	return buf.String()
}
