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

package bundle

import (
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GetProjectWorkSpaceAbilities 获取项目对应环境的集群能力
func (b *Bundle) GetProjectWorkSpaceAbilities(projectID uint64, workspace string, orgID uint64, userID string) (map[string]string, error) {
	abilities := make(map[string]string)
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rsp apistructs.ProjectWorkSpaceAbilityResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/project-workspace-abilities/%s/%s", strconv.FormatUint(projectID, 10), workspace)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Header(httputil.UserHeader, userID).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return nil, toAPIError(resp.StatusCode(), rsp.Error)
	}

	err = json.Unmarshal([]byte(rsp.Data.Abilities), &abilities)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(errors.Errorf("can not Unmarshal deployment_abilities to map"))
	}

	return abilities, nil
}

// CreateProjectWorkSpaceAbilities 创建项目对应环境的集群能力
func (b *Bundle) CreateProjectWorkSpaceAbilities(req apistructs.ProjectWorkSpaceAbility, orgID uint64, userID string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var rsp apistructs.ProjectWorkSpaceAbilityResponse
	resp, err := hc.Post(host).Path(fmt.Sprint("/api/project-workspace-abilities")).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&rsp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return toAPIError(resp.StatusCode(), rsp.Error)
	}

	return nil
}

// UpdateProjectWorkSpaceAbilities 更新项目对应环境的集群能力
func (b *Bundle) UpdateProjectWorkSpaceAbilities(req apistructs.ProjectWorkSpaceAbility, orgID uint64, userID string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var rsp apistructs.ProjectWorkSpaceAbilityResponse
	resp, err := hc.Put(host).Path(fmt.Sprint("/api/project-workspace-abilities")).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&rsp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return toAPIError(resp.StatusCode(), rsp.Error)
	}

	return nil
}

// DeleteProjectWorkSpaceAbilities 删除项目对应环境的集群能力
func (b *Bundle) DeleteProjectWorkSpaceAbilities(projectID uint64, workspace string, orgID uint64, userID string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var rsp apistructs.ProjectWorkSpaceAbilityResponse
	if projectID == 0 {
		apierrors.ErrInvalidParameter.InvalidParameter(errors.New("projectID is invalid"))
	}
	url := ""
	if workspace == "" {
		url = fmt.Sprintf("/api/project-workspace-abilities?projectID=%s", strconv.FormatUint(projectID, 10))
	} else {
		url = fmt.Sprintf("/api/project-workspace-abilities?projectID=%s&workspace=%s", strconv.FormatUint(projectID, 10), workspace)
	}
	resp, err := hc.Delete(host).Path(url).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, strconv.FormatUint(orgID, 10)).
		Header(httputil.UserHeader, userID).
		Do().JSON(&rsp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return toAPIError(resp.StatusCode(), rsp.Error)
	}

	return nil
}
