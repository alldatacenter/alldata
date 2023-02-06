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

// 内存 + etcd， 内存中的数据自动与 etcd 中同步
package memetcd

import (
	"context"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/jsonstore/mem"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

type MemEtcdStore struct {
	// 保护 revision 的修改 && em与etcd之间的同步修改
	sync.Mutex
	mem  *mem.MemStore
	etcd *etcd.Store
	// 被同步的 etcd 目录
	etcdDir         string
	loadPeriod      int
	currentRevision int64
	cb              func(k, v string, t storetypes.ChangeType)
}

func New(ctx context.Context, etcdDir string, cb func(k, v string, t storetypes.ChangeType)) (*MemEtcdStore, error) {
	mem, err := mem.New()
	if err != nil {
		return nil, err
	}
	etcd, err := etcd.New()
	if err != nil {
		return nil, err
	}
	s := &MemEtcdStore{
		mem:        mem,
		etcd:       etcd,
		etcdDir:    etcdDir,
		loadPeriod: 30,
		cb:         cb,
	}
	if err := s.load(); err != nil {
		return nil, err
	}
	go func() {
		for {
			select {
			case <-ctx.Done():
				return
			default:
			}
			if err := s.sync(ctx); err != nil {
				logrus.Errorf("MemEtcdStore sync: %v", err)
			}
		}
	}()
	return s, nil
}

func (s *MemEtcdStore) load() error {
	kvs, err := s.etcd.PrefixGet(context.Background(), s.etcdDir)
	if err != nil {
		return err
	}
	if len(kvs) > 0 && kvs[0].Revision > s.currentRevision {
		s.currentRevision = kvs[0].Revision
	} else {
		// run load again?
		return nil
	}
	mem, err := mem.New()
	if err != nil {
		return err
	}
	oldMem := s.mem
	for _, kv := range kvs {
		if err := mem.Put(context.Background(), string(kv.Key), string(kv.Value)); err != nil {
			return err
		}
	}
	s.Lock()
	s.mem = mem
	s.Unlock()
	if s.cb != nil {
		if r, err := diff(oldMem, s.mem, s.etcdDir); err != nil {
			return err
		} else {
			adds := r[0]
			dels := r[1]
			updates := r[2]
			for _, kv := range adds {
				s.cb(string(kv.Key), string(kv.Value), storetypes.Add)
			}
			for _, kv := range dels {
				s.cb(string(kv.Key), string(kv.Value), storetypes.Del)
			}
			for _, kv := range updates {
				s.cb(string(kv.Key), string(kv.Value), storetypes.Update)
			}
		}
	}
	return nil
}

// return ([adds, dels, updates], error)
func diff(old, new_ *mem.MemStore, prefix string) ([3][]storetypes.KeyValue, error) {
	dels := []storetypes.KeyValue{}
	adds := []storetypes.KeyValue{}
	updates := []storetypes.KeyValue{}
	kvs, err := old.PrefixGet(context.Background(), prefix)
	if err != nil {
		return [3][]storetypes.KeyValue{}, err
	}
	for _, kv := range kvs {
		_, err := new_.Get(context.Background(), string(kv.Key))
		if err != nil && strings.Contains(err.Error(), "not found") {
			dels = append(dels, kv)
		} else if err != nil {
			return [3][]storetypes.KeyValue{}, err
		}
	}
	kvs, err = new_.PrefixGet(context.Background(), prefix)
	if err != nil {
		return [3][]storetypes.KeyValue{}, err
	}
	for _, kv := range kvs {
		kv_, err := old.Get(context.Background(), string(kv.Key))
		if err != nil && strings.Contains(err.Error(), "not found") {
			adds = append(adds, kv)
		} else if err != nil {
			return [3][]storetypes.KeyValue{}, err
		} else if string(kv.Value) != string(kv_.Value) {
			updates = append(updates, kv)
		}

	}
	return [3][]storetypes.KeyValue{adds, dels, updates}, nil
}

func (s *MemEtcdStore) sync(ctx context.Context) error {
	logrus.Info("MemEtcdStore sync start")
	quit := make(chan struct{}, 1)
	go func() {
		for {
			select {
			case <-quit:
				return
			case <-ctx.Done():
				return
			case <-time.After(time.Duration(s.loadPeriod) * time.Second):
				if err := s.load(); err != nil {
					logrus.Errorf("MemEtcdStore: load err: %v", err)
				}
			}
		}

	}()
	ch, err := s.etcd.Watch(ctx, s.etcdDir, true, false)
	if err != nil {
		quit <- struct{}{}
		return err
	}

	for kvs := range ch {
		if kvs.Err != nil {
			logrus.Errorf("MemEtcdStore: %v", kvs.Err)
			continue
		}
		s.Lock()
		for _, kv := range kvs.Kvs {
			if kv.Revision <= s.currentRevision {
				break // kvs's revision is same
			}
			if kv.T == storetypes.Del {
				if _, err := s.mem.Remove(ctx, string(kv.Key)); err != nil {
					logrus.Errorf("MemEtcdStore: mem remove: %v", err)
				}
			} else {
				if err := s.mem.Put(ctx, string(kv.Key), string(kv.Value)); err != nil {
					logrus.Errorf("MemEtcdStore: mem put: %v", err)
				}
			}
			if s.cb != nil {
				s.cb(string(kv.Key), string(kv.Value), kv.T)
			}
		}
		s.Unlock()
	}
	quit <- struct{}{}
	logrus.Info("MemEtcdStore sync stopped")
	return nil
}

func (s *MemEtcdStore) Put(ctx context.Context, key, value string) error {
	if !strings.HasPrefix(key, s.etcdDir) {
		return errors.Errorf("MemEtcdStore: Put: key[%s] prefix is not [%s]", key, s.etcdDir)
	}
	err := s.etcd.Put(context.Background(), key, value)
	if err != nil {
		return errors.Errorf("MemEtcdStore: etcd Put failed: %v", err)
	}
	s.Lock()
	defer s.Unlock()
	if err := s.mem.Put(context.Background(), key, value); err != nil {
		return errors.Errorf("MemEtcdStore: mem Put failed: %v", err)
	}
	return nil
}

func (s *MemEtcdStore) PutWithOption(ctx context.Context, key, value string, opts []interface{}) (interface{}, error) {
	return nil, s.Put(ctx, key, value)
}

func (s *MemEtcdStore) Get(ctx context.Context, key string) (storetypes.KeyValue, error) {
	if !strings.HasPrefix(key, s.etcdDir) {
		return storetypes.KeyValue{}, errors.Errorf("MemEtcdStore: Get: key[%s] prefix is not [%s]", key, s.etcdDir)
	}
	s.Lock()
	kv, err := s.mem.Get(context.Background(), key)
	s.Unlock()
	// err: not found
	if err != nil {
		if kv, err = s.etcd.Get(ctx, key); err != nil {
			return kv, err
		}
	}
	return kv, nil
}

func (s *MemEtcdStore) Remove(ctx context.Context, key string) (*storetypes.KeyValue, error) {
	if !strings.HasPrefix(key, s.etcdDir) {
		return nil, errors.Errorf("MemEtcdStore: Remove: key[%s] prefix is not [%s]", key, s.etcdDir)
	}
	kv, err := s.etcd.Remove(context.Background(), key)
	if err != nil {
		return nil, errors.Errorf("MemEtcdStore: etcd Remove failed: %v", err)
	}
	s.Lock()
	defer s.Unlock()
	if _, err := s.mem.Remove(context.Background(), key); err != nil {
		return nil, errors.Errorf("MemEtcdStore: mem Remove failed: %v", err)
	}
	return kv, nil
}

func (s *MemEtcdStore) PrefixRemove(pctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	if !strings.HasPrefix(prefix, s.etcdDir) {
		return nil, errors.Errorf("MemEtcdStore: PrefixRemove: key[%s] prefix is not [%s]", prefix, s.etcdDir)
	}
	kvs, err := s.etcd.PrefixRemove(context.Background(), prefix)
	if err != nil {
		return nil, errors.Errorf("MemEtcdStore: etcd PrefixRemove failed: %v", err)
	}
	s.Lock()
	defer s.Unlock()
	if _, err := s.mem.PrefixRemove(context.Background(), prefix); err != nil {
		return nil, errors.Errorf("MemEtcdStore: mem PrefixRemove failed: %v", err)
	}
	return kvs, nil

}
func (s *MemEtcdStore) PrefixGet(ctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	if !strings.HasPrefix(prefix, s.etcdDir) {
		return nil, errors.Errorf("MemEtcdStore: PrefixGet: key[%s] prefix is not [%s]", prefix, s.etcdDir)
	}
	s.Lock()
	defer s.Unlock()
	return s.mem.PrefixGet(context.Background(), prefix)
}

func (s *MemEtcdStore) PrefixGetKey(ctx context.Context, prefix string) ([]storetypes.Key, error) {
	if !strings.HasPrefix(prefix, s.etcdDir) {
		return nil, errors.Errorf("MemEtcdStore: PrefixGet: key[%s] prefix is not [%s]", prefix, s.etcdDir)
	}
	s.Lock()
	defer s.Unlock()
	return s.mem.PrefixGetKey(context.Background(), prefix)
}

func (s *MemEtcdStore) Watch(ctx context.Context, key string, isPrefix bool, filterDelete bool) (storetypes.WatchChan, error) {
	if !strings.HasPrefix(key, s.etcdDir) {
		return nil, errors.Errorf("MemEtcdStore: Watch: key[%s] prefix is not [%s]", key, s.etcdDir)
	}
	return s.etcd.Watch(ctx, key, isPrefix, filterDelete)
}
