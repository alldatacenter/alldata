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

package models

import (
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
)

func (svc *Service) CheckPermission(repo *gitmodule.Repository, user *User, permission Permission, resourceRoleList []string) error {
	resourceRole := ""
	if len(resourceRoleList) > 0 {
		resourceRole = strings.Join(resourceRoleList, ",")
	}
	checkPermission, err := svc.bundle.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:       user.Id,
		Scope:        "app",
		ScopeID:      uint64(repo.ApplicationId),
		Resource:     "repo",
		Action:       string(permission),
		ResourceRole: resourceRole,
	})
	if err != nil {
		return err
	}
	if !checkPermission.Access {
		return fmt.Errorf("no permission: %s for user: %s", permission, user.NickName)
	}
	return nil
}
