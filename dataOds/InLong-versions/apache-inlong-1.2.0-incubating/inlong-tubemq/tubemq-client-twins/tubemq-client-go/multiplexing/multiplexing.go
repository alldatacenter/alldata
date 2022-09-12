// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Package multiplexing defines the multiplex connection pool for sending
// request and receiving response. After receiving the response, the decoded
// response will be returned to the client. It is used for the communication
// with TubeMQ.
package multiplexing

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"errors"
	"io/ioutil"
	"net"
	"sync"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/codec"
)

var (
	// ErrConnClosed indicates the connection has been closed
	ErrConnClosed = errors.New("connection has been closed")
	// ErrChanClosed indicates the recv chan has been closed
	ErrChanClosed = errors.New("unexpected recv chan closing")
	// ErrWriteBufferDone indicates write buffer done
	ErrWriteBufferDone = errors.New("write buffer done")
	// ErrAssertConnection indicates connection assertion error
	ErrAssertConnection = errors.New("connection assertion error")
)

// The state of the connection.
const (
	Initial int = iota
	Connected
	Closing
	Closed
)

const queueSize = 10000

// Pool maintains the multiplex connections of different addresses.
type Pool struct {
	connections *sync.Map
	mu          sync.RWMutex
}

// NewPool will construct a default multiplex connections pool.
func NewPool() *Pool {
	m := &Pool{
		connections: new(sync.Map),
	}
	return m
}

// Get will return a multiplex connection
// 1. If no underlying TCP connection has been created, a TCP connection will be created first.
// 2. A new multiplex connection with the serialNo will be created and returned.
func (p *Pool) Get(ctx context.Context, address string, serialNo uint32,
	opts *DialOptions) (*MultiplexConnection, error) {
	select {
	case <-ctx.Done():
		return nil, ctx.Err()
	default:
	}

	if v, ok := p.connections.Load(address); ok {
		if c, ok := v.(*Connection); ok {
			return c.new(ctx, serialNo)
		}
		return nil, ErrAssertConnection
	}
	p.mu.Lock()
	defer p.mu.Unlock()
	// If the concurrent request has created the connection, the connection can be returned directly.
	if v, ok := p.connections.Load(address); ok {
		if c, ok := v.(*Connection); ok {
			mc, err := c.new(ctx, serialNo)
			return mc, err
		}
		return nil, ErrAssertConnection
	}

	c := &Connection{
		address:     address,
		connections: make(map[uint32]*MultiplexConnection),
		done:        make(chan struct{}),
		mDone:       make(chan struct{}),
		state:       Initial,
		pool:        p,
	}
	c.buffer = &writerBuffer{
		buffer: make(chan []byte, queueSize),
		done:   c.done,
	}
	p.connections.Store(address, c)

	conn, dialOpts, err := dial(ctx, opts)
	c.dialOpts = dialOpts
	if err != nil {
		c.close(err, c.done)
		return nil, err
	}
	c.decoder = codec.New(conn)
	c.conn = conn
	c.state = Connected
	go c.reader()
	go c.writer()
	return c.new(ctx, serialNo)
}

func dial(ctx context.Context, opts *DialOptions) (net.Conn, *DialOptions, error) {
	var timeout time.Duration
	t, ok := ctx.Deadline()
	if ok {
		timeout = t.Sub(time.Now())
	}
	opts.Timeout = timeout
	select {
	case <-ctx.Done():
		return nil, opts, ctx.Err()
	default:
	}
	conn, err := dialWithTimeout(opts)
	return conn, opts, err
}

func dialWithTimeout(opts *DialOptions) (net.Conn, error) {
	if len(opts.CACertFile) == 0 {
		return net.DialTimeout(opts.Network, opts.Address, opts.Timeout)
	}

	tlsConf := &tls.Config{}
	if opts.CACertFile == "" {
		tlsConf.InsecureSkipVerify = true
	} else {
		if len(opts.TLSServerName) == 0 {
			opts.TLSServerName = opts.Address
		}
		tlsConf.ServerName = opts.TLSServerName
		certPool, err := getCertPool(opts.CACertFile)
		if err != nil {
			return nil, err
		}

		tlsConf.RootCAs = certPool

		if len(opts.TLSCertFile) != 0 {
			cert, err := tls.LoadX509KeyPair(opts.TLSCertFile, opts.TLSKeyFile)
			if err != nil {
				return nil, err
			}
			tlsConf.Certificates = []tls.Certificate{cert}
		}
	}
	return tls.DialWithDialer(&net.Dialer{Timeout: opts.Timeout}, opts.Network, opts.Address, tlsConf)
}

func getCertPool(caCertFile string) (*x509.CertPool, error) {
	if caCertFile != "root" {
		ca, err := ioutil.ReadFile(caCertFile)
		if err != nil {
			return nil, err
		}
		certPool := x509.NewCertPool()
		ok := certPool.AppendCertsFromPEM(ca)
		if !ok {
			return nil, err
		}
		return certPool, nil
	}
	return nil, nil
}

// Close will release all the connections.
func (p *Pool) Close() {
	p.connections.Range(func(key, value interface{}) bool {
		connection, ok := value.(*Connection)
		if !ok {
			return false
		}
		close(connection.done)
		close(connection.mDone)
		connection.conn.Close()
		return true
	})
}

type recvReader struct {
	ctx  context.Context
	recv chan codec.Response
}

