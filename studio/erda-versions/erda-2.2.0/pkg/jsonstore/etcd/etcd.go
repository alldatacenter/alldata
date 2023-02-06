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

// Package etcd 是 jsonstore 使用 etcd 作为 backend 的实现
package etcd

import (
	"context"
	"crypto/tls"
	"math/rand"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/coreos/etcd/mvcc/mvccpb"
	"github.com/coreos/etcd/pkg/transport"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/jsonstore/stm"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

const (
	// The short keepalive timeout and interval have been chosen to aggressively
	// detect a failed etcd server without introducing much overhead.
	keepaliveTime    = 30 * time.Second
	keepaliveTimeout = 10 * time.Second
	// 对etcd操作的超时时间
	parentCtxTimeout = 10 * time.Second

	// 客户端第一次等待etcd请求返回的超时时间
	firstTryEtcdRequestTimeout = 5 * time.Second

	// 客户端第二次等待etcd请求返回的超时时间
	secondTryEtcdRequestTimeout = 3 * time.Second

	// etcd客户端连接池容量
	etcdClientBufferCapacity = 20

	// 当前etcd客户端连接池中连接数数量
	etcdClientBufferLen = 3

	// WatchChan 的默认 buffer size
	defaultWatchChanBufferSize = 100
)

// Store etcd backend 的 storetypes.Store 实现
type Store struct {
	pool chan *clientv3.Client
}

type Option struct {
	Endpoints []string
}

type OpOption func(*Option)

func WithEndpoints(endpoints []string) OpOption {
	return func(opt *Option) {
		opt.Endpoints = endpoints
	}
}

func getEnvOrDefault(key, defaultVal string) string {
	if val := os.Getenv(key); val != "" {
		return val
	}

	return defaultVal
}

// New creates a etcd store with etcd client, be used to access the etcd cluster.
func New(ops ...OpOption) (*Store, error) {
	// apply option
	opt := Option{}
	for _, op := range ops {
		op(&opt)
	}

	if len(opt.Endpoints) == 0 {
		env := os.Getenv("ETCD_ENDPOINTS")
		if env == "" {
			opt.Endpoints = []string{"http://127.0.0.1:2379"}
		} else {
			opt.Endpoints = strings.Split(env, ",")
		}
	}

	var tlsConfig *tls.Config
	if len(opt.Endpoints) < 1 {
		return nil, errors.New("Invalid Etcd endpoints")
	}
	url, err := url.Parse(opt.Endpoints[0])
	if err != nil {
		return nil, errors.Wrap(err, "Invalid Etcd endpoints")
	}

	teleRootPathPrefix := os.Getenv("TELEPRESENCE_ROOT")

	if url.Scheme == "https" {
		tlsInfo := transport.TLSInfo{
			CertFile:      teleRootPathPrefix + getEnvOrDefault("ETCD_CERT_FILE", "/certs/etcd-client.pem"),
			KeyFile:       teleRootPathPrefix + getEnvOrDefault("ETCD_CERT_KEY_FILE", "/certs/etcd-client-key.pem"),
			TrustedCAFile: teleRootPathPrefix + getEnvOrDefault("ETCD_CA_FILE", "/certs/etcd-ca.pem"),
		}
		tlsConfig, err = tlsInfo.ClientConfig()
		if err != nil {
			return nil, errors.Wrap(err, "Invalid Etcd TLS config")
		}
	}

	pool := make(chan *clientv3.Client, etcdClientBufferCapacity)
	for i := 0; i < etcdClientBufferLen; i++ {
		c, err := clientv3.New(clientv3.Config{
			Endpoints:            shuffle(opt.Endpoints),
			DialKeepAliveTime:    keepaliveTime,
			DialKeepAliveTimeout: keepaliveTimeout,
			TLS:                  tlsConfig,
		})
		if err != nil {
			return nil, err
		}

		pool <- c
	}

	store := &Store{
		pool: pool,
	}
	return store, nil
}

func (s *Store) getClient() *clientv3.Client {
	c := <-s.pool
	s.pool <- c
	return c
}

// GetClient 获取 Store 内部的 etcd client
func (s *Store) GetClient() *clientv3.Client {
	return s.getClient()
}

// nolint
func (s *Store) retry(do func(cli *clientv3.Client) (interface{}, error)) (interface{}, error) {
	cli := s.getClient()

	errC1 := make(chan error, 1)
	respC1 := make(chan interface{}, 1)

	go func() {
		resp, err := do(cli)
		if err != nil {
			errC1 <- err
			return
		}
		respC1 <- resp
	}()

	select {
	case err := <-errC1:
		return nil, err
	case resp := <-respC1:
		return resp, nil
	case <-time.After(firstTryEtcdRequestTimeout):
		// 超时后，就换一个 client 实例
		cli = s.getClient()
	}

	// 超时重试第二次, 注意上面的 goroutine 可能还在运行
	errC2 := make(chan error, 1)
	respC2 := make(chan interface{}, 1)

	go func() {
		resp, err := do(cli)
		if err != nil {
			errC2 <- err
			return
		}
		respC2 <- resp
	}()

	select {
	case err := <-errC1:
		return nil, err
	case err := <-errC2:
		return nil, err
	case resp := <-respC1:
		return resp, nil
	case resp := <-respC2:
		return resp, nil
	case <-time.After(secondTryEtcdRequestTimeout):
		return nil, errors.New("time out")
	}
}

// Put writes the keyvalue pair into etcd.
func (s *Store) Put(pctx context.Context, key, value string) error {
	_, err := s.PutWithOption(pctx, key, value, nil)
	if err != nil {
		return err
	}
	// 检查 etcd 中的确已存入 key
	for i := 0; i < 2; i++ {
		if _, err := s.Get(context.Background(), key); err != nil {
			if strings.Contains(err.Error(), "not found") {
				time.Sleep(1 * time.Second)
				continue
			}
		} else {
			return nil
		}

	}
	return nil
}

// PutWithRev 向 etcd 写入 kv，并且返回 revision
func (s *Store) PutWithRev(ctx context.Context, key, value string) (int64, error) {
	resp, err := s.PutWithOption(ctx, key, value, nil)
	if err != nil {
		return 0, err
	}
	return resp.(*clientv3.PutResponse).Header.GetRevision(), nil
}

// PutWithOption 向 etcd 写入 kv 时能指定 option
func (s *Store) PutWithOption(ctx context.Context, key, value string, opts []interface{}) (interface{}, error) {
	etcdopts := []clientv3.OpOption{}
	for _, opt := range opts {
		etcdopts = append(etcdopts, opt.(clientv3.OpOption))
	}

	put := func(cli *clientv3.Client) (interface{}, error) {
		ctx, cancel := context.WithTimeout(context.Background(), parentCtxTimeout)
		defer cancel()
		return cli.Put(ctx, key, value, etcdopts...)
	}
	result, err := s.retry(put)
	if err != nil {
		return nil, err
	}
	resp, ok := result.(*clientv3.PutResponse)
	if !ok {
		return nil, errors.New("invalid response type")
	}
	return resp, nil
}

// Get returns the value of the key.
func (s *Store) Get(pctx context.Context, key string) (storetypes.KeyValue, error) {
	get := func(cli *clientv3.Client) (interface{}, error) {
		ctx, cancel := context.WithTimeout(context.Background(), parentCtxTimeout)
		defer cancel()
		return cli.Get(ctx, key)
	}

	result, err := s.retry(get)
	if err != nil {
		return storetypes.KeyValue{}, err
	}
	resp, ok := result.(*clientv3.GetResponse)
	if !ok {
		return storetypes.KeyValue{}, errors.New("invalid response type")
	}

	if len(resp.Kvs) != 0 {
		return storetypes.KeyValue{
			Key:         resp.Kvs[0].Key,
			Value:       resp.Kvs[0].Value,
			Revision:    resp.Header.GetRevision(),
			ModRevision: resp.Kvs[0].ModRevision,
		}, nil
	}
	return storetypes.KeyValue{}, errors.Errorf("not found")
}

// PrefixGet returns the all key value with specify prefix.
func (s *Store) PrefixGet(pctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	resp, err := s.prefixGet(pctx, prefix, false)
	if err != nil {
		return nil, err
	}
	kvs := make([]storetypes.KeyValue, len(resp.Kvs))
	for i, kv := range resp.Kvs {
		kvs[i] = storetypes.KeyValue{
			Key:         kv.Key,
			Value:       kv.Value,
			Revision:    resp.Header.GetRevision(),
			ModRevision: kv.ModRevision,
		}
	}
	return kvs, nil
}

// PrefixGetKey 只获取 key
func (s *Store) PrefixGetKey(pctx context.Context, prefix string) ([]storetypes.Key, error) {
	resp, err := s.prefixGet(pctx, prefix, true)
	if err != nil {
		return nil, err
	}
	ks := make([]storetypes.Key, len(resp.Kvs))
	for i, kv := range resp.Kvs {
		ks[i] = storetypes.Key(kv.Key)
	}
	return ks, nil
}

func (s *Store) prefixGet(_ context.Context, prefix string, keyOnly bool) (*clientv3.GetResponse, error) {
	prefixGet := func(cli *clientv3.Client) (interface{}, error) {
		ctx, cancel := context.WithTimeout(context.Background(), parentCtxTimeout)
		defer cancel()
		options := []clientv3.OpOption{clientv3.WithPrefix()}
		if keyOnly {
			options = append(options, clientv3.WithKeysOnly())
		}

		return cli.Get(ctx, prefix, options...)
	}

	result, err := s.retry(prefixGet)
	if err != nil {
		return nil, err
	}
	resp, ok := result.(*clientv3.GetResponse)
	if !ok {
		return nil, errors.New("invalid response type")
	}
	return resp, nil
}

// Remove 删除一个 keyvalue, 同时返回被删除的 kv 对象.
func (s *Store) Remove(pctx context.Context, key string) (*storetypes.KeyValue, error) {
	remove := func(cli *clientv3.Client) (interface{}, error) {
		ctx, cancel := context.WithTimeout(context.Background(), parentCtxTimeout)
		defer cancel()
		return cli.Delete(ctx, key, clientv3.WithPrevKV())
	}

	result, err := s.retry(remove)
	if err != nil {
		return nil, err
	}
	resp, ok := result.(*clientv3.DeleteResponse)
	if !ok {
		return nil, errors.New("invalid response type")
	}

	if len(resp.PrevKvs) == 1 {
		return &storetypes.KeyValue{
			Key:         resp.PrevKvs[0].Key,
			Value:       resp.PrevKvs[0].Value,
			Revision:    resp.Header.GetRevision(),
			ModRevision: resp.PrevKvs[0].ModRevision,
		}, nil
	}
	return nil, nil
}

func (s *Store) prefixRemove(_ context.Context, prefix string) (*clientv3.DeleteResponse, error) {
	prefixRemove := func(cli *clientv3.Client) (interface{}, error) {
		ctx, cancel := context.WithTimeout(context.Background(), parentCtxTimeout)
		defer cancel()
		options := []clientv3.OpOption{clientv3.WithPrefix(), clientv3.WithPrevKV()}

		return cli.Delete(ctx, prefix, options...)
	}

	result, err := s.retry(prefixRemove)
	if err != nil {
		return nil, err
	}
	resp, ok := result.(*clientv3.DeleteResponse)
	if !ok {
		return nil, errors.New("invalid response type")
	}
	return resp, nil
}

// PrefixRemove 删除 prefix 开头的所有 kv
func (s *Store) PrefixRemove(pctx context.Context, prefix string) ([]storetypes.KeyValue, error) {
	resp, err := s.prefixRemove(pctx, prefix)
	if err != nil {
		return nil, err
	}

	kvs := make([]storetypes.KeyValue, len(resp.PrevKvs))
	for i, kv := range resp.PrevKvs {
		kvs[i] = storetypes.KeyValue{
			Key:   kv.Key,
			Value: kv.Value,
		}
	}

	return kvs, nil
}

// Watch key 的变化，如果 filterDelete=true，则忽略删除事件
func (s *Store) Watch(ctx context.Context, key string, isPrefix, filterDelete bool) (storetypes.WatchChan, error) {
	op := []clientv3.OpOption{clientv3.WithPrevKV()}
	if isPrefix {
		op = append(op, clientv3.WithPrefix())
	}
	if filterDelete {
		op = append(op, clientv3.WithFilterDelete())
	}
	ch := s.getClient().Watch(ctx, key, op...)
	watchCh := make(chan storetypes.WatchResponse, defaultWatchChanBufferSize)

	go func() {
		for r := range ch {
			if err := r.Err(); err != nil {
				watchCh <- storetypes.WatchResponse{
					Kvs: []storetypes.KeyValueWithChangeType{},
					Err: err,
				}
				close(watchCh)
				return
			}
			kvs := []storetypes.KeyValueWithChangeType{}
			for _, e := range r.Events {
				t := eventType(e)
				value := e.Kv.Value
				if len(value) == 0 && e.PrevKv != nil {
					value = e.PrevKv.Value
				}
				kvs = append(kvs, storetypes.KeyValueWithChangeType{
					KeyValue: storetypes.KeyValue{
						Key:         e.Kv.Key,
						Value:       value,
						Revision:    r.Header.GetRevision(),
						ModRevision: e.Kv.ModRevision,
					},
					T: t,
				})
			}
			watchCh <- storetypes.WatchResponse{Kvs: kvs, Err: nil}
		}
		close(watchCh)
	}()
	return watchCh, nil
}

func eventType(e *clientv3.Event) storetypes.ChangeType {
	if e.Type == mvccpb.DELETE {
		return storetypes.Del
	}
	if e.Type == mvccpb.PUT && e.Kv.CreateRevision == e.Kv.ModRevision {
		return storetypes.Add
	}
	return storetypes.Update
}

// NewSTM etcd concurrency.NewSTM + json (un)marshal
func (s *Store) NewSTM(f func(stm stm.JSONStoreSTMOP) error) error {
	impl := stm.NewJSONStoreWithSTMImpl(s.GetClient())
	return impl.NewSTM(f)
}

func shuffle(s []string) []string {
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	for n := len(s); n > 0; n-- {
		randIndex := r.Intn(n)
		s[n-1], s[randIndex] = s[randIndex], s[n-1]
	}
	return s
}
