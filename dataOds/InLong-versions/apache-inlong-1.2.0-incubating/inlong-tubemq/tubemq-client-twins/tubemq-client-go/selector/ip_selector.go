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
)

func init() {
	s := &ipSelector{}
	s.services = make(map[string]*ipServices)
	Register("ip", s)
	Register("dns", s)
}

type ipSelector struct {
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
	if len(serviceName) == 0 {
		return nil, errors.New("serviceName empty")
	}

	num := strings.Count(serviceName, ",") + 1
	if num == 1 {
		return &Node{
			ServiceName: serviceName,
			Address:     serviceName,
			HasNext:     false,
		}, nil
	}

	var addresses []string
	nextIndex := 0
	if _, ok := s.services[serviceName]; !ok {
		addresses = strings.Split(serviceName, ",")
	} else {
		services := s.services[serviceName]
		addresses = services.addresses
		nextIndex = services.nextIndex
	}

	address := addresses[nextIndex]
	nextIndex = (nextIndex + 1) % num
	s.services[serviceName] = &ipServices{
		addresses: addresses,
		nextIndex: nextIndex,
	}

	node := &Node{
		ServiceName: serviceName,
		Address:     address,
	}
	if nextIndex > 0 {
		node.HasNext = true
	}
	return node, nil
}
