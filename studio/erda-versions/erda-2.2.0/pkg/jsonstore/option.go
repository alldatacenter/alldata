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

package jsonstore

import (
	"context"
	"encoding/json"
	"reflect"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/jsonstore/cacheetcd"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/jsonstore/lru"
	"github.com/erda-project/erda/pkg/jsonstore/mem"
	"github.com/erda-project/erda/pkg/jsonstore/memetcd"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
	"github.com/erda-project/erda/pkg/jsonstore/timeout"
)

type BackendType = int

const (
	EtcdStore BackendType = iota
	MemStore
	MemEtcdStore
	CacheEtcdStore
)

var defaultOption Option = Option{backend: EtcdStore, isLru: false}

type OptionOperator func(*Option)

type Option struct {
	backend BackendType
	// lru
	isLru  bool
	lruCap int

	// timeout-store
	isTimeout bool
	timeout   int

	// memetcd & cacheetcd
	ctx      context.Context
	etcdDir  string
	callback func(k string, obj interface{}, t storetypes.ChangeType)
	cbobj    interface{} // callback's value type

	// cacheetcd
	cacheetcdCap int
}

func (op *Option) Apply(opts []OptionOperator) {
	for _, opt := range opts {
		opt(op)
	}
}

func (op *Option) GetBackend() (storetypes.Store, error) {
	var backend storetypes.Store
	var err error
	switch op.backend {
	case EtcdStore:
		backend, err = etcd.New()
		if err != nil {
			return backend, err
		}
	case MemStore:
		backend, err = mem.New()
		if err != nil {
			return backend, err
		}
	case MemEtcdStore:
		var cb func(k, v string, t storetypes.ChangeType) = nil
		if op.callback != nil {
			objTp := reflect.TypeOf(op.cbobj)
			cb = func(k, v string, t storetypes.ChangeType) {
				v_ := reflect.New(objTp).Interface()
				if err := json.Unmarshal([]byte(v), v_); err != nil {
					logrus.Errorf("MemEtcdStore: unmarshal key(%v) failed: %v: %v", k, err, v)
					// skip
				}
				op.callback(k, v_, t)
			}
		}
		backend, err = memetcd.New(op.ctx, op.etcdDir, cb)
		if err != nil {
			return backend, err
		}
	case CacheEtcdStore:
		backend, err = cacheetcd.New(op.ctx, op.etcdDir, op.cacheetcdCap)
		if err != nil {
			return backend, err
		}
	}
	if op.isLru {
		backend, err = lru.New(op.lruCap, backend)
	}
	if op.isTimeout {
		backend, err = timeout.New(op.timeout, backend)
	}
	return backend, err
}

func UseEtcdStore() OptionOperator {
	return func(op *Option) {
		op.backend = EtcdStore
	}
}

func UseMemStore() OptionOperator {
	return func(op *Option) {
		op.backend = MemStore
	}
}

func UseLruStore(cap int) OptionOperator {
	return func(op *Option) {
		op.isLru = true
		op.lruCap = cap
	}
}

func UseMemEtcdStore(ctx context.Context, etcdDir string, cb func(k string, v interface{}, t storetypes.ChangeType), cbobj interface{}) OptionOperator {
	return func(op *Option) {
		op.backend = MemEtcdStore
		op.ctx = ctx
		op.etcdDir = etcdDir
		op.callback = cb
		op.cbobj = cbobj
	}
}

func UseCacheEtcdStore(ctx context.Context, etcdDir string, cap int) OptionOperator {
	return func(op *Option) {
		op.backend = CacheEtcdStore
		op.ctx = ctx
		op.etcdDir = etcdDir
		op.cacheetcdCap = cap
	}
}

// timeout: second
func UseTimeoutStore(timeout int) OptionOperator {
	return func(op *Option) {
		op.isTimeout = true
		op.timeout = timeout
	}
}
