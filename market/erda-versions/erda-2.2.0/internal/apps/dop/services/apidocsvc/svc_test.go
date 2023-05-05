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

package apidocsvc_test

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apidocsvc"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
)

func TestNew(t *testing.T) {
	var (
		db      *dbclient.DBClient
		bdl     *bundle.Bundle
		ruleSvc *branchrule.BranchRule
		trans   i18n.Translator
	)
	apidocsvc.New(
		apidocsvc.WithDBClient(db),
		apidocsvc.WithBundle(bdl),
		apidocsvc.WithBranchRuleSvc(ruleSvc),
		apidocsvc.WithTrans(trans),
	)
}
