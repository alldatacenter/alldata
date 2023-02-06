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

package project

import (
	"github.com/erda-project/erda-infra/providers/i18n"
	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/namespace"
)

// Option the is fun to set *Project property
type Option func(project *Project)

// WithBundle sets the bundle to invoke other services
func WithBundle(bundle *bundle.Bundle) Option {
	return func(p *Project) {
		p.bdl = bundle
	}
}

// WithTrans sets the translator for i18n
func WithTrans(translator i18n.Translator) Option {
	return func(p *Project) {
		p.trans = translator
	}
}

// WithCMP sets the gRPC client to invoke CMP service
// Todo: the dependency on CMP will be moved to a service which is more suitable
func WithCMP(cmpServer dashboardPb.ClusterResourceServer) Option {
	return func(p *Project) {
		p.cmp = cmpServer
	}
}

func WithNamespace(ns *namespace.Namespace) Option {
	return func(p *Project) {
		p.namespace = ns
	}
}

func WithTokenSvc(tokenService tokenpb.TokenServiceServer) Option {
	return func(p *Project) {
		p.tokenService = tokenService
	}
}

func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(p *Project) {
		p.clusterSvc = clusterSvc
	}
}
