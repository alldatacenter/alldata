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

package framer

import (
	"encoding/binary"
	"errors"
)

// Framer types and other constants
const (
	defaultSize = 64 * 1024
)

// Framer errors
var (
	ErrIncompleteFrame                   = errors.New("incomplete frame")
	ErrInvalidFrameLen                   = errors.New("invalid frame length")
	ErrInvalidFrameLenCfg                = errors.New("invalid field length for length field base Framer, expect(1, 2, 3, 4, 8)")
	ErrExceedMaxFrameLen                 = errors.New("exceed max frame length")
	ErrFrameLenLessThanLenFieldEndOffset = errors.New("frame length less then length field end offset")
	ErrNoEnoughBytesToTrip               = errors.New("no enough bytes to trip")
)

// Framer is the interface of a stream Framer
type Framer interface {
	// ReadFrame reads a frame from the raw stream input according to its config,
	// return the frame length, the payload beginning and ending offset
	ReadFrame(input []byte) (frameLen, payloadOffset, payloadOffsetEnd int, err error)
}

// LengthFieldCfg is the config of a length field base framer
type LengthFieldCfg struct {
	LittleEndian bool // use little endian or not, default: false
	MaxFrameLen  int  // max frame length in bytes
	FieldOffset  int  // the offset of the length field
	FieldLength  int  // the length of the length field itself
	Adjustment   int  // adjustment value, can be positive or negative
	BytesToStrip int  // bytes to strip
}

type lengthFieldBased struct {
	byteOrder binary.ByteOrder
	cfg       LengthFieldCfg
}

// NewLengthField news a length field based framer
func NewLengthField(cfg LengthFieldCfg) (Framer, error) {
	if cfg.FieldOffset < 0 {
		return nil, errors.New("invalid field offset for length field base Framer")
	}

	if cfg.FieldLength <= 0 {
		return nil, errors.New("invalid field length for length field base Framer")
	}

	if cfg.BytesToStrip < 0 {
		return nil, errors.New("invalid bytes to trip for length field base Framer")
	}

	if cfg.FieldLength != 1 && cfg.FieldLength != 2 && cfg.FieldLength != 3 && cfg.FieldLength != 4 && cfg.FieldLength != 8 {
		return nil, ErrInvalidFrameLenCfg
	}

	if cfg.MaxFrameLen <= 0 {
		cfg.MaxFrameLen = defaultSize
	}

	if cfg.FieldOffset+cfg.FieldLength >= cfg.MaxFrameLen {
		return nil, errors.New("invalid field offset and max frame length for length field base Framer")
	}

	var byteOrder binary.ByteOrder = binary.BigEndian
	if cfg.LittleEndian {
		byteOrder = binary.LittleEndian
	}

	return &lengthFieldBased{byteOrder: byteOrder, cfg: cfg}, nil
}

func (c *lengthFieldBased) ReadFrame(input []byte) (frameLen, payloadOffset, payloadOffsetEnd int, err error) {
	inLen := len(input)
	if inLen <= 0 {
		return 0, 0, 0, ErrIncompleteFrame
	}

	// length field end offset
	fieldEndOffset := c.cfg.FieldOffset + c.cfg.FieldLength

	// input length is not enough to read the length of a frame
	if inLen < fieldEndOffset {
		return 0, 0, 0, ErrIncompleteFrame
	}

	// length field buffer
	lenFieldBuf := input[c.cfg.FieldOffset:fieldEndOffset]
	// get the unadjusted length
	var unadjustedLen uint64
	switch c.cfg.FieldLength {
	case 1:
		unadjustedLen = uint64(lenFieldBuf[0])
	case 2:
		unadjustedLen = uint64(c.byteOrder.Uint16(lenFieldBuf))
	case 3:
		unadjustedLen = uint24(c.byteOrder, lenFieldBuf)
	case 4:
		unadjustedLen = uint64(c.byteOrder.Uint32(lenFieldBuf))
	case 8:
		unadjustedLen = c.byteOrder.Uint64(lenFieldBuf)
	default:
		return 0, 0, 0, ErrInvalidFrameLenCfg
	}

	// the unadjusted length is invalid
	if unadjustedLen <= 0 {
		return 0, 0, 0, ErrInvalidFrameLen
	}

	// get the adjusted length
	frameLen = fieldEndOffset + c.cfg.Adjustment + int(unadjustedLen)
	// adjusted length less than the length field end offset, no payload
	if frameLen < fieldEndOffset {
		return 0, 0, 0, ErrFrameLenLessThanLenFieldEndOffset
	}

	// adjusted length greater than the max frame length
	if frameLen > c.cfg.MaxFrameLen {
		return 0, 0, 0, ErrExceedMaxFrameLen
	}

	// adjusted length greater than the input length, input is not enough
	if frameLen > inLen {
		return 0, 0, 0, ErrIncompleteFrame
	}

	// no enough bytes to trip
	if c.cfg.BytesToStrip > frameLen {
		return 0, 0, 0, ErrNoEnoughBytesToTrip
	}

	// the actual frame length we need
	actualFrameLen := frameLen - c.cfg.BytesToStrip
	// the actual frame offset
	actualFrameOffset := c.cfg.BytesToStrip
	// actual frame end offset
	actualFrameOffsetEnd := actualFrameOffset + actualFrameLen

	return frameLen, actualFrameOffset, actualFrameOffsetEnd, nil
}

func uint24(byteOrder binary.ByteOrder, b []byte) uint64 {
	_ = b[2]
	if byteOrder == binary.LittleEndian {
		return uint64(b[0]) | uint64(b[1])<<8 | uint64(b[2])<<16
	}
	return uint64(b[2]) | uint64(b[1])<<8 | uint64(b[0])<<16
}
