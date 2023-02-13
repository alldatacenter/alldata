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
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"

	"github.com/erda-project/erda/pkg/common"

	// providers and modules
	_ "github.com/erda-project/erda-infra/providers"
	_ "github.com/erda-project/erda-infra/providers/component-protocol"
	_ "github.com/erda-project/erda-infra/providers/serviceregister"
	_ "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/client"
	_ "github.com/erda-project/erda-proto-go/core/monitor/alert/client"
	_ "github.com/erda-project/erda-proto-go/core/monitor/metric/client"
	_ "github.com/erda-project/erda-proto-go/core/pipeline/cron/client"
	_ "github.com/erda-project/erda-proto-go/core/token/client"

	_ "github.com/erda-project/erda/internal/apps/cmp"
	_ "github.com/erda-project/erda/internal/apps/msp/configcenter"
	_ "github.com/erda-project/erda/internal/apps/msp/registercenter"

	// components
	_ "github.com/erda-project/erda/internal/apps/cmp/component-protocol/components"
)

//go:embed bootstrap.yaml
var bootstrapCfg string

func main() {
	common.RegisterHubListener(cpregister.NewHubListener())
	common.Run(&servicehub.RunOptions{
		Content: bootstrapCfg,
	})
}
