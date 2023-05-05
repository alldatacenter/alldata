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

package table

import (
	"context"
	"fmt"
	"strconv"

	"google.golang.org/protobuf/types/known/structpb"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/common/transaction"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/pkg/common/errors"
)

var (
	columnPath        = &Column{Key: string(transaction.ColumnTransactionName), Name: "Transaction Name"}
	columnReqCount    = &Column{Key: string(transaction.ColumnReqCount), Name: "Req Count", Sortable: true}
	columnErrorCount  = &Column{Key: string(transaction.ColumnErrorCount), Name: "Error Count", Sortable: true}
	columnAvgDuration = &Column{Key: string(transaction.ColumnAvgDuration), Name: "Avg Duration", Sortable: true}
)

var TransactionTableSortFieldSqlMap = map[string]string{
	columnReqCount.Key:    "sum(elapsed_count::field)",
	columnErrorCount.Key:  "count(error::tag)",
	columnAvgDuration.Key: "avg(elapsed_mean::field)",
}

type TransactionTableRow struct {
	TransactionName string
	ReqCount        float64
	ErrorCount      float64
	AvgDuration     string
}

func (t *TransactionTableRow) GetCells() []*Cell {
	return []*Cell{
		{Key: columnPath.Key, Value: t.TransactionName},
		{Key: columnReqCount.Key, Value: t.ReqCount},
		{Key: columnErrorCount.Key, Value: t.ErrorCount},
		{Key: columnAvgDuration.Key, Value: t.AvgDuration},
	}
}

type TransactionTableBuilder struct {
	*BaseBuildParams
}

func (t *TransactionTableBuilder) GetBaseBuildParams() *BaseBuildParams {
	return t.BaseBuildParams
}

func (t *TransactionTableBuilder) GetTable(ctx context.Context) (*Table, error) {
	table := &Table{
		Columns: []*Column{columnPath, columnReqCount, columnErrorCount, columnAvgDuration},
	}
	pathField := common.GetLayerPathKeys(t.Layer)[0]
	var layerPathParam *structpb.Value
	if t.FuzzyPath {
		layerPathParam = common.NewStructValue(map[string]interface{}{"regex": ".*" + t.LayerPath + ".*"})
	} else {
		layerPathParam = structpb.NewStringValue(t.LayerPath)
	}
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(t.TenantId),
		"service_id":   structpb.NewStringValue(t.ServiceId),
		"layer_path":   layerPathParam,
	}

	// calculate total count
	statement := fmt.Sprintf("SELECT DISTINCT(%s) "+
		"FROM %s "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"%s ",
		pathField,
		common.GetDataSourceNames(t.Layer),
		common.BuildServerSideServiceIdFilterSql("$service_id", t.Layer),
		common.BuildLayerPathFilterSql(t.LayerPath, "$layer_path", t.FuzzyPath, t.Layer),
	)
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(t.StartTime, 10),
		End:       strconv.FormatInt(t.EndTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := t.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	table.Total = response.Results[0].Series[0].Rows[0].Values[0].GetNumberValue()

	// query list items
	statement = fmt.Sprintf("SELECT "+
		"%s,"+
		"sum(elapsed_count::field),"+
		"sum(if(eq(error::tag, 'true'),elapsed_count::field,0)),"+
		"format_duration(avg(elapsed_mean::field),'',2) "+
		"FROM %s "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"%s "+
		"GROUP BY %s "+
		"ORDER BY %s "+
		"LIMIT %v OFFSET %v",
		pathField,
		common.GetDataSourceNames(t.Layer),
		common.BuildServerSideServiceIdFilterSql("$service_id", t.Layer),
		common.BuildLayerPathFilterSql(t.LayerPath, "$layer_path", t.FuzzyPath, t.Layer),
		pathField,
		common.GetSortSql(TransactionTableSortFieldSqlMap, "sum(elapsed_count::field) DESC", t.OrderBy...),
		t.PageSize,
		(t.PageNo-1)*t.PageSize,
	)
	request = &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(t.StartTime, 10),
		End:       strconv.FormatInt(t.EndTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err = t.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	for _, row := range response.Results[0].Series[0].Rows {
		transRow := &TransactionTableRow{
			TransactionName: row.Values[0].GetStringValue(),
			ReqCount:        row.Values[1].GetNumberValue(),
			ErrorCount:      row.Values[2].GetNumberValue(),
			AvgDuration:     row.Values[3].GetStringValue(),
		}
		table.Rows = append(table.Rows, transRow)
	}

	return table, nil
}
