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

package cq

import (
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

func (cq *CQ) TriggerByMR(mr apistructs.MergeRequestInfo) (uint64, error) {
	//// 目标分支是否是保护分支
	//if mr.TargetBranchRule == nil {
	//	logrus.Warnf("MR CQ: no need analyze, reason: target branch doesn't have branch rule, mr info: %s", printMRInfo(mr))
	//	return 0, nil
	//}
	//if !mr.TargetBranchRule.IsProtect {
	//	logrus.Debugf("MR CQ: no need analyze, reason: target branch is not protected, mr info: %s", printMRInfo(mr))
	//}

	// TODO
	// 查询目标分支开关: 流水线配置
	// 查询分支规则，找到目标分支对应的环境
	// 通过环境找到流水线配置是否存在指定的 KEY: MR_CQ=true
	enable := true

	if !enable {
		logrus.Debugf("CQ MR: no need analyze, reason: MR CQ not enabled, mr info: %s", printMRInfo(mr))
	}

	// 构造流水线执行
	return cq.Analyze(CQRequest{
		AppID:    uint64(mr.AppID),
		Commit:   mr.SourceSha,
		Language: LanguageGo, // 当前只支持 dice go
	}, mr.AuthorId)
}

func printMRInfo(mr apistructs.MergeRequestInfo) string {
	return fmt.Sprintf("appID: %d, mr title: %s, source branch: %s(%s), target branch: %s(%s), author: %s, assignee: %s",
		mr.AppID, mr.Title, mr.SourceBranch, mr.SourceSha, mr.TargetBranch, mr.TargetSha, mr.AuthorId, mr.AssigneeId)
}
