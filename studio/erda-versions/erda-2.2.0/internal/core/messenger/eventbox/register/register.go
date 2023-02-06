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

package register

import (
	"context"
	"path/filepath"
	"strings"
	"sync"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/constant"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/register/label"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/pkg/jsonstore"
)

type Register interface {
	PrefixGet(key string) map[types.LabelKey]map[types.LabelKey]interface{}
	Put(key string, labels map[types.LabelKey]interface{}) error
	Del(key string) error
}

type registerImpl struct {
	js     jsonstore.JsonStore
	stopch chan struct{}
	stopwg sync.WaitGroup
}

func New() (*registerImpl, error) {
	opt := jsonstore.UseMemEtcdStore(context.Background(), constant.RegisterDir, nil, nil)
	js, err := jsonstore.New(opt)
	if err != nil {
		return nil, err
	}
	r := &registerImpl{
		js:     js,
		stopch: make(chan struct{}),
	}
	// put default labels
	for registerLabel, v := range label.DefaultLabels {
		if err := r.Put(string(registerLabel), v); err != nil {
			logrus.Errorf("Register: %v", err)
			return nil, err
		}
	}
	return r, nil
}

// return: map[registered-label]map[converted-label]<label-value>
func (r *registerImpl) PrefixGet(key string) map[types.LabelKey]map[types.LabelKey]interface{} {
	normalizedKey := types.LabelKey(key).Normalize()
	path := filepath.Join(constant.RegisterDir, normalizedKey)
	res := map[types.LabelKey]map[types.LabelKey]interface{}{}
	if err := r.js.ForEach(context.Background(), path, map[types.LabelKey]interface{}{},
		func(k string, v_ interface{}) error {
			v := v_.(*map[types.LabelKey]interface{})
			stripped := strings.TrimPrefix(k, constant.RegisterDir)
			res[types.LabelKey(stripped).NormalizeLabelKey()] = *v
			return nil
		}); err != nil {
		return nil
	}
	return res
}

func (r *registerImpl) Put(key string, labels map[types.LabelKey]interface{}) error {
	normalizedKey := types.LabelKey(key).Normalize()
	normalizedLabels := map[types.LabelKey]interface{}{}
	for k, l := range labels {
		normalizedLabels[k.NormalizeLabelKey()] = l
	}

	path := filepath.Join(constant.RegisterDir, normalizedKey)
	if err := r.js.Put(context.Background(), path, normalizedLabels); err != nil {
		return err
	}
	return nil
}

func (r *registerImpl) Del(key string) error {
	normalizedKey := types.LabelKey(key).Normalize()
	path := filepath.Join(constant.RegisterDir, normalizedKey)
	labels := make(map[types.LabelKey]interface{})
	if err := r.js.Remove(context.Background(), path, &labels); err != nil {
		return err
	}
	return nil
}
