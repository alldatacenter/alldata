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

// Package configmap manipulates the k8s api of configmap object
package configmap

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	defaultAPIPrefix = "/api/v1/namespaces/"
)

// ConfigMap is the object to encapsulate ConfigMap
type ConfigMap struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an ConfigMap
type Option func(*ConfigMap)

// New news an ConfigMap
func New(options ...Option) *ConfigMap {
	cm := &ConfigMap{}

	for _, op := range options {
		op(cm)
	}

	return cm
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(r *ConfigMap) {
		r.addr = addr
		r.client = client
	}
}

// Create create a k8s ConfigMap
func (c *ConfigMap) Create(cm *corev1.ConfigMap) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, cm.Namespace, "/configmaps")

	resp, err := c.client.Post(c.addr).
		Path(path).
		JSONBody(cm).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create configmap, namespace: %s, name: %s, (%v)", cm.Namespace, cm.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create configmap, namespace: %s, name: %s, statuscode: %v, body: %v",
			cm.Namespace, cm.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Get gets a k8s configmap
func (c *ConfigMap) Get(namespace, name string) (*corev1.ConfigMap, error) {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, namespace, "/configmaps/", name)

	resp, err := c.client.Get(c.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get configmaps, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get configmaps, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	cm := &corev1.ConfigMap{}
	if err := json.NewDecoder(&b).Decode(cm); err != nil {
		return nil, err
	}
	return cm, nil
}

// Update updates a k8s configmaps
func (c *ConfigMap) Update(cm *corev1.ConfigMap) error {
	var b bytes.Buffer

	oriObj, err := c.Get(cm.Namespace, cm.Name)
	if err == nil {
		cm.ObjectMeta.ResourceVersion = oriObj.ObjectMeta.ResourceVersion
	}

	path := strutil.Concat(defaultAPIPrefix, cm.Namespace, "/configmaps/", cm.Name)
	resp, err := c.client.Put(c.addr).
		Path(path).
		JSONBody(cm).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to put k8s configmap, name: %s, (%v)", cm.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to put k8s configmap, name: %s, statuscode: %v, body: %v",
			cm.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Patch patches a k8s configmap object
func (c *ConfigMap) Patch(cm *corev1.ConfigMap) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, cm.Namespace, "/configmaps/", cm.Name)

	resp, err := c.client.Patch(c.addr).
		Path(path).
		JSONBody(cm).
		Header("Accept", "application/json").
		Header("Content-Type", "application/strategic-merge-patch+json").
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to patch configmap, name: %s, (%v)", cm.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to patch configmap, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

// Delete delete a k8s configmap
func (c *ConfigMap) Delete(namespace, name string) error {
	var b bytes.Buffer
	path := strutil.Concat(defaultAPIPrefix, namespace, "/configmaps/", name)

	resp, err := c.client.Delete(c.addr).
		Path(path).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete k8s configmap, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}
		return errors.Errorf("failed to delete k8s configmap, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}

	return nil
}

// Exists decides whether a configmap exists
func (c *ConfigMap) Exists(namespace, name string) error {
	path := strutil.Concat(defaultAPIPrefix, namespace, "/configmaps/", name)
	resp, err := c.client.Get(c.addr).
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
		return errors.Errorf("failed to get configmap, ns: %s, name: %s, statuscode: %v",
			namespace, name, resp.StatusCode())
	}

	return nil
}

// DeleteIfExists delete if k8s configmap exists
func (c *ConfigMap) DeleteIfExists(namespace, name string) error {
	var getErr error

	_, getErr = c.Get(namespace, name)
	if getErr == k8serror.ErrNotFound {
		return nil
	}
	return c.Delete(namespace, name)
}
