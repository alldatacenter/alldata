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

package utils

import "fmt"

func MakeUserOrgPipelineCmsNs(userID string, orgID uint64) string {
	return fmt.Sprintf("user-%s-org-%d", userID, orgID)
}

func MakeOrgGittarTokenPipelineCmsNsConfig() string {
	return "gittar.password"
}

func MakeOrgGittarUsernamePipelineCmsNsConfig() string {
	return "gittar.username"
}

func MakeGittarRepoSecret() string {
	return "gittar.repo"
}

func MakeGittarBranchSecret() string {
	return "gittar.branch"
}

func MakeGittarCommitSecret() string {
	return "gittar.commit"
}

func MakeGittarCommitAbbrevSecret() string {
	return "gittar.commit.abbrev"
}

func MakeGittarMessageSecret() string {
	return "gittar.message"
}

func MakeGittarAuthorSecret() string {
	return "gittar.author"
}
