//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dataproxy

import (
	"bytes"
	"context"
	"encoding/binary"
	"strconv"
	"strings"
	"time"
	"unsafe"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/bufferpool"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/syncx"

	"github.com/klauspost/compress/snappy"
)

var (
	byteOrder       = binary.BigEndian
	heartbeatRsp    = []byte{0x00, 0x00, 0x00, 0x01, 0x01}
	heartbeatRspLen = len(heartbeatRsp)
)

const (
	msgTypeBatch     uint8 = 5
	msgTypeHeartbeat uint8 = 1
)

type heartbeatReq struct {
}

func (h heartbeatReq) encode(buffer *bytes.Buffer) []byte {
	var bodyLen uint32 = 0
	var attrLen uint32 = 0
	totalLen := 1 + 4 + bodyLen + 4 + attrLen

	buffer.Grow(int(totalLen + 4))
	uint32LenBuf := make([]byte, 4)
	byteOrder.PutUint32(uint32LenBuf, totalLen)
	_, _ = buffer.Write(uint32LenBuf)

	_, _ = buffer.Write([]byte{msgTypeHeartbeat})

	byteOrder.PutUint32(uint32LenBuf, bodyLen)
	_, _ = buffer.Write(uint32LenBuf)

	byteOrder.PutUint32(uint32LenBuf, attrLen)
	_, _ = buffer.Write(uint32LenBuf)

	return buffer.Bytes()
}

type batchCallback func()
type batchReq struct {
	workerID     string
	batchID      string
	groupID      string
	streamID     string
	dataReqs     []*sendDataReq
	dataSize     int
	batchTime    time.Time
	lastSendTime time.Time
	retries      int
	bufferPool   bufferpool.BufferPool
	bytePool     bufferpool.BytePool
	buffer       *bytes.Buffer
	callback     batchCallback
	metrics      *metrics
	addColumns   string
}

func (b *batchReq) append(req *sendDataReq) {
	b.dataReqs = append(b.dataReqs, req)
	b.dataSize += len(req.msg.Payload)
}

func (b *batchReq) done(err error) {
	errorCode := getErrorCode(err)
	for _, req := range b.dataReqs {
		req.done(err, errorCode)
	}

	if b.callback != nil {
		b.callback()
	}

	if b.buffer != nil && b.bufferPool != nil {
		b.bufferPool.Put(b.buffer)
		b.buffer = nil
	}

	if b.metrics != nil {
		if errorCode != errOK.strCode {
			b.metrics.incError(errorCode)
		}
		b.metrics.observeTime(errorCode, time.Since(b.batchTime).Milliseconds())
		b.metrics.observeSize(errorCode, b.dataSize)
	}
}

func (b *batchReq) encode() []byte {
	if b.bufferPool == nil {
		panic("batch req buffer pool is not set")
	}

	if b.bytePool == nil {
		panic("batch req byte pool is not set")
	}

	if b.buffer != nil {
		return b.buffer.Bytes()
	}

	b.buffer = b.bufferPool.Get()
	bodyBuffer := b.bufferPool.Get()
	defer b.bufferPool.Put(bodyBuffer)

	uint32LenBuf := make([]byte, 4)
	for _, req := range b.dataReqs {
		// fix：no line feed at the end of the payload, or it will be a NULL record in the data warehouse
		payload := req.msg.Payload
		payloadLen := len(payload)
		if payload[payloadLen-1] == '\n' {
			payload = payload[0 : payloadLen-1]
			payloadLen = len(payload)
		}
		dataLen := uint32(payloadLen)
		// write body len, 4 bytes
		byteOrder.PutUint32(uint32LenBuf, dataLen)
		_, _ = bodyBuffer.Write(uint32LenBuf)
		// write one body payload
		_, _ = bodyBuffer.Write(payload)
	}

	// uncompressed body
	bodyLen := bodyBuffer.Len()
	body := bodyBuffer.Bytes()

	// compress it when the body length is > 256
	compress := bodyLen > 256
	var compressBuf []byte
	if compress {
		compressBuf = b.bytePool.Get()
		defer b.bytePool.Put(compressBuf)

		requiredCompressBufSize := snappy.MaxEncodedLen(bodyLen)
		if requiredCompressBufSize <= 0 {
			compress = false
		} else {
			if cap(compressBuf) < requiredCompressBufSize {
				compressBuf = make([]byte, requiredCompressBufSize)
			}
			body = snappy.Encode(compressBuf, body)
			bodyLen = len(body)
		}
	}

	sb := strings.Builder{}
	sb.Grow(256)
	sb.WriteString("groupId=")
	sb.WriteString(b.groupID)

	sb.WriteString("&streamId=")
	sb.WriteString(b.streamID)

	sb.WriteString("&dt=")
	sb.WriteString(strconv.FormatInt(time.Now().UnixMilli(), 10))

	sb.WriteString("&mid=")
	sb.WriteString(b.batchID)

	// use batchID as sid, the server use sid to de-duping a batch
	sb.WriteString("&sid=")
	sb.WriteString(b.batchID)

	sb.WriteString("&wid=")
	sb.WriteString(b.workerID)

	sb.WriteString("&cnt=")
	sb.WriteString(strconv.FormatInt(int64(len(b.dataReqs)), 10))

	// if compressed, add one more attribute
	if compress {
		sb.WriteString("&cp=snappy")
	}

	// add columns
	if b.addColumns != "" {
		sb.WriteString(b.addColumns)
	}

	attr := sb.String()
	attrLen := sb.Len()

	// msg type: 1 byte, body length: 4 bytes, body, attributes length: 4 byte, attributes
	totalLen := 1 + 4 + bodyLen + 4 + attrLen

	// add more 4 bytes to store the total length
	b.buffer.Grow(int(totalLen + 4))
	// total length: 4 bytes
	byteOrder.PutUint32(uint32LenBuf, uint32(totalLen))
	_, _ = b.buffer.Write(uint32LenBuf)

	// msg type: 1 byte
	_, _ = b.buffer.Write([]byte{msgTypeBatch})

	// body length: 4 bytes
	byteOrder.PutUint32(uint32LenBuf, uint32(bodyLen))
	_, _ = b.buffer.Write(uint32LenBuf)
	// body
	_, _ = b.buffer.Write(body)

	// attributes length: 4 byte
	byteOrder.PutUint32(uint32LenBuf, uint32(attrLen))
	_, _ = b.buffer.Write(uint32LenBuf)
	// attributes
	_, _ = b.buffer.Write(*(*[]byte)(unsafe.Pointer(&attr)))

	return b.buffer.Bytes()
}

