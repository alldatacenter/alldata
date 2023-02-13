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

package httpserver

import (
	"net/http"
	"testing"
)

const (
	defaultStatus  = http.StatusAccepted
	defaultContent = "content"
)

func fakeHTTPResponse() Responser {
	return &HTTPResponse{
		Status:  defaultStatus,
		Content: defaultContent,
	}
}

func TestHTTPResponse_GetContent(t *testing.T) {
	responser := fakeHTTPResponse()
	content := responser.GetContent()
	if content != defaultContent {
		t.Errorf("Expected content %s, but got %s", defaultContent, content)
	}
}

func TestHTTPResponse_GetStatus(t *testing.T) {
	responser := fakeHTTPResponse()
	status := responser.GetStatus()
	if status != defaultStatus {
		t.Errorf("Exepcted status code %d, but got %d", defaultStatus, status)
	}
}
