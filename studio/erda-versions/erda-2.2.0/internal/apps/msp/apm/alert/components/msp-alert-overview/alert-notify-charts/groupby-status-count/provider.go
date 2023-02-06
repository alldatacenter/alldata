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

package groupby_status_count

import (
	"strconv"

	"github.com/erda-project/erda-infra/base/logs"
	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/complexgraph"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/complexgraph/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	messengerpb "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-overview/common"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/common/errors"
)

type provider struct {
	impl.DefaultComplexGraph

	Log       logs.Logger
	Messenger messengerpb.NotifyServiceServer `autowired:"erda.core.messenger.notify.NotifyService"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		data, err := p.getNotifyStatusChart(sdk)
		if err != nil {
			p.Log.Errorf("failed to render chart: %s", err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		return &impl.StdStructuredPtr{
			StdDataPtr: data,
		}
	}
}

func (p *provider) getNotifyStatusChart(sdk *cptype.SDK) (*complexgraph.Data, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	request := &messengerpb.GetNotifyHistogramRequest{
		StartTime: strconv.FormatInt(inParams.StartTime, 10),
		EndTime:   strconv.FormatInt(inParams.EndTime, 10),
		ScopeId:   inParams.ScopeId,
		ScopeType: inParams.Scope,
		Points:    30,
		Statistic: "status",
	}
	context := utils.NewContextWithHeader(sdk.Ctx)
	response, err := p.Messenger.GetNotifyHistogram(context, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	timestamp := common.ToInterface(response.Data.Timestamp)
	xAxisBuilder := complexgraph.NewAxisBuilder().
		WithType(complexgraph.Category).
		WithDataStructure(structure.Timestamp, "", true).
		WithData(timestamp...)
	yAxisBuilder := complexgraph.NewAxisBuilder().
		WithType(complexgraph.Value).
		WithDataStructure(structure.Number, "", true)

	dataBuilder := complexgraph.NewDataBuilder().
		WithTitle(sdk.I18n(common.ComponentNameAlertNotifyGroupByStatusCountLine)).
		WithXAxis(xAxisBuilder.Build())

	for status, data := range response.Data.Value {
		yAxisBuilder.WithDimensions(sdk.I18n(status))
		value := common.ToInterface(data.Value)
		sere := complexgraph.NewSereBuilder().WithType(complexgraph.Line).
			WithDimension(sdk.I18n(status)).WithData(value...).Build()
		dataBuilder.WithDimensions(sere.Dimension)
		dataBuilder.WithSeries(sere)
	}
	dataBuilder.WithYAxis(yAxisBuilder.Build())

	return dataBuilder.Build(), nil
}

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameAlertNotifyGroupByStatusCountLine, &provider{})
}
