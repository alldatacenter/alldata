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

package gitmodule

import (
	"container/list"
	"errors"
	"reflect"
	"sync"
	"sync/atomic"
)

type CacheStatus struct {
	Gets        int64
	Hits        int64
	MaxItemSize int
	CurrentSize int
}

//this is a interface which defines some common functions
type Cache interface {
	Set(key string, value interface{}) error
	Get(key string, outValue interface{}) error
	Delete(key string) error
	Status() *CacheStatus
}

// An AtomicInt is an int64 to be accessed atomically.
type AtomicInt int64

// MemCache is an LRU cache. It is safe for concurrent access.
type MemCache struct {
	mutex       sync.RWMutex
	maxItemSize int
	prefix      string
	cacheList   *list.List
	cache       map[interface{}]*list.Element
	hits, gets  AtomicInt
}

type entry struct {
	key   interface{}
	value interface{}
}

//NewMemCache If maxItemSize is zero, the cache has no limit.
//if maxItemSize is not zero, when cache's size beyond maxItemSize,start to swap
func NewMemCache(maxItemSize int, prefix string) *MemCache {
	return &MemCache{
		maxItemSize: maxItemSize,
		cacheList:   list.New(),
		prefix:      prefix,
		cache:       make(map[interface{}]*list.Element),
	}
}

//Status return the status of cache
func (c *MemCache) Status() *CacheStatus {
	c.mutex.Lock()
	status := &CacheStatus{
		MaxItemSize: c.maxItemSize,
		CurrentSize: c.cacheList.Len(),
		Gets:        c.gets.Get(),
		Hits:        c.hits.Get(),
	}
	c.mutex.Unlock()
	return status
}

func (c *MemCache) GetCacheMap() map[string]interface{} {
	result := map[string]interface{}{}
	for k, v := range c.cache {
		result[k.(string)] = v.Value
	}
	return result
}

//Get value with key
func (c *MemCache) Get(key string, out interface{}) error {
	c.mutex.Lock()
	defer c.mutex.Unlock()
	key = c.prefix + key
	c.gets.Add(1)
	if ele, hit := c.cache[key]; hit {
		c.hits.Add(1)
		c.cacheList.MoveToFront(ele)

		val := reflect.ValueOf(ele.Value.(*entry).value)
		valTyp := reflect.TypeOf(ele.Value.(*entry).value)
		outTyp := reflect.TypeOf(out)
		if outTyp.Kind() != reflect.Ptr {
			return errors.New("out type is not ptr")
		}
		if valTyp != outTyp.Elem() {
			return errors.New("type not match")
		}

		outVal := reflect.ValueOf(out)
		outVal.Elem().Set(val)
		return nil
	}
	return errors.New("key not found")
}

//Set a value with key
func (c *MemCache) Set(key string, value interface{}) error {
	c.mutex.Lock()
	defer c.mutex.Unlock()
	key = c.prefix + key

	if c.cache == nil {
		c.cache = make(map[interface{}]*list.Element)
		c.cacheList = list.New()
	}

	if ele, ok := c.cache[key]; ok {
		c.cacheList.MoveToFront(ele)
		ele.Value.(*entry).value = value
		return nil
	}

	ele := c.cacheList.PushFront(&entry{key: key, value: value})
	c.cache[key] = ele
	if c.maxItemSize != 0 && c.cacheList.Len() > c.maxItemSize {
		c.RemoveOldest()
	}
	return nil
}

//Delete delete the key
func (c *MemCache) Delete(key string) error {
	c.mutex.Lock()
	defer c.mutex.Unlock()
	key = c.prefix + key

	if c.cache == nil {
		return nil
	}
	if ele, ok := c.cache[key]; ok {
		c.cacheList.Remove(ele)
		key := ele.Value.(*entry).key
		delete(c.cache, key)
		return nil
	}
	return nil
}

//RemoveOldest remove the oldest key
func (c *MemCache) RemoveOldest() {
	if c.cache == nil {
		return
	}
	ele := c.cacheList.Back()
	if ele != nil {
		c.cacheList.Remove(ele)
		key := ele.Value.(*entry).key
		delete(c.cache, key)
	}
}

// Add atomically adds n to i.
func (i *AtomicInt) Add(n int64) {
	atomic.AddInt64((*int64)(i), n)
}

// Get atomically gets the value of i.
func (i *AtomicInt) Get() int64 {
	return atomic.LoadInt64((*int64)(i))
}
