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

package auth

import (
	"fmt"
	"net/http"

	"github.com/mitchellh/mapstructure"
)

type BasicAuth struct {
	Username string `mapstructure:"username"`
	Password string `mapstructure:"password"`
}

func NewBasicAuth(cfg map[string]interface{}) (*BasicAuth, error) {
	ba := BasicAuth{}
	err := mapstructure.Decode(cfg, &ba)
	if err != nil {
		return nil, fmt.Errorf("decode err: %w", err)
	}
	if ba.Username == "" || ba.Password == "" {
		return nil, fmt.Errorf("empty username or password: %+v", ba)
	}
	return &ba, nil
}

func (ba *BasicAuth) Secure(req *http.Request) {
	req.SetBasicAuth(ba.Username, ba.Password)
}
