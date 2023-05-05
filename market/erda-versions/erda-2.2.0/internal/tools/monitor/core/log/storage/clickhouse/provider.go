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

package clickhouse

import (
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/clickhouse"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/settings/retention-strategy"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/creator"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

type config struct {
	ReadPageSize    int               `file:"read_page_size" default:"200"`
	FieldNameMapper map[string]string `file:"field_name_mapper"`
	QueryTimeout    time.Duration     `file:"query_timeout" default:"1m"`
	QueryMaxThreads int               `file:"query_max_threads" default:"0"`
	QueryMaxMemory  int64             `file:"query_max_memory" default:"0"`
}

type provider struct {
	Cfg       *config
	Log       logs.Logger
	Creator   creator.Interface   `autowired:"clickhouse.table.creator@log" optional:"true"`
	Loader    loader.Interface    `autowired:"clickhouse.table.loader@log"`
	Retention retention.Interface `autowired:"storage-retention-strategy@log" optional:"true"`

	clickhouse clickhouse.Interface
}

var _ storage.Storage = (*provider)(nil)

func (p *provider) Init(ctx servicehub.Context) error {
	svc := ctx.Service("clickhouse@log")
	if svc == nil {
		svc = ctx.Service("clickhouse")
	}
	if svc == nil {
		return fmt.Errorf("service clickhouse is required")
	}
	p.clickhouse = svc.(clickhouse.Interface)
	return nil
}

func init() {
	servicehub.Register("log-storage-clickhouse", &servicehub.Spec{
		Services:             []string{"log-storage-clickhouse-reader", "log-storage-clickhouse-writer"},
		Dependencies:         []string{"clickhouse"},
		OptionalDependencies: []string{"clickhouse.table.creator"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator:              func() servicehub.Provider { return &provider{} },
	})
}
