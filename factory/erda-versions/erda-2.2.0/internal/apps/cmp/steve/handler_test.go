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

package steve

import (
	"context"
	"fmt"
	"net/http"
	"net/url"
	"testing"

	"github.com/rancher/apiserver/pkg/server"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/steve/pkg/schema"
	"github.com/rancher/wrangler/pkg/schemas"
	"k8s.io/apiserver/pkg/authentication/user"
	"k8s.io/apiserver/pkg/endpoints/request"
)

type userInfo struct {
	user.Info
	Name string
}

func (u *userInfo) GetName() string {
	return u.Name
}

type factory struct {
	schema.Factory
}

func (f *factory) Schemas(user user.Info) (*types.APISchemas, error) {
	if user.GetName() == "errorUser" {
		return nil, fmt.Errorf("testError")
	}

	if user.GetName() != "testUser" {
		return nil, fmt.Errorf("test failed, expected user name %s, actual %s", "testUser", user.GetName())
	}

	return &types.APISchemas{
		Schemas: map[string]*types.APISchema{
			"testSchema": {
				Schema: &schemas.Schema{
					ID:          "test",
					Description: "used for unit testing",
					PluralName:  "tests",
				},
			},
		},
	}, nil
}

type responseWriter struct {
	http.ResponseWriter
	Head int
	Body string
}

func (r *responseWriter) Write(b []byte) (int, error) {
	r.Body = string(b)
	return 0, nil
}

func (r *responseWriter) WriteHeader(h int) {
	r.Head = h
}

func TestCommon(t *testing.T) {
	api := apiServer{
		sf:     &factory{},
		server: server.DefaultAPIServer(),
	}

	url, err := url.Parse("https://unit.test")
	if err != nil {
		t.Error(err)
	}

	req := &http.Request{
		Method: http.MethodPost,
		URL:    url,
		Proto:  "https",
	}
	ctx := request.WithUser(context.Background(), &userInfo{Name: "testUser"})
	req1 := req.WithContext(ctx)

	rw := &responseWriter{}
	apiReq, ok := api.common(rw, req1, "test")
	if !ok {
		t.Errorf("test failed, expected result %t, actual %t", true, ok)
	}

	schema, ok := apiReq.Schemas.Schemas["testSchema"]
	if !ok {
		t.Errorf("test failed, expected schema \"testSchema\" is not found in the result")
	}

	if schema.ID != "test" {
		t.Errorf("test failed, expected schema id %s, actual %s", "test", schema.ID)
	}

	ctx2 := request.WithUser(context.Background(), &userInfo{Name: "errorUser"})
	req2 := req.WithContext(ctx2)

	apiReq, ok = api.common(rw, req2, "test")

	if rw.Body != "testError" {
		t.Errorf("test failed, expoected body %s, actual %s", "testError", rw.Body)
	}

	if rw.Head != http.StatusInternalServerError {
		t.Errorf("test failed, expoected body %d, actual %d", http.StatusInternalServerError, rw.Head)
	}
}

func TestGetURLPrefix(t *testing.T) {
	res := GetURLPrefix("c-test")
	if res != "/api/k8s/clusters/c-test" {
		t.Errorf("test failed, expected result %s, actual %s", "/k8s/clusters/c-test", res)
	}
}
