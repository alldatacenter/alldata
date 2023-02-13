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

package nexus

import (
	"fmt"

	"github.com/erda-project/erda/pkg/strutil"
)

//////////////////////////////////////////
// repo
//////////////////////////////////////////

// ${format}-${type}-publisher-${publisherID}
// example: npm-proxy-publisher-1
func MakePublisherRepoName(format RepositoryFormat, _type RepositoryType, publisherID uint64) string {
	return fmt.Sprintf("%s-%s-publisher-%d", format, _type, publisherID)
}

// ${format}-${type}-org-${orgID}
// example: npm-proxy-org-2
func MakeOrgRepoName(format RepositoryFormat, _type RepositoryType, orgID uint64, suffix ...string) string {
	name := fmt.Sprintf("%s-%s-org-%d", format, _type, orgID)
	if len(suffix) > 0 && suffix[0] != "" {
		name = fmt.Sprintf("%s-%s", name, suffix[0])
	}
	return name
}

// ${format}-${type}-org-${orgID}-thirdparty-${thirdpartyName}
func MakeOrgThirdPartyRepoName(format RepositoryFormat, _type RepositoryType, orgID uint64, thirdPartyName string) string {
	orgRepoNamePrefix := MakeOrgRepoName(format, _type, orgID)
	return strutil.Concat(orgRepoNamePrefix, "-thirdparty-", thirdPartyName)
}

//////////////////////////////////////////
// user
//////////////////////////////////////////

// ${repoName}-deployment
func MakeDeploymentUserName(repoName string) string {
	return fmt.Sprintf("%s-deployment", repoName)
}

// ${repoName}-readonly
func MakeReadonlyUserName(repoName string) string {
	return fmt.Sprintf("%s-readonly", repoName)
}

//////////////////////////////////////////
// pipeline cms
//////////////////////////////////////////

// publisher-${publisherID}-nexus
func MakePublisherPipelineCmNs(publisherID uint64) string {
	return fmt.Sprintf("publisher-%d-nexus", publisherID)
}

// org-${orgID}-nexus
func MakeOrgPipelineCmsNs(orgID uint64) string {
	return fmt.Sprintf("org-%d-nexus", orgID)
}

// platform-nexus
func MakePlatformPipelineCmsNs() string {
	return "platform-nexus"
}
