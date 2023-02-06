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

// Package asset API 资产
package assetsvc

import (
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
)

type Service struct {
	trans         i18n.Translator
	branchRuleSvc *branchrule.BranchRule
	bdl           *bundle.Bundle
}

type Option func(*Service)

func New(opts ...Option) *Service {
	r := &Service{}
	for _, op := range opts {
		op(r)
	}
	return r
}

// WithI18n sets the i18n client
func WithI18n(translator i18n.Translator) Option {
	return func(svc *Service) {
		svc.trans = translator
	}
}

// WithBranchRuleSvc sets the branch rule client
func WithBranchRuleSvc(branchRule *branchrule.BranchRule) Option {
	return func(service *Service) {
		service.branchRuleSvc = branchRule
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(svc *Service) {
		svc.bdl = bdl
	}
}
