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

package steve

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/wrangler/pkg/data"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	"k8s.io/api/policy/v1beta1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	apiuser "k8s.io/apiserver/pkg/authentication/user"
	"k8s.io/apiserver/pkg/endpoints/request"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/apps/cmp/cache"
	"github.com/erda-project/erda/internal/apps/cmp/queue"
	"github.com/erda-project/erda/internal/apps/cmp/steve/middleware"
	"github.com/erda-project/erda/internal/apps/cmp/steve/predefined"
	httpapi "github.com/erda-project/erda/pkg/common/httpapi"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/k8sclient"
	"github.com/erda-project/erda/pkg/strutil"
)

const OfflineLabel = "dice/offline"

var queryQueue *queue.QueryQueue

func init() {
	queueSize := 10
	if size, err := strconv.Atoi(os.Getenv("LIST_QUEUE_SIZE")); err == nil && size > queueSize {
		queueSize = size
	}
	queryQueue = queue.NewQueryQueue(queueSize)
}

// GetSteveResource gets k8s resource from steve server.
// Required fields: ClusterName, Name, Type.
func (a *Aggregator) GetSteveResource(ctx context.Context, req *apistructs.SteveRequest) (types.APIObject, error) {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return types.APIObject{}, errors.New("clusterName, name and type fields are required")
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace, req.Name)

	var (
		user apiuser.Info
		err  error
	)
	if req.Type == apistructs.K8SNode || req.NoAuthentication {
		user = &apiuser.DefaultInfo{
			Name: "admin",
			UID:  "admin",
			Groups: []string{
				"system:masters",
				"system:authenticated",
			},
		}
	} else {
		user, err = a.Auth(req.UserID, req.OrgID, req.ClusterName)
		if err != nil {
			return types.APIObject{}, err
		}
	}

	withUser := request.WithUser(ctx, user)
	r, err := http.NewRequestWithContext(withUser, http.MethodGet, path, nil)
	if err != nil {
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(err)
	}

	resp := &Response{}
	apiOp := &types.APIRequest{
		Name:           req.Name,
		Type:           string(req.Type),
		Method:         http.MethodGet,
		Namespace:      req.Namespace,
		ResponseWriter: resp,
		Request:        r,
		Response:       &StatusCodeGetter{Response: resp},
	}
	if err := a.Serve(req.ClusterName, apiOp); err != nil {
		return types.APIObject{}, err
	}

	rawRes, ok := resp.ResponseData.(*types.RawResource)
	if !ok {
		if resp.ResponseData == nil {
			return types.APIObject{}, apierrors.ErrInvoke.InternalError(errors.New("null response data"))
		}
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(errors.Errorf("unknown response data type: %s", reflect.TypeOf(resp.ResponseData).String()))
	}

	obj := rawRes.APIObject
	objData := obj.Data()
	if objData.String("type") == "error" {
		return types.APIObject{}, getAPIError(objData)
	}
	return obj, nil
}

func (a *Aggregator) list(apiOp *types.APIRequest, resp *Response, clusterName string) ([]types.APIObject, error) {
	logrus.Infof("[DEBUG %s] start request steve aggregator at %s", apiOp.Type, time.Now().Format(time.StampNano))
	if err := a.Serve(clusterName, apiOp); err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	logrus.Infof("[DEBUG %s] end request steve aggregator at %s", apiOp.Type, time.Now().Format(time.StampNano))
	return convertResp(resp)
}

func (a *Aggregator) getApiRequest(ctx context.Context, req *apistructs.SteveRequest) (*types.APIRequest, *Response, error) {
	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace)

	var (
		params []string
		query  string
		err    error
	)
	if len(req.LabelSelector) != 0 || len(req.FieldSelector) != 0 {
		values := req.URLQueryString()
		for k, v := range values {
			for _, value := range v {
				params = append(params, fmt.Sprintf("%s=%s", k, value))
			}
		}
		query = strutil.Join(params, "&", true)
	}
	url, err := url.ParseRequestURI(fmt.Sprintf("%s?%s", path, query))
	if err != nil {
		return nil, nil, errors.Errorf("failed to parse url, %v", err)
	}

	var user apiuser.Info
	if req.Type == apistructs.K8SNode || req.NoAuthentication {
		user = &apiuser.DefaultInfo{
			Name: "admin",
			UID:  "admin",
			Groups: []string{
				"system:masters",
				"system:authenticated",
			},
		}
	} else {
		user, err = a.Auth(req.UserID, req.OrgID, req.ClusterName)
		if err != nil {
			return nil, nil, err
		}
	}

	withUser := request.WithUser(ctx, user)
	r, err := http.NewRequestWithContext(withUser, http.MethodGet, url.String(), nil)
	if err != nil {
		return nil, nil, apierrors.ErrInvoke.InternalError(err)
	}

	resp := &Response{}
	apiOp := &types.APIRequest{
		Type:           string(req.Type),
		Method:         http.MethodGet,
		Namespace:      req.Namespace,
		ResponseWriter: resp,
		Request:        r,
		Response:       &StatusCodeGetter{Response: resp},
	}
	return apiOp, resp, nil
}

