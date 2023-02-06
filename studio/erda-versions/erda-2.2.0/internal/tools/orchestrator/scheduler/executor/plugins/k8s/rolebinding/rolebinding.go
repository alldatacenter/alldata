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

// Package rolebinding manipulates the k8s api of rolebinding object
package rolebinding

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"
	rbacv1 "k8s.io/api/rbac/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	defaultAPIPrefix = "/apis/rbac.authorization.k8s.io/v1/namespaces/"
)

// RoleBinding is the object to encapsulate secrets
type RoleBinding struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an RoleBinding
type Option func(*RoleBinding)

// New news an RoleBinding
func New(options ...Option) *RoleBinding {
	ns := &RoleBinding{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(r *RoleBinding) {
		r.addr = addr
		r.client = client
	}
}

// Create create a k8s rolebinding
func (r *RoleBinding) Create(rb *rbacv1.RoleBinding) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, rb.Namespace, "/rolebindings")

	resp, err := r.client.Post(r.addr).
		Path(path).
		JSONBody(rb).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create rolebinding, namespace: %s, name: %s, (%v)", rb.Namespace, rb.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create rolebinding, namespace: %s, name: %s, statuscode: %v, body: %v",
			rb.Namespace, rb.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Get gets a k8s rolebindling
func (r *RoleBinding) Get(namespace, name string) (*rbacv1.RoleBinding, error) {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, namespace, "/rolebindings/", name)

	resp, err := r.client.Get(r.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get rolebinding, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get rolebinding, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	rb := &rbacv1.RoleBinding{}
	if err := json.NewDecoder(&b).Decode(rb); err != nil {
		return nil, err
	}
	return rb, nil
}

// Update updates a k8s rolebinding
func (r *RoleBinding) Update(rb *rbacv1.RoleBinding) error {
	var b bytes.Buffer

	oriObj, err := r.Get(rb.Namespace, rb.Name)
	if err == nil {
		rb.ObjectMeta.ResourceVersion = oriObj.ObjectMeta.ResourceVersion
	}

	path := strutil.Concat(defaultAPIPrefix, rb.Namespace, "/rolebindings/", rb.Name)
	resp, err := r.client.Put(r.addr).
		Path(path).
		JSONBody(rb).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to put k8s role, name: %s, (%v)", rb.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to put k8s role, name: %s, statuscode: %v, body: %v",
			rb.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Patch patches a k8s rolebinding object
func (r *RoleBinding) Patch(rb *rbacv1.RoleBinding) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, rb.Namespace, "/rolebindings/", rb.Name)

	resp, err := r.client.Patch(r.addr).
		Path(path).
		JSONBody(rb).
		Header("Accept", "application/json").
		Header("Content-Type", "application/strategic-merge-patch+json").
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to patch rolebinding, name: %s, (%v)", rb.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to patch rolebinding, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

// Delete delete a k8s rolebinding
func (r *RoleBinding) Delete(namespace, name string) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, namespace, "/rolebindings/", name)

	resp, err := r.client.Delete(r.addr).
		Path(path).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete k8s rolebinding, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}
		return errors.Errorf("failed to delete k8s rolebinding, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}

	return nil
}

// Exists decides whether a rolebinding exists
func (r *RoleBinding) Exists(namespace, name string) error {
	path := strutil.Concat(defaultAPIPrefix, namespace, "/rolebindings/", name)
	resp, err := r.client.Get(r.addr).
		Path(path).
		Do().
		DiscardBody()

	if err != nil {
		return err
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}
		return errors.Errorf("failed to get rolebinding, ns: %s, name: %s, statuscode: %v",
			namespace, name, resp.StatusCode())
	}

	return nil
}

// DeleteIfExists delete if k8s rolebinding exists
func (r *RoleBinding) DeleteIfExists(namespace, name string) error {
	var getErr error

	_, getErr = r.Get(namespace, name)
	if getErr == k8serror.ErrNotFound {
		return nil
	}
	return r.Delete(namespace, name)
}
