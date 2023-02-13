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

package nexussvc

import (
	"context"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/utils"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms"
	"github.com/erda-project/erda/pkg/nexus"
)

func (svc *NexusSvc) SyncRepoConfigToPipelineCM(syncConfig apistructs.NexusSyncConfigToPipelineCM, repo *apistructs.NexusRepository) error {
	ctx := context.Background()
	if syncConfig.SyncPublisher != nil && repo.PublisherID != nil && *repo.PublisherID > 0 {
		if _, err := svc.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(ctx), &cmspb.CmsNsConfigsUpdateRequest{
			Ns:             nexus.MakePublisherPipelineCmNs(*repo.PublisherID),
			PipelineSource: apistructs.PipelineSourceDice.String(),
			KVs:            generateRepoPipelineCmConfigs(repo, syncConfig.SyncPublisher.ConfigPrefix),
		}); err != nil {
			return err
		}
	}
	if syncConfig.SyncOrg != nil && repo.OrgID != nil && *repo.OrgID > 0 {
		if _, err := svc.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(ctx), &cmspb.CmsNsConfigsUpdateRequest{
			Ns:             nexus.MakeOrgPipelineCmsNs(*repo.OrgID),
			PipelineSource: apistructs.PipelineSourceDice.String(),
			KVs:            generateRepoPipelineCmConfigs(repo, syncConfig.SyncOrg.ConfigPrefix),
		}); err != nil {
			return err
		}
	}
	if syncConfig.SyncPlatform != nil {
		if _, err := svc.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(ctx), &cmspb.CmsNsConfigsUpdateRequest{
			Ns:             nexus.MakePlatformPipelineCmsNs(),
			PipelineSource: apistructs.PipelineSourceDice.String(),
			KVs:            generateRepoPipelineCmConfigs(repo, syncConfig.SyncPlatform.ConfigPrefix),
		}); err != nil {
			return err
		}
	}

	return nil
}

func (svc *NexusSvc) SyncUserConfigToPipelineCM(syncConfig apistructs.NexusSyncConfigToPipelineCM, user *apistructs.NexusUser, repoFormat nexus.RepositoryFormat) error {
	ctx := context.Background()
	if syncConfig.SyncPublisher != nil && user.PublisherID != nil && *user.PublisherID > 0 {
		if _, err := svc.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(ctx), &cmspb.CmsNsConfigsUpdateRequest{
			Ns:             nexus.MakePublisherPipelineCmNs(*user.PublisherID),
			PipelineSource: apistructs.PipelineSourceDice.String(),
			KVs:            generateUserPipelineCmConfigs(user.Name, user.Password, syncConfig.SyncPublisher.ConfigPrefix),
		}); err != nil {
			return err
		}
	}
	if syncConfig.SyncOrg != nil && user.OrgID != nil && *user.OrgID > 0 {
		if _, err := svc.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(ctx), &cmspb.CmsNsConfigsUpdateRequest{
			Ns:             nexus.MakeOrgPipelineCmsNs(*user.OrgID),
			PipelineSource: apistructs.PipelineSourceDice.String(),
			KVs:            generateUserPipelineCmConfigs(user.Name, user.Password, syncConfig.SyncOrg.ConfigPrefix),
		}); err != nil {
			return err
		}
	}
	if syncConfig.SyncPlatform != nil {
		if _, err := svc.cms.UpdateCmsNsConfigs(utils.WithInternalClientContext(ctx), &cmspb.CmsNsConfigsUpdateRequest{
			Ns:             nexus.MakePlatformPipelineCmsNs(),
			PipelineSource: apistructs.PipelineSourceDice.String(),
			KVs:            generateUserPipelineCmConfigs(user.Name, user.Password, syncConfig.SyncPlatform.ConfigPrefix),
		}); err != nil {
			return err
		}
	}

	return nil
}

func generateRepoPipelineCmConfigs(repo *apistructs.NexusRepository, keyPrefix string) map[string]*cmspb.PipelineCmsConfigValue {
	configs := make(map[string]*cmspb.PipelineCmsConfigValue)

	configs[keyPrefix+"url"] = &cmspb.PipelineCmsConfigValue{
		Value:       repo.URL,
		EncryptInDB: false,
		Type:        cms.ConfigTypeKV,
		Operations:  &cmspb.PipelineCmsConfigOperations{CanDownload: false, CanEdit: false, CanDelete: false},
		Comment:     "nexus repo url",
	}

	return configs
}

func generateUserPipelineCmConfigs(username, plainPassword, keyPrefix string) map[string]*cmspb.PipelineCmsConfigValue {
	configs := make(map[string]*cmspb.PipelineCmsConfigValue)

	configs[keyPrefix+"username"] = &cmspb.PipelineCmsConfigValue{
		Value:       username,
		EncryptInDB: false,
		Type:        cms.ConfigTypeKV,
		Operations:  &cmspb.PipelineCmsConfigOperations{CanDownload: false, CanEdit: false, CanDelete: false},
		Comment:     "nexus repo username",
	}
	configs[keyPrefix+"password"] = &cmspb.PipelineCmsConfigValue{
		Value:       plainPassword,
		EncryptInDB: true,
		Type:        cms.ConfigTypeKV,
		Operations:  &cmspb.PipelineCmsConfigOperations{CanDownload: false, CanEdit: false, CanDelete: false},
		Comment:     "nexus repo password",
	}

	return configs
}
