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

package cq

import (
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
)

type CQ struct {
	bdl           *bundle.Bundle
	branchRuleSvc *branchrule.BranchRule
}

type Option func(*CQ)

func New(options ...Option) *CQ {
	var cq CQ
	for _, op := range options {
		op(&cq)
	}
	return &cq
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(cq *CQ) {
		cq.bdl = bdl
	}
}

func WithBranchRule(svc *branchrule.BranchRule) Option {
	return func(cq *CQ) {
		cq.branchRuleSvc = svc
	}
}
