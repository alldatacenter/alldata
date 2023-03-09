//go:build linux
// +build linux

/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package system

import (
	"bytes"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"sync/atomic"
	"syscall"
	"time"
	"unicode"

	"github.com/cakturk/go-netstat/netstat"
	utilerrors "k8s.io/apimachinery/pkg/util/errors"
	"k8s.io/klog/v2"
)

var (
	kubeletPortAndPid atomic.Value
	expireTime        = time.Minute
)

type portAndPid struct {
	port           int
	pid            int
	lastUpdateTime time.Time
}

func TCPSocks(fn netstat.AcceptFn) ([]netstat.SockTabEntry, error) {
	v6Socks, v6Err := netstat.TCP6Socks(fn)
	if v6Err == nil && v6Socks != nil && len(v6Socks) > 0 {
		return v6Socks, nil
	}

	socks, err := netstat.TCPSocks(fn)
	if err == nil && socks != nil {
		return socks, nil
	}

	return nil, utilerrors.NewAggregate([]error{err, v6Err})
}

// KubeletPortToPid Query pid by tcp port number with the help of go-netstat
// note: Due to the low efficiency of full traversal, we cache the result and verify each time
func KubeletPortToPid(port int) (int, error) {
	if port < 0 {
		return -1, fmt.Errorf("failed to get pid, the port is invalid")
	}
	if val, ok := kubeletPortAndPid.Load().(portAndPid); ok && val.port == port {
		if time.Now().Before(val.lastUpdateTime.Add(expireTime)) &&
			syscall.Kill(val.pid, 0) == nil {
			return val.pid, nil
		}
	}
	socks, err := TCPSocks(func(entry *netstat.SockTabEntry) bool {
		if entry.State == netstat.Listen && entry.LocalAddr.Port == uint16(port) {
			return true
		}
		return false
	})
	if err != nil {
		return -1, fmt.Errorf("failed to get pid, err is %v", err)
	}
	if len(socks) == 0 || socks[0].Process == nil {
		return -1, fmt.Errorf("failed to get pid, the port is not used")
	}
	pid := socks[0].Process.Pid
	kubeletPortAndPid.Store(portAndPid{port: port, pid: pid, lastUpdateTime: time.Now()})
	return pid, nil
}

// CmdLine returns the command line args of a process.
func ProcCmdLine(procRoot string, pid int) ([]string, error) {
	data, err := ReadFileNoStat(path.Join(procRoot, strconv.Itoa(pid), "cmdline"))
	if err != nil {
		return nil, err
	}

	if len(data) < 1 {
		return []string{}, nil
	}

	return strings.Split(string(bytes.TrimRight(data, string("\x00"))), string(byte(0))), nil
}

// PidOf finds process(es) with a specified name (regexp match)
// and return their pid(s).
var PidOf = pidOfFn

// From k8s.io/kubernetes/pkg/util/procfs/procfs_linux.go
// caller should specify proc root dir
func pidOfFn(procRoot string, name string) ([]int, error) {
	if len(name) == 0 {
		return []int{}, fmt.Errorf("name should not be empty")
	}
	re, err := regexp.Compile("(^|/)" + name + "$")
	if err != nil {
		return []int{}, err
	}
	return getPids(procRoot, re), nil
}

func getPids(procRoot string, re *regexp.Regexp) []int {
	pids := []int{}

	dirFD, err := os.Open(procRoot)
	if err != nil {
		return nil
	}
	defer dirFD.Close()

	for {
		// Read a small number at a time in case there are many entries, we don't want to
		// allocate a lot here.
		ls, err := dirFD.Readdir(10)
		if err == io.EOF {
			break
		}
		if err != nil {
			return nil
		}

		for _, entry := range ls {
			if !entry.IsDir() {
				continue
			}

			// If the directory is not a number (i.e. not a PID), skip it
			pid, err := strconv.Atoi(entry.Name())
			if err != nil {
				continue
			}

			cmdline, err := os.ReadFile(filepath.Join(procRoot, entry.Name(), "cmdline"))
			if err != nil {
				klog.V(4).Infof("Error reading file %s: %+v", filepath.Join(procRoot, entry.Name(), "cmdline"), err)
				continue
			}

			// The bytes we read have '\0' as a separator for the command line
			parts := bytes.SplitN(cmdline, []byte{0}, 2)
			if len(parts) == 0 {
				continue
			}
			// Split the command line itself we are interested in just the first part
			exe := strings.FieldsFunc(string(parts[0]), func(c rune) bool {
				return unicode.IsSpace(c) || c == ':'
			})
			if len(exe) == 0 {
				continue
			}
			// Check if the name of the executable is what we are looking for
			if re.MatchString(exe[0]) {
				// Grab the PID from the directory path
				pids = append(pids, pid)
			}
		}
	}

	return pids
}

// If running in container, exec command by 'nsenter --mount=/proc/1/ns/mnt ${cmds}'.
// return stdout, exitcode, error
var ExecCmdOnHost = execCmdOnHostFn

func execCmdOnHostFn(cmds []string) ([]byte, int, error) {
	if len(cmds) == 0 {
		return nil, -1, fmt.Errorf("nil command")
	}
	cmdPrefix := []string{}
	if AgentMode == DS_MODE {
		cmdPrefix = append(cmdPrefix, "nsenter", fmt.Sprintf("--mount=%s", path.Join(Conf.ProcRootDir, "/1/ns/mnt")))
	}
	cmdPrefix = append(cmdPrefix, cmds...)

	var errB bytes.Buffer
	command := exec.Command(cmdPrefix[0], cmdPrefix[1:]...)
	command.Stderr = &errB
	if out, err := command.Output(); err != nil {
		return out, command.ProcessState.ExitCode(),
			fmt.Errorf("nsenter command('%s') failed: %v, stderr: %s", strings.Join(cmds, " "), err, errB.String())
	} else {
		return out, 0, nil
	}
}

// return working dir of process
func WorkingDirOf(pid int) (string, error) {
	var errB bytes.Buffer
	command := exec.Command("pwdx", fmt.Sprintf("%d", pid))
	command.Stderr = &errB
	if out, err := command.Output(); err != nil {
		return "", fmt.Errorf("pwdx command('%d') failed: %v, stderr: %s", pid, err, errB.String())
	} else {
		tokens := strings.Split(string(out), ":")
		return strings.TrimSpace(tokens[1]), nil
	}
}
