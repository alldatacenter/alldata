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

package collector

import (
	"fmt"
	"io"
	"net/http"

	"github.com/sirupsen/logrus"
)

const (
	GZIPEncoding = "gzip"
)

func doRequest(client *http.Client, req *http.Request) (int, error) {
	resp, err := client.Do(req)
	if err != nil {
		return 0, fmt.Errorf("do request failed: %w", err)
	}
	_, err = io.Copy(io.Discard, resp.Body)
	if err != nil {
		return 0, fmt.Errorf("copy resp.Body: %w", err)
	}
	defer func() {
		err := resp.Body.Close()
		if err != nil {
			logrus.Infof("close body failed: %s", err)
		}
	}()
	return resp.StatusCode, nil
}

func setHeaders(req *http.Request, headers map[string]string) {
	for k, v := range headers {
		req.Header.Set(k, v)
	}
}
