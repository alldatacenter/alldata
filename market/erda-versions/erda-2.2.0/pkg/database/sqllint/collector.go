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

package sqllint

import (
	"sync"
)

var c = &Collector{sync.Map{}}

type Collector struct {
	sync.Map
}

func (c *Collector) Range(f func(key string, factory Factory) bool) {
	c.Map.Range(func(k, v interface{}) bool {
		return f(k.(string), v.(Factory))
	})
}

func (c *Collector) Load(name string) (Factory, bool) {
	value, ok := c.Map.Load(name)
	if !ok {
		return nil, false
	}
	return value.(Factory), true
}

func Register(name string, factory Factory) {
	c.Store(name, factory)
}

func Get() *Collector {
	return c
}
