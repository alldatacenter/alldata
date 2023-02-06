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

// Package secret manipulates the k8s api of secret object
package secret

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"
	apiv1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// Secret is the object to encapsulate docker
type Secret struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an Secret
type Option func(*Secret)

// New news an Secret
func New(options ...Option) *Secret {
	ns := &Secret{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(s *Secret) {
		s.addr = addr
		s.client = client
	}
}

// List list k8s secrets in 'namespace'
func (p *Secret) List(namespace string) (*apiv1.SecretList, error) {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/secrets")
	resp, err := p.client.Get(p.addr).
		Path(path).
		Do().
		Body(&b)
	if err != nil {
		return nil, errors.Errorf("failed to list secrets, namespace: %s, err: %v", namespace, err)
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("failed to list secrets, namespace: %s, statuscode: %d, body: %v",
			namespace, resp.StatusCode(), b.String())
	}
	secrets := &apiv1.SecretList{}
	if err := json.NewDecoder(&b).Decode(secrets); err != nil {
		return nil, err
	}
	return secrets, nil
}

// Get gets a k8s secret
func (p *Secret) Get(namespace, name string) (*apiv1.Secret, error) {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/secrets/", name)

	resp, err := p.client.Get(p.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get secret, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, errors.Errorf("not found")
		}
		return nil, errors.Errorf("failed to get secret, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	secret := &apiv1.Secret{}
	if err := json.NewDecoder(&b).Decode(secret); err != nil {
		return nil, err
	}
	return secret, nil
}

// Create creates a k8s ingress object
func (p *Secret) Create(secret *apiv1.Secret) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", secret.Namespace, "/secrets")

	resp, err := p.client.Post(p.addr).
		Path(path).
		JSONBody(secret).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create secret, name: %s, (%v)", secret.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to create secret, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

// Update creates a k8s ingress object
func (p *Secret) Update(secret *apiv1.Secret) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", secret.Namespace, "/secrets/", secret.Name)

	resp, err := p.client.Put(p.addr).
		Path(path).
		JSONBody(secret).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to update secret, name: %s, (%v)", secret.Name, err)
	}

	if !resp.IsOK() {
		return errors.Errorf("failed to update secret, statuscode: %v, body: %v", resp.StatusCode(), b.String())
	}
	return nil
}

func (p *Secret) CreateIfNotExist(secret *apiv1.Secret) error {
	_, err := p.Get(secret.Namespace, secret.Name)
	if err == nil {
		return nil
	}
	if err.Error() == "not found" {
		return p.Create(secret)
	}
	return err
}
