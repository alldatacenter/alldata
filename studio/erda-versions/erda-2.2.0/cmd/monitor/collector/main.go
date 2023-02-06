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
	"os"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/pkg/common"

	// modules
	_ "github.com/erda-project/erda-infra/providers/health"
	_ "github.com/erda-project/erda-infra/providers/kafka"
	_ "github.com/erda-project/erda-infra/providers/kubernetes"
	_ "github.com/erda-project/erda-infra/providers/pprof"
	_ "github.com/erda-project/erda-infra/providers/serviceregister"

	// providers
	_ "github.com/erda-project/erda/internal/tools/monitor/core/collector"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/authentication"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/interceptor"

	// grpc
	_ "github.com/erda-project/erda-infra/providers/grpcclient"
	_ "github.com/erda-project/erda-proto-go/core/token/client"

	// pipeline collector
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/core"
	_ "github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/all"
)

//go:embed bootstrap.yaml
var centralBootstrapCfg string

//go:embed bootstrap-agent.yaml
var edgeBootstrapCfg string

//go:generate sh -c "cd ${PROJ_PATH} && go generate -v -x github.com/erda-project/erda/internal/tools/monitor/core/collector"
func main() {
	cfg := centralBootstrapCfg
	if os.Getenv("DICE_IS_EDGE") == "true" {
		cfg = edgeBootstrapCfg
	}
	common.Run(&servicehub.RunOptions{
		Content: cfg,
	})
}
