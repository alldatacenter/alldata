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

package spec

import (
	"encoding/json"
	"fmt"
	"net/http"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

type APIs []Spec

// TODO: add cache
func (o APIs) Find(req *http.Request) *Spec {
	for _, spec := range o {
		m := NewMatcher(&spec)
		if m.MatchMethod(req.Method) && m.MatchPath(req.URL.EscapedPath()) {
			return &spec
		}
	}
	return nil
}

func (o APIs) FindOriginPath(req *http.Request) *Spec {
	for _, spec := range o {
		m := NewMatcher(&spec)
		if m.MatchMethod(req.Method) && m.MatchPath(req.Header.Get("Origin-Path")) {
			return &spec
		}
	}
	return nil

}

type Matcher struct {
	spec *Spec
}

func NewMatcher(spec *Spec) *Matcher {
	return &Matcher{spec}
}

func (m *Matcher) MatchMethod(method string) bool {
	return m.spec.Method == "" || strings.ToLower(method) == strings.ToLower(m.spec.Method)
}

func (m *Matcher) MatchPath(path string) bool {
	return m.spec.Path.Match(path)
}
func (m *Matcher) MatchBackendPath(path string) bool {
	return m.spec.BackendPath.Match(path)
}

type Scheme int

const (
	WS Scheme = iota
	HTTP
)

func (t Scheme) String() string {
	switch t {
	case HTTP:
		return "http"
	case WS:
		return "ws"
	default:
		panic("should not be here")
	}
}
func SchemeFromString(s string) (Scheme, error) {
	switch s {
	case "http":
		return HTTP, nil
	case "ws":
		return WS, nil
	default:
		return HTTP, errors.New("illegal scheme")
	}
}

type Spec struct {
	Path            *Path
	BackendPath     *Path
	Host            string
	Scheme          Scheme
	Method          string
	Custom          func(rw http.ResponseWriter, req *http.Request)
	CustomResponse  func(*http.Response) error
	Audit           func(*AuditContext) error
	NeedDesensitize bool // 是否需要对返回的 userinfo 进行脱敏处理，id也会被脱敏
	CheckLogin      bool
	TryCheckLogin   bool // 和CheckLogin区别为如果不登录也会通过,只是没有user-id
	CheckToken      bool
	CheckBasicAuth  bool
	ChunkAPI        bool
	// `Host` 是API原始配置
	// 分别转化为 marathon 和 k8s 的host
	MarathonHost string
	K8SHost      string
	Port         int
}

func (s *Spec) Validate() error {
	return nil
}

/*
Path:
由<xxx>组成url中可变部分
比如:
/dice/<company>/<project>/login
*/
var replaceElem = regexp.MustCompile("<[^*]*?>")

type Path struct {
	path      string
	parts     map[int]string // TODO: better name
	regexPath *regexp.Regexp
}

func NewPath(path string) *Path {
	path = polishPath(path)
	p := &Path{path: path, parts: map[int]string{}}
	p.parse()
	return p
}

// parse /a/<b>/<c> -> {"b":1, "c":2}
func (p *Path) parse() {
	parts := strings.Split(p.path, "/")
	for i, part := range parts {
		if strings.HasPrefix(part, "<") && strings.HasSuffix(part, ">") {
			p.parts[i] = part[1 : len(part)-1]
		}
	}
	regexpath := "^" + replaceElem.ReplaceAllString(p.path, "[^/]+?") + "[/]?$"
	regexpath = strings.Replace(regexpath, "<*>", ".+?", -1)
	p.regexPath = regexp.MustCompile(regexpath)
}

func (p *Path) String() string {
	return p.path
}

func (p *Path) Vars(realPath string) map[string]string {
	r := map[string]string{}
	realPath = polishPath(realPath)
	idx := 0

	for {
		idx++
		realPath = strings.TrimLeft(realPath, "/")
		parts := strings.SplitN(realPath, "/", 2)

		part, ok := p.parts[idx]
		if !ok {
			if len(parts) == 2 {
				realPath = parts[1]
			}
		}

		if part == "*" {
			r[part] = realPath
			break
		} else if len(parts) == 2 {
			r[part] = parts[0]
			realPath = parts[1]
		} else if len(parts) == 1 {
			r[part] = parts[0]
			break
		}
	}
	return r
}

func (p *Path) Match(realPath string) bool {
	realPath = polishPath(realPath)
	return p.regexPath.MatchString(realPath)
}

func (p *Path) Fill(vars map[string]string) string {
	r := p.path
	for k, v := range vars {
		r = strings.Replace(r, "<"+k+">", v, -1)
	}
	return r
}

func polishPath(path string) string {
	if !strings.HasPrefix(path, "/") {
		path = "/" + path
	}
	return path
}

type AuditContext struct {
	UrlParams map[string]string
	UserID    string
	OrgID     int64
	Request   *http.Request
	Response  *http.Response
	Bundle    *bundle.Bundle
	BeginTime time.Time
	EndTime   time.Time
	Result    apistructs.Result
	ClientIP  string
	UserAgent string
	Cache     *sync.Map
}

func (ctx *AuditContext) GetParamInt64(key string) (int64, error) {
	value, err := ctx.GetParamString(key)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(value, 10, 64)
}

func (ctx *AuditContext) GetParamUInt64(key string) (uint64, error) {
	value, err := ctx.GetParamString(key)
	if err != nil {
		return 0, err
	}
	return strconv.ParseUint(value, 10, 64)
}

func (ctx *AuditContext) GetParamString(key string) (string, error) {
	value, ok := ctx.UrlParams[key]
	if !ok {
		return "", fmt.Errorf("key %s not exist", key)
	}
	return value, nil
}

func (ctx *AuditContext) GetOrg(idObject interface{}) (*apistructs.OrgDTO, error) {
	idStr := fmt.Sprintf("%v", idObject)
	cacheKey := "org-" + idStr
	var result *apistructs.OrgDTO
	cacheObject, ok := ctx.Cache.Load(cacheKey)
	if !ok {
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			return nil, err
		}
		orgDTO, err := ctx.Bundle.GetDopOrg(id)
		if err != nil {
			return nil, err
		}
		result = orgDTO
		ctx.Cache.Store(cacheKey, orgDTO)
	} else {
		result = cacheObject.(*apistructs.OrgDTO)
	}
	return result, nil
}

