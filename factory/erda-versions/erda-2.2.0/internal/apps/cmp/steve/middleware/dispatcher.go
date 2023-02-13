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

package middleware

import (
	"context"
	"strings"
	"time"

	"github.com/pkg/errors"
	"modernc.org/mathutil"
)

type Queue interface {
	Front() string
	pushFront(cmd string) error
	Back() string
	pushBack(cmd string) error
	Size() int
	Get(i int) (string, error)
	Set(i int, str string) error
	Begin() int
}

var (
	OutOfLengthSize = errors.New("cmd length out of buffer limit")
)

const (
	startIdx   = 1
	maxBufSize = 2048
)

// start with index 1
type commandQueue struct {
	// cmds contains each cmd of history, start from startIdx
	cmds    []string
	length  int
	maxSize int
}

func (c *commandQueue) Front() string {
	return c.cmds[0]
}
func (c *commandQueue) Begin() int {
	return startIdx
}
func (c *commandQueue) pushFront(cmd string) error {
	c.length = mathutil.Min(c.maxSize, c.length+1)
	for i := c.length - 1; i > 0; i-- {
		c.cmds[i+1] = c.cmds[i]
	}
	c.cmds[startIdx] = cmd
	return nil
}

func (c *commandQueue) Back() string {
	return c.cmds[c.length]
}

func (c *commandQueue) pushBack(cmd string) error {
	c.length = mathutil.Min(c.maxSize, c.length+1)
	c.cmds[c.length-1] = cmd
	return nil
}

func (c *commandQueue) Size() int {
	return c.length
}

func (c *commandQueue) Get(i int) (string, error) {
	if i <= c.length && i >= 0 {
		return c.cmds[i], nil
	}
	return "", errors.Errorf("queue index out of range")
}

func (c *commandQueue) Set(i int, str string) error {
	c.cmds[i] = str
	return nil
}

type dispatcher struct {
	ctx            context.Context
	closeChan      chan struct{}
	queue          Queue
	cursorLine     int
	cursorIdx      int
	cursorBufIdx   int
	length         int
	buf            []byte
	searchBuf      []byte
	inputBuf       []byte
	inputBufLength int
	searchIdx      int
	BufferMaxSize  int
	executeCommand string
	auditReqChan   chan *cmdWithTimestamp
	state          int
}

func (d *dispatcher) Close() error {
	d.closeChan <- struct{}{}
	return nil
}

func (d *dispatcher) MoveLineHead() error {
	return d.CUB(d.length + startIdx)
}

func (d *dispatcher) MoveLineEnd() error {
	return d.CUF(d.length + startIdx)
}

func (d *dispatcher) QuitSearchMode() error {
	d.searchIdx = startIdx
	return nil
}

func (d *dispatcher) Enter() error {
	d.sendAuditReq()
	d.reset()
	return nil
}

func (d *dispatcher) Reset() error {
	d.reset()
	return nil
}

func (d *dispatcher) NextCommand() error {
	return d.CUD(1)
}

func (d *dispatcher) PreviousCommand() error {
	return d.CUU(1)
}

func (d *dispatcher) EnterWithRedisplay() error {
	return d.sendAuditReq()
}

func (d *dispatcher) ReverseSearch(c byte) error {
	return d.reverseSearch(c)
}

func (d *dispatcher) Search(c byte) error {
	return d.search(c)
}

func (d *dispatcher) ShowBuffer() error {
	if d.inputBufLength+d.length > maxBufSize {
		return nil
	}
	copy(d.buf[d.cursorIdx+d.inputBufLength:], d.buf[d.cursorIdx:])
	copy(d.buf[d.cursorIdx:], d.inputBuf[startIdx:d.inputBufLength+startIdx])
	d.length += d.inputBufLength
	d.cursorIdx += d.inputBufLength
	return nil
}

func (d *dispatcher) Clean() error {
	d.resetWithCmdline()
	return nil
}

