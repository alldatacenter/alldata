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

// Package ingress manipulates the k8s api of ingress object
package ingress

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	extensionsv1beta1 "k8s.io/api/extensions/v1beta1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// Ingress is the object to manipulate k8s api of ingress
type Ingress struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an Ingress
type Option func(*Ingress)

// New news an Ingress
func New(options ...Option) *Ingress {
	ns := &Ingress{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(i *Ingress) {
		i.addr = addr
		i.client = client
	}
}

// Create creates a k8s ingress object
func (n *Ingress) Create(ing *extensionsv1beta1.Ingress) error {
	var b bytes.Buffer
	resp, err := n.client.Post(n.addr).
		Path("/apis/extensions/v1beta1/namespaces/" + ing.Namespace + "/ingresses").
		JSONBody(ing).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create ingress, name: %s, (%v)", ing.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create ingress, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

// Delete deletes a k8s ingress object
func (n *Ingress) Delete(namespace, name string) error {
	var b bytes.Buffer
	resp, err := n.client.Delete(n.addr).
		Path("/apis/extensions/v1beta1/namespaces/" + namespace + "/ingresses/" + name).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete ingress, name: %s, (%v)", name, err)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			// The ingress does not exist, the deletion is considered successful
			logrus.Debugf("ingress not found, name: %s", name)
			return nil
		}
		return errors.Errorf("failed to delete ingress, name: %s, statuscode: %v, body: %v", name, resp.StatusCode(), b.String())
	}
	return nil
}

// Update update a k8s ingress object
func (n *Ingress) Update(ing *extensionsv1beta1.Ingress) error {
	var b bytes.Buffer
	resp, err := n.client.Put(n.addr).
		Path("/apis/extensions/v1beta1/namespaces/" + ing.Namespace + "/ingresses/" + ing.Name).
		JSONBody(ing).
		Do().
		Body(&b)
	if err != nil {
		return errors.Errorf("failed to update ingress, name: %v, err: %v", ing.Name, err)
	}
	if !resp.IsOK() {
		return errors.Errorf("failed to update ingress, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

// Get get a k8s ingress object
func (n *Ingress) Get(namespace, name string) (*extensionsv1beta1.Ingress, error) {
	var (
		b       bytes.Buffer
		ingress extensionsv1beta1.Ingress
	)

	resp, err := n.client.Get(n.addr).
		Path("/apis/extensions/v1beta1/namespaces/" + namespace + "/ingresses/" + name).
		Do().
		Body(&b)
	if err != nil {
		return nil, errors.Errorf("failed to get ingress, namespace: %s, name: %v, err: %v", namespace, name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			logrus.Debugf("ingress not found, namespace: %s, name: %s", namespace, name)
			return nil, k8serror.ErrNotFound
		}

		return nil, errors.Errorf("failed to get ingress, namespace: %s, name: %s, statuscode: %v, body: %v",
			namespace, name, resp.StatusCode(), b.String())
	}

	if err := json.NewDecoder(&b).Decode(&ingress); err != nil {
		return nil, err
	}

	return &ingress, nil
}

// CreateOrUpdate create or update a k8s ingress object
func (n *Ingress) CreateOrUpdate(ing *extensionsv1beta1.Ingress) error {
	var getErr error

	_, getErr = n.Get(ing.Namespace, ing.Name)
	if getErr == k8serror.ErrNotFound {
		return n.Create(ing)
	}
	return n.Update(ing)
}

// DeleteIfExists delete if k8s ingress exists
func (n *Ingress) DeleteIfExists(namespace, name string) error {
	var getErr error

	_, getErr = n.Get(namespace, name)
	if getErr == k8serror.ErrNotFound {
		return nil
	}
	return n.Delete(namespace, name)
}
