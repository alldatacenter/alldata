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

package groupby_type_count

import (
	"fmt"
	"strconv"
	"time"

	"github.com/ahmetb/go-linq/v3"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/complexgraph"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/complexgraph/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-overview/common"
	"github.com/erda-project/erda/pkg/common/errors"
)

type provider struct {
	impl.DefaultComplexGraph

	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-alert-overview"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		sdk.Tran = p.I18n
		data, err := p.getAlertEventChart(sdk)
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

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *provider) getAlertEventChart(sdk *cptype.SDK) (*complexgraph.Data, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, errors.NewInvalidParameterError("InParams", err.Error())
	}
	statement := fmt.Sprintf("SELECT timestamp(), alert_type::tag, count(timestamp) "+
		"FROM analyzer_alert "+
		"WHERE alert_scope::tag=$scope AND alert_scope_id::tag=$scope_id AND trigger::tag='alert' AND alert_suppressed::tag='false' "+
		"GROUP BY time(%s),alert_type::tag", common.GetInterval(inParams.StartTime, inParams.EndTime, time.Second, 30))

	params := map[string]*structpb.Value{
		"scope":    structpb.NewStringValue(inParams.Scope),
		"scope_id": structpb.NewStringValue(inParams.ScopeId),
	}

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(inParams.StartTime, 10),
		End:       strconv.FormatInt(inParams.EndTime, 10),
		Statement: statement,
		Params:    params,
	}

	response, err := p.Metric.QueryWithInfluxFormat(sdk.Ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	rows := response.Results[0].Series[0].Rows

	// prepare group
	var types = map[string]*complexgraph.SereBuilder{}
	var groups = map[int64]map[string]float64{}
	for _, row := range rows {

		timestamp := int64(row.Values[1].GetNumberValue())
		typ := row.Values[2].GetStringValue()

		if _, ok := groups[timestamp]; !ok {
			groups[timestamp] = map[string]float64{}
		}

		if len(typ) == 0 {
			continue
		}

		if _, ok := types[typ]; !ok {
			types[typ] = nil
		}

		groups[timestamp][typ] = row.Values[3].GetNumberValue()
	}

	//build the graph
	xAxisBuilder := complexgraph.NewAxisBuilder().
		WithType(complexgraph.Category).
		WithDataStructure(structure.Timestamp, "", true)
	yAxisBuilder := complexgraph.NewAxisBuilder().
		WithType(complexgraph.Value).
		WithDataStructure(structure.Number, "", true)
	for level := range types {
		yAxisBuilder.WithDimensions(sdk.I18n(level))
		types[level] = complexgraph.NewSereBuilder().
			WithType(complexgraph.Line).
			WithDimension(sdk.I18n(level))
	}

	linq.From(groups).
		Select(func(i interface{}) interface{} { return i.(linq.KeyValue).Key }).
		OrderBy(func(i interface{}) interface{} { return i }).
		ForEachIndexed(func(i int, t interface{}) {
			xAxisBuilder.WithData(t.(int64) / 1e6)
			for level, builder := range types {
				if val, ok := groups[t.(int64)][level]; ok {
					builder.WithData(val)
				} else {
					builder.WithData(float64(0))
				}
			}
		})

	dataBuilder := complexgraph.NewDataBuilder().
		WithTitle(sdk.I18n(common.ComponentNameAlertEventGroupByTypeCountLine)).
		WithXAxis(xAxisBuilder.Build()).
		WithYAxis(yAxisBuilder.Build())
	for _, sereBuilder := range types {
		sere := sereBuilder.Build()
		dataBuilder.WithDimensions(sere.Dimension)
		dataBuilder.WithSeries(sere)
	}

	return dataBuilder.Build(), nil
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameAlertEventGroupByTypeCountLine, &provider{})
}
