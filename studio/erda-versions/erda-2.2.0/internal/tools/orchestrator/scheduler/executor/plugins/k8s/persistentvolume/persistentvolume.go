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

// Package persistentvolume manipulates the k8s api of persistentvolume object
package persistentvolume

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"
	apiv1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// PersistentVolume is the object to manipulate k8s api of persistentVolumeClaim
type PersistentVolume struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures a PersistentVolume
type Option func(*PersistentVolume)

// New news a PersistentVolumeClaim
func New(options ...Option) *PersistentVolume {
	pv := &PersistentVolume{}

	for _, op := range options {
		op(pv)
	}

	return pv
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(pv *PersistentVolume) {
		pv.addr = addr
		pv.client = client
	}
}

// Create creates a k8s persistentVolume
func (p *PersistentVolume) Create(pv *apiv1.PersistentVolume) error {
	var b bytes.Buffer

	resp, err := p.client.Post(p.addr).
		Path("/api/v1/persistentvolumes").
		JSONBody(pv).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create pv, name: %s, (%v)", pv.Name, err)
	}
	if !resp.IsOK() {
		return errors.Errorf("failed to create pv, name: %s, statuscode: %v, body: %v",
			pv.Name, resp.StatusCode(), b.String())
	}
	return nil
}

// Delete deletes a k8s persistentVolume
func (p *PersistentVolume) Delete(name string) error {
	var b bytes.Buffer
	path := strutil.Concat("/api/v1/persistentvolumes/", name)

	resp, err := p.client.Delete(p.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete pv, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}

		return errors.Errorf("failed to delete pv, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	return nil
}

// List lists a k8s persistentVolumes
func (p *PersistentVolume) List(name string) (apiv1.PersistentVolumeList, error) {
	var b bytes.Buffer
	var list apiv1.PersistentVolumeList
	path := "/api/v1/persistentvolumes/"

	resp, err := p.client.Get(p.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return list, errors.Errorf("failed to list related pv, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return list, k8serror.ErrNotFound
		}

		return list, errors.Errorf("failed to list related pv, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	if err := json.Unmarshal(b.Bytes(), &list); err != nil {
		return list, err
	}
	return list, nil
}
