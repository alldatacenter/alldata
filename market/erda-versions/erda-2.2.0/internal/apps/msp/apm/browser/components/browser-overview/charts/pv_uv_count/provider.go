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

package pv_uv_count

import (
	"fmt"
	"reflect"
	"strconv"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/linegraph"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/linegraph/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/browser/components/browser-overview/charts/utils"
	"github.com/erda-project/erda/internal/apps/msp/apm/browser/components/browser-overview/models"
)

const chartName = "pvUvCount"
const parseLayout = "2006-01-02T15:04:05Z"
const formatLayout = "2006-01-02 15:04:05"

type provider struct {
	impl.DefaultLineGraph
	models.BrowserOverviewInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		statement := fmt.Sprintf("SELECT count(timestamp), distinct(uid::tag) "+
			"FROM ta_timing "+
			"WHERE tk::tag=$terminus_key "+
			"GROUP BY time(%v)",
			utils.GetInterval(p.InParamsPtr.StartTime, p.InParamsPtr.EndTime, time.Second, 30))
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
			p.Log.Errorf("failed to get %s, err: %s, statement:%s", sdk.Comp.Name, err, statement)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}

		rows := response.Results[0].Series[0].Rows
		chart := linegraph.New(sdk.I18n(chartName))
		var pvYAxis, uvYAxis []interface{}
		for _, row := range rows {
			date := row.Values[0].GetStringValue()
			parse, err := time.ParseInLocation(parseLayout, date, time.Local)
			if err != nil {
				p.Log.Errorf("failed to parse time: %s", date)
				(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
				return nil
			}
			chart.SetXAxis(parse.Format(formatLayout))
			pvYAxis = append(pvYAxis, row.Values[1].GetNumberValue())
			uvYAxis = append(uvYAxis, row.Values[2].GetNumberValue())
		}
		chart.SetYAxis(sdk.I18n("PV"), pvYAxis...)
		chart.SetYAxis(sdk.I18n("UV"), uvYAxis...)
		chart.SetYOptions(linegraph.NewOptionsBuilder().
			WithEnable(true).WithType(structure.Number).WithDimension(sdk.I18n("PV")).Build())
		chart.SetYOptions(linegraph.NewOptionsBuilder().
			WithEnable(true).WithType(structure.Number).WithDimension(sdk.I18n("UV")).Build())
		p.StdDataPtr = chart
		return nil
	}
}

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

// Init .
func (p *provider) Init(ctx servicehub.Context) error {
	p.DefaultLineGraph = impl.DefaultLineGraph{}
	v := reflect.ValueOf(p)
	v.Elem().FieldByName("Impl").Set(v)
	compName := "pvUvCountLine"
	if ctx.Label() != "" {
		compName = ctx.Label()
	}
	protocol.MustRegisterComponent(&protocol.CompRenderSpec{
		Scenario: "browser-overview",
		CompName: compName,
		Creator:  func() cptype.IComponent { return p },
	})
	return nil
}

// Provide .
func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p
}

func init() {
	name := "component-protocol.components.browser-overview.pvUvCountLine"
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	servicehub.Register(name, &servicehub.Spec{
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
