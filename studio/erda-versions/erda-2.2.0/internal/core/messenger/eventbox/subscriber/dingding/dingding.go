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

package dingding

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"math/rand"
	"net/url"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/monitor"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/webhook"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

var (
	DINGDINGSendErr   = errors.New("send DINGDING fail")
	DINGDINGBadURLErr = errors.New("bad DINGDING URL")
)

var (
	maxContentSize = 19902
	overflowText   = "..."
)

// example dingding message:
// {
//      "msgtype": "text",
//      "text": {
//          "content": "我就是我,  @1825718XXXX 是不一样的烟火"
//      },
//      "at": {
//          "atMobiles": [
//              "1825718XXXX"
//          ],
//          "isAtAll": false
//      }
//  }
type DDContent struct {
	Content string `json:"content"`
}
type DDMarkdown struct {
	Title string `json:"title"`
	Text  string `json:"text"`
}
type DDAt struct {
	AtMobiles []string `json:"atMobiles"`
	IsAtAll   bool     `json:"isAtAll"`
}
type DDMessage struct {
	Msgtype  string      `json:"msgtype"`
	Text     *DDContent  `json:"text,omitempty"`
	Markdown *DDMarkdown `json:"markdown,omitempty"`
	At       DDAt        `json:"at"`
}

type LegacyDDDest []string

// [[A,B,C],[D,E,F],[G,H,I]] -> A,B,C 中随机选一个发送消息，如果失败则换这3个中的下一个地址来重试，D,E,F 和 G,H,I也一样
type DDDest [][]string

type DDSubscriber struct {
	proxy     string
	messenger pb.NotifyServiceServer
}

func New(proxy string, messenger pb.NotifyServiceServer) subscriber.Subscriber {
	return &DDSubscriber{
		proxy:     proxy,
		messenger: messenger,
	}
}

// example URL:https://oapi.dingtalk.com/robot/send?access_token=xxxx
func (d *DDSubscriber) Publish(dest string, content string, time int64, msg *types.Message) []error {
	monitor.Notify(monitor.MonitorInfo{Tp: monitor.DINGDINGOutput})
	errs := []error{}
	_, isWebhook := msg.Labels[types.LabelKey("WEBHOOK").NormalizeLabelKey()]
	logrus.Infof("prettyprint labels: %+v", msg.Labels) // delete me
	content = PrettyPrint(content, isWebhook)
	at, ok := msg.Labels["/AT"]
	if !ok {
		at = DDAt{[]string{}, false}
	}
	atRaw, err := json.Marshal(at)
	if err != nil {
		return []error{errors.New("illegal [AT] label value")}
	}
	var ddAt DDAt
	if err := json.NewDecoder(bytes.NewReader(atRaw)).Decode(&ddAt); err != nil {
		return []error{errors.New("illegal [AT] label value, decode fail")}
	}

	m := DDMessage{}
	md, ok := msg.Labels["/MARKDOWN"]
	if !ok {
		m = DDMessage{
			Msgtype: "text",
			Text:    &DDContent{DingPrint(content)},
			At:      ddAt,
		}
	} else {
		mdraw, err := json.Marshal(md)
		if err != nil {
			return []error{errors.New("illegal [MARKDOWN] label value")}
		}
		var ddmd DDMarkdown
		if err := json.NewDecoder(bytes.NewReader(mdraw)).Decode(&ddmd); err != nil {
			return []error{errors.New("illegal [MARKDOWN] label value, decode fail")}
		}
		title := ddmd.Title
		if ddmd.Text != "" {
			content = ddmd.Text
		}
		m = DDMessage{
			Msgtype: "markdown",
			Markdown: &DDMarkdown{
				Title: title,
				Text:  DingPrint(content),
			},
			At: ddAt,
		}
	}
	var dest_ []apistructs.Target
	if err := json.Unmarshal([]byte(dest), &dest_); err != nil {
		return []error{errors.New("illegal dest")}
	}
	urls, err := getURLS(dest_)
	if err != nil {
		return []error{errors.New("illegal urls")}
	}

	for _, urllist := range urls {
		length := len(urllist)
		idx := rand.Intn(length)
		var err error
		for i := 0; i < length; i++ {
			if err = d.DINGDINGSend(urllist[(idx+i)%length], &m); err == nil {
				break // succ
			}
			// if fail, use next dingding url
			idx++
		}
		if err != nil {
			errs = append(errs, err)
		}
	}
	if len(errs) > 0 {
		if msg != nil && msg.CreateHistory != nil {
			msg.CreateHistory.Status = "failed"
		}
	}
	if msg.CreateHistory != nil {
		subscriber.SaveNotifyHistories(msg.CreateHistory, d.messenger)
	}
	return errs
}