func (d *dispatcher) RemoveForwardWord() error {
	start, end := d.findForwardWord()
	d.removeAndStore(start, end)
	return nil
}

func (d *dispatcher) RemoveBackwardWord() error {
	start, end := d.findBackwardWord()
	d.removeAndStore(start, end)
	d.cursorIdx = start
	return nil
}

func (d *dispatcher) RemoveForwardAll() error {
	d.removeAndStore(d.cursorIdx, d.length)
	return nil
}

func (d *dispatcher) RemoveBackwardAll() error {
	d.removeAndStore(startIdx, d.cursorIdx-1)
	d.cursorIdx = startIdx
	return nil
}

// removeAndStore remove buf [start,end] and store it
func (d *dispatcher) removeAndStore(startIdx, endIdx int) {
	if endIdx < startIdx || startIdx <= 0 || endIdx <= 0 {
		return
	}
	newLen := endIdx - startIdx + 1
	copy(d.inputBuf[startIdx+newLen:], d.inputBuf[startIdx:])
	copy(d.inputBuf[startIdx:], d.buf[startIdx:endIdx+1])
	copy(d.buf[startIdx:], d.buf[endIdx+1:])
	d.length -= newLen
	d.inputBufLength += newLen
}

func (d *dispatcher) findForwardWord() (int, int) {
	if d.cursorIdx == d.length+startIdx {
		return d.cursorIdx, 0
	}
	end := d.cursorIdx + 1
	// skip any space
	for ; end <= d.length && d.buf[end] == ' '; end++ {
	}
	// find space to the end
	for ; end < d.length+startIdx; end++ {
		if d.buf[end] == ' ' {
			end--
			break
		}
	}
	if end >= d.length+startIdx {
		end = d.length
	}
	return d.cursorIdx, end
}

func (d *dispatcher) findBackwardWord() (int, int) {
	if d.cursorIdx == startIdx {
		return startIdx, 0
	}
	start := d.cursorIdx - 1
	// skip any space
	for ; start >= startIdx && d.buf[start] == ' '; start-- {
	}
	// find space to the head
	for ; start >= startIdx; start-- {
		if d.buf[start] == ' ' {
			start++
			break
		}
	}
	if start == 0 {
		start = startIdx
	}
	return start, mathutil.Max(d.cursorIdx-1, startIdx)
}

func (d *dispatcher) RemoveForwardCharacterOrClose() error {
	if d.length > 0 {
		if d.cursorIdx < d.length+startIdx {
			copy(d.buf[d.cursorIdx:], d.buf[d.cursorIdx+1:])
		}
		return nil
	}
	return d.Close()
}

func (d *dispatcher) RemoveBackwardCharacter() error {
	d.buf = append(d.buf[:d.cursorIdx-1], d.buf[d.cursorIdx:]...)
	d.cursorIdx = mathutil.Max(d.cursorIdx-1, 1)
	d.length = mathutil.Max(d.length-1, 0)
	return nil
}

func (d *dispatcher) MoveForwardCharacter() error {
	d.cursorIdx = mathutil.Min(d.cursorIdx+1, startIdx+d.length)
	return nil
}

func (d *dispatcher) MoveBackwardCharacter() error {
	d.cursorIdx = mathutil.Max(d.cursorIdx-1, startIdx)
	return nil
}

func (d *dispatcher) MoveForwardWord() error {
	_, end := d.findForwardWord()
	d.cursorIdx = end + 1
	return nil
}

func (d *dispatcher) MoveBackwardWord() error {
	start, _ := d.findBackwardWord()
	d.cursorIdx = start
	return nil
}

func (d *dispatcher) DoubleX() error {
	if d.cursorBufIdx >= d.cursorIdx {
		d.cursorBufIdx = d.cursorIdx
	}
	if d.cursorBufIdx == 0 {
		d.cursorBufIdx = startIdx
	}
	d.cursorBufIdx, d.cursorIdx = d.cursorIdx, d.cursorBufIdx
	return nil
}

