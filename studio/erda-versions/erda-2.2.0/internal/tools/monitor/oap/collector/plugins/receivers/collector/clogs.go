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
	"net/http"

	"github.com/buger/jsonparser"
	"github.com/labstack/echo"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib"
)

func (p *provider) collectLogs(ctx echo.Context) error {
	source := ctx.Param("source")
	if source == "" {
		return ctx.NoContent(http.StatusBadRequest)
	}
	name := source + "_log"

	body, err := lib.ReadBody(ctx.Request())
	if err != nil {
		return fmt.Errorf("fail to read request body, err: %w", err)
	}

	if !lib.IsJSONArray(body) {
		p.Log.Warnf("the body is not a json array. body=%s", string(body))
		return ctx.NoContent(http.StatusNoContent)
	}

	if _, err := jsonparser.ArrayEach(body, func(value []byte, dataType jsonparser.ValueType, offset int, err error) {
		if err != nil {
			p.Log.Errorf("fail to json parse, err: %v", err)
			return
		}
		if err := p.sendRaw(name, value); err != nil {
			p.Log.Errorf("sendRaw err: %s", err)
		}
	}); err != nil {
		return fmt.Errorf("collectLogs: %w", err)
	}

	return ctx.NoContent(http.StatusNoContent)
}
