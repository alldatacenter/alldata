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

// Package storageclass manipulates the k8s api of storageclass object

package storageclass

import (
	"bytes"
	"encoding/json"

	"github.com/pkg/errors"
	storagev1 "k8s.io/api/storage/v1"

	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

// StorageClass is the object to encapsulate docker
type StorageClass struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures an Secret
type Option func(*StorageClass)

// New news an Secret
func New(options ...Option) *StorageClass {
	sc := &StorageClass{}

	for _, op := range options {
		op(sc)
	}

	return sc
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(s *StorageClass) {
		s.addr = addr
		s.client = client
	}
}

// Get get k8s storasge with name 'name'
func (p *StorageClass) Get(name string) (*storagev1.StorageClass, error) {
	var b bytes.Buffer
	path := strutil.Concat("/apis/storage.k8s.io/v1/storageclasses/", name)

	resp, err := p.client.Get(p.addr).
		Path(path).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get storageclass, name: %s, (%v)", name, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, errors.Errorf("not found")
		}
		return nil, errors.Errorf("failed to get stoageclass, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	storageClass := &storagev1.StorageClass{}
	if err := json.NewDecoder(&b).Decode(storageClass); err != nil {
		return nil, err
	}
	return storageClass, nil
}