func (d *dispatcher) SwapLastTwoCharacter() error {
	if d.length == startIdx || d.cursorIdx == startIdx {
		return nil
	}
	if d.cursorIdx != d.length+startIdx {
		d.buf[d.cursorIdx-1], d.buf[d.cursorIdx] = d.buf[d.cursorIdx], d.buf[d.cursorIdx-1]
	} else {
		d.buf[d.cursorIdx-2], d.buf[d.cursorIdx-1] = d.buf[d.cursorIdx-1], d.buf[d.cursorIdx-2]
	}
	d.cursorIdx = mathutil.Max(d.cursorIdx+1, d.length+1)
	return nil
}

// move bufCursor first,then move cursor
//func (d *dispatcher) moveBufCursor(i int) {
//	if d.cursorBufIdx >= d.cursorIdx {
//		d.cursorBufIdx += i
//	}
//}

func (d *dispatcher) Print(b byte) error {
	if d.length >= d.BufferMaxSize {
		d.resetWithCmdline()
		return OutOfLengthSize
	}
	copy(d.buf[d.cursorIdx+1:], d.buf[d.cursorIdx:])
	d.buf[d.cursorIdx] = b
	d.cursorIdx++
	d.length++
	return nil
}

func (d *dispatcher) Execute(b byte) error {
	err := d.sendAuditReq()
	if err != nil {
		return err
	}
	d.resetWithCmdline()
	recover()
	return nil
}

func (d *dispatcher) sendAuditReq() error {
	d.executeCommand = string(d.buf[startIdx : startIdx+d.length])

	if d.executeCommand == "" {
		return nil
	}
	err := d.queue.pushFront(d.executeCommand)
	if err != nil {
		return err
	}
	d.auditReqChan <- &cmdWithTimestamp{
		start: time.Now(),
		cmd:   d.executeCommand,
	}
	return nil
}

func (d *dispatcher) resetWithCmdline() {
	if d.cursorLine != 0 {
		d.queue.Set(d.cursorLine, d.executeCommand)
	}
	d.cursorLine = 0
	d.reset()
}

func (d *dispatcher) reset() {
	d.cursorIdx = startIdx
	d.searchIdx = startIdx
	d.length = 0
	d.inputBufLength = 0
	d.executeCommand = ""
	d.cursorBufIdx = 0
}

func (d *dispatcher) reverseSearch(b byte) error {
	d.searchBuf[d.searchIdx] = b
	d.searchIdx++
	if d.searchIdx >= d.BufferMaxSize {
		d.reset()
		return OutOfLengthSize
	}
	allStr := string(d.searchBuf[startIdx:d.searchIdx])
	for j := len(allStr); j >= 1; j-- {
		searchStr := allStr[:j]
		for i := d.queue.Begin(); i <= d.queue.Size(); i++ {
			str, _ := d.queue.Get(i)
			contains, idx := d.reverseContains(str, searchStr)
			if contains {
				copy(d.buf[startIdx:], str)
				d.cursorIdx = idx
				d.length = len(str)
				d.cursorLine = i
				return nil
			}
		}
	}
	return nil
}

func (d *dispatcher) reverseContains(str, substr string) (bool, int) {
	if len(str) < len(substr) {
		return false, -1
	}
	start := len(str) - len(substr)
	idx := -1
	for i := start; i >= 0; i-- {
		j := 0
		for ; j < len(substr); j++ {
			if substr[j] == str[i+j] {
				idx = i
			} else {
				break
			}
		}
		if j == len(substr) {
			return true, idx
		}
	}
	return false, idx
}

