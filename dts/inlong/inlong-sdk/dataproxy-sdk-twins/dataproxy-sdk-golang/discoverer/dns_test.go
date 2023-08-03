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
	"fmt"
	"testing"
	"time"
)

type handler struct {
}

func (h *handler) OnEndpointUpdate(all, add, del EndpointList) {
	fmt.Println("all:", all)
	fmt.Println("add:", add)
	fmt.Println("del:", del)
}

func TestNewDNS(t *testing.T) {
	dns, err := NewDNS("dev.tglog.com", 20001, 5*time.Second, nil)
	if err != nil {
		t.Error(err)
	}
	fmt.Println("get", dns.GetEndpoints())
	dns.AddEventHandler(&handler{})
	select {}
}
