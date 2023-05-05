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

package engine

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"text/template"
	"time"

	"github.com/jinzhu/now"
	"github.com/mitchellh/mapstructure"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

const (
	ns2ms   = 1000000
	datefmt = "01月02日"
)

var (
	hc *httpclient.HTTPClient

	// const cfg

	templates = map[string]string{
		"email":    `<!DOCTYPE html><html lang="zh"><head><meta charset="UTF-8"><title>{{.DateDisplay}} {{.OrgName}}监控{{.ReportType}}</title>></head><body><h2>{{.OrgName}}{{.ReportType}}</h2><p>{{.DateDisplay}} 监控{{.ReportType}}已生成，<a href="{{.HistoryURL}}">点击查看</a></p></body></html>`,
		"dingding": "## {{.OrgName}}{{.ReportType}}\n{{.DateDisplay}} 监控{{.ReportType}}已生成，[点击查看]({{.HistoryURL}})",
		// 站内信
		"mbox": "## {{.OrgName}}{{.ReportType}}\n{{.DateDisplay}} 监控{{.ReportType}}已生成，[点击查看]({{.HistoryURL}})",
	}

	rightNow time.Time
	// 昨天的23:59:59
	endTimestamp time.Time
	sevenDayAgo  time.Time
)

func init() {
	rightNow = time.Now()
	endTimestamp = now.BeginningOfDay().Add(-time.Second)
	sevenDayAgo = rightNow.AddDate(0, 0, -7)
	hc = httpclient.New()
}

type viewConfig struct {
	W    int    `json:"w"`
	H    int    `json:"h"`
	X    int    `json:"x"`
	Y    int    `json:"y"`
	I    string `json:"i"`
	View struct {
		Title          string      `json:"title"`
		Description    string      `json:"description"`
		ChartType      string      `json:"chartType"`
		DataSourceType string      `json:"dataSourceType"`
		API            apiEntity   `json:"api"`
		Config         interface{} `json:"config"`
		I18n           interface{} `json:"i18n"`
	} `json:"view"`
}

type reqConfig struct {
	Host    string
	Method  string
	Path    string
	Param   map[string]interface{}
	Headers map[string]string
	Body    interface{}
}

type Report struct {
	cfg        *config
	resource   *Resource
	DataConfig []*viewData
}

func New(cfg *config) *Report {
	return &Report{
		cfg: cfg,
	}
}

// 1. fetch report template
// 2. get screenshot from headless
// 3. fetch telemetry to decode view_config add a history record
// 4. combine history URI and image as email html
// 5. send email to notify group
func (r *Report) Run(ctx context.Context) error {
	if err := r.getResource(ctx); err != nil {
		return errors.Wrap(err, "create resource error")
	}

	img, err := r.getImage(ctx)
	if err != nil {
		return errors.Wrap(err, "get screenshot image error")
	}

	history, err := r.record(ctx)
	if err != nil {
		return errors.Wrap(err, "push record error")
	}

	event, err := r.createEventbox(ctx, img, history)
	if err != nil {
		return errors.Wrap(err, "createEventbox error")
	}

	if err := r.send(ctx, event); err != nil {
		return errors.Wrap(err, "send to eventbox error")
	}
	return nil
}

func (r *Report) getResource(ctx context.Context) error {
	var report reportTaskEntity

	if err := r.doRequest(&reqConfig{
		Method: "GET",
		Path:   reportTaskPath + "/" + r.cfg.ReportID,
		Param:  nil,
		Body:   nil,
	}, &report); err != nil {
		return err
	}

	resource, err := NewResource(&report)
	if err != nil {
		return err
	}
	r.resource = resource

	if err := r.CurrentFetchAndConvert(ctx); err != nil {
		return err
	}
	return nil
}

type tmplParams struct {
	HistoryURL  string
	ReportType  string
	DateDisplay string
	OrgName     string
}

