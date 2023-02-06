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
	messengerpb "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-overview/common"
)

const (
	alertTriggerCount  string = "alertTriggerCount"
	alertRecoverCount  string = "alertRecoverCount"
	alertReduceCount   string = "alertReduceCount"
	alertSilenceCount  string = "alertSilenceCount"
	notifySuccessCount string = "notifySuccessCount"
	notifyFailCount    string = "notifyFailCount"
)

type provider struct {
	impl.DefaultKV
	Log       logs.Logger
	I18n      i18n.Translator                 `autowired:"i18n" translator:"msp-alert-overview"`
	Metric    metricpb.MetricServiceServer    `autowired:"erda.core.monitor.metric.MetricService"`
	Messenger messengerpb.NotifyServiceServer `autowired:"erda.core.messenger.notify.NotifyService"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		sdk.Tran = p.I18n
		data := kv.Data{}
		var cell *kv.KV
		var err error

		switch sdk.Comp.Name {
		case alertTriggerCount:
			cell, err = p.alertTriggerCount(sdk)
		case alertRecoverCount:
			cell, err = p.alertRecoverCount(sdk)
		case alertReduceCount:
			cell, err = p.alertReduceCount(sdk)
		case alertSilenceCount:
			cell, err = p.alertSilenceCount(sdk)
		case notifySuccessCount:
			cell, err = p.notifySuccessCount(sdk)
		case notifyFailCount:
			cell, err = p.notifyFailCount(sdk)
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
	compName := common.ComponentNameKvCard
	if ctx.Label() != "" {
		compName = ctx.Label()
	}
	protocol.MustRegisterComponent(&protocol.CompRenderSpec{
		Scenario: common.ScenarioKey,
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
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNameKvCard)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	servicehub.Register(name, &servicehub.Spec{
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
