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

// distributed-lock base etcd
// usage:
// lock, _ := dlock.New(dlock.WithTTL(5), func(){})
// lock.Lock(ctx)
// // do something...
// lock.Unlock()
// // release resource
// lock.Close() // see also lock.UnlockAndClose()
// //see also dlock_test.go

package dlock

import (
	"context"
	"crypto/tls"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/coreos/etcd/clientv3/concurrency"
	"github.com/coreos/etcd/pkg/transport"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

const (
	// The short keepalive timeout and interval have been chosen to aggressively
	// detect a failed etcd server without introducing much overhead.
	keepaliveTime    = 30 * time.Second
	keepaliveTimeout = 10 * time.Second

	// default ttl, time to live when current process exit without unlock (eg. panic)
	defaultTTL = 5
)

type DLock struct {
	client  *clientv3.Client
	session *concurrency.Session
	mutex   *concurrency.Mutex
	option  *option
	lockKey string

	normalClose bool
}

type option struct {
	ttl int
}
type OpOption func(*option)

func WithTTL(ttl int) OpOption {
	return func(op *option) {
		op.ttl = ttl
	}
}

func applyOptions(ops []OpOption, option *option) error {
	for _, op := range ops {
		op(option)
	}
	if option.ttl <= 0 {
		return errors.New("illegal ttl value, must greater than 0")
	}
	return nil
}

func New(lockKey string, locklostCallback func(), ops ...OpOption) (*DLock, error) {
	option := &option{ttl: defaultTTL}

	if err := applyOptions(ops, option); err != nil {
		return nil, err
	}

	var endpoints []string
	env := os.Getenv("ETCD_ENDPOINTS")
	if env == "" {
		endpoints = []string{"http://127.0.0.1:2379"}
	} else {
		endpoints = strings.Split(env, ",")
	}

	var tlsConfig *tls.Config
	if len(endpoints) < 1 {
		return nil, errors.New("Invalid Etcd endpoints")
	}
	url, err := url.Parse(endpoints[0])
	if err != nil {
		return nil, errors.Wrap(err, "Invalid Etcd endpoints")
	}
	if url.Scheme == "https" {
		tlsInfo := transport.TLSInfo{
			CertFile:      getEnvOrDefault("ETCD_CERT_FILE", "/certs/etcd-client.pem"),
			KeyFile:       getEnvOrDefault("ETCD_CERT_KEY_FILE", "/certs/etcd-client-key.pem"),
			TrustedCAFile: getEnvOrDefault("ETCD_CA_FILE", "/certs/etcd-ca.pem"),
		}
		tlsConfig, err = tlsInfo.ClientConfig()
		if err != nil {
			return nil, errors.Wrap(err, "Invalid Etcd TLS config")
		}
	}

	cli, err := clientv3.New(clientv3.Config{
		Endpoints:            endpoints,
		DialKeepAliveTime:    keepaliveTime,
		DialKeepAliveTimeout: keepaliveTimeout,
		TLS:                  tlsConfig,
	})
	if err != nil {
		return nil, err
	}
	session, err := concurrency.NewSession(cli, concurrency.WithTTL(option.ttl))
	if err != nil {
		return nil, err
	}
	mutex := concurrency.NewMutex(session, lockKey)

	l := DLock{
		client:  cli,
		session: session,
		mutex:   mutex,
		lockKey: lockKey,
		option:  option,
	}

	go func() {
		select {
		case <-l.session.Done():
			// invoke l.Close() or l.UnlockAndClose()
			if l.normalClose {
				return
			}
			if locklostCallback != nil {
				locklostCallback()
			}
		}
	}()

	return &l, nil
}

// it's cancelable
func (l *DLock) Lock(ctx context.Context) error {
	return l.mutex.Lock(ctx)
}

func (l *DLock) Unlock() error {
	return l.mutex.Unlock(context.Background())
}

func (l *DLock) Close() error {
	l.normalClose = true
	var errs []string
	if err := l.session.Close(); err != nil {
		logrus.Errorf("dlock: failed to close concurrency session, err: %v", err)
		errs = append(errs, err.Error())
	}
	if err := l.client.Close(); err != nil {
		logrus.Errorf("dlock: failed to close etcd client, err: %v", err)
		errs = append(errs, err.Error())
	}
	if len(errs) == 0 {
		return nil
	}
	return errors.New(strings.Join(errs, "\n"))
}

func (l *DLock) UnlockAndClose() error {
	defer l.Close()
	return l.Unlock()
}

// return locked key belong to this locker: <lockKey>/<lease-ID>
func (l *DLock) Key() string {
	return l.mutex.Key()
}

func (l *DLock) IsOwner() (bool, error) {
	r, err := l.client.Txn(context.Background()).If(l.mutex.IsOwner()).Commit()
	if err != nil {
		return false, err
	}
	return r.Succeeded, nil
}

func getEnvOrDefault(env, v string) string {
	t := os.Getenv(env)
	if t == "" {
		return v
	}
	return t
}
