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

package top_n

import (
	"fmt"
	"reflect"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/topn"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/topn/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/browser/components/browser-overview/models"
)

type provider struct {
	impl.DefaultTop
	models.BrowserOverviewInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

const (
	maxReqDomainTop5  string = "maxReqDomainTop5"
	maxReqPageTop5    string = "maxReqPageTop5"
	slowReqPageTop5   string = "slowReqPageTop5"
	slowReqRegionTop5 string = "slowReqRegionTop5"
	wideSpan          string = "24"
)

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		data := topn.Data{}

		record := topn.Record{Span: wideSpan}
		var err error

		switch sdk.Comp.Name {
		case maxReqDomainTop5:
			record.Title = sdk.I18n(maxReqDomainTop5)
			record.Items, err = p.maxReqDomainTop5(sdk)
		case maxReqPageTop5:
			record.Title = sdk.I18n(maxReqPageTop5)
			record.Items, err = p.maxReqPageTop5(sdk)
		case slowReqPageTop5:
			record.Title = sdk.I18n(slowReqPageTop5)
			record.Items, err = p.slowReqPageTop5(sdk)
		case slowReqRegionTop5:
			record.Title = sdk.I18n(slowReqRegionTop5)
			record.Items, err = p.slowReqRegionTop5(sdk)
		default:
			err = fmt.Errorf("not supported comp name: %s", sdk.Comp.Name)
		}

		if err != nil {
			p.Log.Error("failed to get topN: %s, error: %s", sdk.Comp.Name, err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}

		data.List = append(data.List, record)
		p.StdDataPtr = &data
		return nil
	}
}

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

// Init .
func (p *provider) Init(ctx servicehub.Context) error {
	p.DefaultTop = impl.DefaultTop{}
	v := reflect.ValueOf(p)
	v.Elem().FieldByName("Impl").Set(v)
	compName := "topN"
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
	name := "component-protocol.components.browser-overview.topN"
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	servicehub.Register(name, &servicehub.Spec{
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
