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

package gitflowutil

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

// IsValidBranchWorkspace return if expectWorkspace is valid workspace & artifactWorkspace
func IsValidBranchWorkspace(rules []apistructs.ValidBranch, expectWorkspace apistructs.DiceWorkspace) (validWorkspace, validArtifactWorkspace bool) {
	for _, rule := range rules {
		for _, ws := range strutil.Split(rule.Workspace, ",", true) {
			if ws == expectWorkspace.String() && !validWorkspace {
				validWorkspace = true
			}
		}
		for _, ws := range strutil.Split(rule.ArtifactWorkspace, ",", true) {
			if ws == expectWorkspace.String() && !validArtifactWorkspace {
				validArtifactWorkspace = true
			}
		}
	}
	return
}
