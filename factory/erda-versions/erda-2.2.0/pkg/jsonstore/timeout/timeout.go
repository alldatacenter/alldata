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

/*
重复插入key相同的value, 不更新该key的deadline
*/
package timeout

import (
	"container/heap"
	"context"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/jsonstore/js_util"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

type elem struct {
	deadline time.Time
	key      string
}

type elemHeap struct {
	elems []elem
	keys  map[string]struct{}
}

func (h elemHeap) Len() int           { return len(h.elems) }
func (h elemHeap) Less(i, j int) bool { return h.elems[i].key < h.elems[j].key }
func (h elemHeap) Swap(i, j int)      { h.elems[i], h.elems[j] = h.elems[j], h.elems[i] }

func (h *elemHeap) Push(v interface{}) {
	e := v.(elem)
	if _, ok := h.keys[e.key]; ok {
		return
	}
	h.elems = append(h.elems, e)
	h.keys[e.key] = struct{}{}
}

func (h *elemHeap) Pop() interface{} {
	old := h.elems
	n := len(old)
	v := old[n-1]
	h.elems = old[0 : n-1]
	return v
}

type TimeoutStore struct {
	timeout  time.Duration
	backend  storetypes.Store
	elemheap *elemHeap
	sync.RWMutex
}

func New(timeout int, backend storetypes.Store) (*TimeoutStore, error) {
	if timeout <= 0 {
		return nil, errors.New("illegal timeout value")
	}
	s := &TimeoutStore{
		timeout:  time.Duration(timeout) * time.Second,
		backend:  backend,
		elemheap: &elemHeap{keys: map[string]struct{}{}},
	}
	heap.Init(s.elemheap)
	go s.clear()
	return s, nil
}

func (s *TimeoutStore) clear() {
	for {
		if len(s.elemheap.keys) == 0 {
			time.Sleep(100 * time.Millisecond)
			continue
		}
		s.Lock()
		first := heap.Pop(s.elemheap).(elem)
		s.Unlock()
		<-time.After(time.Until(first.deadline))
		s.Remove(context.Background(), first.key)
		s.Lock()
		delete(s.elemheap.keys, first.key)
		s.Unlock()

		// sync s.elemheap.elems with s.elemheap.keys
		s.Lock()
		gap := len(s.elemheap.elems) - len(s.elemheap.keys)
		if gap > len(s.elemheap.elems)/3 && gap > 10 {
			newElems := []elem{}
			for _, v := range s.elemheap.elems {
				if _, ok := s.elemheap.keys[v.key]; ok {
					newElems = append(newElems, v)
				}
			}
			s.elemheap.elems = newElems
			if len(s.elemheap.elems) != len(s.elemheap.keys) {
				logrus.Errorf("inconsistent num elemheap.elems and elemheap.keys")
			}
		}
		s.Unlock()
	}
}

func (s *TimeoutStore) Put(ctx context.Context, key, value string) error {
	if err := s.backend.Put(ctx, key, value); err != nil {
		return err
	}
	s.Lock()
	heap.Push(s.elemheap, elem{time.Now().Add(s.timeout), key})
	s.Unlock()
	return nil
}
func (s *TimeoutStore) PutWithOption(ctx context.Context, key, value string, opts []interface{}) (interface{}, error) {
	return nil, s.Put(ctx, key, value)
}
func (s *TimeoutStore) Get(ctx context.Context, key string) (storetypes.KeyValue, error) {
	s.RLock()
	defer s.RUnlock()
	if _, ok := s.elemheap.keys[key]; !ok {
		return storetypes.KeyValue{}, errors.New("not found")
	}
	return s.backend.Get(ctx, key)
}

func (s *TimeoutStore) PrefixGet(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	s.RLock()
	defer s.RUnlock()
	ks := []string{}
	for k := range s.elemheap.keys {
		if strings.HasPrefix(k, prefix) {
			ks = append(ks, k)
		}
	}
	kvs, err := s.backend.PrefixGet(ctx, prefix)
	if err != nil {
		return nil, err
	}
	interSectionKvs, _ := js_util.InterSectionKVs(kvs, ks)
	return interSectionKvs, nil
}

func (s *TimeoutStore) Remove(ctx context.Context, key string) (*storetypes.KeyValue, error) {
	s.Lock()
	defer s.Unlock()
	if _, ok := s.elemheap.keys[key]; !ok {
		return nil, nil
	}

	delete(s.elemheap.keys, key)
	return s.backend.Remove(ctx, key)
}

func (s *TimeoutStore) PrefixRemove(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	s.Lock()
	defer s.Unlock()
	for k := range s.elemheap.keys {
		if strings.HasPrefix(k, prefix) {
			delete(s.elemheap.keys, k)
		}
	}
	return s.backend.PrefixRemove(ctx, prefix)
}

func (s *TimeoutStore) PrefixGetKey(ctx context.Context, prefix string) ([]storetypes.Key, error) {
	s.RLock()
	defer s.RUnlock()

	keys := []storetypes.Key{}
	for k := range s.elemheap.keys {
		if strings.HasPrefix(k, prefix) {
			keys = append(keys, storetypes.Key(k))
		}
	}
	return keys, nil
}
