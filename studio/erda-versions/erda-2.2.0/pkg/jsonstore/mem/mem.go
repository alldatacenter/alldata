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

package mem

import (
	"context"
	"strings"
	"sync"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

type MemStore struct {
	pool sync.Map
}

func New() (*MemStore, error) {
	store := &MemStore{
		pool: sync.Map{},
	}
	return store, nil
}

func (s *MemStore) Put(ctx context.Context, key, value string) error {
	s.pool.Store(key, value)
	return nil
}

func (s *MemStore) PutWithOption(ctx context.Context, key, value string, opts []interface{}) (interface{}, error) {
	return nil, s.Put(ctx, key, value)
}

func (s *MemStore) Get(ctx context.Context, key string) (storetypes.KeyValue, error) {
	value, ok := s.pool.Load(key)
	if !ok {
		return storetypes.KeyValue{}, errors.New("not found")
	}
	return storetypes.KeyValue{Key: []byte(key), Value: []byte(value.(string))}, nil
}

// TODO: better impl, instead of O(n)?
func (s *MemStore) PrefixGet(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	r := []storetypes.KeyValue{}
	s.pool.Range(func(k, v interface{}) bool {
		k_ := k.(string)
		if strings.HasPrefix(k_, prefix) {
			r = append(r, storetypes.KeyValue{Key: []byte(k_), Value: []byte(v.(string))})
		}
		return true
	})
	return r, nil
}

func (s *MemStore) Remove(ctx context.Context, key string) (*storetypes.KeyValue, error) {
	value, ok := s.pool.Load(key)
	if !ok {
		return nil, nil
	}
	s.pool.Delete(key)
	return &storetypes.KeyValue{
		Key:   []byte(key),
		Value: []byte(value.(string)),
	}, nil

}

func (s *MemStore) PrefixRemove(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	r := []storetypes.KeyValue{}
	s.pool.Range(func(k, v interface{}) bool {
		k_ := k.(string)
		if strings.HasPrefix(k_, prefix) {
			s.pool.Delete(k)
			r = append(r, storetypes.KeyValue{Key: []byte(k_), Value: []byte(v.(string))})
		}
		return true
	})
	return r, nil

}
func (s *MemStore) PrefixGetKey(ctx context.Context, prefix string) ([]storetypes.Key, error) {
	r := []storetypes.Key{}
	s.pool.Range(func(k, v interface{}) bool {
		k_ := k.(string)
		if strings.HasPrefix(k_, prefix) {
			r = append(r, storetypes.Key([]byte(k_)))
		}
		return true
	})
	return r, nil
}
