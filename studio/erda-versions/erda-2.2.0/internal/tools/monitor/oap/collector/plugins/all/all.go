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

package all

import (
	// receivers
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/receivers/collector"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/receivers/dummy"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/receivers/jaeger"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/receivers/kafka"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/receivers/opentelemetry"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/receivers/promremotewrite"

	// processors
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/aggregator"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/k8s-tagger"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/modifier"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/stdout"

	// exporters
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/exporters/clickhouse"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/exporters/collector"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/exporters/kafka"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/exporters/stdout"
)
