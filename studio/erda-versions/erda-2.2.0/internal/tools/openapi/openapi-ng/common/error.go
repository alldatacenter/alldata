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

package common

import (
	"encoding/json"
	"fmt"
	"net/http"
)

var _ error = (*HTTPError)(nil)

// HTTPError represents an error that occurred while handling a request.
type HTTPError struct {
	Code     int
	Message  interface{}
	Internal error // Stores the error returned by an external dependency
}

func (e *HTTPError) Error() string {
	return fmt.Sprintf("code:%d msg:%q", e.Code, e.Message)
}

// WriteError .
func WriteError(err error, rw http.ResponseWriter) (int, error) {
	if err != nil {
		if herr, ok := err.(*HTTPError); ok {
			rw.Header().Set("Content-Type", "application/json")
			byts, _ := json.Marshal(herr)
			return rw.Write(byts)
		}
		return rw.Write([]byte(err.Error()))
	}
	return 0, nil
}
