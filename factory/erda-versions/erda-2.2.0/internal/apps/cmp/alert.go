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

package cmp

import (
	"context"
	"encoding/json"

	alertpb "github.com/erda-project/erda-proto-go/cmp/alert/pb"
	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/common/errors"
)

func (p *provider) GetAlertConditions(ctx context.Context, request *alertpb.GetAlertConditionsRequest) (*alertpb.GetAlertConditionsResponse, error) {
	conditionReq := &monitor.GetAlertConditionsRequest{
		ScopeType: request.ScopeType,
	}
	context := utils.NewContextWithHeader(ctx)
	result, err := p.Monitor.GetAlertConditions(context, conditionReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	resp := &alertpb.GetAlertConditionsResponse{
		Data: make([]*monitor.Conditions, 0),
	}
	err = json.Unmarshal(data, &resp.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return resp, nil
}

func (p *provider) GetAlertConditionsValue(ctx context.Context, request *alertpb.GetAlertConditionsValueRequest) (*alertpb.GetAlertConditionsValueResponse, error) {
	conditionReq := &monitor.GetAlertConditionsValueRequest{
		Conditions: request.Conditions,
	}
	context := utils.NewContextWithHeader(ctx)
	result, err := p.Monitor.GetAlertConditionsValue(context, conditionReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	resp := &alertpb.GetAlertConditionsValueResponse{
		Data: make([]*monitor.AlertConditionsValue, 0),
	}
	err = json.Unmarshal(data, &resp.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return resp, nil
}
