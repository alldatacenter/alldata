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
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub"
	"github.com/erda-project/erda/pkg/common"

	_ "github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis"
	_ "github.com/erda-project/erda/internal/tools/monitor/extensions/cloud/aliyun/metrics/cloudcat"
	_ "github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/sls-import"

	// infra
	_ "github.com/erda-project/erda-infra/providers/health"
	_ "github.com/erda-project/erda-infra/providers/kafka"
	_ "github.com/erda-project/erda-infra/providers/pprof"
)

//go:embed bootstrap.yaml
var bootstrapCfg string

func main() {
	common.RegisterInitializer(loghub.Init)
	common.Run(&servicehub.RunOptions{
		Content: bootstrapCfg,
	})
}