func getURLS(dest_ []apistructs.Target) (DDDest, error) {
	var resultUrls [][]string
	for _, v := range dest_ {
		u, err := v.GetSignURL()
		if err != nil {
			return nil, err
		}
		resultUrls = append(resultUrls, []string{u})
	}
	return resultUrls, nil
}

func (d *DDSubscriber) Status() interface{} {
	return nil
}

func (d *DDSubscriber) Name() string {
	return "DINGDING"
}

func (d *DDSubscriber) DINGDINGSend(u string, m *DDMessage) error {
	parsed, err := url.Parse(u)
	if err != nil {
		return errors.Wrap(DINGDINGBadURLErr, err.Error())
	}
	tokenQuery, ok := parsed.Query()["access_token"]
	if !ok {
		err = errors.Errorf("DingDing publish: %v, not provide access_token", u)
		logrus.Error(err)
		return errors.Wrap(DINGDINGBadURLErr, err.Error())
	}
	if len(tokenQuery) == 0 {
		err = errors.Errorf("DingDing publish: %v, not provide access_token", u)
		logrus.Error(err)
		return errors.Wrap(DINGDINGBadURLErr, err.Error())
	}
	var buf bytes.Buffer
	resp, err := httpclient.New(httpclient.WithHTTPS(), httpclient.WithProxy(d.proxy), httpclient.WithDialerKeepAlive(30*time.Second)).
		Post(parsed.Host).
		Path(parsed.Path).
		Params(parsed.Query()).
		Header("Content-Type", "application/json;charset=utf-8").
		JSONBody(m).Do().
		Body(&buf)
	if err != nil {
		err = errors.Errorf("DingDing publish: %v , err:%v", u, err)
		logrus.Error(err)
		return errors.Wrap(DINGDINGSendErr, err.Error())
	}
	if !resp.IsOK() {
		err = errors.Errorf("DingDing publish: %v, httpcode:%d", u, resp.StatusCode())
		logrus.Error(err)
		return errors.Wrap(DINGDINGSendErr, err.Error())
	}
	body, err := ioutil.ReadAll(&buf)
	if err != nil {
		err = errors.Errorf("DingDing publish: %v, err: %v", u, err)
		logrus.Error(err)
		return errors.Wrap(DINGDINGSendErr, err.Error())
	}
	var v map[string]interface{}
	if err := json.Unmarshal(body, &v); err != nil {
		err = errors.Errorf("DingDing publish: %v, err: %v, body: %v", u, err, string(body))
		logrus.Error(err)
		return errors.Wrap(DINGDINGSendErr, err.Error())
	}
	errcode, ok := v["errcode"]
	if !ok {
		logrus.Warningf("DingDing publish: %v, no errcode in response body: %v", u, string(body))
		return nil
	}
	errmsg, ok := v["errmsg"]
	if !ok {
		logrus.Warningf("DingDing publish: %v, no errmsg in response body: %v", u, string(body))
		return nil
	}
	errcodeNum := int(errcode.(float64))
	if errcodeNum != 0 {
		err = errors.Errorf("DingDing publish: %v, DingDing errcode: %d, errmsg: %s", u, errcodeNum, errmsg.(string))
		logrus.Error(err)
		return errors.Wrap(DINGDINGSendErr, err.Error())
	}
	return nil
}

func indentJSON(content interface{}) (string, error) {
	v, err := json.MarshalIndent(content, "", "    ")
	return string(v), err
}

// PrettyPrint 钉钉消息显示的处理
// 1. 如果 content 能反序列化成 string，那么使用反序列化后的内容作为钉钉消息内容
// 2. 如果 content 能使用json反序列，那么对使用带缩进的反序列化后的内容作为钉钉消息内容
// 3. 否则，直接使用 content
func PrettyPrint(content string, isWebhook bool) string {
	var contentS string
	if err := json.Unmarshal([]byte(content), &contentS); err == nil {
		return contentS
	}
	var contentI interface{}
	if err := json.Unmarshal([]byte(content), &contentI); err != nil {
		return content
	}
	if isWebhook {
		webhookstr, err := webhook.Format(contentI)
		if err != nil {
			logrus.Infof("failed to format webhook: %v", err)
			indentContent, err := indentJSON(contentI)
			if err != nil {
				return content
			}
			return indentContent
		}
		return webhookstr
	}
	if indentContent, err := indentJSON(contentI); err == nil {
		return indentContent
	}
	return content
}

func DingPrint(content string) string {
	/*
		Please try to process it upstream, this should only be used as a `bottom line`
		Truncating byte arrays, which may cause `garbled characters`
	*/
	if len(content) > maxContentSize {
		return content[:maxContentSize-len(overflowText)] + overflowText
	}
	return content
}
