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
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
	"github.com/golang/snappy"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
)

const (
	vn = iota - 1
	v0
	v1
	v2
	v3
	v4
	TubeMQTDMsgV4MsgFormatSize     = 29
	TubeMQTDMsgV4MsgCountOffset    = 15
	TubeMQTDMsgV4MsgExtFieldOffset = 9
)

// DataItem represents the parsed data.
type DataItem struct {
	Length uint32
	Data   []byte
}

// TubeMQTDMsg represents the structure of td msg.
type TubeMQTDMsg struct {
	IsNumBid   bool
	Version    int32
	CreateTime uint64
	MsgCount   uint32
	AttrCount  uint32
	Attr2Data  map[string][]*DataItem
}

// New decodes a TubeMQTDMsg from the given bytes.
func New(data []byte) (*TubeMQTDMsg, error) {
	m := &TubeMQTDMsg{
		Attr2Data: make(map[string][]*DataItem),
	}
	err := m.parseTDMsg(data)
	return m, err
}

func (m *TubeMQTDMsg) parseTDMsg(data []byte) error {
	if len(data) == 0 {
		return errors.New("length of data is zero")
	}

	pos := 0
	rem := len(data)

	ver, err := getMagic(data)
	if err != nil {
		return err
	}

	pos += 2
	rem -= 2
	m.Version = ver

	switch m.Version {
	case v4:
		return m.parseV4(data[pos : pos+rem])
	default:
		return m.parseDefault(data[pos:pos+rem], m.Version)
	}
}

func (m *TubeMQTDMsg) parseV4(data []byte) error {
	rem := len(data)
	if rem < TubeMQTDMsgV4MsgFormatSize {
		return errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for v4 msg fixed data")
	}
	if rem < 2 {
		return errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for v4 msgCount parameter")
	}
	m.MsgCount = uint32(binary.BigEndian.Uint16(data[TubeMQTDMsgV4MsgCountOffset : TubeMQTDMsgV4MsgCountOffset+2]))
	rem -= 2

	if rem < 2 {
		return errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for v4 extendField parameter")
	}
	isNumBid := uint32(binary.BigEndian.Uint16(data[TubeMQTDMsgV4MsgExtFieldOffset : TubeMQTDMsgV4MsgExtFieldOffset+2]))
	isNumBid &= 0x4
	if isNumBid == 1 {
		m.IsNumBid = true
	}
	return m.parseBinMsg(data)
}

func (m *TubeMQTDMsg) parseBinMsg(data []byte) error {
	bm, err := newBinMsg(data)
	if err != nil {
		return err
	}
	m.CreateTime = bm.dateTime

	var commonAttrMap map[string]string
	if bm.attrLen > 0 {
		attrLenPos := binMsgBodyOffset + bm.bodyLen + binMsgAttrLenSize
		commonAttr := data[attrLenPos : attrLenPos+uint32(bm.attrLen)]
		commonAttrMap = util.SplitToMap(string(commonAttr), "&", "=")
		if len(commonAttrMap) == 0 {
			return errs.New(errs.RetTDMsgParseFailure, "parse v4 common attribute parameter error")
		}
	}

	bodyLen := bm.bodyLen
	body := bm.body
	if (bm.msgType&0xE0)>>5 == 1 {
		var err error
		body, err = getDecodedData(body, int32(v4))
		if err != nil {
			return err
		}
		bodyLen = uint32(len(body))
	}
	commonAttrMap["dt"] = strconv.FormatUint(m.CreateTime, 10)
	if bm.extField&0x4 == 0x0 {
		commonAttrMap["bid"] = strconv.FormatUint(uint64(bm.bidNum), 10)
		commonAttrMap["tid"] = strconv.FormatUint(uint64(bm.tidNum), 10)
	}
	commonAttrMap["cnt"] = "1"

	bodyRemained := bodyLen
	bodyPos := uint32(0)
	for bodyRemained > 0 && bm.msgCount > 0 {
		if bodyRemained < 4 {
			return errs.New(errs.RetTDMsgParseFailure,
				fmt.Sprintf("parse message error: no enough data length for v%d item's msgLength parameter", v4))
		}
		di, err := parseDataItem(body[bodyPos : bodyPos+bodyRemained])
		if err != nil {
			return err
		}
		if di.Length == 0 {
			continue
		}
		bodyPos += 4 + di.Length
		bodyRemained -= 4 + di.Length
		var key string
		if bm.extField&0x1 == 0x0 {
			key = util.Join(commonAttrMap, "&", "=")
		} else {
			singleAttrLen := binary.BigEndian.Uint32(body[bodyPos:bodyRemained])
			bodyPos += 4
			bodyRemained -= 4
			if singleAttrLen <= 0 || singleAttrLen > bodyRemained {
				return errs.New(errs.RetTDMsgParseFailure,
					fmt.Sprintf("parse message error: invalid v4 attr's attr Length"))
			}
			singleAttr := body[bodyPos : bodyPos+singleAttrLen]
			finalAttr := util.SplitToMap(string(singleAttr), "&", "=")
			for k, v := range commonAttrMap {
				finalAttr[k] = v
			}
			if len(finalAttr) == 0 {
				return errs.New(errs.RetTDMsgParseFailure, "parse message error: invalid v4 private attribute")
			}
			key = util.Join(finalAttr, "&", "=")
		}
		m.Attr2Data[key] = append(m.Attr2Data[key], di)
		bm.msgCount--
	}
	return nil
}

