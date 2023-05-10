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

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/jsonstore/stm"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

var (
	NotFoundErr = errors.New("not found")
)

type JsonStore interface {
	Put(ctx context.Context, key string, object interface{}) error
	PutWithOption(ctx context.Context, key string, object interface{}, opts []interface{}) (interface{}, error)
	Get(ctx context.Context, key string, object interface{}) error
	Remove(ctx context.Context, key string, object interface{}) error
	PrefixRemove(ctx context.Context, prefix string) (int, error)
	ForEach(ctx context.Context, prefix string, object interface{}, handle func(string, interface{}) error) error
	ForEachRaw(ctx context.Context, prefix string, handle func(string, []byte) error) error
	ListKeys(ctx context.Context, prefix string) ([]string, error)
	Notfound(ctx context.Context, key string) (bool, error)

	IncludeSTM() JSONStoreWithSTM
	IncludeWatch() JSONStoreWithWatch
}

type JSONStoreWithWatch interface {
	JsonStore
	Watch(ctx context.Context, key string, isPrefix bool, filterDelete bool, keyonly bool, object interface{}, handle func(string, interface{}, storetypes.ChangeType) error) error
}

type JSONStoreWithSTM interface {
	JsonStore
	stm.JSONStoreSTM
}

type JsonStoreImpl struct {
	store storetypes.Store
}

type JSONStoreWithWatchImpl struct {
	JsonStore
	store storetypes.StoreWithWatch
}

type JSONStoreWithSTMImpl struct {
	JsonStore
	store storetypes.StoreWithSTM
}

func New(opts ...OptionOperator) (JsonStore, error) {
	option := defaultOption
	option.Apply(opts)
	store, err := option.GetBackend()

	if err != nil {
		return nil, errors.Wrap(err, "failed to create store instance")
	}
	js := &JsonStoreImpl{store}
	if storeWithWatch, ok := store.(storetypes.StoreWithWatch); ok {
		return &JSONStoreWithWatchImpl{js, storeWithWatch}, nil
	}
	return js, nil
}

func (j *JsonStoreImpl) Put(ctx context.Context, key string, object interface{}) error {
	b, err := json.Marshal(object)
	if err != nil {
		return errors.Wrapf(err, "failed to put jsonstore: %s", key)
	}
	if err := j.store.Put(ctx, key, string(b)); err != nil {
		return errors.Wrapf(err, "failed to put jsonstore: %s", key)
	}
	return nil
}

func (j *JsonStoreImpl) PutWithOption(ctx context.Context, key string, object interface{}, opts []interface{}) (interface{}, error) {
	b, err := json.Marshal(object)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to putwithoption jsonstore: %s", key)
	}
	resp, err := j.store.PutWithOption(ctx, key, string(b), opts)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to putwithoption jsonstore: %s", key)
	}
	return resp, nil
}

func (j *JsonStoreImpl) Get(ctx context.Context, key string, object interface{}) error {
	kv, err := j.store.Get(ctx, key)
	if err != nil && err.Error() == "not found" {
		return NotFoundErr
	}
	if err != nil {
		return errors.Wrapf(err, "failed to get jsonstore: %s", key)
	}
	if err := json.Unmarshal(kv.Value, object); err != nil {
		return errors.Wrapf(err, "failed to get jsonstore: %s", key)
	}
	return nil
}

func (j *JsonStoreImpl) Remove(ctx context.Context, key string, object interface{}) error {
	kv, err := j.store.Remove(ctx, key)
	if err != nil {
		return err
	}
	if kv == nil {
		return nil
	}
	if object == nil {
		return nil
	}
	if err := json.Unmarshal(kv.Value, object); err != nil {
		return errors.Wrapf(err, "failed to decode the removed value: %s", key)
	}
	return nil
}

func (j *JsonStoreImpl) PrefixRemove(ctx context.Context, prefix string) (int, error) {
	kvs, err := j.store.PrefixRemove(ctx, prefix)
	if err != nil {
		return 0, err
	}
	return len(kvs), nil
}

