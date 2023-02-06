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

package email

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"net"
	"net/mail"
	"net/smtp"
	"strconv"
	"strings"

	"github.com/russross/blackfriday/v2"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/pkg/template"
)

type MailSubscriber struct {
	host               string
	port               string
	user               string
	password           string
	displayUser        string
	isSSL              bool
	isSSLStr           string
	insecureSkipVerify bool
	bundle             *bundle.Bundle
	messenger          pb.NotifyServiceServer
}

type MailSubscriberInfo struct {
	Host               string
	Port               string
	User               string
	Password           string
	DisplayUser        string
	IsSSL              bool
	InsecureSkipVerify bool
}

type MailData struct {
	Template    string            `json:"template"`
	Params      map[string]string `json:"params"`
	Type        string            `json:"type"` // 默认不做二次渲染当做html, 值为markdown时:使用模式渲染html
	Attachments []*Attachment     `json:"attachments"`
	OrgID       int64             `json:"orgID"`
}

type Option func(*MailSubscriber)

func NewMailSubscriberInfo(host, port, user, password, displayUser, isSSLStr, insecureSkipVerify string) *MailSubscriberInfo {
	subscriber := &MailSubscriberInfo{
		Host:        host,
		Port:        port,
		User:        user,
		Password:    password,
		DisplayUser: displayUser,
	}
	isSSL, _ := strconv.ParseBool(isSSLStr)
	subscriber.IsSSL = isSSL
	isInsecureSkipVerify, _ := strconv.ParseBool(insecureSkipVerify)
	subscriber.InsecureSkipVerify = isInsecureSkipVerify
	return subscriber
}

func New(host, port, user, password, displayUser, isSSLStr, insecureSkipVerify string, bundle *bundle.Bundle, messenger pb.NotifyServiceServer) subscriber.Subscriber {
	subscriber := &MailSubscriber{
		host:        host,
		port:        port,
		user:        user,
		password:    password,
		displayUser: displayUser,
		isSSLStr:    isSSLStr,
		bundle:      bundle,
		messenger:   messenger,
	}
	isSSL, _ := strconv.ParseBool(isSSLStr)
	subscriber.isSSL = isSSL
	isInsecureSkipVerify, _ := strconv.ParseBool(insecureSkipVerify)
	subscriber.insecureSkipVerify = isInsecureSkipVerify
	return subscriber
}
func (d *MailSubscriber) IsSSL() bool {
	//未显示指定,基于约定开启ssl
	if d.isSSLStr == "" {
		if d.port == "465" {
			return true
		}
		return false
	}
	return d.isSSL
}

