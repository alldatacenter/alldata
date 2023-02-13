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

package endpoints

import (
	"context"
	"net/http"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/http/httpserver"
)

// ReleaseGC 通过GET /gc API触发releaese gc时处理逻辑
func (e *Endpoints) ReleaseGC(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	logrus.Infof("trigger release gc by api[ %s %s]!", r.Method, r.RequestURI)
	go func() {
		if err := e.release.RemoveDeprecatedsReleases(time.Now()); err != nil {
			logrus.Warnf("remove deprecated release error: %v", err)
		}
	}()

	return httpserver.OkResp("trigger release gc success")
}
