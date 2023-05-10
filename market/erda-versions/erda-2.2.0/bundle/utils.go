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

package bundle

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func toAPIError(statusCode int, err apistructs.ErrorResponse) *errorresp.APIError {
	return errorresp.New(errorresp.WithCode(statusCode, err.Code), errorresp.WithMessage(err.Msg))
}