type CacheKey struct {
	Kind        string
	Namespace   string
	ClusterName string
}

func (k *CacheKey) GetKey() string {
	d := sha256.New()
	d.Write([]byte(k.Kind))
	d.Write([]byte(k.Namespace))
	d.Write([]byte(k.ClusterName))
	return hex.EncodeToString(d.Sum(nil))
}

// ListSteveResource lists k8s resource from steve server.
// Required fields: ClusterName, Type.
func (a *Aggregator) ListSteveResource(ctx context.Context, req *apistructs.SteveRequest) ([]types.APIObject, error) {
	if req.Type == "" || req.ClusterName == "" {
		return nil, apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName and type fields are required"))
	}

	apiOp, resp, err := a.getApiRequest(ctx, req)
	if err != nil {
		return nil, err
	}

	if !a.IsServerReady(req.ClusterName) || len(req.LabelSelector) != 0 || len(req.FieldSelector) != 0 {
		return a.list(apiOp, resp, req.ClusterName)
	}

	hasAccess, err := a.HasAccess(req.ClusterName, apiOp, "list")
	if err != nil {
		return nil, err
	}
	if !hasAccess {
		return nil, apierrors.ErrInvoke.AccessDenied()
	}

	key := CacheKey{
		Kind:        string(req.Type),
		Namespace:   req.Namespace,
		ClusterName: req.ClusterName,
	}
	values, lexpired, err := cache.GetFreeCache().Get(key.GetKey())
	if values == nil || err != nil {
		logrus.Infof("can not get cache for %s, list from steve server", req.Type)
		queryQueue.Acquire(req.ClusterName, 1)
		list, err := a.list(apiOp, resp, req.ClusterName)
		queryQueue.Release(req.ClusterName, 1)
		if err != nil {
			return nil, err
		}
		vals, err := cache.GetInterfaceValue(list)
		if err != nil {
			return nil, errors.Errorf("failed to marshal cache data for %s, %v", apiOp.Type, err)
		}
		if err = cache.GetFreeCache().Set(key.GetKey(), vals, time.Second.Nanoseconds()*30); err != nil {
			logrus.Errorf("failed to set cache for %s", apiOp.Type)
		}
		return list, nil
	}

	if lexpired {
		tmp := *req
		task := &queue.Task{
			Key: key.GetKey(),
			Do: func() {
				ctx, cancel := context.WithTimeout(context.Background(), time.Second*10)
				defer cancel()
				apiOp, resp, err := a.getApiRequest(ctx, &tmp)
				if err != nil {
					logrus.Errorf("failed to get api request in task, %v", err)
					return
				}

				list, err := a.list(apiOp, resp, tmp.ClusterName)
				if err != nil {
					logrus.Errorf("failed to list %s in task, %v", apiOp.Type, err)
					return
				}
				value, err := cache.GetInterfaceValue(list)
				if err != nil {
					logrus.Errorf("failed to marshal cache data for %s, %v", apiOp.Type, err)
					return
				}
				if err = cache.GetFreeCache().Set(key.GetKey(), value, time.Second.Nanoseconds()*30); err != nil {
					logrus.Errorf("failed to set cache for %s, %v", apiOp.Type, err)
				}
			},
		}
		cache.ExpireFreshQueue.Enqueue(task)
	}

	logrus.Infof("get %s from cache", req.Type)
	list := values[0].Value().([]types.APIObject)
	return list, nil
}

