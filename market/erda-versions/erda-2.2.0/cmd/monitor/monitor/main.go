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
	"fmt"
	"runtime"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub"
	"github.com/erda-project/erda/pkg/common"

	// modules
	_ "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/client"
	_ "github.com/erda-project/erda-proto-go/core/messenger/notifychannel/client"
	_ "github.com/erda-project/erda-proto-go/core/messenger/notifygroup/client"
	_ "github.com/erda-project/erda-proto-go/core/pipeline/cron/client"
	_ "github.com/erda-project/erda/internal/core/file-manager"
	_ "github.com/erda-project/erda/internal/tools/monitor/apm/runtime"
	_ "github.com/erda-project/erda/internal/tools/monitor/apm/topology"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/alert/details-apis"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/alert/jobs/unrecover-alerts"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/dataview"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/diagnotor/controller"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/entity/query"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/entity/storage"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/entity/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/event/query"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/event/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/expression"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/query"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/storage/cassandra"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/storage/clickhouse"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/storage/elasticsearch"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/log/storage/kubernetes-logs"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query-example"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/settings"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/settings/retention-strategy"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/cleaner"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/retention-strategy"
	_ "github.com/erda-project/erda/internal/tools/monitor/dashboard/node-topo"
	_ "github.com/erda-project/erda/internal/tools/monitor/dashboard/org-apis"
	_ "github.com/erda-project/erda/internal/tools/monitor/dashboard/project-apis"
	_ "github.com/erda-project/erda/internal/tools/monitor/dashboard/report/apis"
	_ "github.com/erda-project/erda/internal/tools/monitor/dashboard/report/apis/v1"
	_ "github.com/erda-project/erda/internal/tools/monitor/dashboard/runtime-apis"
	_ "github.com/erda-project/erda/internal/tools/monitor/dashboard/template"
	_ "github.com/erda-project/erda/internal/tools/monitor/monitoring"
	_ "github.com/erda-project/erda/internal/tools/monitor/notify/template/query"
	_ "github.com/erda-project/erda/pkg/common/permission"
	_ "github.com/erda-project/erda/pkg/k8s-client-manager"

	// providers
	_ "github.com/erda-project/erda-infra/providers"
)

//go:embed bootstrap.yaml
var bootstrapCfg string

func main() {
	fmt.Println(runtime.Caller(0))
	common.RegisterInitializer(loghub.Init)
	common.Run(&servicehub.RunOptions{
		Content: bootstrapCfg,
	})
}
