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

package component_protocol

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/erda-project/erda/apistructs"
)

func errorHandler(rw http.ResponseWriter, r *http.Request, err error) {
	resp := response{
		Success: false,
		Err: apistructs.ErrorResponse{
			Code: proxyErrorCode,
			Msg:  err.Error(),
		},
	}
	bytes, _ := json.Marshal(&resp)
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Content-Length", fmt.Sprint(len(bytes)))
	rw.WriteHeader(http.StatusInternalServerError)
	rw.Write(bytes)
}
