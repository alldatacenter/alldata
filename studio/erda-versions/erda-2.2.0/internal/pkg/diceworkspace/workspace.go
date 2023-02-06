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

package diceworkspace

import (
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
)

func GetByGitReference(ref string, branchRules []*apistructs.BranchRule) (apistructs.DiceWorkspace, error) {
	reference := GetValidBranchByGitReference(ref, branchRules)
	if reference.Workspace == "" {
		return "", fmt.Errorf("not support reference %s", ref)
	}
	return apistructs.DiceWorkspace(reference.Workspace), nil
}
func GetValidBranchByGitReference(ref string, branchRules []*apistructs.BranchRule) *apistructs.ValidBranch {
	for _, branchRule := range branchRules {
		branchFilters := strings.Split(branchRule.Rule, ",")
		for _, branchFilter := range branchFilters {
			if IsRefPatternMatch(ref, []string{branchFilter}) {
				return &apistructs.ValidBranch{
					Name:              ref,
					IsProtect:         branchRule.IsProtect,
					NeedApproval:      branchRule.NeedApproval,
					IsTriggerPipeline: branchRule.IsTriggerPipeline,
					Workspace:         branchRule.Workspace,
					ArtifactWorkspace: branchRule.ArtifactWorkspace,
				}
			}
		}
	}
	return &apistructs.ValidBranch{
		Name:              ref,
		IsProtect:         false,
		NeedApproval:      false,
		IsTriggerPipeline: false,
		Workspace:         "",
		ArtifactWorkspace: "",
	}
}

func IsRefPatternMatch(ref string, branchRulePatterns []string) bool {
	for _, branchRulePattern := range branchRulePatterns {
		if strings.HasSuffix(branchRulePattern, "*") {
			//通配符匹配
			rule := strings.TrimSuffix(branchRulePattern, "*")
			if strings.HasPrefix(ref, rule) {
				return true
			}
		} else {
			//完整路径匹配
			if ref == branchRulePattern {
				return true
			}
		}
	}
	return false
}
