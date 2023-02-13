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
	"net/url"
	"path/filepath"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
)

const (
	secretKeyDockerArtifactRegistry         = "bp.docker.artifact.registry"
	secretKeyDockerArtifactRegistryUsername = "bp.docker.artifact.registry.username"
	secretKeyDockerArtifactRegistryPassword = "bp.docker.artifact.registry.password"

	secretKeyOrgDockerUrl          = "org.docker.url"
	secretKeyOrgDockerPushUsername = "org.docker.push.username"
	secretKeyOrgDockerPushPassword = "org.docker.push.password"
)

// FetchPlatformSecrets 获取平台级别 secrets
// ignoreKeys: 平台只生成不在 ignoreKeys 列表中的 secrets
func (s *provider) FetchPlatformSecrets(ctx context.Context, p *spec.Pipeline, ignoreKeys []string) (map[string]string, error) {
	r := make(map[string]string)

	var operatorID string
	var operatorName string

	if p.Extra.RunUser != nil {
		operatorID = p.GetRunUserID()
		operatorName = p.Extra.RunUser.Name
	}

	var cronTriggerTime string
	if p.Extra.CronTriggerTime != nil && !p.Extra.CronTriggerTime.IsZero() {
		cronTriggerTime = p.Extra.CronTriggerTime.Format(time.RFC3339)
	}

	clusterInfo, err := s.ClusterInfo.GetClusterInfoByName(p.ClusterName)
	if err != nil {
		return nil, apierrors.ErrGetCluster.InternalError(err)
	}
	mountPoint := clusterInfo.CM.Get(apistructs.DICE_STORAGE_MOUNTPOINT)
	if mountPoint == "" {
		return nil, fmt.Errorf("failed to get necessary cluster info parameter: %s", apistructs.DICE_STORAGE_MOUNTPOINT)
	}

	// if url scheme is file, insert mount point
	storageURL := conf.StorageURL()
	convertURL, err := url.Parse(storageURL)
	if err != nil {
		return nil, fmt.Errorf("failed to parse url, url: %s, (%+v)", storageURL, err)
	}

	if convertURL.Scheme == "file" {
		storageURL = strings.Replace(storageURL, convertURL.Path, filepath.Join(mountPoint, convertURL.Path), -1)
	}

	r = map[string]string{
		// dice version
		"dice.version": version.Version,
		// dice
		"dice.org.id":              p.Labels[apistructs.LabelOrgID],
		"dice.org.name":            p.GetOrgName(),
		"dice.project.id":          p.Labels[apistructs.LabelProjectID],
		"dice.project.name":        p.NormalLabels[apistructs.LabelProjectName],
		"dice.application.id":      p.Labels[apistructs.LabelAppID],
		"dice.application.name":    p.NormalLabels[apistructs.LabelAppName],
		"dice.project.application": p.CommitDetail.RepoAbbr,
		"dice.env":                 string(p.Extra.DiceWorkspace),
		"dice.workspace":           string(p.Extra.DiceWorkspace),
		"dice.cluster.name":        p.ClusterName,
		"dice.operator.id":         operatorID,
		"dice.operator.name":       operatorName,
		"dice.user.id":             operatorID,
		"dice.internal.client":     p.Extra.InternalClient,

		// pipeline
		"pipeline.id":                fmt.Sprintf("%d", p.ID),
		"pipeline.type":              string(p.Type),
		"pipeline.trigger.mode":      string(p.TriggerMode),
		"pipeline.cron.expr":         p.Extra.CronExpr,
		"pipeline.cron.trigger.time": cronTriggerTime,

		// openApi
		"dice.openapi.public.url": conf.OpenAPIPublicURL(),
		"dice.openapi.addr":       discover.Openapi(),

		// buildpack
		"bp.repo.prefix":           "", // Compatible with ((bp.repo.prefix)) written in user pipeline.yml
		"bp.repo.default.version":  "", // Compatible with ((bp.repo.default.version)) written in user pipeline.yml
		"bp.nexus.url":             httpclientutil.WrapProto(clusterInfo.CM.Get(apistructs.NEXUS_ADDR)),
		"bp.nexus.username":        clusterInfo.CM.Get(apistructs.NEXUS_USERNAME),
		"bp.nexus.password":        clusterInfo.CM.Get(apistructs.NEXUS_PASSWORD),
		"bp.docker.cache.registry": httpclientutil.RmProto(clusterInfo.CM.Get(apistructs.REGISTRY_ADDR)),

		// storage
		"pipeline.storage.url": storageURL,

		// collector 用于主动日志上报(action-agent)
		"collector.addr":       discover.Collector(),
		"collector.public.url": conf.CollectorPublicURL(),

		// others
		"date.YYYYMMDD": time.Now().Format("20060102"),
	}
	r = addRegistryLabel(r, clusterInfo.CM)

	r = replaceProjectApplication(r)

	// 额外加载 labels，项目级别的流水线，对应的项目名称和企业名称是传递过来的
	if r["dice.org.name"] == "" {
		r["dice.org.name"] = p.Labels[apistructs.LabelOrgName]
	}
	if r["dice.project.name"] == "" {
		r["dice.project.name"] = p.Labels[apistructs.LabelProjectName]
	}

	for _, key := range ignoreKeys {
		delete(r, key)
	}

	return r, nil
}

// addRegistryLabel Support third-party docker registry
func addRegistryLabel(r map[string]string, clusterInfo apistructs.ClusterInfoData) map[string]string {
	r[secretKeyDockerArtifactRegistry] = httpclientutil.RmProto(clusterInfo.Get(apistructs.REGISTRY_ADDR))
	r[secretKeyDockerArtifactRegistryUsername] = httpclientutil.RmProto(clusterInfo.Get(apistructs.REGISTRY_USERNAME))
	r[secretKeyDockerArtifactRegistryPassword] = httpclientutil.RmProto(clusterInfo.Get(apistructs.REGISTRY_PASSWORD))
	return r
}

// replaceProjectApplication Determine the splicing result according to the given registry addresses of different types
func replaceProjectApplication(r map[string]string) map[string]string {
	ss := strings.SplitN(r[secretKeyDockerArtifactRegistry], "/", 4)
	if len(ss) <= 1 {
		return r
	}
	r["dice.project.application"] = strings.ReplaceAll(r["dice.project.application"], "/", "-")
	return r
}