func convertResp(resp *Response) ([]types.APIObject, error) {
	collection, ok := resp.ResponseData.(*types.GenericCollection)
	if !ok {
		if resp.ResponseData == nil {
			return nil, apierrors.ErrInvoke.InternalError(errors.New("null response data"))
		}
		rawResource, ok := resp.ResponseData.(*types.RawResource)
		if !ok {
			return nil, apierrors.ErrInvoke.InternalError(errors.Errorf("unknown response data type: %s", reflect.TypeOf(resp.ResponseData).String()))
		}
		obj := rawResource.APIObject.Data()
		return nil, apierrors.ErrInvoke.InternalError(errors.New(obj.String("message")))
	}

	var objects []types.APIObject
	for _, obj := range collection.Data {
		objects = append(objects, obj.APIObject)
	}
	return objects, nil
}

func setCacheForList(key string, list []types.APIObject) error {
	vals, err := cache.GetInterfaceValue(list)
	if err != nil {
		return err
	}
	if err = cache.GetFreeCache().Set(key, vals, time.Second.Nanoseconds()*30); err != nil {
		return err
	}
	return nil
}

func newReadCloser(obj interface{}) (io.ReadCloser, error) {
	jsonData, err := json.Marshal(obj)
	if err != nil {
		return nil, err
	}

	return io.NopCloser(bytes.NewReader(jsonData)), nil
}

// UpdateSteveResource update a k8s resource described by req.Obj from steve server and creates an audit event.
// Required fields: ClusterName, Type, Name, Obj
func (a *Aggregator) UpdateSteveResource(ctx context.Context, req *apistructs.SteveRequest) (types.APIObject, error) {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return types.APIObject{}, apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName, name and type fields are required"))
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace, req.Name)

	user, err := a.Auth(req.UserID, req.OrgID, req.ClusterName)
	if err != nil {
		return types.APIObject{}, err
	}

	body, err := newReadCloser(req.Obj)
	if err != nil {
		return types.APIObject{}, apierrors.ErrInvoke.InvalidParameter(errors.Errorf("failed to get body, %v", err))
	}

	withUser := request.WithUser(ctx, user)
	r, err := http.NewRequestWithContext(withUser, http.MethodPut, path, body)
	if err != nil {
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(err)
	}

	resp := &Response{}
	apiOp := &types.APIRequest{
		Name:           req.Name,
		Type:           string(req.Type),
		Method:         http.MethodPut,
		Namespace:      req.Namespace,
		ResponseWriter: resp,
		Request:        r,
		Response:       &StatusCodeGetter{Response: resp},
	}
	if err := a.Serve(req.ClusterName, apiOp); err != nil {
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(err)
	}

	rawRes, ok := resp.ResponseData.(*types.RawResource)
	if !ok {
		if resp.ResponseData == nil {
			return types.APIObject{}, apierrors.ErrInvoke.InternalError(errors.New("null response data"))
		}
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(errors.Errorf("unknown response data type: %s", reflect.TypeOf(resp.ResponseData).String()))
	}

	obj := rawRes.APIObject
	objData := obj.Data()
	if objData.String("type") == "error" {
		return types.APIObject{}, getAPIError(objData)
	}

	RemoveCache(req.ClusterName, "", string(req.Type))
	RemoveCache(req.ClusterName, req.Namespace, string(req.Type))

	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceType: req.Type,
		middleware.AuditNamespace:    req.Namespace,
		middleware.AuditResourceName: req.Name,
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditUpdateResource, auditCtx); err != nil {
		logrus.Errorf("failed to audit when update steve resource, %v", err)
	}
	return obj, nil
}

