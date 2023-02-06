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

package elasticsearch

import (
	"context"
	"fmt"
	"math"
	"strconv"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda-proto-go/oap/entity/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/entity/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
)

type (
	config struct {
		WriteTimeout time.Duration `file:"write_timeout" default:"1m"`
		QueryTimeout time.Duration `file:"query_timeout" default:"1m"`
		IndexType    string        `file:"index_type" default:"entity"`
		Pattern      string        `file:"pattern"`
	}
	provider struct {
		Cfg            *config
		Log            logs.Logger
		ES1            elasticsearch.Interface `autowired:"elasticsearch@entity" optional:"true"`
		ES2            elasticsearch.Interface `autowired:"elasticsearch" optional:"true"`
		es             elasticsearch.Interface
		ctx            servicehub.Context
		ptn            *index.Pattern
		writeTimeoutMS string
		queyTimeoutMS  string
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	if p.ES1 != nil {
		p.es = p.ES1
	} else if p.ES2 != nil {
		p.es = p.ES2
	} else {
		return fmt.Errorf("elasticsearch is required")
	}
	if err := p.initIndexPattern(); err != nil {
		return err
	}

	p.writeTimeoutMS = getTimeoutMS(p.Cfg.WriteTimeout)
	p.queyTimeoutMS = getTimeoutMS(p.Cfg.QueryTimeout)
	return nil
}

func (p *provider) initIndexPattern() error {
	if len(p.Cfg.Pattern) <= 0 {
		return fmt.Errorf("pattern is required")
	}
	ptn, err := index.BuildPattern(p.Cfg.Pattern)
	if err != nil {
		return err
	}
	if ptn.KeyNum != 0 && ptn.KeyNum != 1 {
		return fmt.Errorf("pattern(%q) contains too many keys", ptn.Pattern)
	} else if ptn.KeyNum == 1 && ptn.Keys[0] != "type" {
		return fmt.Errorf("pattern(%q) contains type only", ptn.Pattern)
	}
	if ptn.VarNum > 0 {
		return fmt.Errorf("pattern(%q) can't contains vars", ptn.Pattern)
	}
	p.ptn = ptn
	return nil
}

func getTimeoutMS(t time.Duration) string {
	ms := int64(t.Milliseconds())
	if ms < 1 {
		ms = 1
	}
	return strconv.FormatInt(ms, 10) + "ms"
}

var _ storage.Storage = (*provider)(nil)

func (p *provider) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	return &Writer{
		p:   p,
		ctx: ctx,
	}, nil
}

func (p *provider) encodeToDocument(ctx context.Context) func(val interface{}) (index, id, typ string, body interface{}, err error) {
	return func(val interface{}) (index, id, typ string, body interface{}, err error) {
		data := val.(*pb.Entity)
		processInvalidFields(data)
		index = p.getIndex(data.Type, data.Key)
		id = p.getDocumentID(data.Type, data.Key)
		return index, id, p.Cfg.IndexType, data, nil
	}
}

func (p *provider) getIndex(typ, key string) string {
	if p.ptn.KeyNum == 1 {
		index, _ := p.ptn.Fill(index.NormalizeKey(typ))
		return index
	}
	return p.ptn.Pattern
}

func (p *provider) getIndices(typ string) []string {
	if p.ptn.KeyNum == 1 {
		if len(typ) > 0 {
			index, _ := p.ptn.Fill(typ)
			return []string{index}
		}
		index, _ := p.ptn.Fill("*")
		return []string{index}
	}
	return []string{p.ptn.Pattern}
}

func (p *provider) getDocumentID(typ, key string) string {
	return typ + "/" + key
}

const (
	esMaxValue = float64(math.MaxInt64)
	esMinValue = float64(math.MinInt64)
)

func processInvalidFields(data *pb.Entity) {
	fields := data.Values
	if fields == nil {
		return
	}
	for k, v := range fields {
		switch val := v.AsInterface().(type) {
		case float64:
			if val < esMinValue || esMaxValue < val {
				fields[k] = structpb.NewStringValue(strconv.FormatFloat(val, 'f', -1, 64))
			}
		}
	}
}

func init() {
	servicehub.Register("entity-storage-elasticsearch", &servicehub.Spec{
		Services:             []string{"entity-storage", "entity-storage-writer"},
		Dependencies:         []string{"elasticsearch"},
		OptionalDependencies: []string{"elasticsearch.index.initializer"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator:              func() servicehub.Provider { return &provider{} },
	})
}
