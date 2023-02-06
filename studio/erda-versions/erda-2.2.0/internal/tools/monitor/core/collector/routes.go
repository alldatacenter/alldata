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
	"bufio"
	"fmt"
	"hash/adler32"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/buger/jsonparser"
	"github.com/labstack/echo"
	"github.com/rakyll/statik/fs"

	"github.com/erda-project/erda-infra/providers/httpserver"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/collector/statik" // include static files
)

//go:generate statik -src=./ -ns "monitor/metrics-collector" -include=*.jpg,*.txt,*.html,*.css,*.js
func (p *provider) intRoute(r httpserver.Router) error {
	assets, err := fs.NewWithNamespace("monitor/metrics-collector")
	if err != nil {
		return fmt.Errorf("fail to init file system: %s", err)
	}
	fileSystem := http.FileSystem(assets)
	r.File("/ta.js", "/ta.js", httpserver.WithFileSystem(fileSystem))

	// browser and mobile metrics
	r.POST("/collect", p.collectAnalytics) // compatible for legacy
	r.POST("/collect/analytics", p.collectAnalytics)

	// logs and metrics
	r.POST("/collect/:metric", p.collectMetric, p.auth.basicAuth)
	r.POST("/collect/notify-metrics", p.collectNotifyMetric, p.auth.basicAuth)
	r.POST("/collect/logs/:source", p.collectLogs, p.auth.basicAuth)

	// api version one
	// authenticate with access keys
	//todo. deprecated
	groupV1 := "/api/v1/deprecated"
	{
		r.POST(groupV1+"/collect/:metric", p.collectMetric, p.auth.keyAuth())
		r.POST(groupV1+"/collect/logs/:source", p.collectLogs, p.auth.keyAuth())
	}
	return nil
}

func (p *provider) collectLogs(ctx echo.Context) error {
	source := ctx.Param("source")
	if source == "" {
		return ctx.NoContent(http.StatusBadRequest)
	}
	name := source + "_log"

	body, err := ReadRequestBody(ctx.Request())

	if err != nil {
		p.Logger.Errorf("fail to read request body, err: %v", err)
		return err
	}
	if !isJSONArray(body) {
		p.Logger.Warnf("the body is not a json array. body=%s", string(body))
		return ctx.NoContent(http.StatusNoContent)
	}

	if _, err := jsonparser.ArrayEach(body, func(value []byte, dataType jsonparser.ValueType, offset int, err error) {
		if err != nil {
			p.Logger.Errorf("fail to json parse, err: %v", err)
			return
		}
		if err := p.send(name, value); err != nil {
			p.Logger.Errorf("fail to send msg to kafka, name: %s, err: %v", name, err)
		}
	}); err != nil {
		return err
	}
	return ctx.NoContent(http.StatusNoContent)
}

func (p *provider) collectAnalytics(ctx echo.Context) error {
	params, err := ctx.FormParams()
	if err != nil {
		return ctx.NoContent(http.StatusBadRequest)
	}
	ak := ctx.FormValue("ak") // ak as terminus key
	ai := ctx.FormValue("ai")

	if p.Cfg.TaSamplingRate <= 0 {
		return ctx.NoContent(http.StatusOK)
	}
	if p.Cfg.TaSamplingRate < 99.999999 { // 99.999999认为是100，避免浮点数精度问题导致 100.0<100.0 为true
		hash := float64(adler32.Checksum([]byte(ak)) % 100)
		if p.Cfg.TaSamplingRate <= hash {
			return ctx.NoContent(http.StatusOK)
		}
	}
	cid := ctx.FormValue("cid")
	if ak == "" || cid == "" {
		return ctx.String(http.StatusBadRequest, "ak and cid are required")
	}
	uid := ctx.FormValue("uid")

	var ip string
	// k8s 的一个bug，使用 X-Original-Forwarded-For 而不是 X-Forwarded-For
	ipAddrs := ctx.Request().Header.Get("X-Original-Forwarded-For")
	if ipAddrs != "" {
		ip = strings.TrimSpace(strings.SplitN(ipAddrs, ",", 2)[0])
	} else {
		ip = ctx.RealIP()
	}

	// Data from Client
	for _, v := range params["data"] {
		qs, err := url.ParseQuery(v)
		if err != nil {
			return ctx.NoContent(http.StatusBadRequest)
		}
		qs.Set("date", strconv.FormatInt(time.Now().UnixNano()/int64(time.Millisecond), 10))

		if qs.Get("ua") == "" {
			ua := ctx.Request().Header.Get("User-Agent")
			qs.Set("ua", ua)
		}
		var message string
		if len(ai) > 0 {
			message = fmt.Sprintf(`%s,%s,%s,%s,%s,%s`,
				ak, cid, uid, ip, qs.Encode(), ai)
		} else {
			message = fmt.Sprintf(`%s,%s,%s,%s,%s`,
				ak, cid, uid, ip, qs.Encode())
		}
		_ = p.send("analytics", []byte(message))
	}

	return ctx.NoContent(http.StatusNoContent)
}

func (p *provider) collectMetric(ctx echo.Context) error {
	contentType := ctx.Request().Header.Get("Content-Type")
	if strings.Contains(contentType, "application/json") {
		return p.parseJSON(ctx, ctx.Param("metric"))
	}
	return p.parseLine(ctx, ctx.Param("metric"))
}

func (p *provider) collectNotifyMetric(ctx echo.Context) error {
	contentType := ctx.Request().Header.Get("Content-Type")
	if strings.Contains(contentType, "application/json") {
		return p.parseJSON(ctx, ctx.Param("erda-notify-event"))
	}
	return nil
}

func (p *provider) parseJSON(ctx echo.Context, name string) error {
	body, err := ReadRequestBody(ctx.Request())
	if err != nil {
		return err
	}
	if _, err := jsonparser.ArrayEach(body, func(value []byte, dataType jsonparser.ValueType, offset int, err error) {
		if err == nil {
			err = p.send(name, value)
		}
	}, name); err != nil {
		return err
	}

	return ctx.NoContent(http.StatusNoContent)
}

func (p *provider) parseLine(ctx echo.Context, name string) error {
	reader, err := ReadRequestBodyReader(ctx.Request())
	if err != nil {
		return err
	}
	buf := bufio.NewReader(reader)
	for {
		line, err := buf.ReadString('\n')
		if e := p.send(name, []byte(line)); e != nil {
			return e
		}
		if err != nil || err == io.EOF {
			break
		}
	}
	return ctx.NoContent(http.StatusNoContent)
}
