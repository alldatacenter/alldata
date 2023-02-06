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

package impl

import (
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/built-in"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/cors"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/csrf"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/custom"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/ip"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/proxy"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/sbac"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/server-guard"
	_ "github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy/policies/waf"
)
