/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cache

import (
	"fmt"
	"sync"
	"time"

	"k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/klog/v2"
)

const (
	defaultExpiration = 2 * time.Minute
	defaultGCInterval = time.Minute
)

type item struct {
	object         interface{}
	expirationTime time.Time
}

type Cache struct {
	items             map[string]item
	defaultExpiration time.Duration
	gcInterval        time.Duration
	gcStarted         bool
	mu                sync.Mutex
}

func NewCacheDefault() *Cache {
	return &Cache{
		items:             map[string]item{},
		defaultExpiration: defaultExpiration,
		gcInterval:        defaultGCInterval,
	}
}

func NewCache(expiration time.Duration, gcInterval time.Duration) *Cache {
	cache := Cache{
		items:             map[string]item{},
		defaultExpiration: expiration,
		gcInterval:        gcInterval,
	}
	if cache.defaultExpiration <= 0 {
		cache.defaultExpiration = defaultExpiration
	}
	if cache.gcInterval <= time.Second {
		cache.gcInterval = defaultGCInterval
	}
	return &cache
}

func (c *Cache) Run(stopCh <-chan struct{}) error {
	defer runtime.HandleCrash()
	c.gcStarted = true
	go wait.Until(func() {
		c.gcExpiredCache()
	}, c.gcInterval, stopCh)
	return nil
}

func (c *Cache) gcExpiredCache() {
	c.mu.Lock()
	defer c.mu.Unlock()
	gcTime := time.Now()
	var gcKeys []string
	for key, item := range c.items {
		if gcTime.After(item.expirationTime) {
			gcKeys = append(gcKeys, key)
		}
	}
	for _, key := range gcKeys {
		delete(c.items, key)
	}
	klog.V(4).Infof("gc resource update executor, current size %v", len(c.items))
}

func (c *Cache) Set(key string, value interface{}, expiration time.Duration) error {
	return c.set(key, value, expiration)
}

func (c *Cache) SetDefault(key string, value interface{}) error {
	return c.set(key, value, c.defaultExpiration)
}

func (c *Cache) set(key string, value interface{}, expiration time.Duration) error {
	if !c.gcStarted {
		return fmt.Errorf("cache GC is not started yet")
	}
	item := item{
		object:         value,
		expirationTime: time.Now().Add(expiration),
	}
	c.mu.Lock()
	defer c.mu.Unlock()
	c.items[key] = item
	return nil
}

func (c *Cache) Get(key string) (interface{}, bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	item, ok := c.items[key]
	if !ok {
		return nil, false
	}
	if item.expirationTime.Before(time.Now()) {
		return nil, false
	}
	return item.object, true
}
