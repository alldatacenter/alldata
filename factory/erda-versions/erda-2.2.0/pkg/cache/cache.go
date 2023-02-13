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

package cache

import (
	"sync"
	"time"

	"github.com/sirupsen/logrus"
)

var (
	cachesC = make(chan *Cache, 1<<16)
)

func init() {
	go asyncUpdate()
}

func asyncUpdate() {
	for {
		select {
		case c := <-cachesC:
			c.updateSync(<-c.C)
		}
	}
}

// Cache defines the methods LoadWithUpdate and Store,
// the user needs to define the function `update`.
type Cache struct {
	Map sync.Map
	C   chan interface{}

	name    string
	expired time.Duration
	update  Update
}

// New returns the *Cache.
// the function update, if found new *item, returns true, and stores it;
// else returns false, and delete the key from cache.
func New(name string, expired time.Duration, update Update) *Cache {
	c := &Cache{
		C:       make(chan interface{}, 1<<16),
		name:    name,
		expired: expired,
		update:  update,
	}
	return c
}

// Name returns the Cache object's name
func (c *Cache) Name() string {
	return c.name
}

// LoadWithUpdate loads the cached item.
// If the item is not cached, it returns the newest and caches the new item.
// If the time is cached, it returns the item.
// if the cached item is expired, it returns the current item and tries to cache the newest.
func (c *Cache) LoadWithUpdate(key interface{}) (interface{}, bool) {
	value, ok := c.Map.Load(key)
	if !ok {
		return c.updateSync(key)
	}
	item := value.(*item)
	if item.expired.Before(time.Now()) {
		select {
		case c.C <- key:
			cachesC <- c
		default:
			logrus.WithField("func", "*Cache.LoadWithUpdate").
				WithField("name", c.Name()).Warnln("channel is blocked, update cache is skipped")
		}
	}
	return item.Object, true
}

// LoadWithUpdateSync loads the cached item.
// If the item is not cached, it returns the newest and caches the new item.
// If the item is cached, it returns the item.
// If the cached item is expired, it tries to retrieve the newest then caches and returns it.
func (c *Cache) LoadWithUpdateSync(key interface{}) (interface{}, bool) {
	value, ok := c.Map.Load(key)
	if !ok {
		return c.updateSync(key)
	}
	item := value.(*item)
	if item.expired.Before(time.Now()) {
		return c.updateSync(key)
	}
	return item.Object, true
}

// Store caches the key and value, and updates its expired time.
func (c *Cache) Store(key interface{}, value interface{}) {
	c.Map.Store(key, &item{Object: value, expired: time.Now().Add(c.expired)})
}

// Load loads the value from cache.
func (c *Cache) Load(key interface{}) (interface{}, bool) {
	v, ok := c.Map.Load(key)
	if ok {
		return v.(*item).Object, true
	}
	return nil, false
}

func (c *Cache) updateSync(key interface{}) (interface{}, bool) {
	v, ok := c.update(key)
	if ok {
		go c.Store(key, v)
		return v, true
	}
	go c.Map.Delete(key)
	return nil, false
}

// item contains the item be cached
type item struct {
	Object interface{}

	expired time.Time
}

// Update retrieves the caching item
type Update func(interface{}) (interface{}, bool)
