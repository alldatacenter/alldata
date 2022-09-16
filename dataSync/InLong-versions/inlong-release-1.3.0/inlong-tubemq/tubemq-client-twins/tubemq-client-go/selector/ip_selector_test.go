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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSingleIP(t *testing.T) {
	serviceName := "127.0.0.1:9092"
	selector, err := Get("ip")
	assert.Nil(t, err)
	node, err := selector.Select(serviceName)
	assert.Nil(t, err)
	assert.Equal(t, node.HasNext, false)
	assert.Equal(t, node.Address, "127.0.0.1:9092")
	assert.Equal(t, node.ServiceName, "127.0.0.1:9092")
}

func TestSingleDNS(t *testing.T) {
	serviceName := "tubemq:8081"
	selector, err := Get("dns")
	assert.Nil(t, err)
	node, err := selector.Select(serviceName)
	assert.Nil(t, err)
	assert.Equal(t, node.HasNext, false)
	assert.Equal(t, node.Address, "tubemq:8081")
	assert.Equal(t, node.ServiceName, "tubemq:8081")
}

func TestMultipleIP(t *testing.T) {
	serviceName := "127.0.0.1:9091,127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094"
	selector, err := Get("dns")
	assert.Nil(t, err)
	node, err := selector.Select(serviceName)
	assert.Nil(t, err)
	assert.Equal(t, true, node.HasNext)
	assert.Equal(t, "127.0.0.1:9091", node.Address)
	assert.Equal(t, "127.0.0.1:9091,127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094", node.ServiceName)

	node, err = selector.Select(serviceName)
	assert.Equal(t, true, node.HasNext)
	assert.Equal(t, "127.0.0.1:9092", node.Address)
	assert.Equal(t, "127.0.0.1:9091,127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094", node.ServiceName)

	node, err = selector.Select(serviceName)
	assert.Equal(t, true, node.HasNext)
	assert.Equal(t, "127.0.0.1:9093", node.Address)
	assert.Equal(t, "127.0.0.1:9091,127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094", node.ServiceName)

	node, err = selector.Select(serviceName)
	assert.Equal(t, false, node.HasNext)
	assert.Equal(t, "127.0.0.1:9094", node.Address)
	assert.Equal(t, "127.0.0.1:9091,127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094", node.ServiceName)
}

func TestMultipleDNS(t *testing.T) {
	serviceName := "tubemq:8081,tubemq:8082,tubemq:8083,tubemq:8084"
	selector, err := Get("dns")
	assert.Nil(t, err)
	node, err := selector.Select(serviceName)
	assert.Nil(t, err)
	assert.Equal(t, true, node.HasNext)
	assert.Equal(t, "tubemq:8081", node.Address)
	assert.Equal(t, "tubemq:8081,tubemq:8082,tubemq:8083,tubemq:8084", node.ServiceName)

	node, err = selector.Select(serviceName)
	assert.Equal(t, true, node.HasNext)
	assert.Equal(t, "tubemq:8082", node.Address)
	assert.Equal(t, "tubemq:8081,tubemq:8082,tubemq:8083,tubemq:8084", node.ServiceName)

	node, err = selector.Select(serviceName)
	assert.Equal(t, true, node.HasNext)
	assert.Equal(t, "tubemq:8083", node.Address)
	assert.Equal(t, "tubemq:8081,tubemq:8082,tubemq:8083,tubemq:8084", node.ServiceName)

	node, err = selector.Select(serviceName)
	assert.Equal(t, false, node.HasNext)
	assert.Equal(t, "tubemq:8084", node.Address)
	assert.Equal(t, "tubemq:8081,tubemq:8082,tubemq:8083,tubemq:8084", node.ServiceName)
}

func TestEmptyService(t *testing.T) {
	serviceName := ""
	selector, err := Get("ip")
	assert.Nil(t, err)
	_, err = selector.Select(serviceName)
	assert.Error(t, err)
}

func TestInvalidSelector(t *testing.T) {
	_, err := Get("selector")
	assert.Error(t, err)
}
