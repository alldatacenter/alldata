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

package colonyutil

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"
)

// TODO Deprecated util
func WriteJSON(w http.ResponseWriter, v interface{}) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	b, err := json.Marshal(v)
	if err != nil {
		logrus.Debugln(err)
	}
	_, err = w.Write(b)
	if err != nil {
		logrus.Debugln(err)
	}
}

func WriteData(w http.ResponseWriter, v interface{}) {
	WriteJSON(w, map[string]interface{}{
		"success": true,
		"data":    v,
	})
}

func WriteErr(w http.ResponseWriter, code, msg string) {
	WriteJSON(w, map[string]interface{}{
		"success": false,
		"err": map[string]interface{}{
			"code": code,
			"msg":  msg,
			"ctx":  nil,
		},
	})
}

func WriteError(w http.ResponseWriter, code int, err error) {
	c := strconv.Itoa(code)
	msg := fmt.Sprintf("%v", err)

	WriteErr(w, c, msg)
}

func LoggingMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		logrus.Infof("%s host: %s, protocol: %s, method: %s, header: %s, uri: %s",
			time.Now().String(), r.Host, r.Proto, r.Method, r.Header, r.RequestURI)
		next.ServeHTTP(w, r)
	})
}