// CreateSteveResource creates a k8s resource described by req.Obj from steve server and creates an audit event.
// Required fields: ClusterName, Type, Obj
func (a *Aggregator) CreateSteveResource(ctx context.Context, req *apistructs.SteveRequest) (types.APIObject, error) {
	if req.Type == "" || req.ClusterName == "" {
		return types.APIObject{}, apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName and type fields are required"))
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type))

	user, err := a.Auth(req.UserID, req.OrgID, req.ClusterName)
	if err != nil {
		return types.APIObject{}, err
	}

	body, err := newReadCloser(req.Obj)
	if err != nil {
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(errors.Errorf("failed to get body, %v", err))
	}

	withUser := request.WithUser(ctx, user)
	r, err := http.NewRequestWithContext(withUser, http.MethodPost, path, body)
	if err != nil {
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(err)
	}

	resp := &Response{}
	apiOp := &types.APIRequest{
		Type:           string(req.Type),
		Method:         http.MethodPost,
		Namespace:      req.Namespace,
		ResponseWriter: resp,
		Request:        r,
		Response:       &StatusCodeGetter{Response: resp},
	}
	if err := a.Serve(req.ClusterName, apiOp); err != nil {
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(err)
	}

	rawRes, ok := resp.ResponseData.(*types.RawResource)
	if !ok {
		if resp.ResponseData == nil {
			return types.APIObject{}, apierrors.ErrInvoke.InternalError(errors.New("null response data"))
		}
		return types.APIObject{}, apierrors.ErrInvoke.InternalError(errors.Errorf("unknown response data type: %s", reflect.TypeOf(resp.ResponseData).String()))
	}

	obj := rawRes.APIObject
	objData := obj.Data()
	if objData.String("type") == "error" {
		return types.APIObject{}, getAPIError(objData)
	}

	RemoveCache(req.ClusterName, "", string(req.Type))
	RemoveCache(req.ClusterName, req.Namespace, string(req.Type))

	reqObj, err := data.Convert(req.Obj)
	if err != nil {
		logrus.Errorf("failed to convert obj in request to data.Object, %v", err)
		return obj, nil
	}
	namespace := reqObj.String("metadata", "namespace")
	name := reqObj.String("metadata", "name")
	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceType: req.Type,
		middleware.AuditNamespace:    namespace,
		middleware.AuditResourceName: name,
	}

	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditCreateResource, auditCtx); err != nil {
		logrus.Errorf("failed to audit when update steve resource, %v", err)
	}
	return obj, nil
}

// DeleteSteveResource delete a k8s resource from steve server and creates an audit event.
// Required fields: ClusterName, Type, Name
func (a *Aggregator) DeleteSteveResource(ctx context.Context, req *apistructs.SteveRequest) error {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName, name and type fields are required"))
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1", string(req.Type), req.Namespace, req.Name)

	user, err := a.Auth(req.UserID, req.OrgID, req.ClusterName)
	if err != nil {
		return err
	}

	withUser := request.WithUser(ctx, user)
	r, err := http.NewRequestWithContext(withUser, http.MethodDelete, path, nil)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	resp := &Response{}
	apiOp := &types.APIRequest{
		Name:           req.Name,
		Type:           string(req.Type),
		Method:         http.MethodDelete,
		Namespace:      req.Namespace,
		ResponseWriter: resp,
		Request:        r,
		Response:       &StatusCodeGetter{Response: resp},
	}
	if err := a.Serve(req.ClusterName, apiOp); err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	rawRes, ok := resp.ResponseData.(*types.RawResource)
	if ok {
		obj := rawRes.APIObject
		objData := obj.Data()
		if objData.String("type") == "error" {
			return getAPIError(objData)
		}
	}

	RemoveCache(req.ClusterName, "", string(req.Type))
	RemoveCache(req.ClusterName, req.Namespace, string(req.Type))

	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceType: req.Type,
		middleware.AuditNamespace:    req.Namespace,
		middleware.AuditResourceName: req.Name,
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditDeleteResource, auditCtx); err != nil {
		logrus.Errorf("failed to audit when update steve resource, %v", err)
	}
	return nil
}

