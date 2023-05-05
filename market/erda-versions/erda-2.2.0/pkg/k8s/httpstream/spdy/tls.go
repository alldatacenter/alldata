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

package spdy

import (
	"context"
	"crypto/tls"
	"net"
	"strings"
	"time"
)

type timeoutError struct{}

func (timeoutError) Error() string   { return "tls: DialWithDialer timed out" }
func (timeoutError) Timeout() bool   { return true }
func (timeoutError) Temporary() bool { return true }

var emptyConfig tls.Config

func defaultConfig() *tls.Config {
	return &emptyConfig
}

func dialTLS(ctx context.Context, timeout time.Duration, deadline time.Time, dialer func(ctx context.Context, network, address string) (net.Conn, error), network, addr string, config *tls.Config) (*tls.Conn, error) {

	if !deadline.IsZero() {
		deadlineTimeout := time.Until(deadline)
		if timeout == 0 || deadlineTimeout < timeout {
			timeout = deadlineTimeout
		}
	}

	// hsErrCh is non-nil if we might not wait for Handshake to complete.
	var hsErrCh chan error
	if timeout != 0 || ctx.Done() != nil {
		hsErrCh = make(chan error, 2)
	}
	if timeout != 0 {
		timer := time.AfterFunc(timeout, func() {
			hsErrCh <- timeoutError{}
		})
		defer timer.Stop()
	}

	rawConn, err := dialer(ctx, network, addr)
	if err != nil {
		return nil, err
	}

	colonPos := strings.LastIndex(addr, ":")
	if colonPos == -1 {
		colonPos = len(addr)
	}
	hostname := addr[:colonPos]

	if config == nil {
		config = defaultConfig()
	}
	// If no ServerName is set, infer the ServerName
	// from the hostname we're connecting to.
	if config.ServerName == "" {
		// Make a copy to avoid polluting argument or default.
		c := config.Clone()
		c.ServerName = hostname
		config = c
	}

	conn := tls.Client(rawConn, config)

	if hsErrCh == nil {
		err = conn.Handshake()
	} else {
		go func() {
			hsErrCh <- conn.Handshake()
		}()

		select {
		case <-ctx.Done():
			err = ctx.Err()
		case err = <-hsErrCh:
			if err != nil {
				// If the error was due to the context
				// closing, prefer the context's error, rather
				// than some random network teardown error.
				if e := ctx.Err(); e != nil {
					err = e
				}
			}
		}
	}

	if err != nil {
		rawConn.Close()
		return nil, err
	}

	return conn, nil
}
