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

package userinfo

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"strconv"
	"strings"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors"
	"github.com/erda-project/erda/pkg/desensitize"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/ucauth"
)

type config struct {
	Order int `default:"900"`
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger
	uc  *ucauth.UCClient
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.uc = ucauth.NewUCClient(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
	if conf.OryEnabled() {
		p.uc = ucauth.NewUCClient(conf.OryKratosPrivateAddr(), conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
		db, err := ucauth.NewDB()
		if err != nil {
			return err
		}
		p.uc.SetDBClient(db)
	}
	return nil
}

var _ interceptors.Interface = (*provider)(nil)

func (p *provider) List() []*interceptors.Interceptor {
	return []*interceptors.Interceptor{{Order: p.Cfg.Order, Wrapper: p.Interceptor}}
}

func getHeaderValue(v string) string {
	i := strings.Index(v, ";")
	if i < 0 {
		return strings.TrimSpace(strings.ToLower(v))
	}
	return strings.TrimSpace(strings.ToLower(v[0:i]))
}

type responseWriterWithUserInfo struct {
	http.ResponseWriter
	buf        *bytes.Buffer
	statusCode int
	rawWrite   bool
	checked    bool
}

func (rw *responseWriterWithUserInfo) isRawWrite() bool {
	if !rw.checked {
		header := rw.Header()
		if getHeaderValue(header.Get(("Content-Type"))) != "application/json" ||
			getHeaderValue(header.Get(("Content-Disposition"))) == "attachment" ||
			getHeaderValue(header.Get(("Transfer-Encoding"))) == "chunked" {
			rw.rawWrite = true
		} else if ce := getHeaderValue(header.Get(("Content-Encoding"))); len(ce) > 0 && ce != "identity" {
			rw.rawWrite = true
		} else {
			rw.buf = &bytes.Buffer{}
			rw.rawWrite = false
		}
		rw.checked = true
	}
	return rw.rawWrite
}

func (rw *responseWriterWithUserInfo) Write(data []byte) (int, error) {
	if rw.isRawWrite() {
		return rw.ResponseWriter.Write(data)
	}
	return rw.buf.Write(data)
}

func (rw *responseWriterWithUserInfo) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	hijacker, ok := rw.ResponseWriter.(http.Hijacker)
	if ok {
		return hijacker.Hijack()
	}
	return nil, nil, fmt.Errorf("%T does not implement http.Hijacker", rw.ResponseWriter)
}

func (rw *responseWriterWithUserInfo) Flush() {
	if rw.isRawWrite() {
		if f, ok := rw.ResponseWriter.(http.Flusher); ok {
			f.Flush()
		}
	}
}

func (rw *responseWriterWithUserInfo) WriteHeader(statusCode int) {
	if rw.isRawWrite() {
		rw.ResponseWriter.WriteHeader(statusCode)
		return
	}
	rw.statusCode = statusCode
}

func (p *provider) Interceptor(h http.HandlerFunc) http.HandlerFunc {
	return func(rw http.ResponseWriter, r *http.Request) {
		rwu := &responseWriterWithUserInfo{
			ResponseWriter: rw,
			rawWrite:       true,
		}
		h(rwu, r)
		if !rwu.rawWrite {
			body := rwu.buf.Bytes()
			var data map[string]interface{}
			err := json.Unmarshal(body, &data)
			if err == nil {
				userIDs := getUserIDs(data)
				if userIDs != nil {
					if newBody := p.userInfoRetriever(r, data, userIDs); newBody != nil {
						body = newBody
					}
				}
			}
			rw.Header().Set("Content-Length", strconv.FormatInt(int64(len(body)), 10))
			if rwu.statusCode > 0 {
				rw.WriteHeader(rwu.statusCode)
			}
			_, err = rw.Write(body)
			if err != nil {
				p.Log.Error(err)
			}
			if f, ok := rw.(http.Flusher); ok {
				f.Flush()
			}
		}
	}
}

func (p *provider) userInfoRetriever(r *http.Request, data map[string]interface{}, userIDs []string) []byte {
	desensitized, _ := strconv.ParseBool(r.Header.Get(httputil.UserInfoDesensitizedHeader))
	user, err := p.getUsers(userIDs, desensitized)
	if err != nil {
		p.Log.Error(err)
	} else {
		data["userInfo"] = user
		if newbody, err := json.Marshal(data); err == nil {
			return newbody
		}
	}
	return nil
}

func getUserIDs(body map[string]interface{}) []string {
	userIDs, ok := body["userIDs"]
	if !ok {
		return nil
	}
	switch v := userIDs.(type) {
	case []interface{}:
		ids := make([]string, len(v), len(v))
		for i, id := range v {
			idstr, ok := id.(string)
			if !ok {
				return nil
			}
			ids[i] = idstr
		}
		return ids
	case []string:
		return v
	}
	return nil
}

func (p *provider) getUsers(IDs []string, needDesensitize bool) (map[string]apistructs.UserInfo, error) {
	b, err := p.uc.FindUsers(IDs)
	if err != nil {
		return nil, err
	}

	users := make(map[string]apistructs.UserInfo, len(b))
	if needDesensitize {
		for i := range b {
			users[string(b[i].ID)] = apistructs.UserInfo{
				ID:     "",
				Email:  desensitize.Email(b[i].Email),
				Phone:  desensitize.Mobile(b[i].Phone),
				Avatar: b[i].AvatarURL,
				Name:   desensitize.Name(b[i].Name),
				Nick:   desensitize.Name(b[i].Nick),
			}
		}
	} else {
		for i := range b {
			users[string(b[i].ID)] = apistructs.UserInfo{
				ID:     string(b[i].ID),
				Email:  b[i].Email,
				Phone:  b[i].Phone,
				Avatar: b[i].AvatarURL,
				Name:   b[i].Name,
				Nick:   b[i].Nick,
			}
		}
	}
	for _, userID := range IDs {
		_, exist := users[userID]
		if !exist {
			users[userID] = apistructs.UserInfo{
				ID:     userID,
				Email:  "",
				Phone:  "",
				Avatar: "",
				Name:   "用户已注销",
				Nick:   "用户已注销",
			}
		}
	}
	return users, nil
}

func init() {
	servicehub.Register("openapi-interceptor-user-info", &servicehub.Spec{
		Services:   []string{"openapi-interceptor-user-info"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
