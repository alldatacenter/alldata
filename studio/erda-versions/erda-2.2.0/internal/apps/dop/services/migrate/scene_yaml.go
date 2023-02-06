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

package migrate

import (
	"fmt"
	"regexp"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/strutil"
)

// taskName -> stepID
// 老语法 -> 新语法
var (
	oldOutputRegex = regexp.MustCompile(`\${([^{}]+):OUTPUT:([^{}]+)}`)
	oldParamsRegex = regexp.MustCompile(`\${params.([^{}]+)}`)
)

func replacePipelineYmlOutputsForTaskStepID(ymlContent string, taskNameStepIDRelations map[string]*dao.AutoTestSceneStep, caseInode string) string {
	return strutil.ReplaceAllStringSubmatchFunc(oldOutputRegex, ymlContent, func(subs []string) string {
		taskName := subs[1]
		metaKey := subs[2]

		step, ok := taskNameStepIDRelations[taskName]
		if !ok {
			logrus.Warnf("failed to replace pipeline yml for task stepID, taskName %q doesn't have corresponding step, caseInode: %s", taskName, caseInode)
			// 返回原数据，调整格式
			newFormat := fmt.Sprintf("${{ outputs.%s.%s }}", taskName, metaKey)
			return newFormat
		}

		newStr := fmt.Sprintf(`${{ outputs.%d.%s }}`, step.ID, metaKey)
		return newStr
	})
}

func replacePipelineYmlParams(ymlContent string, taskNameStepIDRelations map[string]*dao.AutoTestSceneStep, caseInode string) string {
	return strutil.ReplaceAllStringSubmatchFunc(oldParamsRegex, ymlContent, func(subs []string) string {
		paramKey := subs[1]

		newFormat := fmt.Sprintf(`${{ params.%s }}`, paramKey)
		return newFormat
	})
}
