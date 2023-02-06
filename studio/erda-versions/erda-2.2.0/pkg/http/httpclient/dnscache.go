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

// Package httpclient impl http client
package httpclient

import (
	"net"
	"sync"
	"time"
)

var (
	defaultDNSCache = NewDNSCache(60 * time.Second)
)

// DNSCache struct
type DNSCache struct {
	// 防止 refresh & lookup 中对 DnsCache.m 的并发访问
	sync.Mutex
	// map[hostname][]IP
	m *sync.Map
	// 刷新 cache 事件间隔
	refreshInterval time.Duration
}

// NewDNSCache 创建 DnsCache
func NewDNSCache(refreshInterval time.Duration) *DNSCache {
	c := &DNSCache{
		m:               new(sync.Map),
		refreshInterval: refreshInterval,
	}
	c.startRefresh()
	return c

}

func (d *DNSCache) startRefresh() {
	go func() {
		for {
			time.Sleep(d.refreshInterval)
			d.Lock()
			d.m = new(sync.Map)
			d.Unlock()
		}
	}()
}

func (d *DNSCache) lookup(host string) ([]net.IP, error) {
	d.Lock()
	m := d.m
	d.Unlock()
	v, ok := m.Load(host)
	if ok {
		return v.([]net.IP), nil
	}
	ips, err := net.LookupIP(host)
	if err != nil {
		return nil, err
	}
	m.Store(host, ips)

	return ips, nil
}
