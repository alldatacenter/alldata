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

package lib

import (
	"bytes"
	"compress/gzip"
	"encoding/base64"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"regexp"
	"runtime"
	"strings"
	"time"

	"github.com/golang/snappy"
	_ "go.uber.org/automaxprocs"
)

func NormalizeKey(key string) string {
	key = strings.ReplaceAll(key, ".", "_")
	key = strings.ReplaceAll(key, "/", "_")
	return key
}

func IsJSONArray(b []byte) bool {
	x := bytes.TrimLeft(b, " \t\r\n")
	return len(x) > 0 && x[0] == '['
}

func RegexGroupMap(pattern *regexp.Regexp, s string) map[string]string {
	match := pattern.FindStringSubmatch(s)
	if match == nil {
		return map[string]string{}
	}
	result := make(map[string]string)
	for i, name := range pattern.SubexpNames() {
		if i != 0 && name != "" {
			result[name] = match[i]
		}
	}
	return result
}

// read request's body based on Content-Encoding Header
func ReadBody(req *http.Request) ([]byte, error) {
	defer req.Body.Close()

	var res []byte
	switch req.Header.Get("Content-Encoding") {
	case "gzip":
		r, err := gzip.NewReader(req.Body)
		if err != nil {
			return nil, fmt.Errorf("gzip.NewReader err: %w", err)
		}

		data, err := io.ReadAll(r)
		if err != nil {
			return nil, err
		}
		res = data
	case "snappy":
		data, err := io.ReadAll(req.Body)
		if err != nil {
			return nil, err
		}
		// snappy block format is only supported by decode/encode not snappy reader/writer
		data, err = snappy.Decode(nil, data)
		if err != nil {
			return nil, fmt.Errorf("snappy.Decode err: %w", err)
		}
		res = data
	default:
		data, err := io.ReadAll(req.Body)
		if err != nil {
			return nil, err
		}
		res = data
	}

	switch req.Header.Get("Custom-Content-Encoding") {
	case "base64":
		dst := make([]byte, base64.StdEncoding.DecodedLen(len(res)))
		_, err := base64.StdEncoding.Decode(dst, res)
		if err != nil {
			return nil, fmt.Errorf("base64 decode: %w", err)
		}
		res = dst
	case "":
	default:
		return nil, fmt.Errorf("unsupported custom-content-encoding")
	}
	return res, nil
}

func RandomDuration(interval, jitter time.Duration) time.Duration {
	return interval + time.Duration(rand.Int63n(jitter.Nanoseconds()))
}

func AvailableCPUs() int {
	return runtime.GOMAXPROCS(-1)
}
