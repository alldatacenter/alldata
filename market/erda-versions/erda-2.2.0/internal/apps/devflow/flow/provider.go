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

package flow

import (
	logs "github.com/erda-project/erda-infra/base/logs"
	servicehub "github.com/erda-project/erda-infra/base/servicehub"
	transport "github.com/erda-project/erda-infra/pkg/transport"
	pb "github.com/erda-project/erda-proto-go/apps/devflow/flow/pb"
	issuerelationpb "github.com/erda-project/erda-proto-go/apps/devflow/issuerelation/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/devflowrule"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct{}

// +provider
type provider struct {
	Cfg      *config
	Log      logs.Logger
	Register transport.Register

	devFlowService *Service
	IssueRelation  issuerelationpb.IssueRelationServiceServer `autowired:"erda.apps.devflow.issuerelation.IssueRelationService"`
	DevFlowRule    devflowrule.Interface
	bdl            *bundle.Bundle
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithAllAvailableClients())

	service := &Service{}
	service.p = p
	p.devFlowService = service

	if p.Register != nil {
		pb.RegisterFlowServiceImp(p.Register, p.devFlowService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.apps.devflow.flow.FlowService" || ctx.Type() == pb.FlowServiceServerType() || ctx.Type() == pb.FlowServiceHandlerType():
		return p.devFlowService
	}
	return p
}

func init() {
	servicehub.Register("erda.apps.devflow.flow", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