// PatchNode patch a node described by req.Obj from steve server.
// Required fields: ClusterName, Name, Obj
func (a *Aggregator) PatchNode(ctx context.Context, req *apistructs.SteveRequest) error {
	if req.Type == "" || req.ClusterName == "" || req.Name == "" {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName, name and type fields are required"))
	}

	path := strutil.JoinPath("/api/k8s/clusters", req.ClusterName, "v1/node", req.Name)

	user, err := a.Auth(req.UserID, req.OrgID, req.ClusterName)
	if err != nil {
		return err
	}

	body, err := newReadCloser(req.Obj)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(errors.Errorf("failed to get body, %v", err))
	}

	withUser := request.WithUser(ctx, user)
	r, err := http.NewRequestWithContext(withUser, http.MethodPatch, path, body)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	resp := &Response{}
	apiOp := &types.APIRequest{
		Name:           req.Name,
		Type:           string(req.Type),
		Method:         http.MethodPatch,
		ResponseWriter: resp,
		Request:        r,
		Response:       &StatusCodeGetter{Response: resp},
	}
	if err := a.Serve(req.ClusterName, apiOp); err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	rawRes, ok := resp.ResponseData.(*types.RawResource)
	if !ok {
		if resp.ResponseData == nil {
			return apierrors.ErrInvoke.InternalError(errors.New("null response data"))
		}
		return apierrors.ErrInvoke.InternalError(errors.Errorf("unknown response data type: %s", reflect.TypeOf(resp.ResponseData).String()))
	}

	obj := rawRes.APIObject
	objData := obj.Data()
	if objData.String("type") == "error" {
		return getAPIError(objData)
	}

	RemoveCache(req.ClusterName, "", string(req.Type))
	RemoveCache(req.ClusterName, req.Namespace, string(req.Type))

	return nil
}

func (a *Aggregator) labelNode(ctx context.Context, req *apistructs.SteveRequest, labels map[string]string) error {
	if req.ClusterName == "" || req.Name == "" {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName and name fields are required"))
	}

	if labels == nil || len(labels) == 0 {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("labels are required"))
	}

	metadata := map[string]interface{}{
		"metadata": map[string]interface{}{
			"labels": labels,
		},
	}
	req.Obj = &metadata

	err := a.PatchNode(ctx, req)
	if err != nil {
		return err
	}
	return nil
}

// LabelNode labels a node and creates an audit event.
// Required filed: ClusterName, Name, labels
func (a *Aggregator) LabelNode(ctx context.Context, req *apistructs.SteveRequest, labels map[string]string) error {
	if err := a.labelNode(ctx, req, labels); err != nil {
		return err
	}

	var k, v string
	// there can only be one piece of k/v
	for k, v = range labels {
	}
	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceName: req.Name,
		middleware.AuditTargetLabel:  fmt.Sprintf("%s=%s", k, v),
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditLabelNode, auditCtx); err != nil {
		logrus.Errorf("failed to audit when update steve resource, %v", err)
	}
	return nil
}

func (a *Aggregator) unlabelNode(ctx context.Context, req *apistructs.SteveRequest, labels []string) error {
	if req.ClusterName == "" || req.Name == "" {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName and name fields are required"))
	}

	if len(labels) == 0 {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("labels are required"))
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

	err := a.PatchNode(ctx, req)
	if err != nil {
		return err
	}
	return nil
}

// UnlabelNode unlabels a node and creates an audit event.
// Required filed: ClusterName, Name, labels
func (a *Aggregator) UnlabelNode(ctx context.Context, req *apistructs.SteveRequest, labels []string) error {
	if err := a.unlabelNode(ctx, req, labels); err != nil {
		return err
	}

	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceName: req.Name,
		middleware.AuditTargetLabel:  labels[0],
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditLabelNode, auditCtx); err != nil {
		logrus.Errorf("failed to audit when update steve resource, %v", err)
	}
	return nil
}

func (a *Aggregator) cordonNode(ctx context.Context, req *apistructs.SteveRequest) error {
	if req.ClusterName == "" || req.Name == "" {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName and name fields are required"))
	}

	spec := map[string]interface{}{
		"spec": map[string]interface{}{
			"unschedulable": true,
		},
	}
	req.Obj = &spec

	err := a.PatchNode(ctx, req)
	if err != nil {
		return err
	}
	return err
}

// CordonNode cordons a node and creates an audit event.
// Required fields: ClusterName, Name
func (a *Aggregator) CordonNode(ctx context.Context, req *apistructs.SteveRequest) error {
	if err := a.cordonNode(ctx, req); err != nil {
		return err
	}

	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceName: req.Name,
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditCordonNode, auditCtx); err != nil {
		logrus.Errorf("failed to audit when update steve resource, %v", err)
	}
	return nil
}