func (j *JsonStoreImpl) ForEach(ctx context.Context, prefix string, object interface{}, handle func(string, interface{}) error) error {
	objectType := reflect.TypeOf(object)
	// TODO check objectType

	kvs, err := j.store.PrefixGet(ctx, prefix)
	if err != nil {
		return errors.Wrapf(err, "failed to get jsonstore by prefix: %s", prefix)
	}

	for _, kv := range kvs {
		e := reflect.New(objectType).Interface()
		if err := json.Unmarshal(kv.Value, e); err != nil {
			return errors.Wrapf(err, "failed to unmarshal kv.Value: %s", kv.Value)
		}

		if err := handle(string(kv.Key), e); err != nil {
			return errors.Wrapf(err, "failed to handle kv.Key: %s", kv.Key)
		}
	}

	return nil
}
func (j *JsonStoreImpl) ForEachRaw(ctx context.Context, prefix string, handle func(string, []byte) error) error {
	kvs, err := j.store.PrefixGet(ctx, prefix)
	if err != nil {
		return errors.Wrapf(err, "failed to get jsonstore by prefix: %s", prefix)
	}

	for _, kv := range kvs {
		if err := handle(string(kv.Key), kv.Value); err != nil {
			return errors.Wrapf(err, "failed to handle kv.Key: %s", kv.Key)
		}
	}

	return nil
}
func (j *JsonStoreImpl) ListKeys(ctx context.Context, prefix string) ([]string, error) {
	ks, err := j.store.PrefixGetKey(ctx, prefix)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to get jsonstore by prefix: %s", prefix)
	}
	r := make([]string, len(ks))
	for i, k := range ks {
		r[i] = string(k)
	}
	return r, nil
}

func (j *JsonStoreImpl) Notfound(ctx context.Context, key string) (bool, error) {
	_, err := j.store.Get(ctx, key)
	if err != nil {
		if err.Error() == "not found" {
			return true, nil
		}
		return false, err
	}
	return false, nil
}

func (j *JsonStoreImpl) IncludeSTM() JSONStoreWithSTM {
	s, ok := j.store.(storetypes.StoreWithSTM)
	if !ok {
		return nil
	}
	return &JSONStoreWithSTMImpl{j, s}
}

func (j *JsonStoreImpl) IncludeWatch() JSONStoreWithWatch {
	s, ok := j.store.(storetypes.StoreWithWatch)
	if !ok {
		return nil
	}
	return &JSONStoreWithWatchImpl{j, s}
}

// TODO: refactor this method, use options, instead of so many bool values
func (j *JSONStoreWithWatchImpl) Watch(ctx context.Context, key string, isPrefix bool, filterDelete bool, keyonly bool, object interface{}, handle func(string, interface{}, storetypes.ChangeType) error) error {
	objectType := reflect.TypeOf(object)
	// TODO check objectType

	ch, err := j.store.Watch(ctx, key, isPrefix, filterDelete)
	if err != nil {
		return errors.Wrapf(err, "failed to watch key: %s", key)
	}
	for r := range ch {
		if r.Err != nil {
			return r.Err
		}
		for _, kv := range r.Kvs {
			//if kv.T == storetypes.Del {
			//	if err := handle(string(kv.Key), nil, kv.T); err != nil {
			//		return errors.Wrapf(err, "failed to handle kv.Key: %s", string(kv.Key))
			//	}
			//	continue
			//}
			if keyonly {
				if err := handle(string(kv.Key), nil, kv.T); err != nil {
					return errors.Wrapf(err, "failed to handle kv.Key: %s", string(kv.Key))
				}
				continue
			}
			e := reflect.New(objectType).Interface()
			if err := json.Unmarshal(kv.Value, e); err != nil {
				return errors.Wrapf(err, "failed to unmarshal kv.Value: %s", string(kv.Value))
			}

			if err := handle(string(kv.Key), e, kv.T); err != nil {
				return errors.Wrapf(err, "failed to handle kv.Key: %s", string(kv.Key))
			}
		}
	}
	return nil
}

func (j *JSONStoreWithSTMImpl) NewSTM(f func(stm stm.JSONStoreSTMOP) error) error {
	return j.store.NewSTM(f)
}
