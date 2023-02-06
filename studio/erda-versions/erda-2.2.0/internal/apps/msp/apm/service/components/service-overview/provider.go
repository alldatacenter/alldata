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

package service_overview

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
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/pkg/math"
	"github.com/erda-project/erda/pkg/time"
)

type provider struct {
	impl.DefaultTop
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

const (
	PathRpsMaxTop5       string = "pathRpsMaxTop5"
	PathClientRpsMaxTop5 string = "pathClientRpsMaxTop5"
	PathSlowTop5         string = "pathSlowTop5"
	SqlSlowTop5          string = "sqlSlowTop5"
	ExceptionCountTop5   string = "exceptionCountTop5"
	PathErrorRateTop5    string = "pathErrorRateTop5"
	Span                 string = "24"
)

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		lang := sdk.Lang
		startTime := int64(p.StdInParamsPtr.Get("startTime").(float64))
		endTime := int64(p.StdInParamsPtr.Get("endTime").(float64))
		tenantId := p.StdInParamsPtr.Get("tenantId").(string)
		serviceId := p.StdInParamsPtr.Get("serviceId").(string)
		ctx := context.Background()
		interval := (endTime - startTime) / 1e3

		switch sdk.Comp.Name {
		case PathRpsMaxTop5:
			var records []topn.Record
			pathRpsMaxTop5, err := p.pathRpsMaxTop5(interval, tenantId, serviceId, startTime, endTime, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			pathRpsMaxTop5Records := topn.Record{Title: p.I18n.Text(lang, PathRpsMaxTop5), Span: Span}
			pathRpsMaxTop5Records.Items = pathRpsMaxTop5
			records = append(records, pathRpsMaxTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case PathSlowTop5:
			var records []topn.Record
			pathSlowTop5, err := p.pathSlowTop5(interval, tenantId, serviceId, startTime, endTime, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			pathSlowTop5Records := topn.Record{Title: p.I18n.Text(lang, PathSlowTop5), Span: Span}
			pathSlowTop5Records.Items = pathSlowTop5
			records = append(records, pathSlowTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case PathErrorRateTop5:
			var records []topn.Record
			pathErrorRateTop5, err := p.pathErrorRateTop5(interval, tenantId, serviceId, startTime, endTime, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			pathErrorRateTop5Records := topn.Record{Title: p.I18n.Text(lang, PathErrorRateTop5), Span: Span}
			pathErrorRateTop5Records.Items = pathErrorRateTop5
			records = append(records, pathErrorRateTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case PathClientRpsMaxTop5:
			var records []topn.Record
			pathClientRpsMaxTop5, err := p.pathClientRpsMaxTop5(interval, tenantId, serviceId, startTime, endTime, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			pathClientRpsMaxTop5Records := topn.Record{Title: p.I18n.Text(lang, PathClientRpsMaxTop5), Span: Span}
			pathClientRpsMaxTop5Records.Items = pathClientRpsMaxTop5
			records = append(records, pathClientRpsMaxTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case SqlSlowTop5:
			var records []topn.Record
			sqlSlowRpsMaxTop5, err := p.sqlSlowTop5(interval, tenantId, serviceId, startTime, endTime, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			sqlSlowTop5Records := topn.Record{Title: p.I18n.Text(lang, SqlSlowTop5), Span: Span}
			sqlSlowTop5Records.Items = sqlSlowRpsMaxTop5
			records = append(records, sqlSlowTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		case ExceptionCountTop5:
			var records []topn.Record
			exceptionCountTop5, err := p.exceptionCountTop5(interval, tenantId, serviceId, startTime, endTime, ctx)
			if err != nil {
				p.Log.Error(err)
			}
			exceptionCountTop5Records := topn.Record{Title: p.I18n.Text(lang, ExceptionCountTop5), Span: Span}
			exceptionCountTop5Records.Items = exceptionCountTop5
			records = append(records, exceptionCountTop5Records)
			return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{List: records}}
		}
		return &impl.StdStructuredPtr{StdDataPtr: &topn.Data{}}
	}
}

func (p *provider) exceptionCountTop5(interval int64, tenantId, serviceId string, start int64, end int64, ctx context.Context) ([]topn.Item, error) {
	statement := fmt.Sprintf("SELECT service_id::tag,type::tag,sum(count::field) " +
		"FROM error_alert " +
		"WHERE terminus_key::tag=$terminus_key " +
		"AND service_id::tag=$service_id " +
		"GROUP BY type::tag " +
		"ORDER BY sum(count::field) DESC " +
		"LIMIT 5")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
		"service_id":   structpb.NewStringValue(serviceId),
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
		item.ID = row.Values[1].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		if item.Value == 0 {
			continue
		}
		item.Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)
		items = append(items, item)
	}
	return items, err
}

func (p *provider) sqlSlowTop5(interval int64, tenantId, serviceId string, start int64, end int64, ctx context.Context) ([]topn.Item, error) {
	statement := fmt.Sprintf("SELECT source_service_id::tag,db_statement::tag,max(elapsed_max::field) " +
		"FROM application_db " +
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) " +
		"AND source_service_id::tag=$service_id " +
		"GROUP BY db_statement::tag " +
		"ORDER BY max(elapsed_max::field) DESC " +
		"LIMIT 5")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
		"service_id":   structpb.NewStringValue(serviceId),
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
		item.ID = row.Values[1].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		if item.Value == 0 {
			continue
		}
		item.Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)

		v, unit := time.AutomaticConversionUnit(item.Value)
		item.Value = v
		item.Unit = unit

		items = append(items, item)
	}
	return items, err
}

func (p *provider) pathClientRpsMaxTop5(interval int64, tenantId, serviceId string, start int64, end int64, ctx context.Context) ([]topn.Item, error) {
	statement := fmt.Sprintf("SELECT source_service_id::tag,http_url::tag,sum(elapsed_count::field) " +
		"FROM application_http " +
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) " +
		"AND source_service_id::tag=$service_id AND span_kind::tag=$kind " +
		"GROUP BY http_url::tag " +
		"ORDER BY sum(elapsed_count::field) DESC " +
		"LIMIT 5")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
		"service_id":   structpb.NewStringValue(serviceId),
		"kind":         structpb.NewStringValue("client"),
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
		item.ID = row.Values[1].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		if item.Value == 0 {
			continue
		}
		item.Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)

		items = append(items, item)
	}
	return items, err
}

