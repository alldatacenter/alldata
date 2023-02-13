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

package http

import (
	"fmt"
	"os/exec"
	"strings"
	"time"
)

//func TestHTTP(t *testing.T) {
//	kill, err := RunAHTTPServer(12345, "content")
//	assert.Nil(t, err)
//	defer kill()
//	director := func(r *http.Request) {
//		r.URL.Scheme = "http"
//		r.URL.Host = "127.0.0.1:12345"
//		r.Header.Set("Origin", "http://127.0.0.1:12345")
//	}
//	p := NewReverseProxy(director, nil)
//	go http.ListenAndServe("127.0.0.1:6666", p)
//	time.Sleep(1000 * time.Millisecond)
//	r, err := http.Get("http://127.0.0.1:6666")
//	assert.Nil(t, err)
//	body, err := ioutil.ReadAll(r.Body)
//	assert.Nil(t, err)
//	assert.Equal(t, "content", string(body))
//
//}

// return (kill_function, error)
func RunAHTTPServer(port int, content string) (func() error, error) {
	content_ := strings.Replace(content, "\"", "\\\"", -1)
	socat := fmt.Sprintf("socat tcp-listen:%d,reuseaddr,fork \"exec:printf \\'HTTP/1.0 200 OK\\r\\nContent-Type\\:application/json\\r\\n\\r\\n%s\\'\"", port, content_)
	cmd := exec.Command("sh", "-c", socat)
	err := cmd.Start()
	time.Sleep(500 * time.Millisecond)
	return cmd.Process.Kill, err
}
