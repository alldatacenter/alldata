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
	"bufio"
	"fmt"
	"os"
	"regexp"
	"strings"
	"syscall"
)

// MountResctrlSubsystem mounts resctrl fs under the sysFSRoot to enable the kernel feature on supported environment
// NOTE: Linux kernel (>= 4.10), Intel cpu and bare-mental host are required; Also, Intel RDT
// features should be enabled in kernel configurations and kernel commandline.
// For more info, please see https://github.com/intel/intel-cmt-cat/wiki/resctrl
func MountResctrlSubsystem() (bool, error) {
	schemataPath := GetResctrlSchemataFilePath("")
	// use schemata path to check since the subsystem root dir could keep exist when unmounted
	_, err := os.Stat(schemataPath)
	if err == nil {
		return false, nil
	}
	subsystemPath := GetResctrlSubsystemDirPath()
	err = syscall.Mount(ResctrlName, subsystemPath, ResctrlName, syscall.MS_RELATIME, "")
	if err != nil {
		return false, err
	}
	_, err = os.Stat(schemataPath)
	if err != nil {
		return false, fmt.Errorf("resctrl subsystem is mounted, but path %s does not exist, err: %s",
			subsystemPath, err)
	}
	return true, nil
}

func isResctrlAvailableByCpuInfo(path string) (bool, bool, error) {
	isCatFlagSet := false
	isMbaFlagSet := false

	f, err := os.Open(path)
	if err != nil {
		return false, false, err
	}
	defer f.Close()

	s := bufio.NewScanner(f)
	for s.Scan() {
		if err := s.Err(); err != nil {
			return false, false, err
		}

		line := s.Text()

		// Search "cat_l3" and "mba" flags in first "flags" line
		if strings.Contains(line, "flags") {
			flags := strings.Split(line, " ")
			// "cat_l3" flag for CAT and "mba" flag for MBA
			for _, flag := range flags {
				switch flag {
				case "cat_l3":
					isCatFlagSet = true
				case "mba":
					isMbaFlagSet = true
				}
			}
			return isCatFlagSet, isMbaFlagSet, nil
		}
	}
	return isCatFlagSet, isMbaFlagSet, nil
}

// file content example:
// BOOT_IMAGE=/boot/vmlinuz-4.19.91-24.1.al7.x86_64 root=UUID=231efa3b-302b-4e82-9445-0f7d5d353dda \
// crashkernel=0M-2G:0M,2G-8G:192M,8G-:256M cryptomgr.notests cgroup.memory=nokmem rcupdate.rcu_cpu_stall_timeout=300 \
// vring_force_dma_api biosdevname=0 net.ifnames=0 console=tty0 console=ttyS0,115200n8 noibrs \
// nvme_core.io_timeout=4294967295 nomodeset intel_idle.max_cstate=1 rdt=cmt,l3cat,l3cdp,mba
func isResctrlAvailableByKernelCmd(path string) (bool, bool, error) {
	isCatFlagSet := false
	isMbaFlagSet := false
	f, err := os.Open(path)
	if err != nil {
		return false, false, err
	}
	defer f.Close()
	s := bufio.NewScanner(f)
	for s.Scan() {
		if err := s.Err(); err != nil {
			return false, false, err
		}
		line := s.Text()
		l3Reg, regErr := regexp.Compile(".* rdt=.*l3cat.*")
		if regErr == nil && l3Reg.Match([]byte(line)) {
			isCatFlagSet = true
		}

		mbaReg, regErr := regexp.Compile(".* rdt=.*mba.*")
		if regErr == nil && mbaReg.Match([]byte(line)) {
			isMbaFlagSet = true
		}
	}
	return isCatFlagSet, isMbaFlagSet, nil
}
