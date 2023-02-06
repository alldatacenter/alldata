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
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/erda-project/erda/apistructs"
)

func TestGetWorkspaceByBranch(t *testing.T) {
	rules := []*apistructs.BranchRule{
		{
			Rule:              "feature/*",
			IsProtect:         false,
			Workspace:         "DEV",
			ArtifactWorkspace: "DEV",
		},
		{
			Rule:              "develop",
			IsProtect:         false,
			Workspace:         "TEST",
			ArtifactWorkspace: "TEST",
		},
		{
			Rule:              "release/*",
			IsProtect:         true,
			Workspace:         "STAGING",
			ArtifactWorkspace: "STAGING",
		},
		{
			Rule:              "hotfix/*",
			IsProtect:         true,
			Workspace:         "STAGING",
			ArtifactWorkspace: "STAGING",
		},
		{
			Rule:              "support/*",
			IsProtect:         true,
			Workspace:         "PROD",
			ArtifactWorkspace: "PROD",
		},
		{
			Rule:              "master",
			IsProtect:         true,
			Workspace:         "PROD",
			ArtifactWorkspace: "PROD",
		},
	}
	ws, err := GetByGitReference("bugfix/bugfix", rules)
	require.Error(t, err)

	// PROD
	ws, err = GetByGitReference("master", rules)
	require.NoError(t, err)
	require.Equal(t, apistructs.ProdWorkspace, ws)

	ws, err = GetByGitReference("support/2.13.1", rules)
	require.NoError(t, err)
	require.Equal(t, apistructs.ProdWorkspace, ws)

	// STAGING
	ws, err = GetByGitReference("release/2.13.1", rules)
	require.NoError(t, err)
	require.Equal(t, apistructs.StagingWorkspace, ws)

	ws, err = GetByGitReference("hotfix/2.13.1-hotfix", rules)
	require.NoError(t, err)
	require.Equal(t, apistructs.StagingWorkspace, ws)

	// TEST
	ws, err = GetByGitReference("develop", rules)
	require.NoError(t, err)
	require.Equal(t, apistructs.TestWorkspace, ws)

	ws, err = GetByGitReference("develop-sonar", rules)
	require.Error(t, err)

	// DEV
	ws, err = GetByGitReference("feature/gitflow", rules)
	require.NoError(t, err)
	require.Equal(t, apistructs.DevWorkspace, ws)

	ws, err = GetByGitReference("upgrade/1", rules)
	require.Error(t, err)
}