func (m *TubeMQTDMsg) parseDefault(data []byte, ver int32) error {
	pos := 0
	rem := len(data)
	createTime, err := getCreateTime(data)
	if err != nil {
		return err
	}
	m.CreateTime = createTime
	pos += 8
	rem -= 8

	if m.Version >= 2 {
		if rem < 4 {
			return errs.New(errs.RetTDMsgParseFailure,
				"parse message error: no enough data length for msgCount parameter")
		}
		m.MsgCount = binary.BigEndian.Uint32(data[pos : pos+4])
		pos += 4
		rem -= 4
	}

	if rem < 4 {
		return errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for attrCount parameter")
	}
	m.AttrCount = binary.BigEndian.Uint32(data[pos : pos+4])

	return m.parseDefaultBody(data[pos+4:], ver)
}

func (m *TubeMQTDMsg) parseDefaultBody(data []byte, ver int32) error {
	pos := uint32(0)
	rem := uint32(len(data))
	for i := uint32(0); i < m.AttrCount; i++ {
		if rem <= 2 {
			if i == 0 {
				return errs.New(errs.RetTDMsgParseFailure,
					"parse message error: invalid data body length length")
			} else {
				break
			}
		}

		origAttrLen := uint32(binary.BigEndian.Uint16(data[pos : pos+2]))
		pos += 2
		rem -= 2
		if origAttrLen <= 0 || origAttrLen > rem {
			return errs.New(errs.RetTDMsgParseFailure, "invalid attr length")
		}

		commAttr := data[pos : pos+origAttrLen]
		pos += origAttrLen
		rem -= origAttrLen
		if m.Version == v2 {
			// For dataCount
			if rem < 4 {
				return errs.New(errs.RetTDMsgParseFailure,
					"parse message error: no enough data length for data count parameter")
			}
			pos += 4
			rem -= 4
		}

		if rem < 4 {
			return errs.New(errs.RetTDMsgParseFailure,
				"parse message error: no enough data length for data len parameter")
		}
		bodyDataLen := binary.BigEndian.Uint32(data[pos : pos+4])
		pos += 4
		rem -= 4
		if bodyDataLen <= 0 || bodyDataLen > rem {
			return errs.New(errs.RetTDMsgParseFailure,
				"parse message error: invalid data length")
		}

		if rem < 1 {
			return errs.New(errs.RetTDMsgParseFailure,
				"parse message error: no enough char data length")
		}
		compress := data[pos]
		pos += 1
		rem -= 1
		body := data[pos : pos+bodyDataLen-1]
		bodyLen := int(bodyDataLen - 1)
		if compress != 0 {
			var err error
			body, err = getDecodedData(body, ver)
			if err != nil {
				return err
			}
			bodyLen = len(body)
		}
		pos += bodyDataLen - 1
		rem -= bodyDataLen - 1

		itemPos := uint32(0)
		itemRem := uint32(bodyLen)
		for itemRem > 0 {
			if itemRem < 4 {
				return errs.New(errs.RetTDMsgParseFailure,
					fmt.Sprintf("parse message error: no enough data length for v%d item's msgLength parameter", ver))
			}
			di, err := parseDataItem(body[itemPos : itemPos+itemRem])
			if err != nil {
				return err
			}
			if di.Length == 0 {
				continue
			}
			itemPos += 4 + di.Length
			itemRem -= 4 + di.Length
			attr := commAttr
			if ver == v3 && itemRem > 0 {
				if itemRem < 4 {
					return errs.New(errs.RetTDMsgParseFailure,
						"parse message error: no enough data length for v3 attr's single length parameter")
				}
				singleAttrLen := binary.BigEndian.Uint32(body[itemPos:itemRem])
				itemPos += 4
				itemRem -= 4
				if singleAttrLen <= 0 || singleAttrLen > itemRem {
					return errs.New(errs.RetTDMsgParseFailure,
						"parse message error: invalid v3 attr's attr Length")
				}
				attr = body[itemPos : itemPos+singleAttrLen]
				itemPos += singleAttrLen
				itemRem -= singleAttrLen
			}
			m.Attr2Data[string(attr)] = append(m.Attr2Data[string(attr)], di)
		}
	}
	return nil
}

