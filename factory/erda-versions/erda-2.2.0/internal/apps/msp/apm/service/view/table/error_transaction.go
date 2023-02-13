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
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	error_transaction "github.com/erda-project/erda/internal/apps/msp/apm/service/common/error-transaction"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/strutil"
	pkgtime "github.com/erda-project/erda/pkg/time"
)

var (
	errorTransTableColumnOccurTime = &Column{Key: string(error_transaction.ColumnOccurTime), Name: "Occur Time", Sortable: true}
	errorTransTableColumnDuration  = &Column{Key: string(error_transaction.ColumnDuration), Name: "Duration", Sortable: true}
	errorTransTableColumnTraceId   = &Column{Key: string(error_transaction.ColumnTraceId), Name: "Trace Id"}
)

var errorTransactionTableSortFieldSqlMap = map[string]string{
	errorTransTableColumnOccurTime.Key: "timestamp",
	errorTransTableColumnDuration.Key:  "elapsed_mean::field",
}

type ErrorTransactionTableRow struct {
	OccurTime string
	Duration  string
	TraceId   string
}

func (t *ErrorTransactionTableRow) GetCells() []*Cell {
	return []*Cell{
		{Key: errorTransTableColumnOccurTime.Key, Value: t.OccurTime},
		{Key: errorTransTableColumnDuration.Key, Value: t.Duration},
		{Key: errorTransTableColumnTraceId.Key, Value: t.TraceId},
	}
}

type ErrorTransactionTableBuilder struct {
	*BaseBuildParams
	MinDuration float64
	MaxDuration float64
}

func (t *ErrorTransactionTableBuilder) GetBaseBuildParams() *BaseBuildParams {
	return t.BaseBuildParams
}

func (t *ErrorTransactionTableBuilder) GetTable(ctx context.Context) (*Table, error) {
	table := &Table{
		Columns: []*Column{errorTransTableColumnOccurTime, errorTransTableColumnDuration, errorTransTableColumnTraceId},
	}
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
	statement := fmt.Sprintf("SELECT count(timestamp) "+
		"FROM %s_error "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"%s "+
		"%s ",
		common.GetDataSourceNames(t.Layer),
		common.BuildDurationFilterSql("elapsed_mean::field", t.MinDuration, t.MaxDuration),
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
		"timestamp, "+
		"elapsed_mean::field, "+
		"trace_id::tag, "+
		"request_id::tag "+
		"FROM %s_error "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"%s "+
		"%s "+
		"ORDER BY %s "+
		"LIMIT %v OFFSET %v",
		common.GetDataSourceNames(t.Layer),
		common.BuildDurationFilterSql("elapsed_mean::field", t.MinDuration, t.MaxDuration),
		common.BuildServerSideServiceIdFilterSql("$service_id", t.Layer),
		common.BuildLayerPathFilterSql(t.LayerPath, "$layer_path", t.FuzzyPath, t.Layer),
		common.GetSortSql(errorTransactionTableSortFieldSqlMap, "elapsed_mean::field DESC", t.OrderBy...),
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
		d, u := pkgtime.AutomaticConversionUnit(row.Values[1].GetNumberValue())
		transRow := &ErrorTransactionTableRow{
			OccurTime: time.Unix(0, int64(row.Values[0].GetNumberValue())).Format("2006-01-02 15:04:05"),
			Duration:  fmt.Sprintf("%s%s", strutil.String(d), u),
			TraceId:   strutil.FirstNoneEmpty(row.Values[2].GetStringValue(), row.Values[3].GetStringValue(), "-"),
		}
		table.Rows = append(table.Rows, transRow)
	}

	return table, nil
}
