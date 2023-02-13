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
	"bytes"
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"math"
	"math/big"
	"os"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/crypto/uuid"
)

type Attachment struct {
	Filename string `json:"filename"`
	Content  string `json:"content"`
	Inline   bool   `json:"inline"`
	Encoding string `json:"encoding"`
}

type Message struct {
	From            string
	FromDisplayName string
	To              []string
	Cc              []string
	Subject         string
	Body            string
	BodyContentType string
	Attachments     []*Attachment
}

func (m *Message) Attach(attachment *Attachment) {
	m.Attachments = append(m.Attachments, attachment)
}

func NewMessage(subject string, body string, bodyContentType string) *Message {
	m := &Message{
		Subject:         subject,
		Body:            body,
		BodyContentType: bodyContentType,
	}
	return m
}

func (m *Message) Bytes() []byte {
	buf := bytes.NewBuffer(nil)
	boundary := uuid.UUID()
	header := make(map[string]string)
	header["From"] = "\"" + getEncodedString(m.FromDisplayName) + "\" " + "<" + m.From + ">"
	header["To"] = strings.Join(m.To, ";")
	header["Subject"] = getEncodedString(strings.TrimSpace(m.Subject))
	header["MIME-Version"] = "1.0"
	header["Message-Id"] = generateMessageID()
	if len(m.Cc) > 0 {
		header["Cc"] = strings.Join(m.Cc, ";")
	}
	for k, v := range header {
		buf.WriteString(fmt.Sprintf("%s:%s\n", k, v))
	}

	if len(m.Attachments) > 0 {
		buf.WriteString(fmt.Sprintf("Content-Type: multipart/mixed; boundary=%s\r\n\n", boundary))
		buf.WriteString(fmt.Sprintf("--%s\r\n", boundary))
	}

	buf.WriteString(fmt.Sprintf("Content-Type: %s\"\n\n", m.BodyContentType))
	buf.WriteString(m.Body)
	buf.WriteString("\n")

	if len(m.Attachments) > 0 {
		for _, attachment := range m.Attachments {
			buf.WriteString("--" + boundary + "\n")

			fileName := getEncodedString(attachment.Filename)
			if attachment.Inline {
				buf.WriteString("Content-Type: message/rfc822\n")
				buf.WriteString("Content-Transfer-Encoding: base64\n")
				buf.WriteString("Content-Disposition: inline; filename=\"" + fileName + "\"\n\n")
				if attachment.Encoding == "base64" {
					buf.Write([]byte(attachment.Content))
				} else {
					encodeContent := base64.StdEncoding.EncodeToString([]byte(attachment.Content))
					buf.WriteString(encodeContent)
				}
			} else {
				buf.WriteString("Content-Type: application/octet-stream\n")
				buf.WriteString("Content-Transfer-Encoding: base64\n")
				buf.WriteString("Content-Disposition: attachment; filename=\"" + fileName + "\"\n\n")
				if attachment.Encoding == "base64" {
					buf.Write([]byte(attachment.Content))
				} else {
					encodeContent := base64.StdEncoding.EncodeToString([]byte(attachment.Content))
					buf.WriteString(encodeContent)
				}
			}
		}
		buf.WriteString("\n--" + boundary + "--")
	}

	return buf.Bytes()
}

func getEncodedString(content string) string {
	return fmt.Sprintf("=?UTF-8?B?%s?=", base64.StdEncoding.EncodeToString([]byte(content)))
}

var maxBigInt = big.NewInt(math.MaxInt64)

func generateMessageID() string {
	t := time.Now().UnixNano()
	pid := os.Getpid()
	rint, err := rand.Int(rand.Reader, maxBigInt)
	if err != nil {
		logrus.Errorf("gen MessageID err: %v", err)
		return ""
	}
	h, err := os.Hostname()
	// If we can't get the hostname, we'll use localhost
	if err != nil {
		h = "localhost.localdomain"
	}
	msgid := fmt.Sprintf("<%d.%d.%d@%s>", t, pid, rint, h)
	return msgid
}
