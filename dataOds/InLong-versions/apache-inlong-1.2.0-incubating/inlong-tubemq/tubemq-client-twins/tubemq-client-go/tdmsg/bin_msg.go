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

package tdmsg

import (
	"encoding/binary"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
)

const (
	binMsgTotalLenOffset     = 0
	binMsgBidOffset          = 5
	binMsgTidOffset          = 7
	binMsgExtFieldOffset     = 9
	binMsgDateTimeOffset     = 11
	binMsgCountOffset        = 15
	binMsgTotalLenOffsetSize = 4
	binMsgMsgTypeOffset      = 4
	binMsgBodyLenSize        = 4
	binMsgBodyLenOffset      = 21
	binMsgBodyOffset         = binMsgBodyLenSize + binMsgBodyLenOffset
	binMsgAttrLenSize        = 2
	binMsgMagicSize          = 2
)

type binMsg struct {
	totalLen uint32
	msgType  byte
	bidNum   uint16
	tidNum   uint16
	bodyLen  uint32
	dateTime uint64
	extField uint16
	attrLen  uint16
	msgCount uint16
	magic    uint16
	body     []byte
}

func newBinMsg(data []byte) (*binMsg, error) {
	bm := &binMsg{}
	rem := uint32(len(data))
	if rem < 4 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 totalLen parameter")
	}
	rem -= 4

	if rem < 1 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 msgType parameter")
	}
	bm.msgType = data[binMsgMsgTypeOffset]
	rem -= 1

	if rem < 2 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 bidNum parameter")
	}
	bm.bidNum = binary.BigEndian.Uint16(data[binMsgBidOffset : binMsgBidOffset+2])
	rem -= 2

	if rem < 2 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 tidNum parameter")
	}
	bm.tidNum = binary.BigEndian.Uint16(data[binMsgTidOffset : binMsgTidOffset+2])
	rem -= 2

	if rem < 2 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 extField parameter")
	}
	bm.extField = binary.BigEndian.Uint16(data[binMsgExtFieldOffset : binMsgExtFieldOffset+2])
	rem -= 2

	if rem < 4 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 dataTime parameter")
	}
	dateTime := binary.BigEndian.Uint32(data[binMsgDateTimeOffset : binMsgDateTimeOffset+4])
	rem -= 4
	bm.dateTime = uint64(dateTime) * 1000

	if rem < 2 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 cnt parameter")
	}
	bm.msgCount = binary.BigEndian.Uint16(data[binMsgCountOffset : binMsgCountOffset+2])
	rem -= 2

	// for uniqueID
	if rem < 4 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 uniq parameter")
	}
	rem -= 4

	if rem < 4 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 bodyLen parameter")
	}
	bm.bodyLen = binary.BigEndian.Uint32(data[binMsgBodyLenOffset : binMsgBodyLenOffset+4])
	rem -= 4

	if rem < bm.bodyLen+2 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for data v4 attr_len parameter")
	}
	attrLenPos := binMsgBodyOffset + bm.bodyLen
	bm.attrLen = binary.BigEndian.Uint16(data[attrLenPos : attrLenPos+binMsgAttrLenSize])

	if rem < uint32(bm.attrLen)+2 {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for v4 msgMagic data")
	}
	magicPos := binMsgBodyOffset + bm.bodyLen + binMsgAttrLenSize + uint32(bm.attrLen)
	bm.magic = binary.BigEndian.Uint16(data[magicPos : magicPos+binMsgMagicSize])
	bm.magic &= 0xFFFF

	bm.body = data[binMsgBodyOffset : binMsgBodyOffset+bm.bodyLen]
	return bm, nil
}
