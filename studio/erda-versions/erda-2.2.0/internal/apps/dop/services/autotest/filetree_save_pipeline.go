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
	"strconv"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

const (
	labelKeyApiCount = "apiCount"
)

func (svc *Service) SaveFileTreeNodePipeline(req apistructs.AutoTestCaseSavePipelineRequest) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrSaveAutoTestFileTreeNodePipeline.InvalidParameter(err)
	}

	needUpdate := false

	// 创建历史记录
	if err := svc.CreateFileTreeNodeHistory(req.Inode); err != nil {
		logrus.Errorf("node id %s history create error: %v", req.Inode, err)
	}

	// 操作流水线文件
	if req.PipelineYml != "" {
		// 为当前节点生成 snippetConfig，pipeline engine 直接使用当前未保存的 pipeline yml 文件内容作为 query-snippet-yaml 结果；
		// 否则，pipeline engine 会拿到老的 yaml，导致无法判断循环引用等问题
		var snippetConfigForCurrentNode *apistructs.SnippetConfig
		currentNodeMeta, exist, err := svc.db.GetAutoTestFileTreeNodeMetaByInode(req.Inode)
		if err != nil {
			return nil, apierrors.ErrSaveAutoTestFileTreeNodePipeline.InternalError(err)
		}
		if exist {
			snippetConfigForCurrentNode = currentNodeMeta.SnippetAction.SnippetConfig
		} else {
			snippetConfigForCurrentNode = generateBaseSnippetConfig()
		}
		snippetConfigForCurrentNode.Name = req.Inode

		// 解析 pipeline 内容
		y, err := svc.bdl.ParsePipelineYmlGraph(apistructs.PipelineYmlParseGraphRequest{
			PipelineYmlContent: req.PipelineYml,
			SnippetConfig:      snippetConfigForCurrentNode,
		})
		if err != nil {
			return nil, apierrors.ErrSaveAutoTestFileTreeNodePipeline.InvalidParameter(fmt.Errorf("invalid pipelineYml: %v", err))
		}

		snippetAction := svc.handleSnippetAction(*y)

		polishedYaml, err := svc.handlePipelineYml(req, *y)
		if err != nil {
			return nil, apierrors.ErrSaveAutoTestFileTreeNodePipeline.InternalError(err)
		}

		// 保存用例节点流水线信息
		if err := svc.db.CreateOrUpdateAutoTestFileTreeNodeMetaPipelineYmlAndSnippetObjByInode(req.Inode, polishedYaml, snippetAction); err != nil {
			return nil, apierrors.ErrSaveAutoTestFileTreeNodePipeline.InternalError(err)
		}

		needUpdate = true
	}

	// 操作运行时参数
	if req.RunParams != nil { // 这里不用 len(req.RunParams) == 0 判断，是想支持调用端传 runParams: [] 时 (非 nil)，可以将 runParams 置空
		addExtra := map[string]interface{}{
			apistructs.AutoTestFileTreeNodeMetaKeyRunParams: req.RunParams,
		}
		if err := svc.db.CreateOrUpdateAutoTestFileTreeNodeMetaAddExtraByInode(req.Inode, addExtra); err != nil {
			return nil, apierrors.ErrSaveAutoTestFileTreeNodePipeline.InternalError(err)
		}
		needUpdate = true
	}

	// 更新用例节点操作人
	if needUpdate && req.IdentityInfo.UserID != "" {
		if err := svc.db.UpdateAutoTestFileTreeNodeBasicInfo(req.Inode, map[string]interface{}{"updater_id": req.IdentityInfo.UserID}); err != nil {
			logrus.Errorf("failed to update updater_id while save pipeline success, inode: %v, err: %v", req.Inode, err)
		}
	}

	// 查询
	node, err := svc.GetFileTreeNode(apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Inode,
		IdentityInfo: req.IdentityInfo,
	})
	if err != nil {
		return nil, apierrors.ErrSaveAutoTestFileTreeNodePipeline.InternalError(err)
	}
	return node, nil
}

