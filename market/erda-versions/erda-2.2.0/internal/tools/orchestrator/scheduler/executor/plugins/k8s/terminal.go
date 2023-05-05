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

package k8s

import (
	"bytes"
	"crypto/tls"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"regexp"
	"sync"

	"github.com/gorilla/websocket"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/pkg/clusterdialer"
)

const (
	Error   = '1'
	Ping    = '2'
	Pong    = '3'
	Input   = '4'
	Output  = '5'
	GetSize = '6'
	Size    = '7'
	SetSize = '8'
)

type Winsize struct {
	Rows uint16 // ws_row: Number of rows (in cells)
	Cols uint16 // ws_col: Number of columns (in cells)
	X    uint16 // ws_xpixel: Width in pixels
	Y    uint16 // ws_ypixel: Height in pixels
}

var passRegexp = regexp.MustCompile(`(?i)(?:pass|secret)[^\s\0]*=([^\s\0]+)`)

func hidePassEnv(b []byte) []byte {
	return passRegexp.ReplaceAllFunc(b, func(a []byte) []byte {
		if i := bytes.IndexByte(a, '='); i != -1 {
			for i += 2; i < len(a); i++ { // keep first
				a[i] = '*'
			}
		}
		return a
	})
}

func (k *Kubernetes) Terminal(namespace, podname, containername string, upperConn *websocket.Conn) {
	f := func(cols, rows uint16) (*websocket.Conn, error) {
		path := fmt.Sprintf("/api/v1/namespaces/%s/pods/%s/exec", namespace, podname)
		s := `stty cols %d rows %d; s=/bin/sh; if [ -f /bin/bash ]; then s=/bin/bash; fi; `
		if conf.TerminalSecurity() {
			s += "if [ `id -un` != dice ]; then su -l dice -s $s; exit $?; fi; "
		}
		s += "$s"
		cmd := url.QueryEscape(fmt.Sprintf(s, cols, rows))
		query := "command=sh&command=-c&command=" + cmd + "&container=" + containername + "&stdin=1&stdout=1&tty=true"

		mc := k.cluster.ManageConfig

		if mc == nil {
			return nil, fmt.Errorf("manage config is nil")
		}

		dialer := websocket.DefaultDialer
		dialer.TLSClientConfig = &tls.Config{}
		header := http.Header{}

		var host string

		parseURL, err := url.Parse(mc.Address)
		if err != nil {
			host = mc.Address
		} else {
			host = parseURL.Host
		}

		execURL := url.URL{
			Scheme:   "wss",
			Path:     path,
			Host:     host,
			RawQuery: query,
		}

		// ca cert if empty, skip verify secure
		if mc.CaData != "" {
			caBytes, err := base64.StdEncoding.DecodeString(mc.CaData)
			if err != nil {
				logrus.Errorf("ca bytes load error: %v", err)
				return nil, err
			}

			pool := x509.NewCertPool()
			pool.AppendCertsFromPEM(caBytes)
			dialer.TLSClientConfig.RootCAs = pool
		} else {
			dialer.TLSClientConfig.InsecureSkipVerify = true
		}

		switch mc.Type {
		case apistructs.ManageToken:
			header.Add("Authorization", "Bearer "+mc.Token)
		case apistructs.ManageCert:
			certData, err := base64.StdEncoding.DecodeString(mc.CertData)
			if err != nil {
				logrus.Errorf("decode cert data error: %v", err)
				return nil, err
			}
			keyData, err := base64.StdEncoding.DecodeString(mc.KeyData)
			if err != nil {
				logrus.Errorf("decode key data error: %v", err)
				return nil, err
			}
			pair, err := tls.X509KeyPair(certData, keyData)
			if err != nil {
				logrus.Errorf("load X509Key pair error: %v", err)
				return nil, err
			}
			dialer.TLSClientConfig.Certificates = []tls.Certificate{pair}
		case apistructs.ManageProxy:
			// Dialer
			dialer.NetDialContext = clusterdialer.DialContext(k.clusterName)
			header.Add("Authorization", "Bearer "+mc.Token)
		default:
			return nil, fmt.Errorf("not support manage type: %v", mc.Type)
		}

		conn, resp, err := dialer.Dial(execURL.String(), header)
		if err != nil {
			logrus.Errorf("failed to connect to %s: %v", execURL.String(), err)
			if resp == nil {
				return nil, err
			}
			logrus.Debugf("connect to %s request info: %+v", execURL.String(), resp.Request)
			respBody, _ := ioutil.ReadAll(resp.Body)
			logrus.Debugf("connect to %s response body: %s", execURL.String(), string(respBody))
			return nil, err
		}

		return conn, nil
	}
	var conn *websocket.Conn
	var waitConn sync.WaitGroup
	waitConn.Add(1)
	var setsizeOnce sync.Once
	var wait sync.WaitGroup
	wait.Add(2)
	go func() {
		defer func() {
			wait.Done()
			if conn != nil {
				conn.Close()
			}
			upperConn.Close()
		}()
		waitConn.Wait()
		for {
			if conn == nil {
				return
			}
			tp, m, err := conn.ReadMessage()
			if err != nil {
				return
			}
			m[0] = Output
			if conf.TerminalSecurity() {
				m = hidePassEnv(m)
			}
			if err := upperConn.WriteMessage(tp, m); err != nil {
				return
			}
		}
	}()
	go func() {
		defer func() {
			wait.Done()
			if conn != nil {
				conn.Close()
			}
			upperConn.Close()
		}()
		for {
			tp, m, err := upperConn.ReadMessage()
			if err != nil {
				return
			}
			switch m[0] {
			case Input:
				if conn == nil {
					continue
				}
				m[0] = 0 // k8s 协议, stdin = 0
				if err := conn.WriteMessage(tp, m); err != nil {
					return
				}
			case SetSize:
				var err error
				setsizeOnce.Do(func() {
					var v Winsize
					err = json.Unmarshal(m[1:], &v)
					if err != nil {
						return
					}
					conn, err = f(v.Cols, v.Rows)
					waitConn.Done()
					if err != nil {
						logrus.Errorf("failed to connect k8s exec ws: %v", err)
						return
					}
				})
				if err != nil {
					return
				}
			default:
				continue
			}
		}
	}()
	wait.Wait()
}
