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

package autotest

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

const (
	metaKeyPipelineYml = "pipelineYml"
)

// QueryPipelineSnippetYaml 提供协议接口供 pipeline 引擎在运行时调用，根据 snippet 配置查询 snippet 对应的 pipelineYaml 文件内容
// 规则：
//   - source: autotest
//   - name: file tree node inode
//   - labels: no need now
func (svc *Service) QueryPipelineSnippetYaml(req apistructs.SnippetConfig, identityIndo apistructs.IdentityInfo) (string, error) {
	// 校验 source
	if req.Source != apistructs.PipelineSourceAutoTest.String() {
		return "", apierrors.ErrQueryPipelineSnippetYaml.InvalidParameter(fmt.Errorf("invalid source"))
	}
	// 查询 node
	node, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Name,
		IdentityInfo: identityIndo,
	})
	if err != nil {
		return "", apierrors.ErrQueryPipelineSnippetYaml.InternalError(err)
	}
	y, ok := node.Meta[metaKeyPipelineYml]
	if !ok {
		return "", apierrors.ErrQueryPipelineSnippetYaml.InternalError(fmt.Errorf("node doesn't have pipelineYml"))
	}
	return fmt.Sprintf("%v", y), nil
}
