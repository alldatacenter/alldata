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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) CreateCloudAddon(orgID, userID, pathWithName string, body *map[string]interface{}) (*apistructs.CreateCloudResourceBaseResponseData, error) {
	host, err := b.urls.CMP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	logrus.Infof("CreateCloudAddon path host: %s, path: %s", host, fmt.Sprintf("/api/%s", pathWithName))
	bb, _ := json.Marshal(*body)
	logrus.Infof("CreateCloudAddon body: %s", string(bb))
	var accountResp apistructs.CreateCloudResourceBaseResponse
	resp, err := hc.Post(host).Path(fmt.Sprintf("/api/%s", pathWithName)).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		JSONBody(*body).
		Do().JSON(&accountResp)
	if err != nil {
		logrus.Errorf("CreateCloudAddon error: %+v", err)
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	cc, _ := json.Marshal(accountResp)
	logrus.Infof("CreateCloudAddon accountResp: %s", string(cc))
	if !resp.IsOK() || !accountResp.Success {
		return nil, toAPIError(resp.StatusCode(), accountResp.Error)
	}

	return &(accountResp.Data), nil
}

func (b *Bundle) CreateCloudAddonWithInstance(orgID, userID, pathWithName, resourceName string, body *map[string]interface{}) (*apistructs.CreateCloudResourceBaseResponseData, error) {
	host, err := b.urls.CMP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	logrus.Infof("CreateCloudAddonWithInstance path host: %s, path: %s", host, fmt.Sprintf("/api/%s/actions/create-%s", pathWithName, resourceName))
	bb, _ := json.Marshal(*body)
	logrus.Infof("CreateCloudAddonWithInstance body: %s", string(bb))
	var accountResp apistructs.CreateCloudResourceBaseResponse
	resp, err := hc.Post(host).Path(fmt.Sprintf("/api/%s/actions/create-%s", pathWithName, resourceName)).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		JSONBody(body).
		Do().JSON(&accountResp)
	if err != nil {
		logrus.Errorf("CreateCloudAddonWithInstance error: %+v", err)
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	cc, _ := json.Marshal(accountResp)
	logrus.Infof("CreateCloudAddonWithInstance accountResp: %s", string(cc))
	if !resp.IsOK() || !accountResp.Success {
		return nil, toAPIError(resp.StatusCode(), accountResp.Error)
	}

	return &(accountResp.Data), nil
}

func (b *Bundle) DeleteCloudAddon(orgID, userID, pathWithName string, body *map[string]interface{}) error {
	host, err := b.urls.CMP()
	if err != nil {
		return err
	}
	hc := b.hc
	logrus.Infof("DeleteCloudAddon path host: %s, path: %s", host, fmt.Sprintf("/api/%s", pathWithName))
	bb, _ := json.Marshal(*body)
	logrus.Infof("DeleteCloudAddon body: %s", string(bb))
	var accountResp apistructs.CreateCloudResourceBaseResponse
	resp, err := hc.Delete(host).Path(fmt.Sprintf("/api/%s", pathWithName)).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		JSONBody(body).
		Do().JSON(&accountResp)
	if err != nil {
		logrus.Errorf("DeleteCloudAddon error: %+v", err)
		return apierrors.ErrInvoke.InternalError(err)
	}
	cc, _ := json.Marshal(accountResp)
	logrus.Infof("DeleteCloudAddon accountResp: %s", string(cc))
	if !resp.IsOK() || !accountResp.Success {
		return toAPIError(resp.StatusCode(), accountResp.Error)
	}

	return nil
}

func (b *Bundle) DeleteCloudAddonResource(orgID, userID, pathWithName, resourceName string, body *map[string]interface{}) error {
	host, err := b.urls.CMP()
	if err != nil {
		return err
	}
	hc := b.hc
	logrus.Infof("DeleteCloudAddonResource path host: %s, path: %s", host, fmt.Sprintf("/api/%s/actions/delete-%s", pathWithName, resourceName))
	bb, _ := json.Marshal(*body)
	logrus.Infof("DeleteCloudAddonResource body: %s", string(bb))
	var accountResp apistructs.CreateCloudResourceBaseResponse
	resp, err := hc.Delete(host).Path(fmt.Sprintf("/api/%s/actions/delete-%s", pathWithName, resourceName)).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		JSONBody(body).
		Do().JSON(&accountResp)
	if err != nil {
		logrus.Errorf("DeleteCloudAddonResource error: %+v", err)
		return apierrors.ErrInvoke.InternalError(err)
	}
	cc, _ := json.Marshal(accountResp)
	logrus.Infof("DeleteCloudAddonResource accountResp: %s", string(cc))
	if !resp.IsOK() || !accountResp.Success {
		return toAPIError(resp.StatusCode(), accountResp.Error)
	}

	return nil
}
