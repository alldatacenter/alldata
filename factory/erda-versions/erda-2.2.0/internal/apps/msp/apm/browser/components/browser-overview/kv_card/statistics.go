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

package kv_card

import (
	"context"
	"fmt"
	"strconv"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/kv"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/math"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/time"
)

func (p *provider) getPv(sdk *cptype.SDK) (*kv.KV, error) {
	statement := fmt.Sprintf("SELECT count(uid::tag) " +
		"FROM ta_timing " +
		"WHERE tk::tag=$terminus_key")

	params := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(p.InParamsPtr.TenantId),
	}

	result, err := p.doQuerySql(sdk.Ctx, statement, params)
	if err != nil {
		return nil, err
	}

	return &kv.KV{
		Key:   sdk.I18n(pv),
		Value: strutil.String(result),
	}, nil
}

func (p *provider) getUv(sdk *cptype.SDK) (*kv.KV, error) {
	statement := fmt.Sprintf("SELECT distinct(uid::tag) " +
		"FROM ta_timing " +
		"WHERE tk::tag=$terminus_key")

	params := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(p.InParamsPtr.TenantId),
	}

	result, err := p.doQuerySql(sdk.Ctx, statement, params)
	if err != nil {
		return nil, err
	}

	return &kv.KV{
		Key:   sdk.I18n(uv),
		Value: strutil.String(result),
	}, nil
}

func (p *provider) getApdex(sdk *cptype.SDK) (*kv.KV, error) {
	// Satisfied*1 + Tolerating*0.5 + Frustrated*0）/ Total
	statement := fmt.Sprintf("SELECT count(plt::field) " +
		"FROM ta_timing " +
		"WHERE tk::tag=$terminus_key " +
		"GROUP BY range(plt::field, 0, 2000, 2000, 8000, 8000)")

	params := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(p.InParamsPtr.TenantId),
	}

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(p.InParamsPtr.StartTime, 10),
		End:       strconv.FormatInt(p.InParamsPtr.EndTime, 10),
		Statement: statement,
		Params:    params,
	}
	response, err := p.Metric.QueryWithInfluxFormat(sdk.Ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	rows := response.Results[0].Series[0].Rows
	if len(rows) != 3 {
		return nil, errors.NewInternalServerErrorMessage("unexpected query result")
	}
	satisfiedCount := rows[0].Values[1].GetNumberValue()
	toleratingCount := rows[1].Values[1].GetNumberValue()
	frustratedCount := rows[2].Values[1].GetNumberValue()
	totalCount := satisfiedCount + toleratingCount + frustratedCount
	card := &kv.KV{Key: sdk.I18n(apdex)}

	if totalCount == 0 {
		card.Value = "1.0"
		return card, nil
	}

	score := (satisfiedCount*1 + toleratingCount*0.5 + frustratedCount*0) / totalCount
	card.Value = strutil.String(math.DecimalPlacesWithDigitsNumber(score, 1))
	return card, nil
}

func (p *provider) getAvgPageLoadDuration(sdk *cptype.SDK) (*kv.KV, error) {
	statement := fmt.Sprintf("SELECT avg(plt::field) " +
		"FROM ta_timing " +
		"WHERE tk::tag=$terminus_key")

	params := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(p.InParamsPtr.TenantId),
	}

	result, err := p.doQuerySql(sdk.Ctx, statement, params)
	if err != nil {
		return nil, err
	}

	d, unit := time.AutomaticConversionUnit(result * 1e6)
	return &kv.KV{
		Key:    sdk.I18n(avgPageLoadDuration),
		Value:  strutil.String(d),
		SubKey: unit,
	}, nil
}

func (p *provider) getApiSuccessRate(sdk *cptype.SDK) (*kv.KV, error) {
	statement := fmt.Sprintf("SELECT sum(if(eq(errors::field,false),1,0)),count(errors::field) " +
		"FROM ta_req " +
		"WHERE tk::tag=$terminus_key")

	params := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(p.InParamsPtr.TenantId),
	}

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(p.InParamsPtr.StartTime, 10),
		End:       strconv.FormatInt(p.InParamsPtr.EndTime, 10),
		Statement: statement,
		Params:    params,
	}

	response, err := p.Metric.QueryWithInfluxFormat(sdk.Ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	rows := response.Results[0].Series[0].Rows
	if len(rows) != 1 || len(rows[0].Values) != 2 {
		return nil, errors.NewInternalServerErrorMessage("unexpected query result")
	}

	card := &kv.KV{
		Key:    sdk.I18n(apiSuccessRate),
		SubKey: "%",
	}

	successCount := rows[0].Values[0].GetNumberValue()
	totalCount := rows[0].Values[1].GetNumberValue()

	if totalCount == 0 {
		card.Value = "100"
		return card, nil
	}

	card.Value = strutil.String(math.DecimalPlacesWithDigitsNumber(successCount/totalCount*1e2, 2))
	return card, nil
}

func (p *provider) getResourceLoadErrorCount(sdk *cptype.SDK) (*kv.KV, error) {
	// todo@ggp not support yet
	return &kv.KV{
		Key:   sdk.I18n(resourceLoadErrorCount),
		Value: "-",
	}, nil
}

func (p *provider) getJsErrorCount(sdk *cptype.SDK) (*kv.KV, error) {
	statement := fmt.Sprintf("SELECT sum(count::field) " +
		"FROM ta_error " +
		"WHERE tk::tag=$terminus_key")

	params := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(p.InParamsPtr.TenantId),
	}

	result, err := p.doQuerySql(sdk.Ctx, statement, params)
	if err != nil {
		return nil, err
	}

	return &kv.KV{
		Key:   sdk.I18n(jsErrorCount),
		Value: strutil.String(result),
	}, nil
}

func (p *provider) doQuerySql(ctx context.Context, statement string, params map[string]*structpb.Value) (float64, error) {
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(p.InParamsPtr.StartTime, 10),
		End:       strconv.FormatInt(p.InParamsPtr.EndTime, 10),
		Statement: statement,
		Params:    params,
	}

	response, err := p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return 0, errors.NewInternalServerError(err)
	}
	rows := response.Results[0].Series[0].Rows
	if len(rows) == 0 || len(rows[0].Values) == 0 {
		return 0, errors.NewInternalServerErrorMessage("empty query result")
	}

	val := rows[0].Values[0].GetNumberValue()
	return val, nil
}
