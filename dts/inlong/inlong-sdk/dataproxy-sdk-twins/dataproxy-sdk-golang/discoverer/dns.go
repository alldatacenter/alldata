//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package discoverer

import (
	"errors"
	"net"
	"regexp"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/logger"
)

const (
	// IPRegexp is the regular expression for IP address
	IPRegexp = `^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$|^(([a-fA-F]|[a-fA-F][a-fA-F0-9\-]*[a-fA-F0-9])\.)*([A-Fa-f]|[A-Fa-f][A-Fa-f0-9\-]*[A-Fa-f0-9])$|^(?:(?:(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):){6})(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9]))\.){3}(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9])))))))|(?:(?:::(?:(?:(?:[0-9a-fA-F]{1,4})):){5})(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9]))\.){3}(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9])))))))|(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})))?::(?:(?:(?:[0-9a-fA-F]{1,4})):){4})(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9]))\.){3}(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9])))))))|(?:(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):){0,1}(?:(?:[0-9a-fA-F]{1,4})))?::(?:(?:(?:[0-9a-fA-F]{1,4})):){3})(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9]))\.){3}(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9])))))))|(?:(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):){0,2}(?:(?:[0-9a-fA-F]{1,4})))?::(?:(?:(?:[0-9a-fA-F]{1,4})):){2})(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9]))\.){3}(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9])))))))|(?:(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):){0,3}(?:(?:[0-9a-fA-F]{1,4})))?::(?:(?:[0-9a-fA-F]{1,4})):)(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9]))\.){3}(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9])))))))|(?:(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):){0,4}(?:(?:[0-9a-fA-F]{1,4})))?::)(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9]))\.){3}(?:(?:25[0-5]|(?:[1-9]|1[0-9]|2[0-4])?[0-9])))))))|(?:(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):){0,5}(?:(?:[0-9a-fA-F]{1,4})))?::)(?:(?:[0-9a-fA-F]{1,4})))|(?:(?:(?:(?:(?:(?:[0-9a-fA-F]{1,4})):){0,6}(?:(?:[0-9a-fA-F]{1,4})))?::)))))$`
)

// variables
var (
	ErrInvalidPort         = errors.New("invalid port")
	ErrNoAvailableEndpoint = errors.New("no available endpoints")
)

// NewDNS create a DNS discoverer
func NewDNS(domain string, port int, lookupInterval time.Duration, log logger.Logger) (Discoverer, error) {
	logger := logger.Std()
	if log != nil {
		logger = log
	}

	if port <= 0 || port > 65535 {
		return nil, ErrInvalidPort
	}

	if lookupInterval == 0 {
		lookupInterval = 30 * time.Second
	}

	dis := &dnsDiscoverer{
		domain:         domain,
		port:           port,
		lookupInterval: lookupInterval,
		endpointList:   make([]Endpoint, 0),
		hostListMap:    make(map[string]struct{}),
		eventHandlers:  make(map[EventHandler]struct{}),
		log:            logger,
	}

	regex := regexp.MustCompile(IPRegexp)
	// input domain is an IP
	if regex.MatchString(domain) {
		ip := net.ParseIP(domain)
		dis.endpointList = []Endpoint{{Host: ip.String(), Port: port, Addr: BuildAddr(ip.String(), port)}}
		dis.hostListStr = ip.String()

		return dis, nil
	}

	// domain
	dis.lookup()
	dis.update()
	return dis, nil
}

type dnsDiscoverer struct {
	sync.RWMutex
	domain         string
	port           int
	lookupInterval time.Duration
	endpointList   EndpointList
	hostListStr    string
	hostListMap    map[string]struct{}
	eventHandlers  map[EventHandler]struct{}
	closeFunc      func()
	log            logger.Logger
}

func (d *dnsDiscoverer) GetEndpoints() EndpointList {
	d.RLock()
	defer d.RUnlock()

	return d.endpointList
}

func (d *dnsDiscoverer) AddEventHandler(h EventHandler) {
	d.Lock()
	defer d.Unlock()

	d.eventHandlers[h] = struct{}{}
}

func (d *dnsDiscoverer) DelEventHandler(h EventHandler) {
	d.Lock()
	defer d.Unlock()

	delete(d.eventHandlers, h)
}

func (d *dnsDiscoverer) Close() {
	if d.closeFunc != nil {
		d.closeFunc()
	}
}

func (d *dnsDiscoverer) lookup() {
	hosts := make(map[string]struct{}, 16)
	for i := 1; i <= 32; i++ {
		lookupHosts, err := net.LookupHost(d.domain)
		if err != nil {
			d.log.Errorf("domain lookup failed: %v", err)
			break
		}

		for _, host := range lookupHosts {
			hosts[host] = struct{}{}
		}

		if len(lookupHosts) > 1 {
			// discoverer server return a list of hosts
			break
		} else {
			// discoverer server return only one ip per request
			if i > len(hosts)*2 && i > 12 {
				break
			}
		}
	}
	// will not update if ip list is empty
	if len(hosts) == 0 {
		d.log.Warnf("no hosts were found from domain: %s, we will keep the local cache", d.domain)
		return
	}

	allHosts := make([]string, 0, len(hosts))
	allHostMap := make(map[string]struct{})
	allEndpoints := make([]Endpoint, 0, len(hosts))
	addEndpoints := make([]Endpoint, 0)
	for host := range hosts {
		allHosts = append(allHosts, host)
		allHostMap[host] = struct{}{}
		allEndpoints = append(allEndpoints, Endpoint{Host: host, Port: d.port, Addr: BuildAddr(host, d.port)})

		// new host is not found in the old host list map, it is a new added one
		if _, ok := d.hostListMap[host]; !ok {
			addEndpoints = append(addEndpoints, Endpoint{Host: host, Port: d.port, Addr: BuildAddr(host, d.port)})
		}
	}

	delEndpoints := make([]Endpoint, 0)
	for host := range d.hostListMap {
		// old host is not found in the new host list map, it is deleted
		if _, ok := allHostMap[host]; !ok {
			delEndpoints = append(delEndpoints, Endpoint{Host: host, Port: d.port, Addr: BuildAddr(host, d.port)})
		}
	}

	d.Lock()
	d.endpointList = allEndpoints
	d.hostListMap = allHostMap
	d.Unlock()

	// show this logger only in case of ip list change
	// only ticker will update d.allHostStr, lock is unnecessary
	sort.Strings(allHosts)
	allHostStr := strings.Join(allHosts, ";")
	if allHostStr != d.hostListStr {
		d.hostListStr = allHostStr
		d.log.Infof("update domain host list %s: %v", d.domain, allHostStr)
	}

	d.RLock()
	defer d.RUnlock()
	if len(addEndpoints) > 0 || len(delEndpoints) > 0 {
		for h := range d.eventHandlers {
			h.OnEndpointUpdate(allEndpoints, addEndpoints, delEndpoints)
		}
	}
}

func (d *dnsDiscoverer) update() {
	wg := sync.WaitGroup{}
	ticker := time.NewTicker(d.lookupInterval)
	stopCh := make(chan struct{})

	wg.Add(1)
	go func() {
		defer wg.Done()
		for {
			select {
			case <-ticker.C:
				d.lookup()
			case <-stopCh:
				return
			default:
				time.Sleep(500 * time.Millisecond)
			}
		}
	}()

	d.closeFunc = func() {
		ticker.Stop()
		close(stopCh)
		wg.Wait()
	}
}