// handleSnippetAction 生成 snippet action 配置供其他用例或计划通过 snippet action 方式引用当前节点时，根据该参数拼装 action
func (svc *Service) handleSnippetAction(y apistructs.PipelineYml) apistructs.PipelineYmlAction {

	// 生成 snippet action
	// tips:
	// 生成的 snippet config 没有 name，方便复制；迁移没有问题，迁移仍然使用 name(inode) 进行关联，即迁移的时候被引用的 node 也需要一起迁移，否则运行会报错
	snippetConfig := generateBaseSnippetConfig()

	// 计算接口数
	var apiCount int
	for _, action := range y.FlatActions {
		if action.Type == apistructs.ActionTypeAPITest {
			apiCount++
		}
	}
	snippetConfig.Labels[labelKeyApiCount] = strconv.Itoa(apiCount)

	snippetAction := apistructs.PipelineYmlAction{
		Alias:         "", // alias 调用方赋值，因为一个 snippet 可以被引用多次，alias 不能一样
		Type:          pipelineyml.Snippet,
		Params:        nil, // 调用方统一调用 pipeline 提供的 snippet detail 接口批量获取 params
		SnippetConfig: snippetConfig,
	}

	return snippetAction
}

func generateBaseSnippetConfig() *apistructs.SnippetConfig {
	return &apistructs.SnippetConfig{
		Source: apistructs.PipelineSourceAutoTest.String(), // 固定为 autotest
		Name:   "",                                         // 为了方便复制，在 get node 详情时才赋值，使用 inode 填充
		Labels: map[string]string{},                        // ensure non-nil
	}
}

// handlePipelineYml 处理 pipeline yml 内容
func (svc *Service) handlePipelineYml(req apistructs.AutoTestCaseSavePipelineRequest, y apistructs.PipelineYml) (string, error) {
	needUpdate := false

	// TODO yaml 里 snippet 嵌套返回 actions，则无需查询，直接使用嵌套数据

	// yaml 中若有 snippet autotest case 引用，则查询对应的 snippetConfig 并更新
	for i := range y.Stages {
		for j := range y.Stages[i] {
			action := y.Stages[i][j]
			if action.Type == pipelineyml.Snippet {
				if action.SnippetConfig != nil && action.SnippetConfig.Name != "" &&
					action.SnippetConfig.Source == apistructs.PipelineSourceAutoTest.String() {

					needUpdate = true
					inode := action.SnippetConfig.Name
					meta, exist, err := svc.db.GetAutoTestFileTreeNodeMetaByInode(inode)
					if err != nil {
						return "", fmt.Errorf("failed to get node meta, err: %v", err)
					}
					if !exist {
						return "", fmt.Errorf("node meta not exist, err: %v", err)
					}
					meta.SnippetAction.SnippetConfig.Name = inode

					if y.Stages[i][j].SnippetConfig == nil {
						y.Stages[i][j].SnippetConfig = meta.SnippetAction.SnippetConfig
					} else {
						if meta.SnippetAction.SnippetConfig == nil {
							continue
						}
						for key, value := range meta.SnippetAction.SnippetConfig.Labels {
							y.Stages[i][j].SnippetConfig.Labels[key] = value
						}
					}
				}
			}
		}
	}
	if !needUpdate {
		return req.PipelineYml, nil
	}
	// 序列化
	updatedYaml, err := yaml.Marshal(y)
	if err != nil {
		return "", fmt.Errorf("failed to marshal updated pipeline yml obj, err: %v", err)
	}
	polishedYaml, err := pipelineyml.ConvertGraphPipelineYmlContent(updatedYaml)
	if err != nil {
		return "", fmt.Errorf("failed to polish updated pipeline yml, err: %v", err)
	}
	return string(polishedYaml), nil
}