// MultiplexConnection is used to multiplex a TCP connection.
// It is distinguished by the serialNo.
type MultiplexConnection struct {
	serialNo uint32
	conn     *Connection
	reader   *recvReader
	done     chan struct{}
}

// Write uses the underlying TCP connection to send the bytes.
func (mc *MultiplexConnection) Write(b []byte) error {
	if err := mc.conn.send(b); err != nil {
		mc.conn.remove(mc.serialNo)
		return err
	}
	return nil
}

// Read returns the response from the multiplex connection.
func (mc *MultiplexConnection) Read() (codec.Response, error) {
	select {
	case <-mc.reader.ctx.Done():
		mc.conn.remove(mc.serialNo)
		return nil, mc.reader.ctx.Err()
	case v, ok := <-mc.reader.recv:
		if ok {
			return v, nil
		}
		if mc.conn.err != nil {
			return nil, mc.conn.err
		}
		return nil, ErrChanClosed
	case <-mc.done:
		return nil, mc.conn.err
	}
}

func (mc *MultiplexConnection) recv(rsp codec.Response) {
	mc.reader.recv <- rsp
	mc.conn.remove(rsp.GetSerialNo())
}

// DialOptions represents the dail options of the TCP connection.
// If TLS is not enabled, the configuration of TLS can be ignored.
type DialOptions struct {
	Network       string
	Address       string
	Timeout       time.Duration
	CACertFile    string
	TLSCertFile   string
	TLSKeyFile    string
	TLSServerName string
}

// Connection represents the underlying TCP connection of the multiplex connections of an address
// and maintains the multiplex connections.
type Connection struct {
	err         error
	address     string
	mu          sync.RWMutex
	connections map[uint32]*MultiplexConnection
	decoder     codec.Decoder
	conn        net.Conn
	done        chan struct{}
	mDone       chan struct{}
	buffer      *writerBuffer
	dialOpts    *DialOptions
	state       int
	pool        *Pool
}

func (c *Connection) new(ctx context.Context, serialNo uint32) (*MultiplexConnection, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.err != nil {
		return nil, c.err
	}

	mc := &MultiplexConnection{
		serialNo: serialNo,
		conn:     c,
		done:     c.mDone,
		reader: &recvReader{
			ctx:  ctx,
			recv: make(chan codec.Response, 1),
		},
	}

	if lastConn, ok := c.connections[serialNo]; ok {
		close(lastConn.reader.recv)
	}
	c.connections[serialNo] = mc
	return mc, nil
}

func (c *Connection) close(lastErr error, done chan struct{}) {
	if lastErr == nil {
		return
	}
	c.mu.Lock()
	defer c.mu.Unlock()

	if c.state == Closed {
		return
	}

	select {
	case <-done:
		return
	default:
	}

	c.state = Closing
	c.err = lastErr
	c.connections = make(map[uint32]*MultiplexConnection)
	close(c.done)
	if c.conn != nil {
		c.conn.Close()
	}
	err := c.reconnect()
	if err != nil {
		c.state = Closed
		close(c.mDone)
		c.pool.connections.Delete(c.address)
	}
}

func (c *Connection) reconnect() error {
	conn, err := dialWithTimeout(c.dialOpts)
	if err != nil {
		return err
	}
	c.done = make(chan struct{})
	c.conn = conn
	c.decoder = codec.New(conn)
	c.buffer.done = c.done
	c.state = Connected
	c.err = nil
	go c.reader()
	go c.writer()
	return nil
}

// The response handling logic of the TCP connection.
// 1. Read from the connection and decode it to the Response.
// 2. Send the response to the corresponding multiplex connection based on the serialNo.
func (c *Connection) reader() {
	var lastErr error
	for {
		select {
		case <-c.done:
			return
		default:
		}
		rsp, err := c.decoder.Decode()
		if err != nil {
			lastErr = err
			break
		}
		serialNo := rsp.GetSerialNo()
		c.mu.RLock()
		mc, ok := c.connections[serialNo]
		c.mu.RUnlock()
		if !ok {
			continue
		}
		mc.reader.recv <- rsp
		mc.conn.remove(serialNo)
	}
	c.close(lastErr, c.done)
}

func (c *Connection) writer() {
	var lastErr error
	for {
		select {
		case <-c.done:
			return
		default:
		}
		req, err := c.buffer.get()
		if err != nil {
			lastErr = err
			break
		}
		if err := c.write(req); err != nil {
			lastErr = err
			break
		}
	}
	c.close(lastErr, c.done)
}

func (c *Connection) write(b []byte) error {
	sent := 0
	for sent < len(b) {
		n, err := c.conn.Write(b[sent:])
		if err != nil {
			return err
		}
		sent += n
	}
	return nil
}

func (c *Connection) send(b []byte) error {
	if c.state == Closed {
		return ErrConnClosed
	}

	select {
	case c.buffer.buffer <- b:
		return nil
	case <-c.mDone:
		return c.err
	}
}

func (c *Connection) remove(id uint32) {
	c.mu.Lock()
	delete(c.connections, id)
	c.mu.Unlock()
}

type writerBuffer struct {
	buffer chan []byte
	done   <-chan struct{}
}

func (w *writerBuffer) get() ([]byte, error) {
	select {
	case req := <-w.buffer:
		return req, nil
	case <-w.done:
		return nil, ErrWriteBufferDone
	}
}
