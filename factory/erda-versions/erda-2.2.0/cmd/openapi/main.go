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
	_ "github.com/erda-project/erda-proto-go/core/token/client"
	_ "github.com/erda-project/erda/internal/pkg/audit"
	_ "github.com/erda-project/erda/internal/pkg/service-discover/erda-discover"
	_ "github.com/erda-project/erda/internal/pkg/service-discover/fixed-discover"
	_ "github.com/erda-project/erda/internal/tools/openapi/legacy"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth/compatibility"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth/ory-kratos"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth/password"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth/token"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth/uc-session"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/example/backend"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/example/custom-register"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/example/custom-route-source"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/example/publish"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors/common"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors/csrf"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors/dump"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors/user-info"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/routes/custom"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/routes/dynamic"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/routes/dynamic/temporary"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/routes/openapi-v1"
	_ "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/routes/proto"
)

//go:embed bootstrap.yaml
var bootstrapCfg string

func main() {
	common.Run(&servicehub.RunOptions{
		Content: bootstrapCfg,
	})
}