func (ctx *AuditContext) GetProject(idObject interface{}) (*apistructs.ProjectDTO, error) {
	idStr := fmt.Sprintf("%v", idObject)
	cacheKey := "project-" + idStr
	var result *apistructs.ProjectDTO
	cacheObject, ok := ctx.Cache.Load(cacheKey)
	if !ok {
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			return nil, err
		}
		projectDTO, err := ctx.Bundle.GetProject(id)
		if err != nil {
			return nil, err
		}
		result = projectDTO
		ctx.Cache.Store(cacheKey, projectDTO)
	} else {
		result = cacheObject.(*apistructs.ProjectDTO)
	}
	return result, nil
}

func (ctx *AuditContext) GetApp(idObject interface{}) (*apistructs.ApplicationDTO, error) {
	idStr := fmt.Sprintf("%v", idObject)
	cacheKey := "app-" + idStr
	var result *apistructs.ApplicationDTO
	cacheObject, ok := ctx.Cache.Load(cacheKey)
	if !ok {
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			return nil, err
		}
		applicationDTO, err := ctx.Bundle.GetApp(id)
		if err != nil {
			return nil, err
		}
		result = applicationDTO
		ctx.Cache.Store(cacheKey, applicationDTO)
	} else {
		result = cacheObject.(*apistructs.ApplicationDTO)
	}
	return result, nil
}

func (ctx *AuditContext) BindRequestData(body interface{}) error {
	if err := json.NewDecoder(ctx.Request.Body).Decode(body); err != nil {
		return fmt.Errorf("can't decode request body err:%s", err)
	}
	return nil
}

func (ctx *AuditContext) BindResponseData(body interface{}) error {
	if err := json.NewDecoder(ctx.Response.Body).Decode(body); err != nil {
		return fmt.Errorf("can't decode response body err:%s", err)
	}
	return nil
}

func (ctx *AuditContext) BindResponseHeader(key string) string {
	return ctx.Response.Header.Get(key)
}

// CreateAudit 创建审计事件
func (ctx *AuditContext) CreateAudit(audit *apistructs.Audit) error {
	if err := ctx.setScopeIDsAndScopeName(audit); err != nil {
		logrus.Errorf("try to get project and app id failed: %v", err)
	}

	if audit.OrgID == 0 {
		audit.OrgID = uint64(ctx.OrgID)
	}
	audit.UserID = ctx.UserID
	audit.StartTime = strconv.FormatInt(ctx.BeginTime.Unix(), 10)
	audit.EndTime = strconv.FormatInt(ctx.EndTime.Unix(), 10)
	audit.Result = ctx.Result
	audit.ClientIP = ctx.ClientIP
	audit.UserAgent = ctx.UserAgent

	return ctx.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{
		Audit: *audit,
	})
}

func (ctx *AuditContext) setScopeIDsAndScopeName(audit *apistructs.Audit) error {
	switch audit.ScopeType {
	case apistructs.OrgScope:
		if audit.OrgID == 0 {
			audit.OrgID = audit.ScopeID
			org, err := ctx.GetOrg(audit.ScopeID)
			if err != nil {
				return err
			}
			audit.Context["orgName"] = org.Name
		}
	case apistructs.ProjectScope:
		if audit.ProjectID == 0 {
			project, err := ctx.GetProject(audit.ScopeID)
			if err != nil {
				return err
			}
			audit.ProjectID = audit.ScopeID
			audit.Context["projectName"] = project.Name
		}
	case apistructs.AppScope:
		if audit.AppID == 0 || audit.ProjectID == 0 {
			app, err := ctx.GetApp(audit.ScopeID)
			if err != nil {
				return err
			}
			audit.ProjectID = app.ProjectID
			audit.AppID = audit.ScopeID
			project, err := ctx.GetProject(app.ProjectID)
			if err != nil {
				return err
			}
			audit.Context["projectName"] = project.Name
			audit.Context["appName"] = app.Name
		}
	}

	return nil
}