// ParseAttrValue parses the given attrs to a map of attr to value.
func (m *TubeMQTDMsg) ParseAttrValue(attr string) (map[string]string, error) {
	if len(attr) == 0 {
		return nil, errors.New("parameter attr value is empty")
	}
	if strings.Index(attr, "&") == -1 {
		return nil, errors.New("un-regular attr_value error: not found token '&'")
	}
	return util.SplitToMap(attr, "&", "="), nil
}

func getMagic(data []byte) (int32, error) {
	pos := 0
	rem := len(data)
	if rem < 4 {
		return vn, errs.New(errs.RetTDMsgParseFailure, "parse message error: no enough data length for magic data")
	}

	if data[pos] == 0xf && data[pos+1] == 0x2 && data[pos+rem-2] == 0xf && data[pos+rem-1] == 0x2 {
		return v2, nil
	}

	if data[pos] == 0xf && data[pos+1] == 0x1 && data[pos+rem-2] == 0xf && data[pos+rem-1] == 0x1 {
		return v1, nil
	}

	if data[pos] == 0xf && data[pos+1] == 0x4 && data[pos+rem-2] == 0xf && data[pos+rem-1] == 0x4 {
		return v4, nil
	}

	if data[pos] == 0xf && data[pos+1] == 0x3 && data[pos+rem-2] == 0xf && data[pos+rem-1] == 0x3 {
		return v3, nil
	}

	if data[pos] == 0xf && data[pos+1] == 0x0 && data[pos+rem-2] == 0xf && data[pos+rem-1] == 0x0 {
		return v0, nil
	}

	return vn, errs.New(errs.RetTDMsgParseFailure, "parse message error: Unsupported message format")
}

func getCreateTime(data []byte) (uint64, error) {
	remain := len(data)
	if remain < 8 {
		return 0, errs.New(errs.RetTDMsgParseFailure,
			"parse message error: no enough data length for createTime data")
	}
	return binary.BigEndian.Uint64(data[0:8]), nil
}

func getDecodedData(data []byte, ver int32) ([]byte, error) {
	decodedLen, err := snappy.DecodedLen(data)
	if err != nil {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			fmt.Sprintf("parse message error:  snappy uncompressed v%d's compress's length failure", ver))
	}
	decodedData := make([]byte, 0, decodedLen)
	decodedData, err = snappy.Decode(decodedData, data)
	if err != nil {
		return nil, errs.New(errs.RetTDMsgParseFailure,
			fmt.Sprintf("parse message error:  snappy uncompressed v%d's compress's data failure", ver))
	}
	return decodedData, nil
}

func parseDataItem(data []byte) (*DataItem, error) {
	di := &DataItem{
		Length: 0,
	}
	pos := uint32(0)
	rem := uint32(len(data))
	singleMsgLen := binary.BigEndian.Uint32(data[0:4])
	if singleMsgLen <= 0 {
		return di, nil
	}
	pos += 4
	rem -= 4
	if singleMsgLen > rem {
		return nil, errs.New(errs.RetTDMsgParseFailure, "parse message error: invalid default attr's msg Length")
	}
	di.Length = singleMsgLen
	di.Data = data[pos : pos+singleMsgLen]
	return di, nil
}
