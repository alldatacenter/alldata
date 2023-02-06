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

	"github.com/erda-project/erda/internal/apps/dop/dao"
)

func (svc *Service) DeleteFileTreeNodeHistory(inode string) {
	go func() {
		var history dao.AutoTestFileTreeNodeHistory
		if err := svc.db.Where("inode = ?", inode).Delete(&history).Error; err != nil {
			logrus.Errorf("node id %s history delete error: %v", inode, err)
		}
	}()
}
