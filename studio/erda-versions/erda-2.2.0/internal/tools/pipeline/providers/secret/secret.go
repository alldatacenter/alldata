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

package secret

import (
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/pkg/strutil"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
	"github.com/erda-project/erda/pkg/nexus"
)

// FetchSecrets return secrets, cmsDiceFiles and error.
// holdOnKeys: 声明 key 需要持有，不能被平台 secrets 覆盖，与 ignoreKeys 配合使用
func (s *provider) FetchSecrets(ctx context.Context, p *spec.Pipeline) (secrets, cmsDiceFiles map[string]string, holdOnKeys, encryptSecretKeys []string, err error) {
	secrets = make(map[string]string)
	cmsDiceFiles = make(map[string]string)

	namespaces := p.GetConfigManageNamespaces()

	// 制品是否需要跨集群
	needCrossCluster := false
	if p.Snapshot.AnalyzedCrossCluster != nil && *p.Snapshot.AnalyzedCrossCluster {
		// 企业级 nexus 配置，包含 platform docker registry
		if orgIDStr := p.Labels[apistructs.LabelOrgID]; orgIDStr != "" {
			orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
			if err != nil {
				return nil, nil, nil, nil, fmt.Errorf("invalid org id from label %q, err: %v", apistructs.LabelOrgID, err)
			}
			org, err := s.bdl.GetOrg(orgID)
			if err != nil {
				return nil, nil, nil, nil, err
			}
			if org.EnableReleaseCrossCluster {
				namespaces = append(namespaces, nexus.MakeOrgPipelineCmsNs(orgID))
			}
			namespaces = strutil.DedupSlice(namespaces, true)
			needCrossCluster = true
		}
	}

	batchConfigs, err := s.CmsService.BatchGetCmsNsConfigs(apis.WithInternalClientContext(context.Background(), "pipeline"),
		&pb.CmsNsConfigsBatchGetRequest{
			PipelineSource: p.PipelineSource.String(),
			Namespaces:     namespaces,
			GlobalDecrypt:  true,
		})
	if err != nil {
		return nil, nil, nil, nil, err
	}

	for _, c := range batchConfigs.Configs {
		if c.EncryptInDB && c.Type == cms.ConfigTypeKV {
			encryptSecretKeys = append(encryptSecretKeys, c.Key)
		}
		secrets[c.Key] = c.Value

		// DiceFile 类型，value 为 diceFileUUID
		if c.Type == cms.ConfigTypeDiceFile {
			cmsDiceFiles[c.Key] = c.Value
		}
	}

	// docker artifact registry
	if needCrossCluster {
		secrets[secretKeyDockerArtifactRegistry] = httpclientutil.RmProto(secrets[secretKeyOrgDockerUrl])
		secrets[secretKeyDockerArtifactRegistryUsername] = secrets[secretKeyOrgDockerPushUsername]
		secrets[secretKeyDockerArtifactRegistryPassword] = secrets[secretKeyOrgDockerPushPassword]
		holdOnKeys = append(holdOnKeys,
			secretKeyDockerArtifactRegistry,
			secretKeyDockerArtifactRegistryUsername,
			secretKeyDockerArtifactRegistryPassword,
		)
	}

	return secrets, cmsDiceFiles, holdOnKeys, encryptSecretKeys, nil
}
