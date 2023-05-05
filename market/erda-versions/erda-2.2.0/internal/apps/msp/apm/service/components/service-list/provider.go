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

package service_list

import (
	"context"
	"fmt"
	"sort"
	"strconv"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/topn"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/topn/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/common/custom"
	"github.com/erda-project/erda/pkg/math"
	pkgtime "github.com/erda-project/erda/pkg/time"
)

type provider struct {
	base.DefaultProvider
	impl.DefaultTop
	*custom.ServiceInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

const (
	RpsMaxTop5      string = "rpsMaxTop5"
	RpsMinTop5      string = "rpsMinTop5"
	AvgDurationTop5 string = "avgDurationTop5"
	ErrorRateTop5   string = "errorRateTop5"
	Span            string = "24"
)

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		lang := sdk.Lang
		ctx := sdk.Ctx

		start := p.InParamsPtr.StartTime
		end := p.InParamsPtr.EndTime
		tenantId := p.InParamsPtr.TenantId
		interval := (end - start) / 1e3
		switch sdk.Comp.Name {
		case RpsMaxTop5:
			var records []topn.Record
			rpsMaxTop5, err := p.rpsMaxTop5(interval, tenantId, start, end, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			rpsMaxTop5Records := topn.Record{Title: p.I18n.Text(lang, RpsMaxTop5), Span: Span}
			rpsMaxTop5Records.Items = rpsMaxTop5
			records = append(records, rpsMaxTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case RpsMinTop5:
			var records []topn.Record
			rpsMinTop5, err := p.rpsMinTop5(interval, tenantId, start, end, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			rpsMinTop5Records := topn.Record{Title: p.I18n.Text(lang, RpsMinTop5), Span: Span}
			rpsMinTop5Records.Items = rpsMinTop5
			records = append(records, rpsMinTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case AvgDurationTop5:
			var records []topn.Record
			avgDurationTop5, err := p.avgDurationTop5(interval, tenantId, start, end, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			avgDurationTop5Records := topn.Record{Title: p.I18n.Text(lang, AvgDurationTop5), Span: Span}
			avgDurationTop5Records.Items = avgDurationTop5
			records = append(records, avgDurationTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case ErrorRateTop5:
			var records []topn.Record
			errorRateTop5, err := p.errorRateTop5(interval, tenantId, start, end, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			errorRateTop5Records := topn.Record{Title: p.I18n.Text(lang, ErrorRateTop5), Span: Span}
			errorRateTop5Records.Items = errorRateTop5
			records = append(records, errorRateTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		}
		return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{}}
	}
}

func (p *provider) errorRateTop5(interval int64, tenantId interface{}, start int64, end int64, ctx context.Context) ([]topn.Item, error) {

	statement := fmt.Sprintf("SELECT target_service_id::tag,target_service_name::tag,sum(errors_sum::field)/sum(count_sum::field) " +
		"FROM application_http_service,application_rpc_service " +
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) " +
		"GROUP BY target_service_id::tag ")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId.(string)),
	}

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	var items []topn.Item
	rows := response.Results[0].Series[0].Rows
	if rows == nil || len(rows) == 0 {
		return items, nil
	}
	for _, row := range rows {
		var item topn.Item
		item.ID = row.Values[0].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue()*1e2, 2)
		if item.Value == 0 {
			continue
		}
		item.Unit = "%"
		items = append(items, item)
	}

	sort.SliceStable(items, func(i, j int) bool {
		if items[i].Value > items[j].Value {
			return true
		}
		return false
	})

	total := float64(0)
	limit := len(items)
	if len(items) >= 5 {
		limit = 5
	}
	items = items[:limit]
	for i, item := range items {
		if i == 0 {
			total = item.Value
		}
		items[i].Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)
	}

	return items, err
}

func (p *provider) avgDurationTop5(interval int64, tenantId interface{}, start int64, end int64, ctx context.Context) ([]topn.Item, error) {

	statement := fmt.Sprintf("SELECT target_service_id::tag,target_service_name::tag,avg(elapsed_mean::field) " +
		"FROM application_http,application_rpc " +
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) " +
		"GROUP BY target_service_id::tag " +
		"ORDER BY avg(elapsed_mean::field) DESC " +
		"LIMIT 5")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId.(string)),
	}
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	var items []topn.Item
	rows := response.Results[0].Series[0].Rows
	if rows == nil || len(rows) == 0 {
		return items, nil
	}
	total := math.DecimalPlacesWithDigitsNumber(rows[0].Values[2].GetNumberValue(), 2)
	for _, row := range rows {
		var item topn.Item
		item.ID = row.Values[0].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		if item.Value == 0 {
			continue
		}
		item.Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)
		v, unit := pkgtime.AutomaticConversionUnit(item.Value)
		item.Value = v
		item.Unit = unit
		items = append(items, item)
	}
	return items, err
}

func (p *provider) rpsMinTop5(interval int64, tenantId interface{}, start int64, end int64, ctx context.Context) ([]topn.Item, error) {
	statement := fmt.Sprintf("SELECT target_service_id::tag,target_service_name::tag,sum(count_sum::field)/%v "+
		"FROM application_http_service,application_rpc_service "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"GROUP BY target_service_id::tag "+
		"ORDER BY sum(count_sum::field) ASC "+
		"LIMIT 5", interval)
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId.(string)),
	}

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	var items []topn.Item
	rows := response.Results[0].Series[0].Rows
	if rows == nil || len(rows) == 0 {
		return items, nil
	}
	total := math.DecimalPlacesWithDigitsNumber(rows[len(rows)-1].Values[2].GetNumberValue(), 2)
	for _, row := range rows {
		var item topn.Item
		item.ID = row.Values[0].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		if item.Value == 0 {
			continue
		}
		item.Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)
		item.Unit = "reqs/s"
		items = append(items, item)
	}
	return items, err
}

func (p *provider) rpsMaxTop5(interval int64, tenantId interface{}, start int64, end int64, ctx context.Context) ([]topn.Item, error) {

	statement := fmt.Sprintf("SELECT target_service_id::tag,target_service_name::tag,sum(count_sum::field)/%v "+
		"FROM application_http_service,application_rpc_service "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"GROUP BY target_service_id::tag "+
		"ORDER BY sum(count_sum::field) DESC "+
		"LIMIT 5", interval)
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId.(string)),
	}

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, err
	}
	var items []topn.Item
	rows := response.Results[0].Series[0].Rows
	if rows == nil || len(rows) == 0 {
		return items, nil
	}
	total := math.DecimalPlacesWithDigitsNumber(rows[0].Values[2].GetNumberValue(), 2)
	for _, row := range rows {
		var item topn.Item
		item.ID = row.Values[0].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		if item.Value == 0 {
			continue
		}
		item.Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)
		item.Unit = "reqs/s"
		items = append(items, item)
	}

	return items, err
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.ServiceInParams = &custom.ServiceInParams{}
	return nil
}

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p
}

func init() {
	cpregister.RegisterProviderComponent("service-list", "service-list", &provider{})
}