func (r *Report) createEventbox(ctx context.Context, img []byte, history *historyEntity) (body *eventboxEntity, err error) {
	var (
		rt          string
		dateDisplay string
	)
	switch r.resource.ReportTask.Type {
	case "daily":
		rt = "日报"
		dateDisplay = endTimestamp.Format(datefmt)
	case "weekly":
		rt = "周报"
		dateDisplay = fmt.Sprintf("%s-%s", sevenDayAgo.Format(datefmt), endTimestamp.Format(datefmt))
	// case "monthly":
	// 	rt = "月报"
	// 	dateDisplay = fmt.Sprintf("%s-%s", rightNow.AddDate(0, -1, 0).Format(datefmt), rightNow.Format(datefmt))
	default:
		return nil, errors.New("unsupported report type")
	}
	target := fmt.Sprintf("%s/r/report/%d?recordId=%d", r.cfg.DomainAddr, r.resource.ReportTask.ID, history.ID)
	allType := strings.Split(r.resource.ReportTask.Notifier.GroupType, ",")
	channels := make([]notifyChannel, len(allType))
	for i := 0; i < len(allType); i++ {
		tmpl, ok := templates[allType[i]]
		if !ok {
			logrus.Errorf("not support %s", allType[i])
			continue
		}
		d, err := renderContent(tmpl, &tmplParams{target, rt, dateDisplay, r.cfg.OrgName})
		if err != nil {
			return nil, err
		}
		channels[i] = notifyChannel{
			Name:     allType[i],
			Template: d,
			Params:   make(map[string]interface{}),
		}
	}

	orgID, err := strconv.Atoi(r.resource.ReportTask.ScopeID)
	if err != nil {
		return nil, err
	}
	title := fmt.Sprintf("%s %s监控%s", dateDisplay, r.cfg.OrgName, rt)
	eventboxJsonBody := &eventboxEntity{
		Sender: "adapter",
		Content: map[string]interface{}{
			"sourceName":            "unknown",
			"sourceType":            "unknown",
			"sourceId":              "1",
			"notifyName":            title,
			"notifyItemDisplayName": title,
			"module":                "monitor",
			"orgID":                 orgID,
			"channels":              channels,
		},
		Labels: map[string]int{"GROUP": r.resource.ReportTask.Notifier.GroupID},
	}

	return eventboxJsonBody, nil
}

func renderContent(tmpl string, params *tmplParams) (d string, err error) {
	var res bytes.Buffer
	writer := bufio.NewWriter(&res)
	t, err := template.New("report").Parse(tmpl)
	if err != nil {
		return "", err
	}
	if err := t.Execute(&res, params); err != nil {
		return "", err
	}
	if err := writer.Flush(); err != nil {
		return "", err
	}
	return res.String(), nil
}

func (r *Report) send(ctx context.Context, body *eventboxEntity) error {
	if err := r.doRequest(&reqConfig{
		Host:    r.cfg.CoreServicesAddr,
		Method:  "POST",
		Path:    eventboxPath,
		Param:   nil,
		Headers: nil,
		Body:    body,
	}, nil); err != nil {
		return err
	}
	return nil
}

func (r *Report) record(ctx context.Context) (historyID *historyEntity, err error) {
	// add to blocks
	blockReq := blockEntity{
		ScopeID:    r.resource.ReportTask.ScopeID,
		Scope:      r.resource.ReportTask.Scope,
		Desc:       r.resource.Block.Desc,
		Name:       r.resource.Block.Name,
		ViewConfig: r.resource.Block.ViewConfig,
		DataConfig: r.DataConfig,
	}
	var blockResp blockEntity
	if err := r.doRequest(&reqConfig{
		Method:  "POST",
		Path:    userBlocksPath,
		Param:   nil,
		Headers: nil,
		Body:    &blockReq,
	}, &blockResp); err != nil {
		return nil, err
	}

	// upload to history
	historyReq := historyEntity{
		baseEntity:  baseEntity{ScopeID: r.resource.ReportTask.ScopeID, Scope: r.resource.ReportTask.Scope},
		TaskID:      r.resource.ReportTask.ID,
		DashboardID: blockResp.ID,
		End:         int(endTimestamp.UnixNano() / ns2ms),
	}
	var historyResp historyEntity
	if err := r.doRequest(&reqConfig{
		Method:  "POST",
		Path:    reportHistoryPath,
		Param:   nil,
		Headers: nil,
		Body:    &historyReq,
	}, &historyResp); err != nil {
		return nil, err
	}
	return &historyResp, nil
}

func (r *Report) getImage(ctx context.Context) ([]byte, error) {
	return nil, nil
}

func (r *Report) fetch(api *apiEntity) (data *json.RawMessage, err error) {
	if api.URL == "" {
		return nil, errors.New("url is empty")
	}
	reqcfg := &reqConfig{
		Method:  api.Method,
		Path:    api.URL,
		Param:   api.Query,
		Headers: api.Header,
		Body:    api.Body,
	}
	var metrics json.RawMessage
	if err := r.doRequest(reqcfg, &metrics); err != nil {
		return nil, err
	}
	return &metrics, nil
}

