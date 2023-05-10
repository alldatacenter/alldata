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

package lru

import (
	"container/list"
	"context"
	"strings"
	"sync"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/jsonstore/js_util"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

type LruStore struct {
	cap     int
	backend storetypes.Store
	keyList *list.List
	keyPool map[string]*list.Element
	sync.Mutex
}

func New(cap int, backend storetypes.Store) (*LruStore, error) {
	if cap <= 0 {
		return nil, errors.New("illegal cap value")
	}
	return &LruStore{cap: cap, backend: backend, keyList: new(list.List), keyPool: make(map[string]*list.Element)}, nil
}

func (s *LruStore) Put(ctx context.Context, key, value string) error {
	s.Lock()
	defer s.Unlock()

	if e, ok := s.keyPool[key]; ok {
		if err := s.backend.Put(ctx, key, value); err != nil {
			return err
		}
		s.keyList.MoveToFront(e)
		return nil
	}
	if err := s.backend.Put(ctx, key, value); err != nil {
		return err
	}

	e := s.keyList.PushFront(key)
	s.keyPool[key] = e

	if s.keyList.Len() > s.cap {
		e = s.keyList.Back()
		keyDel := s.keyList.Remove(e)
		delete(s.keyPool, keyDel.(string))
		// 删除后端的 kv 可能失败，忽略该错误
		// 1. 因为 Put 成功的
		// 2. 不影响后续操作
		s.backend.Remove(ctx, keyDel.(string))
	}
	return nil
}

func (s *LruStore) PutWithOption(ctx context.Context, key, value string, opts []interface{}) (interface{}, error) {
	return nil, s.Put(ctx, key, value)
}

func (s *LruStore) Get(ctx context.Context, key string) (storetypes.KeyValue, error) {
	s.Lock()
	defer s.Unlock()
	e, ok := s.keyPool[key]
	if !ok {
		return storetypes.KeyValue{}, errors.New("not found")
	}
	kv, err := s.backend.Get(ctx, key)
	if err != nil {
		return kv, err
	}
	s.keyList.MoveToFront(e)
	return kv, nil
}

func (s *LruStore) PrefixGet(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	s.Lock()
	defer s.Unlock()

	keys := []string{}
	es := []*list.Element{}
	for k, e := range s.keyPool {
		if strings.HasPrefix(k, prefix) {
			keys = append(keys, k)
			es = append(es, e)
		}
	}

	backendKVs, err := s.backend.PrefixGet(ctx, prefix)
	if err != nil {
		return backendKVs, err
	}
	interSectionKVs, interSectionKeys := js_util.InterSectionKVs(backendKVs, keys)

	for _, key := range interSectionKeys {
		e := s.keyPool[key]
		s.keyList.MoveToFront(e)
	}

	return interSectionKVs, nil
}
func (s *LruStore) remove(ctx context.Context, key string) (*storetypes.KeyValue, error) {
	var kv *storetypes.KeyValue
	if e, ok := s.keyPool[key]; ok {
		kv, err := s.backend.Remove(ctx, key)
		if err != nil {
			return kv, err
		}
		s.keyList.Remove(e)
		delete(s.keyPool, key)
	}
	return kv, nil
}
func (s *LruStore) Remove(ctx context.Context, key string) (*storetypes.KeyValue, error) {
	s.Lock()
	defer s.Unlock()
	return s.remove(ctx, key)
}

func (s *LruStore) PrefixRemove(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	s.Lock()
	defer s.Unlock()

	keys := []string{}
	for k := range s.keyPool {
		if strings.HasPrefix(k, prefix) {
			keys = append(keys, k)
		}
	}
	r := []storetypes.KeyValue{}
	for _, k := range keys {
		kv, err := s.remove(ctx, k)
		if err != nil {
			return nil, err
		}
		r = append(r, *kv)
	}
	return r, nil

}
func (s *LruStore) PrefixGetKey(ctx context.Context, prefix string) ([]storetypes.Key, error) {
	s.Lock()
	defer s.Unlock()

	keys := []storetypes.Key{}
	for k := range s.keyPool {
		if strings.HasPrefix(k, prefix) {
			keys = append(keys, storetypes.Key([]byte(k)))
		}
	}
	return keys, nil
}