type batchRsp struct {
	errCode  int
	workerID string
	batchID  string
	groupID  string
	streamID string
	dt       string
}

func (b *batchRsp) decode(input []byte) {
	// var totalLen uint32
	// var msgType uint8
	var bodyLen uint32
	var attrLen uint32

	// buffer := bytes.NewBuffer(input)
	// _ = binary.Read(buffer, byteOrder, &totalLen)
	// totalLen = byteOrder.Uint32(input[0:4])

	// _ = binary.Read(buffer, byteOrder, &msgType)
	// msgType = input[4]

	// _ = binary.Read(buffer, byteOrder, &bodyLen)
	bodyLen = byteOrder.Uint32(input[5:9])

	// body := make([]byte, bodyLen)
	// _ = binary.Read(buffer, byteOrder, body)
	// body := input[9 : 9+bodyLen]

	// _ = binary.Read(buffer, byteOrder, &attrLen)
	attrLen = byteOrder.Uint32(input[9+bodyLen : 9+bodyLen+4])
	// attr := make([]byte, attrLen)
	// _ = binary.Read(buffer, byteOrder, attr)
	attr := input[9+bodyLen+4 : 9+bodyLen+4+attrLen]

	// fmt.Println(string(attr))

	attrList := strings.Split(string(attr), "&")
	for _, item := range attrList {
		kv := strings.Split(item, "=")
		if len(kv) != 2 {
			continue
		}

		k := kv[0]
		v := kv[1]
		switch k {
		case "errCode":
			b.errCode, _ = strconv.Atoi(v)
		case "groupId":
			b.groupID = v
		case "mid":
			b.batchID = v
		case "streamId":
			b.streamID = v
		case "wid":
			b.workerID = v
		case "dt":
			b.dt = v
		}
	}
}

type sendDataReq struct {
	ctx              context.Context
	msg              Message
	callback         Callback
	flushImmediately bool
	publishTime      time.Time
	semaphore        syncx.Semaphore
	metrics          *metrics
	workerID         string
}

func (s *sendDataReq) done(err error, errCode string) {
	if s.semaphore != nil {
		s.semaphore.Release()
	}

	if s.callback != nil {
		s.callback(s.msg, err)
	}

	if s.metrics != nil {
		if s.semaphore != nil {
			s.metrics.decPending(s.workerID)
		}
		if errCode == "" {
			errCode = getErrorCode(err)
		}

		s.metrics.incMessage(errCode)
	}
}

type closeReq struct {
	doneCh chan struct{}
}

func getWorkerIndex(workerID string) int {
	if workerID == "" {
		return -1
	}

	index, err := strconv.Atoi(workerID)
	if err != nil {
		return -1
	}
	return index
}
