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

package clusterdialer

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"testing"
	"time"

	"github.com/erda-project/erda/pkg/discover"
)

const (
	queryIPAddr = "127.0.0.1"
	queryIPPort = "18751"
)

func TestQueryClusterManagerIP(t *testing.T) {
	http.HandleFunc("/clusterdialer/ip", func(rw http.ResponseWriter, req *http.Request) {
		res := map[string]interface{}{
			"succeeded": true,
			"IP":        queryIPAddr,
		}
		data, _ := json.Marshal(res)
		io.WriteString(rw, string(data))
	})
	targetEndpoint := fmt.Sprintf("%s:%s", queryIPAddr, queryIPPort)
	go http.ListenAndServe(targetEndpoint, nil)

	time.Sleep(1 * time.Second)
	os.Setenv(discover.EnvClusterDialer, targetEndpoint)
	res, ok := queryClusterManagerIP("")
	if !ok {
		t.Error("failed to get cluster manager ip")
	}

	ip, _ := res.(string)
	if ip != targetEndpoint {
		t.Errorf("got IP: %s, want: %s", ip, targetEndpoint)
	}
}
