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

package adapt

import (
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/bundle-ex/cmdb"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/cql"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	"github.com/erda-project/erda/internal/tools/monitor/core/event/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
)

const (
	orgScope = "org"

	orgCustomizeType = "org_customize"
)

var (
	// ErrorAlreadyExists .
	ErrorAlreadyExists = fmt.Errorf("alert already exists")
)

type invalidParameterError struct {
	text string
}

// Error
func (e *invalidParameterError) Error() string { return e.text }

func invalidParameter(format string, args ...interface{}) error {
	return &invalidParameterError{
		text: fmt.Sprintf(format, args...),
	}
}

// IsInvalidParameterError .
func IsInvalidParameterError(err error) bool {
	_, ok := err.(*invalidParameterError)
	return ok
}

// IsAlreadyExistsError .
func IsAlreadyExistsError(err error) bool {
	return err == ErrorAlreadyExists
}

// Adapt .
type Adapt struct {
	l                           logs.Logger
	metricq                     metricq.Queryer
	eventStorage                storage.Storage
	t                           i18n.Translator
	db                          *db.DB
	cql                         *cql.Cql
	bdl                         *bundle.Bundle
	cmdb                        *cmdb.Cmdb
	dashboardAPI                block.DashboardAPI
	orgFilterTags               map[string]bool
	microServiceFilterTags      map[string]bool
	microServiceOtherFilterTags map[string]bool
	silencePolicies             map[string]bool
}

// New .
func New(
	l logs.Logger,
	metricq metricq.Queryer,
	eventStorage storage.Storage,
	t i18n.Translator,
	db *db.DB,
	cql *cql.Cql,
	bdl *bundle.Bundle,
	cmdb *cmdb.Cmdb,
	dashapi block.DashboardAPI,
	orgFilterTags map[string]bool,
	microServiceFilterTags map[string]bool,
	microServiceOtherFilterTags map[string]bool,
	silencePolicies map[string]bool,
) *Adapt {
	return &Adapt{
		l:                           l,
		metricq:                     metricq,
		eventStorage:                eventStorage,
		t:                           t,
		db:                          db,
		bdl:                         bdl,
		cmdb:                        cmdb,
		cql:                         cql,
		orgFilterTags:               orgFilterTags,
		microServiceFilterTags:      microServiceFilterTags,
		microServiceOtherFilterTags: microServiceOtherFilterTags,
		silencePolicies:             silencePolicies,
		dashboardAPI:                dashapi,
	}
}
