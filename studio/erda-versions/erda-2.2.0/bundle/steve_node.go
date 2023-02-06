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
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// PatchNode patch a node described by req.Obj from steve server.
// Required fields: ClusterName, Name, Obj
func (b *Bundle) PatchNode(req *apistructs.SteveRequest) error {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return errors.New("clusterName, name and type fields are required")
	}
	if !isObjInvalid(req.Obj) {
		return errors.New("obj in req is invalid")
	}

	host, err := b.urls.CMP()
	if err != nil {
		return err
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1/node", req.Name)
	headers := http.Header{
		httputil.InternalHeader: []string{"bundle"},
		httputil.UserHeader:     []string{req.UserID},
		httputil.OrgHeader:      []string{req.OrgID},
	}
	hc := b.hc

	resp, err := hc.Patch(host).Path(path).Headers(headers).JSONBody(req.Obj).Do().RAW()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	obj := map[string]interface{}{}
	if err = json.Unmarshal(data, &obj); err != nil {
		return fmt.Errorf(string(data))
	}
	return isSteveError(obj)
}

// LabelNode labels a node.
// Required filed: ClusterName, Name
func (b *Bundle) LabelNode(req *apistructs.SteveRequest, labels map[string]string) error {
	if req.ClusterName == "" || req.Name == "" {
		return errors.New("clusterName and name fields are required")
	}

	if labels == nil || len(labels) == 0 {
		return errors.New("labels are required")
	}

	metadata := map[string]interface{}{
		"metadata": map[string]interface{}{
			"labels": labels,
		},
	}
	req.Obj = &metadata
	return b.PatchNode(req)
}

// UnlabelNode unlabels a node.
// Required filed: ClusterName, Name
func (b *Bundle) UnlabelNode(req *apistructs.SteveRequest, labels []string) error {
	if req.ClusterName == "" || req.Name == "" {
		return errors.New("clusterName and name fields are required")
	}

	if len(labels) == 0 {
		return errors.New("labels are required")
	}

	toUnlabel := make(map[string]interface{})
	for _, label := range labels {
		toUnlabel[label] = nil
	}
	metadata := map[string]interface{}{
		"metadata": map[string]interface{}{
			"labels": toUnlabel,
		},
	}
	req.Obj = &metadata
	return b.PatchNode(req)
}

// CordonNode cordons a node.
// Required fields: ClusterName, Name
func (b *Bundle) CordonNode(req *apistructs.SteveRequest) error {
	if req.ClusterName == "" || req.Name == "" {
		return errors.New("clusterName and name fields are required")
	}

	spec := map[string]interface{}{
		"spec": map[string]interface{}{
			"unschedulable": true,
		},
	}
	req.Obj = &spec
	return b.PatchNode(req)
}

// UnCordonNode uncordons a node.
// Required fields: ClusterName, Name
func (b *Bundle) UnCordonNode(req *apistructs.SteveRequest) error {
	if req.ClusterName == "" || req.Name == "" {
		return errors.New("clusterName and name fields are required")
	}

	spec := map[string]interface{}{
		"spec": map[string]interface{}{
			"unschedulable": false,
		},
	}
	req.Obj = &spec
	return b.PatchNode(req)
}
