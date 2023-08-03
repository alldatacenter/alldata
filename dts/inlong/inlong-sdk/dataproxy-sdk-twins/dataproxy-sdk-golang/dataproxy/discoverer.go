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

package dataproxy

import (
	"crypto/tls"
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/discoverer"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/logger"

	"github.com/go-resty/resty/v2"
)

// NewDiscoverer news a DataProxy discoverer
func NewDiscoverer(url, groupID string, lookupInterval time.Duration, log logger.Logger) (discoverer.Discoverer, error) {
	if url == "" {
		return nil, errors.New("URL is not given")
	}
	if groupID == "" {
		return nil, errors.New("group ID is not given")
	}
	if lookupInterval <= 0 {
		lookupInterval = 5 * time.Minute
	}
	if log == nil {
		log = logger.Std()
	}

	dis := &dataProxyDiscoverer{
		url:             url,
		groupID:         groupID,
		lookupInterval:  lookupInterval,
		endpointList:    make([]discoverer.Endpoint, 0),
		endpointListMap: make(map[string]discoverer.Endpoint),
		eventHandlers:   make(map[discoverer.EventHandler]struct{}),
		log:             log,
	}

	// initial lookup
	dis.lookup()
	dis.update()
	return dis, nil
}

type dataProxyDiscoverer struct {
	sync.RWMutex
	url             string
	groupID         string
	lookupInterval  time.Duration
	endpointList    discoverer.EndpointList
	endpointListStr string
	endpointListMap map[string]discoverer.Endpoint
	eventHandlers   map[discoverer.EventHandler]struct{}
	closeFunc       func()
	log             logger.Logger
}

func (d *dataProxyDiscoverer) GetEndpoints() discoverer.EndpointList {
	d.RLock()
	defer d.RUnlock()
	return d.endpointList
}

func (d *dataProxyDiscoverer) AddEventHandler(h discoverer.EventHandler) {
	d.Lock()
	defer d.Unlock()
	d.eventHandlers[h] = struct{}{}
}

func (d *dataProxyDiscoverer) DelEventHandler(h discoverer.EventHandler) {
	d.Lock()
	defer d.Unlock()
	delete(d.eventHandlers, h)
}

func (d *dataProxyDiscoverer) Close() {
	if d.closeFunc != nil {
		d.closeFunc()
	}
}

func (d *dataProxyDiscoverer) lookup() {
	c, err := d.get(2)
	if err != nil {
		d.log.Error("get server endpoint list failed:", err)
		return
	}

	if len(c.NodeList) == 0 {
		d.log.Warn("server endpoint list is empty")
		return
	}

	allEndpointAddrs := make([]string, 0, len(c.NodeList))
	allEndpointAddrMap := make(map[string]discoverer.Endpoint)
	allEndpoints := make([]discoverer.Endpoint, 0, len(c.NodeList))
	addEndpoints := make([]discoverer.Endpoint, 0)
	for _, ep := range c.NodeList {
		addr := discoverer.BuildAddr(ep.IP, ep.Port)
		newEndpoint := discoverer.Endpoint{Host: ep.IP, Port: ep.Port, Addr: addr}
		allEndpointAddrs = append(allEndpointAddrs, addr)
		allEndpointAddrMap[addr] = newEndpoint
		allEndpoints = append(allEndpoints, newEndpoint)

		// new endpoint is not found in the old endpoint list map, it is a new added one
		if _, ok := d.endpointListMap[addr]; !ok {
			addEndpoints = append(addEndpoints, newEndpoint)
		}
	}

	delEndpoints := make([]discoverer.Endpoint, 0)
	for addr, oldEndpoint := range d.endpointListMap {
		// old endpoint is not found in the new endpoint list map, it is deleted
		if _, ok := allEndpointAddrMap[addr]; !ok {
			delEndpoints = append(delEndpoints, oldEndpoint)
		}
	}

	d.Lock()
	d.endpointList = allEndpoints
	d.endpointListMap = allEndpointAddrMap
	d.Unlock()
	// show this logger only in case of ip list change
	// only ticker will update d.allEndpointAddrStr, lock is unnecessary
	sort.Strings(allEndpointAddrs)
	allEndpointAddrStr := strings.Join(allEndpointAddrs, ";")
	if allEndpointAddrStr != d.endpointListStr {
		d.endpointListStr = allEndpointAddrStr
		d.log.Infof("update server endpoint list %s: %v", d.url, allEndpointAddrStr)
	}

	d.RLock()
	defer d.RUnlock()
	if len(addEndpoints) > 0 || len(delEndpoints) > 0 {
		for h := range d.eventHandlers {
			h.OnEndpointUpdate(allEndpoints, addEndpoints, delEndpoints)
		}
	}
}

func (d *dataProxyDiscoverer) update() {
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

// get gets endpoint list from DataProxy service registry
func (d *dataProxyDiscoverer) get(retry int) (*cluster, error) {
	reqURL := fmt.Sprintf("%s/%s?protocolType=tcp", d.url, d.groupID)
	client := resty.New().SetTLSClientConfig(&tls.Config{InsecureSkipVerify: true})

	httpRsp, err := client.R().Post(reqURL)
	if err != nil {
		d.log.Error("get server endpoint list failed:", err)
		if retry <= 1 {
			return nil, err
		}

		retry--
		return d.get(retry)
	}

	rsp := &response{}
	err = json.Unmarshal(httpRsp.Body(), rsp)
	if err != nil {
		return nil, err
	}

	if !rsp.Success {
		return nil, errors.New(rsp.ErrMsg)
	}

	return &rsp.Data, nil
}

// endpoint is the config of a cluster endpoint
type endpoint struct {
	ID           int    `json:"id"`
	IP           string `json:"ip"`
	Port         int    `json:"port"`
	NodeLoad     int    `json:"nodeLoad"`
	ProtocolType string `json:"protocolType"`
}

// cluster is the config of a DataProxy cluster
type cluster struct {
	ClusterID  int        `json:"clusterId"`
	Load       int        `json:"load"`
	IsIntranet int        `json:"isIntranet"`
	IsSwitch   int        `json:"isSwitch"`
	NodeList   []endpoint `json:"nodeList"`
}

type response struct {
	Success bool    `json:"success"`
	ErrMsg  string  `json:"errMsg"`
	Data    cluster `json:"data"`
}
