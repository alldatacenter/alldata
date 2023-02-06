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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

func (svc *Service) UpdateFileTreeNodeBasicInfo(req apistructs.UnifiedFileTreeNodeUpdateBasicInfoRequest) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrUpdateAutoTestSetBasicInfo.InvalidParameter(err)
	}
	if err := validateBeforeUpdateFileTreeNodeBasicInfo(req); err != nil {
		return nil, apierrors.ErrUpdateAutoTestSetBasicInfo.InvalidParameter(err)
	}
	// 查询
	getReq := apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        req.Inode,
		IdentityInfo: req.IdentityInfo,
	}
	originNode, err := svc.GetFileTreeNode(getReq)
	if err != nil {
		return nil, apierrors.ErrUpdateAutoTestSetBasicInfo.InvalidParameter(err)
	}
	// 计算需要更新的字段
	updateColumns := make(map[string]interface{})
	if req.Name != nil && *req.Name != "" && *req.Name != originNode.Name { // name 不允许为空
		// 若重名，则使用默认重名规则
		if originNode.Pinode != rootDirNodePinode {
			newName, err := svc.ensureNodeName(originNode.Pinode, *req.Name)
			if err != nil {
				return nil, apierrors.ErrUpdateAutoTestSetBasicInfo.InternalError(err)
			}
			req.Name = &newName
		}
		updateColumns["name"] = *req.Name
	}
	if req.Desc != nil && *req.Desc != originNode.Desc { // desc 允许为空
		updateColumns["desc"] = *req.Desc
	}
	// 无需更新，直接返回
	if len(updateColumns) == 0 {
		return originNode, nil
	}

	// 保存历史
	if err := svc.CreateFileTreeNodeHistory(req.Inode); err != nil {
		logrus.Errorf("node id %s history create error: %v", req.Inode, err)
	}

	// 更新
	updateColumns["updater_id"] = req.IdentityInfo.UserID
	if err := svc.db.UpdateAutoTestFileTreeNodeBasicInfo(req.Inode, updateColumns); err != nil {
		return nil, apierrors.ErrUpdateAutoTestSetBasicInfo.InternalError(err)
	}
	// 查询
	node, err := svc.GetFileTreeNode(getReq)
	if err != nil {
		return nil, apierrors.ErrUpdateAutoTestSetBasicInfo.InternalError(err)
	}
	return node, nil
}

func validateBeforeUpdateFileTreeNodeBasicInfo(req apistructs.UnifiedFileTreeNodeUpdateBasicInfoRequest) error {
	// name
	if req.Name != nil && *req.Name != "" {
		if err := strutil.Validate(*req.Name, strutil.MaxLenValidator(maxSetNameLen)); err != nil {
			return fmt.Errorf("invalid name: %v", err)
		}
	}
	// desc
	if req.Desc != nil {
		if err := strutil.Validate(*req.Desc, strutil.MaxLenValidator(maxSetDescLen)); err != nil {
			return fmt.Errorf("invalid desc: %v", err)
		}
	}
	return nil
}
