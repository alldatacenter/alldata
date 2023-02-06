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
	"io/ioutil"
	"net/http"
	"reflect"
	"strconv"

	"github.com/pkg/errors"
	"github.com/rancher/wrangler/pkg/data"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// All methods here need bundle withCMP

// GetSteveResource gets k8s resource from steve server.
// Required fields: ClusterName, Name, Type.
func (b *Bundle) GetSteveResource(req *apistructs.SteveRequest) (data.Object, error) {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return nil, errors.New("clusterName, name and type fields are required")
	}

	host, err := b.urls.CMP()
	if err != nil {
		return nil, err
	}

	// path format: /api/k8s/clusters/{clusterName}/v1/{type}/{namespace}/{name}
	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace, req.Name)
	headers := http.Header{
		httputil.InternalHeader: []string{"bundle"},
		httputil.UserHeader:     []string{req.UserID},
		httputil.OrgHeader:      []string{req.OrgID},
	}
	hc := b.hc

	resp, err := hc.Get(host).Path(path).Headers(headers).Do().RAW()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	obj := map[string]interface{}{}
	if err = json.Unmarshal(data, &obj); err != nil {
		return nil, fmt.Errorf(string(data))
	}

	if err = isSteveError(obj); err != nil {
		return nil, err
	}
	return obj, nil
}

// ListSteveResource lists k8s resource from steve server.
// Required fields: ClusterName, Type.
func (b *Bundle) ListSteveResource(req *apistructs.SteveRequest) (data.Object, error) {
	if req.Type == "" || req.ClusterName == "" {
		return nil, errors.New("clusterName and type fields are required")
	}

	host, err := b.urls.CMP()
	if err != nil {
		return nil, err
	}

	// path format: /k8s/clusters/{clusterName}/v1/{type}/{namespace}?{label selectors}
	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace)
	headers := http.Header{
		httputil.InternalHeader: []string{"bundle"},
		httputil.UserHeader:     []string{req.UserID},
		httputil.OrgHeader:      []string{req.OrgID},
	}
	hc := b.hc

	resp, err := hc.Get(host).Path(path).Headers(headers).Params(req.URLQueryString()).Do().RAW()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	obj := map[string]interface{}{}
	if err = json.Unmarshal(data, &obj); err != nil {
		return nil, fmt.Errorf(string(data))
	}

	if err = isSteveError(obj); err != nil {
		return nil, err
	}
	return obj, nil
}

// UpdateSteveResource update a k8s resource described by req.Obj from steve server.
// Required fields: ClusterName, Type, Name, Obj
func (b *Bundle) UpdateSteveResource(req *apistructs.SteveRequest) (data.Object, error) {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return nil, errors.New("clusterName, name and type fields are required")
	}
	if !isObjInvalid(req.Obj) {
		return nil, errors.New("obj in req is invalid")
	}

	host, err := b.urls.CMP()
	if err != nil {
		return nil, err
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace, req.Name)
	headers := http.Header{
		httputil.InternalHeader: []string{"bundle"},
		httputil.UserHeader:     []string{req.UserID},
		httputil.OrgHeader:      []string{req.OrgID},
	}
	hc := b.hc

	resp, err := hc.Put(host).Path(path).Headers(headers).JSONBody(req.Obj).Do().RAW()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	obj := map[string]interface{}{}
	if err = json.Unmarshal(data, &obj); err != nil {
		return nil, fmt.Errorf(string(data))
	}

	if err = isSteveError(obj); err != nil {
		return nil, err
	}
	return obj, nil
}

// CreateSteveResource creates a k8s resource described by req.Obj from steve server.
// Required fields: ClusterName, Namespace, Type, Obj
func (b *Bundle) CreateSteveResource(req *apistructs.SteveRequest) (data.Object, error) {
	if req.Type == "" || req.ClusterName == "" {
		return nil, errors.New("clusterName and type fields are required")
	}
	if !isObjInvalid(req.Obj) {
		return nil, errors.New("obj in req is invalid")
	}

	host, err := b.urls.CMP()
	if err != nil {
		return nil, err
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace)
	headers := http.Header{
		httputil.InternalHeader: []string{"bundle"},
		httputil.UserHeader:     []string{req.UserID},
		httputil.OrgHeader:      []string{req.OrgID},
	}
	hc := b.hc

	resp, err := hc.Post(host).Path(path).Headers(headers).JSONBody(req.Obj).Do().RAW()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	obj := map[string]interface{}{}
	if err = json.Unmarshal(data, &obj); err != nil {
		return nil, fmt.Errorf(string(data))
	}

	if err = isSteveError(obj); err != nil {
		return nil, err
	}
	return obj, nil
}

// DeleteSteveResource delete a k8s resource from steve server.
// Required fields: ClusterName, Type, Name
func (b *Bundle) DeleteSteveResource(req *apistructs.SteveRequest) error {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return errors.New("clusterName, name and type fields are required")
	}

	host, err := b.urls.CMP()
	if err != nil {
		return err
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace, req.Name)
	headers := http.Header{
		httputil.InternalHeader: []string{"bundle"},
		httputil.UserHeader:     []string{req.UserID},
		httputil.OrgHeader:      []string{req.OrgID},
	}
	hc := b.hc

	resp, err := hc.Delete(host).Path(path).Headers(headers).Do().RAW()
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

func isObjInvalid(obj interface{}) bool {
	v := reflect.ValueOf(obj)
	return v.Kind() == reflect.Ptr && !v.IsNil()
}

func isSteveError(obj data.Object) error {
	if obj.String("type") != "error" {
		return nil
	}
	status, _ := strconv.ParseInt(obj.String("status"), 10, 64)
	code := obj.String("code")
	message := obj.String("message")
	return toAPIError(int(status), apistructs.ErrorResponse{
		Code: code,
		Msg:  message,
	})
}