func (r *Report) CurrentFetchAndConvert(ctx context.Context) error {
	var all []*viewConfig
	if err := mapstructure.Decode(r.resource.Block.ViewConfig, &all); err != nil {
		return err
	} else if len(all) == 0 {
		return errors.New("view_config is empty, pls check")
	}
	r.mapAPIQuery(all)
	r.DataConfig = make([]*viewData, len(all))

	var wg sync.WaitGroup
	wg.Add(len(all))
	for i := 0; i < len(all); i++ {
		go func(idx int) {
			defer wg.Done()
			view := all[idx]
			staticData, err := r.fetch(&view.View.API)
			if err != nil {
				logrus.WithError(err).Error("fetch telemetry data error")
				return
			}
			r.DataConfig[idx] = &viewData{
				I:          view.I,
				StaticData: staticData,
			}
		}(i)
	}
	wg.Wait()
	// update view_config
	r.resource.Block.ViewConfig = all
	return nil

}

// 特殊处理查询参数
func (r *Report) mapAPIQuery(vc []*viewConfig) {
	todayMidnight := now.BeginningOfDay().UnixNano() / 1000000
	for i := 0; i < len(vc); i++ {
		q := vc[i].View.API.Query
		if val, ok := q["filter_org_name"]; ok && val == "" {
			q["filter_org_name"] = r.cfg.OrgName
		}
		if val, ok := q["filter_dice_org_id"]; ok && val == "" {
			q["filter_dice_org_id"] = r.resource.ReportTask.ScopeID
		}
		if _, ok := q["timestamp"]; !ok {
			q["timestamp"] = todayMidnight
		}
	}
}

func (r *Report) doRequest(reqcfg *reqConfig, result interface{}) error {
	host := r.cfg.MonitorAddr
	if reqcfg.Host != "" {
		host = reqcfg.Host
	}
	var req *httpclient.Request
	switch reqcfg.Method {
	case "", "GET":
		req = hc.Get(host)
	case "POST":
		req = hc.Post(host)
	default:
		return errors.New("only support GET and POST")
	}
	param := url.Values{}
	if reqcfg.Param != nil {
		for k, v := range reqcfg.Param {
			err := addParam(param, k, v)
			if err != nil {
				return errors.Wrap(err, "add param error")
			}
		}
	}
	headers := http.Header{}
	if reqcfg.Headers != nil {
		for k, v := range reqcfg.Headers {
			headers.Add(k, v)
		}
	}

	req = req.Headers(headers).Path(reqcfg.Path).Params(param).JSONBody(reqcfg.Body)
	br := baseResponse{}
	br.SetDataReceiver(&result)

	var (
		resp *httpclient.Response
		err  error
	)
	if result == nil {
		resp, err = req.Do().DiscardBody()
	} else {
		resp, err = req.Do().JSON(&br)
	}
	if err != nil {
		return err
	}

	if !resp.IsOK() {
		if reqcfg.Method == "POST" && logrus.GetLevel() == logrus.DebugLevel {
			d, _ := json.Marshal(&reqcfg.Body)
			fmt.Printf("\n******\n%s\n******\n", string(d))
		}
		url_ := host + reqcfg.Path
		return errors.Wrapf(br, fmt.Sprintf("do %s request to %s error, http code is %d", reqcfg.Method, url_, resp.StatusCode()))
	}
	return nil
}

func addParam(param url.Values, k string, v interface{}) error {
	switch v.(type) {
	case []interface{}:
		for _, e := range v.([]interface{}) {
			if err := addParam(param, k, e); err != nil {
				return err
			}
		}
	case string:
		param.Add(k, v.(string))
	case int, int8, int16, int32, int64:
		param.Add(k, strconv.Itoa(int(v.(int64))))
	case float64, float32:
		param.Add(k, strconv.Itoa(int(v.(float64))))
	case bool:
		param.Add(k, strconv.FormatBool(v.(bool)))
	default:
		d, _ := json.Marshal(v)
		param.Add(k, string(d))
		logrus.Infof("unknown type of param %s:%v", v, v)
		// return errors.Errorf("can not convert %v to string", v)
	}
	return nil
}
