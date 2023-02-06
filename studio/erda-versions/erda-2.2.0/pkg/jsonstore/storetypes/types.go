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

// Package storetypes define jsonstore public types
package storetypes

import (
	"context"

	"github.com/erda-project/erda/pkg/jsonstore/stm"
)

// ChangeType kv 变化类型
type ChangeType int

func (c ChangeType) String() string {
	switch c {
	case Add:
		return "ADD"
	case Del:
		return "DEL"
	case Update:
		return "UPDATE"
	}
	return ""
}

const (
	// Add 新增 kv
	Add ChangeType = iota
	// Del 删除 kv
	Del
	// Update 更新 kv
	Update
)

// Key key 类型
type Key []byte

// KeyValue kv 类型，如果不是etcd backend，Revision 和 ModRevision 可能为空
type KeyValue struct {
	Key         []byte
	Value       []byte
	Revision    int64
	ModRevision int64
}

// KeyValueWithChangeType = KeyValue + ChangeType
type KeyValueWithChangeType struct {
	KeyValue
	T ChangeType
}

// WatchResponse watch 到的 kv 变化结果
type WatchResponse struct {
	Kvs []KeyValueWithChangeType
	Err error
}

// WatchChan watchResponse chan
type WatchChan <-chan WatchResponse

// Store 包括了实现 backend 所需的基本接口
type Store interface {
	Put(ctx context.Context, key, value string) error
	Get(ctx context.Context, key string) (KeyValue, error)
	Remove(ctx context.Context, key string) (*KeyValue, error)
	PrefixRemove(pctx context.Context, prefix string) ([]KeyValue, error)
	PrefixGet(ctx context.Context, prefix string) ([]KeyValue, error)
	PrefixGetKey(ctx context.Context, prefix string) ([]Key, error)

	PutWithOption(ctx context.Context, key, value string, opts []interface{}) (interface{}, error)
}

// StoreWithWatch = Store + Watch
type StoreWithWatch interface {
	Store
	Watch(ctx context.Context, key string, isPrefix bool, filterDelete bool) (WatchChan, error)
}

// StoreWithSTM = Store + STM
type StoreWithSTM interface {
	Store
	stm.JSONStoreSTM
}
