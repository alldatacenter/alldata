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

package udp

import (
	"fmt"
	"net"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/exporter"
)

type config struct {
	Addr           string        `file:"addr"`
	Timeout        time.Duration `file:"timeout"`
	FragmentSize   int           `file:"fragment_size" default:"1024" env:"UDP_FRAGMENT_SIZE"`
	PacketSplitter string        `file:"packet_splitter" default:"\n" env:"UDP_PACKET_SPLITTER"`
}

type provider struct {
	C   *config
	exp exporter.Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.exp = ctx.Service("logs-exporter-base").(exporter.Interface)
	return nil
}

func (p *provider) Start() error {
	return p.exp.NewConsumer(p.newOutput)
}

func (p *provider) Close() error { return nil }

func (p *provider) newOutput(i int) (exporter.Output, error) {
	udpAddr, err := net.ResolveUDPAddr("udp", p.C.Addr)
	if err != nil {
		return nil, fmt.Errorf("fail to resolve udp addr %s", err)
	}
	conn, err := net.DialUDP("udp", nil, udpAddr)
	if err != nil {
		return nil, err
	}
	return &udpOutput{udpAddr, conn, p.C.Timeout, p.C.FragmentSize, p.C.PacketSplitter}, nil
}

type udpOutput struct {
	addr           *net.UDPAddr
	conn           *net.UDPConn
	timeout        time.Duration
	fragmentSize   int
	packetSplitter string
}

func (o *udpOutput) Write(logkey string, data []byte) error {
	if len(o.packetSplitter) > 0 {
		data = append(data, []byte(o.packetSplitter)...)
	}
	if o.fragmentSize <= 0 {
		return o.WriteOnce(data)
	}
	for newLen := len(data); newLen > 0; newLen = len(data) {
		if newLen <= o.fragmentSize {
			return o.WriteOnce(data)
		}

		fragment := data[:o.fragmentSize]
		err := o.WriteOnce(fragment)
		if err != nil {
			return err
		}

		data = data[o.fragmentSize:]
	}
	return nil
}

func (o *udpOutput) WriteOnce(data []byte) error {
	o.conn.SetWriteDeadline(time.Now().Add(o.timeout))
	_, err := o.conn.Write(data)
	if err != nil {
		conn, err := net.DialUDP("udp", nil, o.addr)
		if err != nil {
			return err
		}
		o.conn = conn
	}
	return err
}

func init() {
	servicehub.Register("logs-exporter-udp", &servicehub.Spec{
		Services:     []string{"logs-exporter-udp"},
		Dependencies: []string{"logs-exporter-base"},
		Description:  "logs export to udp",
		ConfigFunc: func() interface{} {
			return &config{Timeout: 30 * time.Second}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
	servicehub.Register("logs-exporter-logstash-udp", &servicehub.Spec{
		Services:     []string{"logs-exporter-logstash-udp"},
		Dependencies: []string{"logs-exporter-base"},
		Description:  "logs export to udp",
		ConfigFunc: func() interface{} {
			return &config{Timeout: 30 * time.Second}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
