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

package alert_duration_distribution

import (
	"fmt"
	"strconv"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/bubblegraph"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/bubblegraph/impl"
	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-overview/common"
)

const parseLayout = "2006-01-02T15:04:05Z"
const formatLayout = "2006-01-02 15:04:05"

type provider struct {
	impl.DefaultBubbleGraph
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-alert-overview"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		sdk.Tran = p.I18n
		chart, err := p.getAlertDurationDistributionChart(sdk)
		if err != nil {
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		p.StdDataPtr = chart
		return nil
	}
}

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *provider) getAlertDurationDistributionChart(sdk *cptype.SDK) (*bubblegraph.Data, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, err
	}
	statement := fmt.Sprintf("SELECT avg(trigger_duration::field), count(timestamp) "+
		"FROM analyzer_alert "+
		"WHERE alert_scope::tag=$scope AND alert_scope_id::tag=$scope_id AND trigger::tag='recover' "+
		"GROUP BY time(%v) ",
		common.GetInterval(inParams.StartTime, inParams.EndTime, time.Second, 30))
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
		p.Log.Errorf("failed to get %s, err: %s, statement:%s", sdk.Comp.Name, err, statement)
		return nil, err
	}

	rows := response.Results[0].Series[0].Rows
	builder := bubblegraph.NewDataBuilder(). //WithTitle(sdk.I18n(common.ComponentNameAlertDurationDistributionBubble)).
							WithYOptions(bubblegraph.NewOptionsBuilder().WithType(structure.Time).WithPrecision(structure.Millisecond).WithEnable(true).Build())
	for _, row := range rows {
		date := row.Values[0].GetStringValue()
		parse, err := time.ParseInLocation(parseLayout, date, time.Local)
		if err != nil {
			p.Log.Errorf("failed to parse time: %s", date)
			return nil, err
		}

		builder.WithBubble(bubblegraph.NewBubbleBuilder().
			WithValueX(parse.Format(formatLayout)).
			WithValueY(row.Values[1].GetNumberValue()).
			WithValueSize(row.Values[2].GetNumberValue()).
			WithDimension("Avg Duration").
			Build())
	}

	chart := builder.Build()
	return chart, nil
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameAlertDurationDistributionBubble, &provider{})
}