// UnCordonNode uncordons a node and creates an audit event.
// Required fields: ClusterName, Name
func (a *Aggregator) UnCordonNode(ctx context.Context, req *apistructs.SteveRequest) error {
	if req.ClusterName == "" || req.Name == "" {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("clusterName and name fields are required"))
	}

	spec := map[string]interface{}{
		"spec": map[string]interface{}{
			"unschedulable": false,
		},
	}
	req.Obj = &spec

	err := a.PatchNode(ctx, req)
	if err != nil {
		return err
	}

	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceName: req.Name,
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditUncordonNode, auditCtx); err != nil {
		logrus.Errorf("failed to audit when update steve resource, %v", err)
	}
	return nil
}

// DrainNode drains a node and creates an audit event.
func (a *Aggregator) DrainNode(ctx context.Context, req *apistructs.SteveRequest) error {
	if err := a.cordonNode(ctx, req); err != nil {
		return err
	}

	podReq := &apistructs.SteveRequest{
		UserID:      req.UserID,
		OrgID:       req.OrgID,
		Type:        apistructs.K8SPod,
		ClusterName: req.ClusterName,
	}
	list, err := a.ListSteveResource(ctx, podReq)
	if err != nil {
		return errors.Errorf("failed to list pods, %v", err)
	}

	client, err := k8sclient.New(req.ClusterName)
	if err != nil {
		return errors.Errorf("failed to get k8s client, %v", err)
	}

	go func() {
		for _, obj := range list {
			pod := obj.Data()
			if pod.String("spec", "nodeName") != req.Name {
				continue
			}
			namespace := pod.String("metadata", "namespace")
			name := pod.String("metadata", "name")
			if err = client.ClientSet.PolicyV1beta1().Evictions(namespace).Evict(context.Background(), &v1beta1.Eviction{
				ObjectMeta: metav1.ObjectMeta{
					Name:      name,
					Namespace: namespace,
				},
			}); err != nil {
				logrus.Errorf("failed to evict pod %s:%s, %v", namespace, name, err)
				continue
			} else {
				logrus.Infof("drain node %s: pod %s:%s evicted", req.Name, namespace, name)
			}
		}
	}()

	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceName: req.Name,
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditDrainNode, auditCtx); err != nil {
		logrus.Errorf("failed to audit when drain node, %v", err)
	}
	return nil
}

// OfflineNode offlines a node by sending request to monitor. And creates an audit event.
// nodeID format: nodeName/hostIP
func (a *Aggregator) OfflineNode(ctx context.Context, userID, orgID, clusterName string, nodeIDs []string) error {
	var names, ips []string
	for _, id := range nodeIDs {
		splits := strings.Split(id, "/")
		if len(splits) != 2 {
			logrus.Errorf("failed to offline host, invalid id %s", id)
			continue
		}
		name, ip := splits[0], splits[1]
		names = append(names, name)
		ips = append(ips, ip)

		req := &apistructs.SteveRequest{
			UserID:      userID,
			OrgID:       orgID,
			Type:        apistructs.K8SNode,
			ClusterName: clusterName,
			Name:        name,
		}
		if err := a.labelNode(ctx, req, map[string]string{OfflineLabel: "true"}); err != nil {
			logrus.Errorf("failed to label node %s, %v", name, err)
			continue
		}
	}

	go func() {
		for i := 0; i < 5; i++ {
			host := discover.Monitor()
			path := "/api/resources/hosts/actions/offline"
			headers := http.Header{
				httputil.UserHeader: []string{userID},
				httputil.OrgHeader:  []string{orgID},
			}
			client := httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
			req := map[string]interface{}{
				"clusterName": clusterName,
				"hostIPs":     ips,
			}

			var resp httpapi.Response
			_, err := client.Post(host).Path(path).Headers(headers).JSONBody(&req).Do().JSON(&resp)
			if err != nil {
				logrus.Errorf("failed to post offline host, %v", err)
			}
		}
	}()

	nodeNames := strings.Join(names, ",")
	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  clusterName,
		middleware.AuditResourceName: nodeNames,
	}
	if err := a.Audit(userID, orgID, middleware.AuditOfflineNode, auditCtx); err != nil {
		logrus.Errorf("failed to audit when offline node, %v", err)
	}
	return nil
}

