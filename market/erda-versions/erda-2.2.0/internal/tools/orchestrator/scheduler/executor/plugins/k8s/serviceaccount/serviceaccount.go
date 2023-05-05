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

// Package serviceaccount manipulates the k8s api of serviceaccount object
package serviceaccount

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"
	apiv1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// ServiceAccount is the object to encapsulate secrets
type ServiceAccount struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an ServiceAccount
type Option func(*ServiceAccount)

// New news an ServiceAccount
func New(options ...Option) *ServiceAccount {
	ns := &ServiceAccount{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(s *ServiceAccount) {
		s.addr = addr
		s.client = client
	}
}

// Create create a k8s serviceaccount
func (s *ServiceAccount) Create(sa *apiv1.ServiceAccount) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", sa.Namespace, "/serviceaccounts")

	resp, err := s.client.Post(s.addr).
		Path(path).
		JSONBody(sa).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create serviceaccounts, namespace: %s, name: %s, (%v)", sa.Namespace, sa.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create serviceaccounts, namespace: %s, name: %s, statuscode: %v, body: %v",
			sa.Namespace, sa.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Get gets a k8s serviceaccount
func (s *ServiceAccount) Get(namespace, name string) (*apiv1.ServiceAccount, error) {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/serviceaccounts/", name)

	resp, err := s.client.Get(s.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get serviceaccounts, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get serviceaccounts, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	sa := &apiv1.ServiceAccount{}
	if err := json.NewDecoder(&b).Decode(sa); err != nil {
		return nil, err
	}
	return sa, nil
}

// Patch patches a k8s serviceaccount object
func (s *ServiceAccount) Patch(sa *apiv1.ServiceAccount) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", sa.Namespace, "/serviceaccounts/", sa.Name)

	resp, err := s.client.Patch(s.addr).
		Path(path).
		JSONBody(sa).
		Header("Accept", "application/json").
		Header("Content-Type", "application/strategic-merge-patch+json").
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to Patch serviceaccount, name: %s, (%v)", sa.Name, err)
	}

	if !resp.IsOK() {
		if resp.StatusCode() == 409 && strutil.Contains(b.String(), "Conflict") {
			return errors.New("Conflict")
		}
		return errors.Errorf("failed to Patch serviceaccount, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

// Delete deletes a k8s serviceaccount object
func (s *ServiceAccount) Delete(namespace, name string) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/serviceaccounts/", name)

	resp, err := s.client.Delete(s.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete serviceaccounts, namespace: %s, name: %s, (%v)", namespace, name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil
		}
		return errors.Errorf("failed to delete serviceaccounts, namespace: %s, name: %s, statuscode: %v, body: %v",
			namespace, name, resp.StatusCode(), b.String())
	}

	return nil
}

// Exists decides whether a serviceaccount exists
func (s *ServiceAccount) Exists(namespace, name string) error {
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/serviceaccounts/", name)
	resp, err := s.client.Get(s.addr).
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
		return errors.Errorf("failed to get serviceaccount, ns: %s, name: %s, statuscode: %v",
			namespace, name, resp.StatusCode())
	}

	return nil
}
