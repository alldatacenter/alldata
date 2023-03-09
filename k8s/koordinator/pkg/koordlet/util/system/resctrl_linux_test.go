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
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_isResctrlAvailableByCpuInfo(t *testing.T) {
	type args struct {
		name               string
		cpuInfoContents    string
		expectIsCatFlagSet bool
		expectIsMbaFlagSet bool
	}

	tests := []args{
		{
			name: "testResctrlEnable",
			cpuInfoContents: "flags		: fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm pbe syscall nx pdpe1gb rdtscp lm constant_tsc art arch_perfmon pebs bts rep_good nopl xtopology nonstop_tsc cpuid aperfmperf pni pclmulqdq dtes64 monitor ds_cpl vmx smx est tm2 ssse3 sdbg fma cx16 xtpr pdcm pcid dca sse4_1 sse4_2 x2apic movbe popcnt tsc_deadline_timer aes xsave avx f16c rdrand lahf_lm abm 3dnowprefetch cpuid_fault epb cat_l3 cdp_l3 invpcid_single intel_ppin ssbd mba ibrs ibpb stibp ibrs_enhanced tpr_shadow vnmi flexpriority ept vpid ept_ad tsc_adjust bmi1 avx2 smep bmi2 erms invpcid cqm mpx rdt_a avx512f avx512dq rdseed adx smap clflushopt clwb intel_pt avx512cd avx512bw avx512vl xsaveopt xsavec xgetbv1 xsaves cqm_llc cqm_occup_llc cqm_mbm_total cqm_mbm_local dtherm ida arat pln pts pku ospke avx512_vnni md_clear flush_l1d arch_capabilities",
			expectIsCatFlagSet: true,
			expectIsMbaFlagSet: true,
		},
		{
			name: "testResctrlUnable",
			cpuInfoContents: "flags		: fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush mmx fxsr sse sse2 ss ht syscall nx pdpe1gb rdtscp lm constant_tsc rep_good nopl nonstop_tsc cpuid tsc_known_freq pni pclmulqdq monitor ssse3 fma cx16 pcid sse4_1 sse4_2 x2apic movbe popcnt aes xsave avx f16c rdrand hypervisor lahf_lm abm 3dnowprefetch cpuid_fault invpcid_single ibrs_enhanced tsc_adjust bmi1 avx2 smep bmi2 erms invpcid avx512f avx512dq rdseed adx smap avx512ifma clflushopt clwb avx512cd sha_ni avx512bw avx512vl xsaveopt xsavec xgetbv1 xsaves wbnoinvd arat avx512vbmi pku ospke avx512_vbmi2 gfni vaes vpclmulqdq avx512_vnni avx512_bitalg avx512_vpopcntdq rdpid fsrm arch_capabilities",
			expectIsCatFlagSet: false,
			expectIsMbaFlagSet: false,
		},
		{
			name:               "testContentsInvalid",
			cpuInfoContents:    "invalid contents",
			expectIsCatFlagSet: false,
			expectIsMbaFlagSet: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := NewFileTestUtil(t)

			helper.WriteProcSubFileContents("cpuinfo", tt.cpuInfoContents)
			gotIsCatFlagSet, gotIsMbaFlagSet, err := isResctrlAvailableByCpuInfo(filepath.Join(Conf.ProcRootDir, "cpuinfo"))
			assert.NoError(t, err, "testError")
			assert.Equal(t, tt.expectIsCatFlagSet, gotIsCatFlagSet, "checkIsCatFlagSet")
			assert.Equal(t, tt.expectIsMbaFlagSet, gotIsMbaFlagSet, "checkIsMbaFlagSet")
		})
	}
}

func Test_isResctrlAvailableByKernelCmd(t *testing.T) {
	type args struct {
		content string
	}
	tests := []struct {
		name    string
		args    args
		wantCat bool
		wantMba bool
	}{
		{
			name: "testResctrlEnable",
			args: args{
				content: "BOOT_IMAGE=/boot/vmlinuz-4.19.91-24.1.al7.x86_64 root=UUID=231efa3b-302b-4e82-9445-0f7d5d353dda rdt=cmt,l3cat,l3cdp,mba",
			},
			wantCat: true,
			wantMba: true,
		},
		{
			name: "testResctrlCatDisable",
			args: args{
				content: "BOOT_IMAGE=/boot/vmlinuz-4.19.91-24.1.al7.x86_64 root=UUID=231efa3b-302b-4e82-9445-0f7d5d353dda rdt=cmt,mba,l3cdp",
			},
			wantCat: false,
			wantMba: true,
		},
		{
			name: "testResctrlMBADisable",
			args: args{
				content: "BOOT_IMAGE=/boot/vmlinuz-4.19.91-24.1.al7.x86_64 root=UUID=231efa3b-302b-4e82-9445-0f7d5d353dda rdt=cmt,l3cat,l3cdp",
			},
			wantCat: true,
			wantMba: false,
		},
		{
			name: "testContentsInvalid",
			args: args{
				content: "invalid contents",
			},
			wantCat: false,
			wantMba: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := NewFileTestUtil(t)
			helper.WriteProcSubFileContents("cmdline", tt.args.content)
			isCatFlagSet, isMbaFlagSet, _ := isResctrlAvailableByKernelCmd(filepath.Join(Conf.ProcRootDir, "cmdline"))
			if isCatFlagSet != tt.wantCat {
				t.Errorf("isResctrlAvailableByKernelCmd() isCatFlagSet = %v, want %v", isCatFlagSet, tt.wantCat)
			}
			if isMbaFlagSet != tt.wantMba {
				t.Errorf("isResctrlAvailableByKernelCmd() got1 = %v, want %v", isMbaFlagSet, tt.wantMba)
			}
		})
	}
}
