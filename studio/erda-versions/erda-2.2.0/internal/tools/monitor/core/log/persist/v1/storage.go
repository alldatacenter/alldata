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

package storage

import (
	"bytes"
	"compress/gzip"
	"fmt"
	"io"
	"io/ioutil"
	"strings"
	"time"
	"unsafe"

	"github.com/erda-project/erda-infra/providers/cassandra"
	logmodule "github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/persist/v1/schema"
)

func (p *provider) createLogStatementBuilder() cassandra.StatementBuilder {
	var buf bytes.Buffer
	return &LogStatement{
		p:          p,
		gzipWriter: gzip.NewWriter(&buf),
	}
}

type LogStatement struct {
	gzipWriter *gzip.Writer
	p          *provider
}

func (ls *LogStatement) GetStatement(data interface{}) (string, []interface{}, error) {
	switch data.(type) {
	case *logmodule.Log:
		return ls.p.getLogStatement(data.(*logmodule.Log), ls.gzipWriter)
	case *logmodule.Meta:
		return ls.p.getMetaStatement(data.(*logmodule.Meta))
	default:
		return "", nil, fmt.Errorf("value %#v must be Log or LogMeta", data)
	}
}

func (p *provider) getLogStatement(log *logmodule.Log, reusedWriter *gzip.Writer) (string, []interface{}, error) {
	var requestID *string // request_id 字段不存在时为null，所以使用指针
	if rid, ok := log.Tags["request-id"]; ok {
		requestID = &rid
	}

	table := schema.DefaultBaseLogTable
	if org, ok := log.Tags[diceOrgNameKey]; ok {
		table = schema.BaseLogWithOrgName(org)
	}

	content, err := gzipContentV2(log.Content, reusedWriter)
	if err != nil {
		return "", nil, err
	}
	// nolint
	cql := fmt.Sprintf(`INSERT INTO %s (source, id, stream, time_bucket, timestamp, offset, content, level, request_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) USING TTL ?;`, table)
	return cql, []interface{}{
		log.Source,
		log.ID,
		log.Stream,
		trncateDate(log.Timestamp),
		log.Timestamp,
		log.Offset,
		content,
		log.Tags["level"],
		requestID,
		int(p.Retention.GetTTLByTags(log.Source, log.Tags).Seconds()),
	}, nil
}

func (p *provider) getMetaStatement(meta *logmodule.Meta) (string, []interface{}, error) {
	cql := `INSERT INTO spot_prod.base_log_meta (source, id, tags) VALUES (?, ?, ?) USING TTL ?;`
	return cql, []interface{}{
		meta.Source,
		meta.ID,
		meta.Tags,
		int(p.Retention.GetTTLByTags(meta.Source, meta.Tags).Seconds()),
	}, nil
}

func trncateDate(unixNano int64) int64 {
	const day = time.Hour * 24
	return unixNano - unixNano%int64(day)
}

func gzipContent(content string) ([]byte, error) {
	reader, err := compressWithPipe(strings.NewReader(content))
	if err != nil {
		return nil, err
	}
	return ioutil.ReadAll(reader)
}

func compressWithPipe(reader io.Reader) (io.Reader, error) {
	pipeReader, pipeWriter := io.Pipe()
	gzipWriter := gzip.NewWriter(pipeWriter)

	var err error
	go func() {
		_, err = io.Copy(gzipWriter, reader)
		gzipWriter.Close()
		// subsequent reads from the read half of the pipe will
		// return no bytes and the error err, or EOF if err is nil.
		pipeWriter.CloseWithError(err)
	}()

	return pipeReader, err
}

func gzipContentV2(content string, reusedWriter *gzip.Writer) ([]byte, error) {
	reader, err := compressWithPipeV2(bytes.NewReader(*(*[]byte)(unsafe.Pointer(&content))), reusedWriter)
	if err != nil {
		return nil, err
	}
	return ioutil.ReadAll(reader)
}

func compressWithPipeV2(reader io.Reader, reusedWriter *gzip.Writer) (io.Reader, error) {
	pipeReader, pipeWriter := io.Pipe()
	reusedWriter.Reset(pipeWriter)

	go func() {
		_, err := io.Copy(reusedWriter, reader)
		if err != nil {
			fmt.Printf("gzip copy failed: %s\n", err)
		}
		reusedWriter.Close()
		pipeWriter.CloseWithError(err)
	}()

	return pipeReader, nil
}
