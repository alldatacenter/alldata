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

package workbench

import (
	"github.com/erda-project/erda-infra/providers/i18n"
	menupb "github.com/erda-project/erda-proto-go/msp/menu/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/bundle"
)

type Workbench struct {
	bdl              *bundle.Bundle
	tran             i18n.Translator
	tenantProjectSvc projectpb.ProjectServiceServer
	menuSvc          menupb.MenuServiceServer
}

type Option func(bench *Workbench)

func New(options ...Option) *Workbench {
	w := &Workbench{}
	for _, op := range options {
		op(w)
	}
	return w
}

// WithBundle with bundle bdl
func WithBundle(bdl *bundle.Bundle) Option {
	return func(w *Workbench) {
		w.bdl = bdl
	}
}

func WithTranslator(tran i18n.Translator) Option {
	return func(w *Workbench) {
		w.tran = tran
	}
}

func WithProjectSvc(tenantProjectSvc projectpb.ProjectServiceServer) Option {
	return func(w *Workbench) {
		w.tenantProjectSvc = tenantProjectSvc
	}
}

func WithMenuSvc(menuSvc menupb.MenuServiceServer) Option {
	return func(w *Workbench) {
		w.menuSvc = menuSvc
	}
}
