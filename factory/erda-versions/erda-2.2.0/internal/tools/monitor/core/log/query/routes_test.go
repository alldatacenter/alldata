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
	"context"
	"net/http"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
)

func Test_downloadLog(t *testing.T) {
	r, _ := http.NewRequest("GET", "localhost:9200/", nil)
	w := mockResponseWriter{}
	p := &provider{}
	req := &LogRequest{
		ClusterName: "erda",
		ID:          "",
	}

	monkey.Patch((*logQueryService).walkLogItems, func(s *logQueryService, ctx context.Context, req Request, fn func(sel *storage.Selector) (*storage.Selector, error), walk func(item *pb.LogItem) error) error {
		sel := &storage.Selector{
			Options: map[string]interface{}{},
		}
		fn(sel)
		return nil
	})
	defer monkey.Unpatch((*logQueryService).walkLogItems)

	p.downloadLog(w, r, req)
}

type mockResponseWriter struct {
}

func (m mockResponseWriter) Header() http.Header {
	return map[string][]string{}
}

func (m mockResponseWriter) Write(bytes []byte) (int, error) {
	return 0, nil
}

func (m mockResponseWriter) WriteHeader(statusCode int) {

}

func (m mockResponseWriter) Flush() {

}
