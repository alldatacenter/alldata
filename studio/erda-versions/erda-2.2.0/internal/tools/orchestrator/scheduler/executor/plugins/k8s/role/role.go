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

// Package role manipulates the k8s api of role object
package role

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

// Role is the object to encapsulate secrets
type Role struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an Role
type Option func(*Role)

// New news an Role
func New(options ...Option) *Role {
	ns := &Role{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(r *Role) {
		r.addr = addr
		r.client = client
	}
}

// Create create a k8s role
func (r *Role) Create(role *rbacv1.Role) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, role.Namespace, "/roles")

	resp, err := r.client.Post(r.addr).
		Path(path).
		JSONBody(role).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create roles, namespace: %s, name: %s, (%v)", role.Namespace, role.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create roles, namespace: %s, name: %s, statuscode: %v, body: %v",
			role.Namespace, role.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Get gets a k8s role
func (r *Role) Get(namespace, name string) (*rbacv1.Role, error) {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, namespace, "/roles/", name)

	resp, err := r.client.Get(r.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get roles, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get roles, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	role := &rbacv1.Role{}
	if err := json.NewDecoder(&b).Decode(role); err != nil {
		return nil, err
	}
	return role, nil
}

// Update updates a k8s role
func (r *Role) Update(role *rbacv1.Role) error {
	var b bytes.Buffer

	oriObj, err := r.Get(role.Namespace, role.Name)
	if err == nil {
		role.ObjectMeta.ResourceVersion = oriObj.ObjectMeta.ResourceVersion
	}

	path := strutil.Concat(defaultAPIPrefix, role.Namespace, "/roles/", role.Name)
	resp, err := r.client.Put(r.addr).
		Path(path).
		JSONBody(role).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to put k8s role, name: %s, (%v)", role.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to put spark application, name: %s, statuscode: %v, body: %v",
			role.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Patch patches a k8s role object
func (r *Role) Patch(role *rbacv1.Role) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, role.Namespace, "/roles/", role.Name)

	resp, err := r.client.Patch(r.addr).
		Path(path).
		JSONBody(role).
		Header("Accept", "application/json").
		Header("Content-Type", "application/strategic-merge-patch+json").
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to patch role, name: %s, (%v)", role.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to Patch role, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

// Delete delete a k8s role
func (r *Role) Delete(namespace, name string) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, namespace, "/roles/", name)

	resp, err := r.client.Delete(r.addr).
		Path(path).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete k8s role, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}
		return errors.Errorf("failed to delete k8s role, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}

	return nil
}

// Exists decides whether a role exists
func (r *Role) Exists(namespace, name string) error {
	path := strutil.Concat(defaultAPIPrefix, namespace, "/roles/", name)
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
		return errors.Errorf("failed to get role, ns: %s, name: %s, statuscode: %v",
			namespace, name, resp.StatusCode())
	}

	return nil
}

// DeleteIfExists delete if k8s role exists
func (r *Role) DeleteIfExists(namespace, name string) error {
	var getErr error

	_, getErr = r.Get(namespace, name)
	if getErr == k8serror.ErrNotFound {
		return nil
	}
	return r.Delete(namespace, name)
}
