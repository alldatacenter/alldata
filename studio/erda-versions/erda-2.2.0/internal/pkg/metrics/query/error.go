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

package query

import (
	"encoding/json"
	"fmt"
)

type returnedErrorFormat struct {
	Success string            `json:"success"`
	Err     map[string]string `json:"err"`
}

type ServerError struct {
	errorCode string
	message   string
	context   string
}

func NewServerError(body []byte) error {
	data := &returnedErrorFormat{}
	if err := json.Unmarshal(body, data); err != nil {
		return err
	}
	return &ServerError{
		errorCode: data.Err["code"],
		message:   data.Err["msg"],
		context:   data.Err["context"],
	}
}

func (e ServerError) Error() string {
	return fmt.Sprintf("SDK.ServerError:\nErrorCode: %s\nContext: %s\nMessage: %s", e.errorCode, e.context, e.message)
}
