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
	"context"
	"fmt"
	"time"

	v3 "github.com/coreos/etcd/clientv3"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/loop"
)

const etcdClusterAgentAccessibleKeyTemplate = "/devops/pipeline/action-agent/cluster/%s"
const etcdClusterAgentDownloadingKeyTemplate = "/devops/pipeline/action-agent/dlock/downloading/cluster/%s"

type AgentAccessible struct {
	Image string `json:"image"`
	MD5   string `json:"md5"`
	OK    bool   `json:"ok"`
}

// Ensure 保证 agent 可用:
// 1. 当集群类型为 k8s 时，通过 initContainer 进行下载
// 2. 当集群类型为非 k8s 时，通过现有路径调用 soldier 进行下载
func (s *ActionAgentSvc) Ensure(clusterInfo apistructs.ClusterInfoData, agentImage string, agentMD5 string) error {
	// edas same with k8s cluster
	if clusterInfo.IsK8S() || clusterInfo.IsEDAS() {
		return nil
	}

	clusterName := clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME)

	// 查询缓存 agent 是否已经可用
	ok, err := s.accessible(clusterInfo, agentImage, agentMD5)
	if err != nil && err != jsonstore.NotFoundErr {
		return err
	}
	if ok {
		return nil
	}
	// 没有可用的，尝试下载
	// 获取分布式锁成功才能执行下载
	downloadingLockKey := fmt.Sprintf(etcdClusterAgentDownloadingKeyTemplate, clusterName)
	r, err := s.etcdctl.GetClient().Txn(context.Background()).
		If(v3.Compare(v3.Version(downloadingLockKey), "=", 0)).
		Then(v3.OpPut(downloadingLockKey, "")).
		Commit()
	defer func() {
		_, _ = s.etcdctl.GetClient().Txn(context.Background()).Then(v3.OpDelete(downloadingLockKey)).Commit()
	}()
	if err != nil {
		return apierrors.ErrDownloadActionAgent.InternalError(err)
	}
	// 获取分布式锁失败，说明正在下载中，轮训几次可用状态
	if r != nil && !r.Succeeded {
		err := loop.New(loop.WithInterval(time.Second), loop.WithMaxTimes(3)).
			Do(func() (bool, error) { return s.accessible(clusterInfo, agentImage, agentMD5) })
		if err != nil {
			return apierrors.ErrDownloadActionAgent.InternalError(err)
		}
		return nil
	}
	// 获取分布式锁成功
	// 尝试获取一次 agent 可用状态
	ok, _ = s.accessible(clusterInfo, agentImage, agentMD5)
	if ok {
		return nil
	}
	// 尝试下载
	if err := s.downloadAgent(clusterInfo, agentImage, agentMD5); err != nil {
		return err
	}
	// 写入缓存
	cacheItem := AgentAccessible{Image: agentImage, MD5: agentMD5, OK: true}
	// 设置缓存失效时间
	lease := v3.NewLease(s.etcdctl.GetClient())
	grant, err := lease.Grant(context.Background(), conf.AgentAccessibleCacheTTL())
	if err != nil {
		logrus.Errorf("[alert] failed to grant lease for agent accessible cache: %v, err: %v", cacheItem, err)
		return nil
	}
	_, err = s.accessibleCache.PutWithOption(context.Background(),
		fmt.Sprintf(etcdClusterAgentAccessibleKeyTemplate, clusterName),
		&cacheItem, []interface{}{v3.WithLease(grant.ID)})
	if err != nil {
		// 写入缓存失败，不影响结果
		logrus.Errorf("[alert] failed to write agent accessible cache: %v, err: %v", cacheItem, err)
	}
	return nil
}

func (s *ActionAgentSvc) accessible(clusterInfo apistructs.ClusterInfoData, agentImage, agentMD5 string) (bool, error) {
	// 查询缓存 agent 是否已经可用
	accessibleKey := fmt.Sprintf(etcdClusterAgentAccessibleKeyTemplate, clusterInfo.MustGet(apistructs.DICE_CLUSTER_NAME))
	var accessible AgentAccessible
	if err := s.accessibleCache.Get(context.Background(), accessibleKey, &accessible); err != nil && err != jsonstore.NotFoundErr {
		return false, apierrors.ErrDownloadActionAgent.InternalError(err)
	}
	// 已经可用，直接返回
	if accessible.OK && accessible.Image == agentImage && accessible.MD5 == agentMD5 {
		return true, nil
	}
	return false, jsonstore.NotFoundErr
}
