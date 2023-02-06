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

// Package stm impl jsonstore stm with etcd concurrency package
package stm

import (
	"encoding/json"

	"github.com/coreos/etcd/clientv3"
	"github.com/coreos/etcd/clientv3/concurrency"
)

// JSONStoreSTMOP 包括了在 STM 中能使用的 API
type JSONStoreSTMOP interface {
	Get(key string, object interface{}) error
	Put(key string, object interface{}) error
	Remove(key string)
}

// JSONStoreSTM 包括了实现STM所需的接口
type JSONStoreSTM interface {
	NewSTM(f func(stm JSONStoreSTMOP) error) error
}

// JSONStoreSTMImpl 实现了 stm operations
type JSONStoreSTMImpl struct {
	stm concurrency.STM
}

// Get 作用与JSONStore.Get 相同，在STM中使用
func (j *JSONStoreSTMImpl) Get(key string, object interface{}) error {
	v := j.stm.Get(key)
	if err := json.Unmarshal([]byte(v), object); err != nil {
		return err
	}
	return nil
}

// Put 作用与 JSONStore.Put 相同，在 STM 中使用
func (j *JSONStoreSTMImpl) Put(key string, object interface{}) error {
	v, err := json.Marshal(object)
	if err != nil {
		return err
	}
	j.stm.Put(key, string(v))
	return nil
}

// Remove 作用与 JSONStore.Remove 相同，在 STM 中使用
func (j *JSONStoreSTMImpl) Remove(key string) {
	j.stm.Del(key)
}

// JSONStoreWithSTMImpl 实现 STM
type JSONStoreWithSTMImpl struct {
	client *clientv3.Client
}

// NewJSONStoreWithSTMImpl 创建 JSONStoreWithSTMImpl
func NewJSONStoreWithSTMImpl(client *clientv3.Client) JSONStoreWithSTMImpl {
	return JSONStoreWithSTMImpl{client}
}

// NewSTM 创建 STM
func (j *JSONStoreWithSTMImpl) NewSTM(f func(stm JSONStoreSTMOP) error) error {
	rawF := func(stm concurrency.STM) error {
		jsstm := JSONStoreSTMImpl{stm}
		return f(&jsstm)
	}
	if _, err := concurrency.NewSTM(j.client, rawF); err != nil {
		return err
	}
	return nil
}