func (p *provider) pathErrorRateTop5(interval int64, tenantId, serviceId string, start int64, end int64, ctx context.Context) ([]topn.Item, error) {
	statement := fmt.Sprintf("SELECT target_service_id::tag,http_path::tag,sum(if(eq(error::tag, 'true'),elapsed_count::field,0))/sum(elapsed_count::field) " +
		"FROM application_http " +
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) AND target_service_id::tag=$service_id " +
		"GROUP BY http_path::tag ")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
		"service_id":   structpb.NewStringValue(serviceId),
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
		item.ID = row.Values[1].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue()*100, 2)
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

func (p *provider) pathSlowTop5(interval int64, tenantId, serviceId string, start int64, end int64, ctx context.Context) ([]topn.Item, error) {
	statement := fmt.Sprintf("SELECT target_service_id::tag,http_path::tag,max(elapsed_max::field) " +
		"FROM application_http " +
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) AND target_service_id::tag=$service_id " +
		"GROUP BY http_path::tag " +
		"ORDER BY max(elapsed_max::field) DESC " +
		"LIMIT 5")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
		"service_id":   structpb.NewStringValue(serviceId),
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
		item.ID = row.Values[1].GetStringValue()
		item.Name = row.Values[1].GetStringValue()
		item.Value = math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2)
		if item.Value == 0 {
			continue
		}
		item.Percent = math.DecimalPlacesWithDigitsNumber(item.Value/total*1e2, 2)

		v, unit := time.AutomaticConversionUnit(item.Value)
		item.Value = v
		item.Unit = unit
		items = append(items, item)
	}
	return items, err
}

func (p *provider) pathRpsMaxTop5(interval int64, tenantId, serviceId string, start int64, end int64, ctx context.Context) ([]topn.Item, error) {
	statement := fmt.Sprintf("SELECT target_service_id::tag,http_path::tag,sum(elapsed_count::field)/%v "+
		"FROM application_http "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) AND target_service_id::tag=$service_id "+
		"GROUP BY http_path::tag "+
		"ORDER BY sum(elapsed_count::field) DESC "+
		"LIMIT 5", interval)
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
		"service_id":   structpb.NewStringValue(serviceId),
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
		item.ID = row.Values[1].GetStringValue()
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

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

// Provide .
func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p
}

func init() {
	cpregister.RegisterProviderComponent("service-overview", "service-overview", &provider{})
}
