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

package command

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/terminal/color_str"
	"github.com/erda-project/erda/tools/cli/status"
)

var ctx = Context{
	Sessions: make(map[string]status.StatusInfo),
}

func GetContext() *Context {
	return &ctx
}

type Context struct {
	Sessions           map[string]status.StatusInfo
	CurrentOpenApiHost string
	CurrentOrg         OrgInfo
	CurrentProject     ProjectInfo
	CurrentApplication ApplicationInfo
	Applications       []ApplicationInfo
	Debug              bool
	Token              string // uc token
	HttpClient         *httpclient.HTTPClient
}

func (c *Context) Get() *httpclient.Request {
	return c.wrapRequest(ctx.HttpClient.Get)
}

func (c *Context) Post() *httpclient.Request {
	return c.wrapRequest(ctx.HttpClient.Post)
}

func (c *Context) Put() *httpclient.Request {
	return c.wrapRequest(ctx.HttpClient.Put)
}

func (c *Context) Patch() *httpclient.Request {
	return c.wrapRequest(ctx.HttpClient.Patch)
}

func (c *Context) Delete() *httpclient.Request {
	return c.wrapRequest(ctx.HttpClient.Delete)
}

func (c *Context) wrapRequest(m func(host string, retry ...httpclient.RetryOption) *httpclient.Request) *httpclient.Request {
	req := m(c.CurrentOpenApiHost)
	req.Header("USE-TOKEN", "true")
	if c.Token != "" {
		req.Header("Authorization", c.Token)
	}
	if v, ok := c.Sessions[c.CurrentOpenApiHost]; ok && v.SessionID != "" {
		req.Cookie(&http.Cookie{Name: "OPENAPISESSION", Value: c.Sessions[c.CurrentOpenApiHost].SessionID})
	}
	return req
}

func (c *Context) GetUserID() string {
	if sessionInfo, ok := ctx.Sessions[ctx.CurrentOpenApiHost]; ok {
		return sessionInfo.ID
	}

	return ""
}

func (c *Context) Info(format string, a ...interface{}) {
	f := "[INFO] " + strings.TrimSuffix(format, "\n") + "\n"
	fmt.Printf(f, a...)
}

func (c *Context) Warn(format string, a ...interface{}) {
	f := "[WARN] " + strings.TrimSuffix(format, "\n") + "\n"
	fmt.Printf(f, a...)
}

func (c *Context) Error(format string, a ...interface{}) {
	f := "[ERROR] " + strings.TrimSuffix(format, "\n") + "\n"
	fmt.Printf(f, a...)
}

func (c *Context) Succ(format string, a ...interface{}) {
	f := color_str.Green("✔ ") + strings.TrimSuffix(format, "\n") + "\n"
	fmt.Printf(f, a...)
}

func (c *Context) Fail(format string, a ...interface{}) {
	f := color_str.Red("✗ ") + strings.TrimSuffix(format, "\n") + "\n"
	fmt.Printf(f, a...)
}
