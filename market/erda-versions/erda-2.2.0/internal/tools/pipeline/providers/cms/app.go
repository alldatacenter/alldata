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

package cms

import (
	"fmt"

	"github.com/erda-project/erda/internal/pkg/gitflowutil"
)

const PipelineAppConfigNameSpacePrefix = "pipeline-secrets-app"

func MakeAppDefaultSecretNamespace(appID string) string {
	return fmt.Sprintf("%s-%s-default", PipelineAppConfigNameSpacePrefix, appID)
}

func MakeAppBranchPrefixSecretNamespace(appID, branch string) (string, error) {
	branchPrefix, err := gitflowutil.GetReferencePrefix(branch)
	if err != nil {
		return "", err
	}
	return MakeAppBranchPrefixSecretNamespaceByBranchPrefix(appID, branchPrefix), nil
}

func MakeAppBranchPrefixSecretNamespaceByBranchPrefix(appID, branchPrefix string) string {
	return fmt.Sprintf("%s-%s-%s", PipelineAppConfigNameSpacePrefix, appID, branchPrefix)
}