func (d *MailSubscriber) Publish(dest string, content string, time int64, msg *types.Message) []error {
	errs := []error{}
	var mails []string
	var mailData MailData
	err := json.Unmarshal([]byte(dest), &mails)
	if err != nil {
		return []error{err}
	}
	err = json.Unmarshal([]byte(content), &mailData)
	if err != nil {
		return []error{err}
	}
	err = d.sendToMail(mails, &mailData)
	if err != nil {
		errs = append(errs, err)
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

func (d *MailSubscriber) Status() interface{} {
	return nil
}

func (d *MailSubscriber) Name() string {
	return "EMAIL"
}

func (d *MailSubscriber) sendToMail(mails []string, mailData *MailData) error {
	mailsAddrs := []string{}
	for _, m := range mails {
		if _, err := mail.ParseAddress(m); err == nil {
			mailsAddrs = append(mailsAddrs, m)
		}
	}
	smtpUser := d.user
	smtpPassword := d.password
	smtpHost := d.host
	smtpPort := d.port
	isSSL := d.IsSSL()
	var err error
	notifyChannel, err := d.bundle.GetEnabledNotifyChannelByType(mailData.OrgID, apistructs.NOTIFY_CHANNEL_TYPE_EMAIL)
	if err != nil {
		return fmt.Errorf("no enabled email channel, orgID: %d, err: %v", mailData.OrgID, err)
	}

	if notifyChannel.Config != nil {
		smtpUser = notifyChannel.Config.SMTPUser
		smtpPassword = notifyChannel.Config.SMTPPassword
		smtpHost = notifyChannel.Config.SMTPHost
		smtpPort = strconv.Itoa(int(notifyChannel.Config.SMTPPort))
		isSSL = notifyChannel.Config.SMTPIsSSL
	} else {
		org, err := d.bundle.GetOrg(mailData.OrgID)
		if err != nil {
			logrus.Errorf("failed to get org info err:%s", err)
		}
		if org.Config.SMTPUser != "" && org.Config.SMTPPassword != "" {
			orgConfig := org.Config
			smtpHost = orgConfig.SMTPHost
			smtpUser = orgConfig.SMTPUser
			smtpPassword = orgConfig.SMTPPassword
			smtpPort = strconv.FormatInt(orgConfig.SMTPPort, 10)
			isSSL = orgConfig.SMTPIsSSL
		}
	}

	if smtpHost == "" {
		return fmt.Errorf("send email host is null")
	}

	params := mailData.Params
	templateStr := mailData.Template
	typ := mailData.Type
	subject, ok := params["title"]
	if !ok {
		subject = "通知"
	}
	subject = template.Render(subject, params)
	body := template.Render(templateStr, params)
	if typ == "markdown" {
		// 库不支持&nbsp;转空行
		body = strings.Replace(body, "&nbsp;", "</br>", -1)
		body = strings.Replace(body, "&times", "&amp;times", -1)
		body = string(blackfriday.Run([]byte(body)))
	}
	displayUser := d.displayUser
	if displayUser == "" {
		displayUser = smtpUser
	}
	msg := NewMessage(subject, body, "text/html;charset=UTF-8")
	msg.To = mailsAddrs
	msg.FromDisplayName = displayUser
	msg.From = smtpUser
	for _, attachment := range mailData.Attachments {
		msg.Attach(attachment)
	}

	auth := smtp.PlainAuth("", smtpUser, smtpPassword, smtpHost)
	if isSSL {
		err = SendMailUsingTLS(fmt.Sprintf("%s:%s", smtpHost, smtpPort), auth, smtpUser, mailsAddrs, d.insecureSkipVerify, msg.Bytes())
	} else {
		err = smtp.SendMail(fmt.Sprintf("%s:%s", smtpHost, smtpPort), auth, smtpUser, mailsAddrs, msg.Bytes())
	}
	return err
}

func SendMailUsingTLS(addr string, auth smtp.Auth, from string, tos []string, insecureSkipVerify bool, msg []byte) (err error) {
	c, err := DialTLS(addr, insecureSkipVerify)
	if err != nil {
		return err
	}
	defer c.Close()
	if auth != nil {
		if ok, _ := c.Extension("AUTH"); ok {
			if err = c.Auth(auth); err != nil {
				return err
			}
		}
	}
	if err = c.Mail(from); err != nil {
		return err
	}
	for _, addr := range tos {
		if err = c.Rcpt(addr); err != nil {
			fmt.Print(err)
			return err
		}
	}
	w, err := c.Data()
	if err != nil {
		return err
	}
	_, err = w.Write(msg)
	if err != nil {
		return err
	}
	err = w.Close()
	if err != nil {
		return err
	}
	return c.Quit()
}
func DialTLS(addr string, insecureSkipVerify bool) (*smtp.Client, error) {
	tlsconfig := &tls.Config{}
	if insecureSkipVerify {
		tlsconfig.InsecureSkipVerify = true
	}

	conn, err := tls.Dial("tcp", addr, tlsconfig)
	if err != nil {
		return nil, err
	}
	//分解主机端口字符串
	host, _, _ := net.SplitHostPort(addr)
	return smtp.NewClient(conn, host)
}
