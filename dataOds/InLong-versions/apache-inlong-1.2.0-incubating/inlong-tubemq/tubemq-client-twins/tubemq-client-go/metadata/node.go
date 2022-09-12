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

package metadata

import (
	"strconv"
	"strings"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
)

// Node represents the metadata of a node.
type Node struct {
	id      uint32
	host    string
	port    uint32
	address string
}

// NewNode constructs a node from a given string.
// If the given string is invalid, it will return error.
// The format of node string: nodeID:host:port
func NewNode(isBroker bool, node string) (*Node, error) {
	res := strings.Split(node, ":")
	if len(res) == 1 {
		return nil, errs.ErrInvalidNodeString
	}
	nodeID := 0
	host := ""
	port := 8123
	var err error
	if isBroker {
		nodeID, err = strconv.Atoi(res[0])
		if err != nil {
			return nil, err
		}
		host = res[1]
		if len(res) >= 3 {
			port, err = strconv.Atoi(res[2])
			if err != nil {
				return nil, err
			}
		}
	} else {
		host = res[0]
		if len(res) >= 2 {
			port, err = strconv.Atoi(res[1])
			if err != nil {
				return nil, err
			}
		}
	}
	return &Node{
		id:      uint32(nodeID),
		host:    host,
		port:    uint32(port),
		address: host + ":" + strconv.Itoa(port),
	}, nil
}

// GetID returns the id of a node.
func (n *Node) GetID() uint32 {
	return n.id
}

// GetPort returns the port of a node.
func (n *Node) GetPort() uint32 {
	return n.port
}

// GetHost returns the hostname of a node.
func (n *Node) GetHost() string {
	return n.host
}

// GetAddress returns the address of a node.
func (n *Node) GetAddress() string {
	return n.address
}

// String returns the metadata of a node as a string.
func (n *Node) String() string {
	return strconv.Itoa(int(n.id)) + ":" + n.host + ":" + strconv.Itoa(int(n.port))
}

// SetHost sets the host.
func (n *Node) SetHost(host string) {
	n.host = host
}

// SetAddress sets the address.
func (n *Node) SetAddress(address string) error {
	port, err := strconv.Atoi(strings.Split(address, ":")[1])
	if err != nil {
		return err
	}
	n.address = address
	n.port = uint32(port)
	return nil
}
