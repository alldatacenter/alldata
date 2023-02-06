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

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
)

func GetGittarRepoURL(clusterName, repoAbbr string) string {
	return getCenterOrEdgeURL(conf.DiceClusterName(), clusterName, httpclientutil.WrapHttp(discover.Gittar()), httpclientutil.WrapHttp(conf.GittarPublicURL())) + "/" + repoAbbr
}

func getCenterOrEdgeURL(diceCluster, requestCluster, center, sass string) string {
	if sass == "" {
		return center
	}
	// 如果和 daemon 在一个集群，则用内网地址
	if diceCluster == requestCluster {
		return center
	}
	return sass
}

func GetGittarSecrets(clusterName, branch string, detail apistructs.CommitDetail) map[string]string {
	return map[string]string{
		MakeGittarRepoSecret():         GetGittarRepoURL(clusterName, detail.RepoAbbr),
		MakeGittarBranchSecret():       branch,
		MakeGittarCommitSecret():       detail.CommitID,
		MakeGittarCommitAbbrevSecret(): getAbbrevCommit(detail.CommitID),
		MakeGittarMessageSecret():      detail.Comment,
		MakeGittarAuthorSecret():       detail.Author,
	}
}

func getAbbrevCommit(commitID string) string {
	if len(commitID) >= 8 {
		return commitID[:8]
	}
	return commitID
}
