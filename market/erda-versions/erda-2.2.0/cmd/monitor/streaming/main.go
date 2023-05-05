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

package main

import (
	_ "embed"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/pkg/common"

	_ "github.com/erda-project/erda/internal/apps/msp/apm/browser"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/trace/persist"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/trace/storage/cassandra_v1"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/trace/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/notify/storage/notify-record"

	// modules
	_ "github.com/erda-project/erda/internal/tools/monitor/core/alert/storage/alert-event"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/alert/storage/alert-record"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/entity/persist"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/entity/storage"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/entity/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/event/persist"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/event/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/persist"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/persist/v1"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/storage/clickhouse"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/persist"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/settings/retention-strategy"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/creator"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/initializer"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/creator"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/initializer"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/rollover"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/kafka/topic/initializer"

	// erda stream pipeline
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/core"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/all"

	// providers
	_ "github.com/erda-project/erda-infra/providers"
)

//go:embed bootstrap.yaml
var bootstrapCfg string

func main() {
	common.Run(&servicehub.RunOptions{
		Content: bootstrapCfg,
	})
}
