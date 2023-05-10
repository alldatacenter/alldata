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

package ipdb

import (
	"bytes"
	"encoding/binary"
	"errors"
	"fmt"
	"io/ioutil"
	"net"
)

var (
	// ErrInvalidIP invalid ip format
	ErrInvalidIP = errors.New("invalid ip format")
)

// NewLocator .
func NewLocator(dataFile string) (loc *Locator, err error) {
	data, err := ioutil.ReadFile(dataFile)
	if err != nil {
		return nil, err
	}
	return NewLocatorWithData(data), nil
}

// NewLocatorWithData .
func NewLocatorWithData(data []byte) (loc *Locator) {
	loc = new(Locator)
	loc.init(data)
	return
}

// Locator .
type Locator struct {
	textData   []byte
	indexData1 []uint32
	indexData2 []uint32
	indexData3 []uint16
	index      []uint32
}

// LocationInfo .
type LocationInfo struct {
	Country string
	Region  string
	City    string
	Isp     string
}

// Find .
func (loc *Locator) Find(ipstr string) (*LocationInfo, error) {
	ip := net.ParseIP(ipstr).To4()
	if ip == nil || ip.To4() == nil {
		return nil, ErrInvalidIP
	}
	return loc.FindByUint(binary.BigEndian.Uint32([]byte(ip)))
}

// FindByUint .
func (loc *Locator) FindByUint(ip uint32) (*LocationInfo, error) {
	p := ip >> 24
	var start uint32
	if p == 0 {
		start = 0
	} else {
		start = loc.index[p-1]
	}
	end := loc.index[p]
	idx := loc.findIndexOffset(ip, start, end)
	return newLocationInfo(loc.textData[loc.indexData2[idx]:(loc.indexData2[idx] + uint32(loc.indexData3[idx]))])
}

func (loc *Locator) findIndexOffset(ip uint32, start, end uint32) uint32 {
	for start < end {
		mid := (start + end) / 2
		if ip > loc.indexData1[mid] {
			start = mid + 1
		} else if ip < loc.indexData1[mid] {
			end = mid
		} else {
			return mid
		}
	}
	return start
}

func (loc *Locator) init(data []byte) {
	nidx := binary.BigEndian.Uint32(data[0:4])
	loc.textData = data[(4 + 256*4 + nidx*10):]

	loc.index = make([]uint32, 256)
	for i := 0; i < 256; i++ {
		off := 4 + i*4
		loc.index[i] = binary.BigEndian.Uint32(data[off : off+4])
	}

	loc.indexData1 = make([]uint32, nidx)
	loc.indexData2 = make([]uint32, nidx)
	loc.indexData3 = make([]uint16, nidx)

	for i := 0; i < int(nidx); i++ {
		off := 4 + 256*4 + i*10
		loc.indexData1[i] = binary.BigEndian.Uint32(data[off : off+4])
		loc.indexData2[i] = binary.BigEndian.Uint32(data[off+4 : off+8])
		loc.indexData3[i] = binary.BigEndian.Uint16(data[off+8 : off+10])
	}
	return
}

func newLocationInfo(str []byte) (*LocationInfo, error) {
	var info *LocationInfo
	fields := bytes.Split(str, []byte("|"))
	switch len(fields) {
	case 3:
		info = &LocationInfo{
			Country: string(fields[0]),
			Region:  string(fields[1]),
			City:    string(fields[2]),
		}
	case 4:
		info = &LocationInfo{
			Country: string(fields[0]),
			Region:  string(fields[1]),
			City:    string(fields[2]),
			Isp:     string(fields[3]),
		}
	default:
		return nil, fmt.Errorf("unexpected ip info: %s", string(str))
	}
	/* const Null = "N/A"
	if len(info.Country) == 0 {
		info.Country = Null
	}
	if len(info.Region) == 0 {
		info.Region = Null
	}
	if len(info.City) == 0 {
		info.City = Null
	}
	if len(info.Isp) == 0 {
		info.Isp = Null
	} */
	return info, nil
}