// OnlineNode onlines a node by removing node offline label. And creates an audit event.
func (a *Aggregator) OnlineNode(ctx context.Context, req *apistructs.SteveRequest) error {
	if err := a.unlabelNode(ctx, req, []string{OfflineLabel}); err != nil {
		return err
	}
	auditCtx := map[string]interface{}{
		middleware.AuditClusterName:  req.ClusterName,
		middleware.AuditResourceName: req.Name,
	}
	if err := a.Audit(req.UserID, req.OrgID, middleware.AuditOnlineNode, auditCtx); err != nil {
		logrus.Errorf("failed to audit when online node, %v", err)
	}
	return nil
}

// Auth authenticates by userID and orgID.
func (a *Aggregator) Auth(userID, orgID, clusterName string) (apiuser.Info, error) {
	if a == nil {
		return nil, errors.New("steve server is not initialized yet")
	}
	scopeID, err := strconv.ParseUint(orgID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrInvoke.InvalidParameter(fmt.Sprintf("invalid org id %s, %v", orgID, err))
	}

	clusters, err := a.listCluster(scopeID, "k8s", "edas")
	if err != nil {
		return nil, err
	}

	found := false
	for _, cluster := range clusters {
		if cluster.Name == clusterName {
			found = true
			break
		}
	}
	if !found {
		return nil, apierrors.ErrInvoke.InvalidParameter(fmt.Sprintf("cluster %s not found in org %s", clusterName, orgID))
	}

	r := &apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: apistructs.OrgScope,
			ID:   orgID,
		},
	}
	rsp, err := a.Bdl.ScopeRoleAccess(userID, r)
	if err != nil {
		return nil, err
	}
	if !rsp.Access {
		return nil, apierrors.ErrInvoke.AccessDenied()
	}

	name := fmt.Sprintf("erda-user-%s", userID)
	user := &apiuser.DefaultInfo{
		Name: name,
		UID:  name,
	}
	for _, role := range rsp.Roles {
		group, ok := predefined.RoleToGroup[role]
		if !ok {
			continue
		}
		user.Groups = append(user.Groups, group)
	}

	if len(user.Groups) == 0 {
		return nil, apierrors.ErrInvoke.AccessDenied()
	}

	return user, nil
}

// Audit creates an audit event by bundle.
func (a *Aggregator) Audit(userID, orgID, templateName string, ctx map[string]interface{}) error {
	if ctx == nil || len(ctx) == 0 {
		return apierrors.ErrInvoke.InvalidParameter(errors.New("template context can not be empty"))
	}

	scopeID, err := strconv.ParseUint(orgID, 10, 64)
	if err != nil {
		return apierrors.ErrInvoke.InvalidParameter(fmt.Errorf("invalid org id %s, %v", orgID, err))
	}

	now := strconv.FormatInt(time.Now().Unix(), 10)
	auditReq := apistructs.AuditCreateRequest{
		Audit: apistructs.Audit{
			UserID:       userID,
			ScopeType:    apistructs.OrgScope,
			ScopeID:      scopeID,
			OrgID:        scopeID,
			Context:      ctx,
			TemplateName: apistructs.TemplateName(templateName),
			Result:       "success",
			StartTime:    now,
			EndTime:      now,
		},
	}
	return a.Bdl.CreateAuditEvent(&auditReq)
}

func (a *Aggregator) listCluster(orgID uint64, types ...string) ([]*clusterpb.ClusterInfo, error) {
	var result []*clusterpb.ClusterInfo
	ctx := transport.WithHeader(a.Ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	for _, typ := range types {
		resp, err := a.clusterSvc.ListCluster(ctx, &clusterpb.ListClusterRequest{ClusterType: typ})
		if err != nil {
			return nil, err
		}
		result = append(result, resp.Data...)
	}
	return result, nil
}

func RemoveCache(clusterName, namespace, kind string) {
	key := CacheKey{
		Kind:        kind,
		Namespace:   namespace,
		ClusterName: clusterName,
	}
	if _, err := cache.GetFreeCache().Remove(key.GetKey()); err != nil {
		logrus.Errorf("failed to remove cache for %s (namespace: %s) in cluster %s, %v",
			key.Kind, key.Namespace, key.ClusterName, err)
	}
}

func getAPIError(err data.Object) *errorresp.APIError {
	status, ok := err["status"].(int)
	if !ok {
		status = 500
	}
	code := err.String("code")
	msg := err.String("message")
	return errorresp.New(errorresp.WithCode(status, code), errorresp.WithMessage(fmt.Sprintf("%s: %s", code, msg)))
}
