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

package helper

import (
	"bytes"
	"errors"
	"io"
)

const (
	ASCII_0 = 48
	ASCII_9 = 57
	ASCII_a = 97
)

//https://github.com/git/git/blob/master/Documentation/technical/http-protocol.txt
func ReadGitSendPackHeader(reqBody io.ReadCloser) ([]byte, error) {
	var headBuffer bytes.Buffer
	reader := reqBody
	for {
		//每一行以4字节的十六进制开始，用于指定整行的长度
		headLenBytes, err := readBytesBySize(reader, 4)
		if err == io.EOF {
			headBuffer.Write(headLenBytes)
			break
		}
		if err != nil {
			return nil, err
		}
		headBuffer.Write(headLenBytes)
		headLen := getLengthInt(headLenBytes)
		if headLen == 0 {
			break
		}
		line, err := readBytesBySize(reader, int(headLen)-4)
		headBuffer.Write(line)
	}
	return headBuffer.Bytes(), nil
}

//https://github.com/git/git/blob/master/Documentation/technical/pack-protocol.txt
func NewPtkLine(line string) []byte {
	lineBytes := []byte(line + "\n")
	lineLen := len(lineBytes) + 4 //加上len自身长度
	lenBytes := getLengthBytes(lineLen)
	return append(lenBytes, lineBytes...)
}

func NewReportStatus(unpackStatus string, refState string, remoteMsg string) []byte {
	var statusBuffer bytes.Buffer
	var totalBuffer bytes.Buffer
	var headLines []string
	headLines = append(headLines, unpackStatus)
	headLines = append(headLines, refState)
	for _, line := range headLines {
		statusBuffer.Write(NewPtkLine(line))
	}
	statusBuffer.Write([]byte("0000"))

	headBytes := statusBuffer.Bytes()
	headLen := len(headBytes) + 4 //加上len自身长度
	totalBuffer.Write(getLengthBytes(headLen + 1))
	totalBuffer.WriteByte(1)
	totalBuffer.Write(headBytes)

	var msgBuffer bytes.Buffer
	msgBuffer.Write([]byte(remoteMsg + "\n"))
	msgBuffer.Write([]byte("0000"))
	msgBytes := msgBuffer.Bytes()
	msgLen := len(msgBytes)
	totalBuffer.Write(getLengthBytes(msgLen + 1))
	totalBuffer.WriteByte(2)
	totalBuffer.Write(msgBytes)
	return totalBuffer.Bytes()
}

func getLengthBytes(length int) []byte {
	hexArray := [4]byte{}
	for i := 0; i < 4; i++ {
		hexArray[3-i] = converIntToHexChar(length % 16)
		length /= 16
	}
	return hexArray[:]
}
func getLengthInt(b []byte) int {
	return convertHexCharToInt(b[3]) | convertHexCharToInt(b[2])<<4 | convertHexCharToInt(b[1])<<8 | convertHexCharToInt(b[0])<<16
}

//十六进制字符转int
func convertHexCharToInt(b byte) int {
	if b <= ASCII_9 && b >= ASCII_0 {
		return int(b - ASCII_0)
	} else {
		return int(b - ASCII_a + 10)
	}
}

func converIntToHexChar(num int) byte {
	if num < 10 {
		return byte(ASCII_0 + num)
	} else {
		return byte(ASCII_a + num - 10)
	}
}

func readBytesBySize(reader io.Reader, size int) ([]byte, error) {
	data := make([]byte, size)
	readed := 0
	for {
		read, err := reader.Read(data[readed:])
		if err == io.EOF {
			return data, err
		}
		if err != nil {
			return nil, err
		}
		readed += read
		if readed == size {
			return data, nil
		} else if readed > size {
			return nil, errors.New("read byte large than input")
		}
	}
}
