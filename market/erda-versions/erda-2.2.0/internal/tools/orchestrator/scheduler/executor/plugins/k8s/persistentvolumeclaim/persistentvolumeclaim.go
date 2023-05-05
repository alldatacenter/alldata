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

// Package persistentvolumeclaim manipulates the k8s api of persistentvolumeclaim object
package persistentvolumeclaim

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/pkg/errors"
	apiv1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// PersistentVolumeClaim is the object to manipulate k8s api of persistentVolumeClaim
type PersistentVolumeClaim struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures a PersistentVolumeClaim
type Option func(*PersistentVolumeClaim)

// New news a PersistentVolumeClaim
func New(options ...Option) *PersistentVolumeClaim {
	pvc := &PersistentVolumeClaim{}

	for _, op := range options {
		op(pvc)
	}

	return pvc
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(n *PersistentVolumeClaim) {
		n.addr = addr
		n.client = client
	}
}

// Create creates a k8s persistentVolumeClaim
func (p *PersistentVolumeClaim) Create(pvc *apiv1.PersistentVolumeClaim) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", pvc.Namespace, "/persistentvolumeclaims")

	resp, err := p.client.Post(p.addr).
		Path(path).
		JSONBody(pvc).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create pvc, name: %s, (%v)", pvc.Name, err)
	}
	if !resp.IsOK() {
		return errors.Errorf("failed to create pvc, name: %s, statuscode: %v, body: %v",
			pvc.Name, resp.StatusCode(), b.String())
	}
	return nil
}
func (p *PersistentVolumeClaim) CreateIfNotExists(pvc *apiv1.PersistentVolumeClaim) error {
	var getb bytes.Buffer
	getpath := fmt.Sprintf("/api/v1/namespaces/%s/persistentvolumeclaims/%s", pvc.Namespace, pvc.Name)

	resp, err := p.client.Get(p.addr).
		Path(getpath).
		Do().
		Body(&getb)
	if err != nil {
		return errors.Errorf("failed to get pvc, name: %s, (%v)", pvc.Name, err)
	}
	if resp.IsOK() {
		existedPVC := &apiv1.PersistentVolumeClaim{}
		if err := json.NewDecoder(&getb).Decode(existedPVC); err != nil {
			return err
		}
		if *existedPVC.Spec.StorageClassName != *pvc.Spec.StorageClassName {
			return errors.Errorf("pvc %s/%s existed, but storageclass %s is not as expected %s ", pvc.Namespace, pvc.Name, *existedPVC.Spec.StorageClassName, *pvc.Spec.StorageClassName)
		}
		return nil
	}
	if !resp.IsNotfound() {
		return errors.Errorf("failed to get pvc, name: %s, statuscode: %v, body: %v",
			pvc.Name, resp.StatusCode(), getb.String())
	}
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", pvc.Namespace, "/persistentvolumeclaims")

	resp, err = p.client.Post(p.addr).
		Path(path).
		JSONBody(pvc).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create pvc, name: %s, (%v)", pvc.Name, err)
	}
	if !resp.IsOK() {
		return errors.Errorf("failed to create pvc, name: %s, statuscode: %v, body: %v",
			pvc.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Delete deletes a k8s persistentVolumeClaim
func (p *PersistentVolumeClaim) Delete(namespace, name string) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/persistentvolumeclaims/", name)

	resp, err := p.client.Delete(p.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete pvc, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}

		return errors.Errorf("failed to delete pvc, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	return nil
}

// Get get a k8s persistentVolumeClaim
func (p *PersistentVolumeClaim) Get(namespace, name string) (*apiv1.PersistentVolumeClaim, error) {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/namespaces/", namespace, "/persistentvolumeclaims/", name)

	resp, err := p.client.Get(p.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get pvc info, namespace: %s name: %s", namespace, name)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get pvc info, namespace: %s name: %s, statuscode: %v, body: %v", namespace, name, resp.StatusCode(), b.String())
	}

	pvc := &apiv1.PersistentVolumeClaim{}
	if err := json.NewDecoder(&b).Decode(pvc); err != nil {
		return nil, err
	}
	return pvc, nil
}
