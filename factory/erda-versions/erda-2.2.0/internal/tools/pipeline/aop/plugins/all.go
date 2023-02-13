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

package plugins

import (
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/apitest_report"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/basic"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/definitionreport"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/precheck_before_pop"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/project"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/scene_after"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/scene_before"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/testplan_after"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/pipeline/testplan_before"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/task/autotest_cookie_keep_after"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/task/autotest_cookie_keep_before"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/task/definitionreport"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins/task/unit_test_report"
)