func (d *dispatcher) search(b byte) error {
	d.searchBuf[d.searchIdx] = b
	d.searchIdx++
	if d.searchIdx >= d.BufferMaxSize {
		d.reset()
		return OutOfLengthSize
	}
	allStr := string(d.searchBuf[startIdx : d.searchIdx+startIdx])
	for j := len(allStr); j >= 1; j-- {
		searchStr := allStr[:j]
		for i := d.queue.Size(); i >= d.queue.Begin(); i-- {
			str, _ := d.queue.Get(i)
			if strings.Contains(str, searchStr) {
				copy(d.buf[startIdx:], str)
				d.cursorIdx = len(str) + 1
				d.length = len(str)
				d.cursorLine = i
				return nil
			}
		}
	}
	return nil
}

func (d *dispatcher) CUU(i int) error {
	d.cursorLine = mathutil.Min(d.queue.Size(), d.cursorLine+i)
	c, err := d.queue.Get(d.cursorLine)
	if err != nil {
		return err
	}
	copy(d.buf[startIdx:], c)
	d.length = len(c)
	d.cursorIdx = len(c) + startIdx
	return nil
}

func (d *dispatcher) CUD(i int) error {
	d.cursorLine = mathutil.Max(0, d.cursorLine-i)
	c, err := d.queue.Get(d.cursorLine)
	if err != nil {
		return err
	}
	copy(d.buf[startIdx:], c)
	d.length = len(c)
	d.cursorIdx = len(c) + startIdx
	return nil
}

func (d *dispatcher) CUF(i int) error {
	d.cursorIdx = mathutil.Min(d.cursorIdx+i, d.length+startIdx)
	return nil
}

func (d *dispatcher) CUB(i int) error {
	d.cursorIdx = mathutil.Max(d.cursorIdx-i, startIdx)
	return nil
}

func (d *dispatcher) CNL(i int) error {
	return nil
}

func (d *dispatcher) CPL(i int) error {
	return nil
}

func (d *dispatcher) CHA(i int) error {
	return nil
}

func (d *dispatcher) VPA(i int) error {
	return nil
}

func (d *dispatcher) CUP(i int, i2 int) error {
	return nil
}

func (d *dispatcher) HVP(i int, i2 int) error {
	return nil
}

func (d *dispatcher) DECTCEM(b bool) error {
	return nil
}

func (d *dispatcher) DECOM(b bool) error {
	return nil
}

func (d *dispatcher) DECCOLM(b bool) error {
	return nil
}

func (d *dispatcher) ED(i int) error {
	return nil
}

func (d *dispatcher) EL(i int) error {
	return nil
}

func (d *dispatcher) IL(i int) error {
	return nil
}

func (d *dispatcher) DL(i int) error {
	return nil
}

func (d *dispatcher) ICH(i int) error {
	return nil
}

func (d *dispatcher) DCH(i int) error {
	return nil
}

func (d *dispatcher) SGR(ints []int) error {
	return nil
}

func (d *dispatcher) SU(i int) error {
	return nil
}

func (d *dispatcher) SD(i int) error {
	return nil
}

func (d *dispatcher) DA(strings []string) error {
	return nil
}

func (d *dispatcher) DECSTBM(i int, i2 int) error {
	return nil
}

func (d *dispatcher) IND() error {
	return nil
}

func (d *dispatcher) RI() error {
	return nil
}

func (d *dispatcher) Flush() error {
	return nil
}

func NewDispatcher(auditReqChan chan *cmdWithTimestamp, closeChan chan struct{}) *dispatcher {
	maxSize := 100

	d := &dispatcher{queue: &commandQueue{
		cmds:    make([]string, maxSize+1),
		length:  0,
		maxSize: maxSize,
	},
		BufferMaxSize: maxBufSize,
		cursorIdx:     startIdx,
		buf:           make([]byte, maxBufSize+2),
		searchBuf:     make([]byte, maxBufSize+2),
		inputBuf:      make([]byte, maxBufSize+2),
		auditReqChan:  auditReqChan,
		closeChan:     closeChan,
	}
	return d
}
