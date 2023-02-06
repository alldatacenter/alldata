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

package dingding_worknotice

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"net/url"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/monitor"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber/dingding"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

var (
	// ErrSend 发送钉钉消息失败
	ErrSend = errors.New("send DINGDING fail")

	// ErrBadURL 钉钉 URL 格式不正确
	ErrBadURL = errors.New("bad DINGDING URL")
)

// https://open-doc.dingtalk.com/microapp/serverapi2/pgoxpy

// {"msgtype":"text","text":{"content":"消息内容"}}

type dst []struct {
	URL        string   `json:"url"`
	AgentID    string   `json:"agent_id"`
	UserIDList []string `json:"userid_list"`
}

// WorkNoticeSubscriber 输出插件：工作通知
type WorkNoticeSubscriber struct {
	proxy     string
	messenger pb.NotifyServiceServer
}

// New 创建 WorkNoticeSubscriber
func New(proxy string, messenger pb.NotifyServiceServer) subscriber.Subscriber {
	return WorkNoticeSubscriber{
		proxy:     proxy,
		messenger: messenger,
	}
}

// Name WorkNoticeSubscriber 在 label 中对应的名字
func (WorkNoticeSubscriber) Name() string {
	return "DINGDING-WORKNOTICE"
}

// Status 目前没有用
func (WorkNoticeSubscriber) Status() interface{} {
	return nil
}

// Publish 发送 工作通知
func (s WorkNoticeSubscriber) Publish(dest, content string, time int64, msg *types.Message) []error {
	monitor.Notify(monitor.MonitorInfo{Tp: monitor.DINGDINGWorkNoticeOutput})
	errs := []error{}

	content = dingding.PrettyPrint(content, false)

	dsts := dst{}
	if err := json.NewDecoder(strings.NewReader(dest)).Decode(&dsts); err != nil {
		return []error{errors.Wrap(err, "illegal dest")}
	}
	for _, dst := range dsts {
		if err := s.worknoticeSend(dst.URL, dst.AgentID, dst.UserIDList, content); err != nil {
			errs = append(errs, err)
		}
	}
	if len(errs) > 0 {
		msg.CreateHistory.Status = "failed"
	}
	subscriber.SaveNotifyHistories(msg.CreateHistory, s.messenger)
	return errs
}

func (s WorkNoticeSubscriber) worknoticeSend(u, agentID string, userIDList []string, content string) error {
	parsed, err := url.Parse(u)
	if err != nil {
		return errors.Wrap(ErrBadURL, err.Error())
	}
	tokenQuery, ok := parsed.Query()["access_token"]
	if !ok {
		err = errors.Errorf("WorkNotice publish: %v, not provide access_token", u)
		logrus.Error(err)
		return errors.Wrap(ErrBadURL, err.Error())
	}
	if len(tokenQuery) == 0 {
		err = errors.Errorf("WorkNotice publish: %v, not provide access_token", u)
		logrus.Error(err)
		return errors.Wrap(ErrBadURL, err.Error())
	}
	token := tokenQuery[0]
	body := make(url.Values)
	body.Set("agent_id", agentID)
	body.Set("userid_list", strings.Join(userIDList, ","))
	msg := map[string]interface{}{
		"msgtype": "text", "text": map[string]interface{}{
			"content": dingding.DingPrint(content),
		},
	}
	msgstr, _ := json.Marshal(msg)
	body.Set("msg", string(msgstr))
	var buf bytes.Buffer
	resp, err := httpclient.New(httpclient.WithHTTPS(), httpclient.WithProxy(s.proxy)).
		Post(parsed.Host).
		Path(parsed.Path).
		Param("access_token", token).
		FormBody(body).Do().
		Body(&buf)
	if err != nil {
		err = errors.Errorf("WorkNotice publish: %v, err: %v", u, err)
		logrus.Error(err)
		return errors.Wrap(ErrSend, err.Error())
	}
	if !resp.IsOK() {
		err = errors.Errorf("WorkNotice publish: %v, httpcode: %d", u, resp.StatusCode())
		logrus.Error(err)
		return errors.Wrap(ErrSend, err.Error())
	}
	bodybuf, err := ioutil.ReadAll(&buf)
	if err != nil {
		err = errors.Errorf("WorkNotice publish: %v, err: %v", u, err)
		logrus.Error(err)
		return errors.Wrap(ErrSend, err.Error())
	}
	var v map[string]interface{}
	if err = json.Unmarshal(bodybuf, &v); err != nil {
		err = errors.Errorf("WorkNotice publish: %v, err: %v", u, err)
		logrus.Error(err)
		return errors.Wrap(ErrSend, err.Error())
	}
	errcode, ok := v["errcode"]
	if !ok {
		logrus.Warningf("WorkNotice publish: %v, no errcode in resp body: %v", u, string(bodybuf))
		return nil
	}
	errmsg, ok := v["errmsg"]
	if !ok {
		return nil
	}
	errcodeNum := int(errcode.(float64))
	if errcodeNum != 0 {
		err = errors.Errorf("WorkNotice publish: %v, errcode: %d, errmsg: %s", u, errcodeNum, errmsg.(string))
		logrus.Error(err)
		return errors.Wrap(ErrSend, err.Error())
	}
	return nil
}
