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

package actionagentsvc

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
)

const agentDownloadScript = "download_file_from_image"

func (s *ActionAgentSvc) downloadAgent(clusterInfo apistructs.ClusterInfoData, agentImage string, agentMD5 string) error {
	scriptParams := map[string]string{
		"IMAGE_NAME":      agentImage,
		"IMAGE_FILE_PATH": "/opt/action/agent",
		"FILE_NAME":       "agent",
		"EXPECT_MD5":      agentMD5,
		"EXECUTABLE":      "true",
	}
	err := RunScript(clusterInfo, agentDownloadScript, scriptParams)
	if err != nil {
		logrus.Errorf("[alert] failed to download action agent, cluster: %s, err: %v", clusterInfo.Get(apistructs.DICE_CLUSTER_NAME), err)
		return apierrors.ErrDownloadActionAgent.InternalError(err)
	}
	return nil
}
