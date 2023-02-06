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

package cards

import (
	"context"
	"fmt"
	"strconv"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/kv"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	messengerpb "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-overview/common"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/strutil"
)

func (p *provider) alertTriggerCount(sdk *cptype.SDK) (*kv.KV, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	statement := fmt.Sprintf("SELECT count(timestamp) " +
		"FROM analyzer_alert " +
		"WHERE alert_scope::tag=$scope AND alert_scope_id::tag=$scope_id AND trigger::tag=$trigger AND alert_suppressed::tag='false' ")

	params := map[string]*structpb.Value{
		"scope":    structpb.NewStringValue(inParams.Scope),
		"scope_id": structpb.NewStringValue(inParams.ScopeId),
		"trigger":  structpb.NewStringValue("alert"),
	}

	result, err := p.doQuerySql(sdk.Ctx, inParams.StartTime, inParams.EndTime, statement, params)
	if err != nil {
		return nil, err
	}

	return &kv.KV{
		Key:   sdk.I18n(alertTriggerCount),
		Value: strutil.String(result),
	}, nil
}

func (p *provider) alertRecoverCount(sdk *cptype.SDK) (*kv.KV, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	statement := fmt.Sprintf("SELECT count(timestamp) " +
		"FROM analyzer_alert " +
		"WHERE alert_scope::tag=$scope AND alert_scope_id::tag=$scope_id AND trigger::tag=$trigger AND alert_suppressed::tag='false' ")

	params := map[string]*structpb.Value{
		"scope":    structpb.NewStringValue(inParams.Scope),
		"scope_id": structpb.NewStringValue(inParams.ScopeId),
		"trigger":  structpb.NewStringValue("recover"),
	}

	result, err := p.doQuerySql(sdk.Ctx, inParams.StartTime, inParams.EndTime, statement, params)
	if err != nil {
		return nil, err
	}

	return &kv.KV{
		Key:   sdk.I18n(alertRecoverCount),
		Value: strutil.String(result),
	}, nil
}

func (p *provider) alertReduceCount(sdk *cptype.SDK) (*kv.KV, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	statement := fmt.Sprintf("SELECT sum(reduced::field) " +
		"FROM analyzer_alert_notify " +
		"WHERE alert_scope::tag=$scope AND alert_scope_id::tag=$scope_id")

	params := map[string]*structpb.Value{
		"scope":    structpb.NewStringValue(inParams.Scope),
		"scope_id": structpb.NewStringValue(inParams.ScopeId),
	}

	result, err := p.doQuerySql(sdk.Ctx, inParams.StartTime, inParams.EndTime, statement, params)
	if err != nil {
		return nil, err
	}

	return &kv.KV{
		Key:   sdk.I18n(alertReduceCount),
		Value: strutil.String(result),
	}, nil
}

func (p *provider) alertSilenceCount(sdk *cptype.SDK) (*kv.KV, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	statement := fmt.Sprintf("SELECT sum(silenced::field) " +
		"FROM analyzer_alert_notify " +
		"WHERE alert_scope::tag=$scope AND alert_scope_id::tag=$scope_id")

	params := map[string]*structpb.Value{
		"scope":    structpb.NewStringValue(inParams.Scope),
		"scope_id": structpb.NewStringValue(inParams.ScopeId),
	}

	result, err := p.doQuerySql(sdk.Ctx, inParams.StartTime, inParams.EndTime, statement, params)
	if err != nil {
		return nil, err
	}

	return &kv.KV{
		Key:   sdk.I18n(alertSilenceCount),
		Value: strutil.String(result),
	}, nil
}

func (p *provider) notifySuccessCount(sdk *cptype.SDK) (*kv.KV, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	result, err := p.queryStatusCount(sdk.Ctx, inParams)
	if err != nil {
		return nil, err
	}
	return &kv.KV{
		Key:   sdk.I18n(notifySuccessCount),
		Value: strutil.String(result["success"]),
	}, nil
}

func (p *provider) notifyFailCount(sdk *cptype.SDK) (*kv.KV, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	result, err := p.queryStatusCount(sdk.Ctx, inParams)
	if err != nil {
		return nil, err
	}
	return &kv.KV{
		Key:   sdk.I18n(notifyFailCount),
		Value: strutil.String(result["failed"]),
	}, nil
}

func (p *provider) queryStatusCount(ctx context.Context, params *common.InParams) (map[string]int64, error) {
	statusRequest := &messengerpb.GetNotifyStatusRequest{
		StartTime: strconv.FormatInt(params.StartTime, 10),
		EndTime:   strconv.FormatInt(params.EndTime, 10),
		ScopeType: params.Scope,
		ScopeId:   params.ScopeId,
	}
	context := utils.NewContextWithHeader(ctx)
	response, err := p.Messenger.GetNotifyStatus(context, statusRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return response.Data, nil
}

func (p *provider) doQuerySql(ctx context.Context, startTime, endTime int64, statement string, params map[string]*structpb.Value) (float64, error) {
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
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
