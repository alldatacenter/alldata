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

// LRU + Watch
package cacheetcd

import (
	"container/list"
	"context"
	"fmt"
	"strings"
	"sync"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

type CacheEtcdStore struct {
	etcd *etcd.Store
	// 被同步的 etcd 目录
	etcdDir         string
	currentRevision int64

	cap     int
	keyList *list.List
	keyPool map[string]struct {
		e *list.Element
		v string
	}
	sync.Mutex
}

func New(ctx context.Context, etcdDir string, cap int) (*CacheEtcdStore, error) {
	etcd, err := etcd.New()
	if err != nil {
		return nil, err
	}
	s := &CacheEtcdStore{
		etcd:    etcd,
		etcdDir: etcdDir,
		cap:     cap,
		keyList: new(list.List),
		keyPool: make(map[string]struct {
			e *list.Element
			v string
		}),
	}
	go func() {
		for {
			if err := s.sync(ctx); err != nil {
				logrus.Errorf("CacheEtcdStore sync: %v", err)
			}
		}
	}()
	return s, nil

}

func (s *CacheEtcdStore) sync(ctx context.Context) error {
	ch, err := s.etcd.Watch(ctx, s.etcdDir, true, false)
	if err != nil {
		return err
	}
	for kvs := range ch {
		if kvs.Err != nil {
			logrus.Errorf("CacheEtcdStore: %v", kvs.Err)
			continue
		}
		s.Lock()
		for _, kv := range kvs.Kvs {
			if kv.Revision <= s.currentRevision {
				break
			}
			switch kv.T {
			case storetypes.Update:
				fallthrough
			case storetypes.Del:
				if kv_, ok := s.keyPool[string(kv.Key)]; ok {
					keyDel := s.keyList.Remove(kv_.e)
					delete(s.keyPool, keyDel.(string))
				}
			}
		}
		s.Unlock()
	}
	return nil
}

func (s *CacheEtcdStore) Put(ctx context.Context, key, value string) error {
	if !strings.HasPrefix(key, s.etcdDir) {
		return fmt.Errorf("CacheEtcdStore: Put: key[%s] prefix is not [%s]", key, s.etcdDir)
	}

	s.Lock()
	defer s.Unlock()

	if kv, ok := s.keyPool[key]; ok {
		rev, err := s.etcd.PutWithRev(ctx, key, value)
		if err != nil {
			return err
		}
		if s.currentRevision < rev {
			s.currentRevision = rev
		}
		s.keyList.MoveToFront(kv.e)
		s.keyPool[key] = struct {
			e *list.Element
			v string
		}{
			e: kv.e,
			v: value,
		}
		return nil
	}
	rev, err := s.etcd.PutWithRev(ctx, key, value)
	if err != nil {
		return err
	}
	if s.currentRevision < rev {
		s.currentRevision = rev
	}

	// update lru
	e := s.keyList.PushFront(key)
	s.keyPool[key] = struct {
		e *list.Element
		v string
	}{
		e: e,
		v: value,
	}

	if s.keyList.Len() > s.cap {
		e = s.keyList.Back()
		keyDel := s.keyList.Remove(e)
		delete(s.keyPool, keyDel.(string))
	}
	return nil
}

func (s *CacheEtcdStore) PutWithOption(ctx context.Context, key, value string, opts []interface{}) (interface{}, error) {
	return nil, s.Put(ctx, key, value)
}

func (s *CacheEtcdStore) Get(ctx context.Context, key string) (storetypes.KeyValue, error) {
	if !strings.HasPrefix(key, s.etcdDir) {
		return storetypes.KeyValue{}, fmt.Errorf("CacheEtcdStore: Get: key[%s] prefix is not [%s]",
			key, s.etcdDir)
	}

	s.Lock()
	defer s.Unlock()

	kv, ok := s.keyPool[key]
	if !ok {
		kv, err := s.etcd.Get(ctx, key)
		if err != nil {
			return storetypes.KeyValue{}, err
		}
		if s.currentRevision < kv.Revision {
			s.currentRevision = kv.Revision
		}
		// update lru
		e := s.keyList.PushFront(key)
		s.keyPool[key] = struct {
			e *list.Element
			v string
		}{e: e, v: string(kv.Value)}

		if s.keyList.Len() > s.cap {
			e = s.keyList.Back()
			keyDel := s.keyList.Remove(e)
			delete(s.keyPool, keyDel.(string))
		}
		return kv, nil
	}
	s.keyList.MoveToFront(kv.e)
	return storetypes.KeyValue{Key: []byte(key), Value: []byte(kv.v)}, nil
}

func (s *CacheEtcdStore) Remove(ctx context.Context, key string) (*storetypes.KeyValue, error) {
	if !strings.HasPrefix(key, s.etcdDir) {
		return nil, fmt.Errorf("CacheEtcdStore: Remove: key[%s] prefix is not [%s]", key, s.etcdDir)
	}
	s.Lock()
	defer s.Unlock()

	if kv, ok := s.keyPool[key]; ok {
		delete(s.keyPool, key)
		s.keyList.Remove(kv.e)
	}
	return s.etcd.Remove(context.Background(), key)

}
func (s *CacheEtcdStore) PrefixRemove(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	if !strings.HasPrefix(prefix, s.etcdDir) {
		return nil, fmt.Errorf("CacheEtcdStore: Remove: key[%s] prefix is not [%s]", prefix, s.etcdDir)
	}

	s.Lock()
	defer s.Unlock()

	for k, kv := range s.keyPool {
		if !strings.HasPrefix(k, prefix) {
			continue
		}
		delete(s.keyPool, k)
		s.keyList.Remove(kv.e)
	}

	return s.etcd.PrefixRemove(context.Background(), prefix)
}

// 直接从etcd prefixget, 因为 mem 中是 lru, 所以读 etcd 是必然的, 那么直接跳过 lru prefixget
func (s *CacheEtcdStore) PrefixGet(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	if !strings.HasPrefix(prefix, s.etcdDir) {
		return nil, fmt.Errorf("CacheEtcdStore: PrefixGet: key[%s] prefix is not [%s]", prefix, s.etcdDir)
	}
	return s.etcd.PrefixGet(context.Background(), prefix)
}

// 直接从etcd prefixgetkey, 因为 mem 中是 lru, 所以读 etcd 是必然的, 那么直接跳过 lru prefixgetkey
func (s *CacheEtcdStore) PrefixGetKey(ctx context.Context, prefix string) ([]storetypes.Key, error) {
	if !strings.HasPrefix(prefix, s.etcdDir) {
		return nil, fmt.Errorf("CacheEtcdStore: PrefixGetKey: key[%s] prefix is not [%s]", prefix, s.etcdDir)
	}
	return s.etcd.PrefixGetKey(context.Background(), prefix)
}

func (s *CacheEtcdStore) Watch(ctx context.Context, key string, isPrefix bool, filterDelete bool) (storetypes.WatchChan, error) {
	if !strings.HasPrefix(key, s.etcdDir) {
		return nil, fmt.Errorf("CacheEtcdStore: Watch: key[%s] prefix is not [%s]", key, s.etcdDir)
	}
	return s.etcd.Watch(ctx, key, isPrefix, filterDelete)
}
