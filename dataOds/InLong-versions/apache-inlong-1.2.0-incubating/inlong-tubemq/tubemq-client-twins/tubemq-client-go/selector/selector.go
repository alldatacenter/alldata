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

// Package selector defines the route selector which is responsible for service discovery.
package selector

import (
	"fmt"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
)

// Selector is abstraction of route selector which can return an available address
// from the service name.
type Selector interface {
	// Select will return a service node which contains an available address.
	Select(serviceName string) (*Node, error)
}

var (
	selectors = make(map[string]Selector)
)

// Register registers a selector.
func Register(name string, s Selector) {
	selectors[name] = s
}

// Get returns the corresponding selector.
func Get(name string) (Selector, error) {
	if _, ok := selectors[name]; !ok {
		return nil, errs.New(errs.RetSelectorNotExist, fmt.Sprintf("selector %s is invalid", name))
	}
	return selectors[name], nil
}

// Node represents the service node.
type Node struct {
	// ServiceName of the node.
	ServiceName string
	// Address of the node.
	Address string
	// HasNext indicates whether or not the service has next node.
	HasNext bool
}
