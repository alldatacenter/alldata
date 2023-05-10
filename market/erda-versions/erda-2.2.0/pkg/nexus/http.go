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

package nexus

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/pkg/errors"
)

const (
	HeaderAuthorization = "Authorization"
	HeaderContentType   = "Content-Type"
)

var (
	ErrNotFound          = errors.New("not found")
	ErrMissingRepoFormat = errors.New("missing repo format")
)

func (n *Nexus) basicAuthBase64Value() string {
	return "Basic " + base64.StdEncoding.EncodeToString([]byte(n.Username+":"+n.Password))
}

func printJSON(o interface{}) {
	b, _ := json.MarshalIndent(o, "", "  ")
	fmt.Println(string(b))
}

func ErrNotOK(statusCode int, body string) error {
	if statusCode == http.StatusNotFound {
		return ErrNotFound
	}
	return errors.Errorf("status code: %d, err: %v", statusCode, body)
}
