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
	"fmt"
	"reflect"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/kv"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/kv/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/browser/components/browser-overview/models"
)

const (
	pv                     string = "pv"
	uv                     string = "uv"
	apdex                  string = "apdex"
	avgPageLoadDuration    string = "avgPageLoadDuration"
	apiSuccessRate         string = "apiSuccessRate"
	resourceLoadErrorCount string = "resourceLoadErrorCount"
	jsErrorCount           string = "jsErrorCount"
)

type provider struct {
	impl.DefaultKV
	models.BrowserOverviewInParams
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		data := kv.Data{}
		var cell *kv.KV
		var err error

		switch sdk.Comp.Name {
		case pv:
			cell, err = p.getPv(sdk)
		case uv:
			cell, err = p.getUv(sdk)
		case apdex:
			cell, err = p.getApdex(sdk)
		case avgPageLoadDuration:
			cell, err = p.getAvgPageLoadDuration(sdk)
		case apiSuccessRate:
			cell, err = p.getApiSuccessRate(sdk)
		case resourceLoadErrorCount:
			cell, err = p.getResourceLoadErrorCount(sdk)
		case jsErrorCount:
			cell, err = p.getJsErrorCount(sdk)
		default:
			err = fmt.Errorf("not supported comp name: %s", sdk.Comp.Name)
		}

		if err != nil {
			p.Log.Error("failed to get card: %s", err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}

		data.List = append(data.List, cell)
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
	p.DefaultKV = impl.DefaultKV{}
	v := reflect.ValueOf(p)
	v.Elem().FieldByName("Impl").Set(v)
	compName := "kvCard"
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
	name := "component-protocol.components.browser-overview.kvCard"
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	servicehub.Register(name, &servicehub.Spec{
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
