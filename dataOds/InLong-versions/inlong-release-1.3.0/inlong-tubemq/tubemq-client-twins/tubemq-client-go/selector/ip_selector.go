// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package selector

import (
	"errors"
	"strings"
	"sync"
)

func init() {
	s := &ipSelector{}
	s.services = make(map[string]*ipServices)
	Register("ip", s)
	Register("dns", s)
}

type ipSelector struct {
	mu       sync.Mutex
	services map[string]*ipServices
}

type ipServices struct {
	nextIndex int
	addresses []string
}

// Select implements Selector interface.
// Select will return the address in the serviceName sequentially.
// The first address will be returned after reaching the end of the addresses.
func (s *ipSelector) Select(serviceName string) (*Node, error) {
	if serviceName == "" {
		return nil, errors.New("serviceName empty")
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	services, ok := s.services[serviceName]
	if !ok {
		services = &ipServices{
			addresses: strings.Split(serviceName, ","),
			nextIndex: 0,
		}
		s.services[serviceName] = services
	}
	node := &Node{
		ServiceName: serviceName,
		Address:     services.addresses[services.nextIndex],
	}
	services.nextIndex = (services.nextIndex + 1) % len(services.addresses)
	if services.nextIndex > 0 {
		node.HasNext = true
	}
	return node, nil
}

// Refresh will refresh a service address cache data.
func (s *ipSelector) Refresh(serviceName string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	services, ok := s.services[serviceName]
	if !ok {
		return
	}
	services.nextIndex = 0
}
