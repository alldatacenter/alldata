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

	// providers and modules
	_ "github.com/erda-project/erda-infra/providers"
	_ "github.com/erda-project/erda-infra/providers/grpcclient"
	_ "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/client"
	_ "github.com/erda-project/erda-proto-go/msp/tenant/client"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/api_policy"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/domain"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/endpoint_api"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/global"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/legacy_consumer"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/legacy_upstream"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/legacy_upstream_lb"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/micro_api"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/openapi_consumer"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/openapi_rule"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/org_client"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/providers/runtime_service"
)

//go:embed bootstrap.yaml
var bootstrapCfg string

func main() {
	common.Run(&servicehub.RunOptions{
		Content: bootstrapCfg,
	})
}
