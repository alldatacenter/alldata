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
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

func (svc *Service) CreateFileTreeNodeHistory(inode string) error {

	// 查询数量，超过 10 个的全部删除, 这里异步删除，查询是按照创建时间倒序
	histories, err := svc.db.ListAutoTestFileTreeNodeHistoryByinode(inode)
	if err != nil {
		return err
	}
	// 异步删除
	if len(histories) >= 10 {
		for i := 9; i < len(histories); i++ {
			go func(i int) {
				if err := svc.db.DeleteAutoTestFileTreeNodeHistory(&histories[i]); err != nil {
					logrus.Errorf("delete > 10 history %v error: %v", histories[i], err)
				}
			}(i)
		}
	}

	// 异步的去添加
	go func() {
		// 获取当前 inode 的详情
		var req = apistructs.UnifiedFileTreeNodeGetRequest{
			Inode: inode,
		}

		// 查询
		node, exist, err := svc.db.GetAutoTestFileTreeNodeByInode(req.Inode)
		if err != nil {
			logrus.Errorf("save fileTree node history error: query node %s info error: %v", inode, err)
			return
		}
		if !exist {
			logrus.Errorf("save fileTree node history error: query node %s info error: %v", inode, err)
			return
		}

		// 查询节点 meta
		meta, _, err := svc.db.GetAutoTestFileTreeNodeMetaByInode(node.Inode)
		if err != nil {
			logrus.Errorf("save fileTree node history error: query node %s meta error: %v", inode, err)
			return
		}

		// 转化为 history
		var history = dao.AutoTestFileTreeNodeHistory{
			Pinode:    node.Pinode,
			Inode:     node.Inode,
			CreatorID: node.CreatorID,
			UpdaterID: node.UpdaterID,
			Name:      node.Name,
			Desc:      node.Desc,
		}
		if meta != nil {
			history.PipelineYml = meta.PipelineYml
			history.SnippetAction = meta.SnippetAction
			history.Extra = meta.Extra
		}
		// 创建历史记录
		if err := svc.db.CreateAutoTestFileTreeNodeHistory(&history); err != nil {
			logrus.Errorf("save fileTree node %s history error: %v", inode, err)
		}
	}()
	return nil
}
