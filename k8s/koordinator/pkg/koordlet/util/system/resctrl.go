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
	"fmt"
	"math"
	"math/bits"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"

	"k8s.io/klog/v2"
)

const (
	CPUInfoFileName       string = "cpuinfo"
	KernelCmdlineFileName string = "cmdline"

	ResctrlName string = "resctrl"

	ResctrlDir string = "resctrl/"
	RdtInfoDir string = "info"
	L3CatDir   string = "L3"

	ResctrlSchemataName string = "schemata"
	ResctrlCbmMaskName  string = "cbm_mask"
	ResctrlTasksName    string = "tasks"

	// L3SchemataPrefix is the prefix of l3 cat schemata
	L3SchemataPrefix = "L3"
	// MbSchemataPrefix is the prefix of mba schemata
	MbSchemataPrefix = "MB"
)

var (
	initLock         sync.Mutex
	isInit           bool
	isSupportResctrl bool
)

func isCPUSupportResctrl() (bool, error) {
	isCatFlagSet, isMbaFlagSet, err := isResctrlAvailableByCpuInfo(filepath.Join(Conf.ProcRootDir, CPUInfoFileName))
	if err != nil {
		klog.Errorf("isResctrlAvailableByCpuInfo error: %v", err)
		return false, err
	}
	klog.Infof("isResctrlAvailableByCpuInfo result, isCatFlagSet: %v, isMbaFlagSet: %v", isCatFlagSet, isMbaFlagSet)
	isInit = true
	return isCatFlagSet && isMbaFlagSet, nil
}

func isKernelSupportResctrl() (bool, error) {
	isCatFlagSet, isMbaFlagSet, err := isResctrlAvailableByKernelCmd(filepath.Join(Conf.ProcRootDir, KernelCmdlineFileName))
	if err != nil {
		klog.Errorf("isResctrlAvailableByKernelCmd error: %v", err)
		return false, err
	}
	klog.Infof("isResctrlAvailableByKernelCmd result,isCatFlagSet: %v,isMbaFlagSet: %v", isCatFlagSet, isMbaFlagSet)
	isInit = true
	return isCatFlagSet && isMbaFlagSet, nil
}

func IsSupportResctrl() (bool, error) {
	initLock.Lock()
	defer initLock.Unlock()
	if !isInit {
		cpuSupport, err := isCPUSupportResctrl()
		if err != nil {
			return false, err
		}
		kernelSupport, err := isKernelSupportResctrl()
		if err != nil {
			return false, err
		}
		isInit = true
		isSupportResctrl = kernelSupport && cpuSupport
	}
	return isSupportResctrl, nil
}

var (
	ResctrlSchemata  = NewCommonResctrlResource(ResctrlSchemataName, "")
	ResctrlTasks     = NewCommonResctrlResource(ResctrlTasksName, "")
	ResctrlL3CbmMask = NewCommonResctrlResource(ResctrlCbmMaskName, filepath.Join(RdtInfoDir, L3CatDir))
)

var _ Resource = &ResctrlResource{}

type ResctrlResource struct {
	Type           ResourceType
	FileName       string
	Subdir         string
	CheckSupported func(r Resource, parentDir string) (isSupported bool, msg string)
	Validator      ResourceValidator
}

func (r *ResctrlResource) ResourceType() ResourceType {
	if len(r.Type) > 0 {
		return r.Type
	}
	return ResourceType(filepath.Join(r.Subdir, r.FileName))
}

func (r *ResctrlResource) Path(parentDir string) string {
	// parentDir for resctrl is like: `/`, `LS/`, `BE`
	return filepath.Join(Conf.SysFSRootDir, ResctrlDir, parentDir, r.Subdir, r.FileName)
}

func (r *ResctrlResource) IsSupported(parentDir string) (bool, string) {
	if r.CheckSupported == nil {
		return true, ""
	}
	return r.CheckSupported(r, parentDir)
}

func (r *ResctrlResource) IsValid(v string) (bool, string) {
	if r.Validator == nil {
		return true, ""
	}
	return r.Validator.Validate(v)
}

func (r *ResctrlResource) WithValidator(validator ResourceValidator) Resource {
	r.Validator = validator
	return r
}

func (r *ResctrlResource) WithSupported(isSupported bool, msg string) Resource {
	return r
}

func (r *ResctrlResource) WithCheckSupported(checkSupportedFn func(r Resource, parentDir string) (isSupported bool, msg string)) Resource {
	r.CheckSupported = checkSupportedFn
	return r
}

func NewCommonResctrlResource(filename string, subdir string) Resource {
	return &ResctrlResource{
		Type:           ResourceType(filename),
		FileName:       filename,
		Subdir:         subdir,
		CheckSupported: SupportedIfFileExists,
	}
}

type ResctrlSchemataRaw struct {
	L3    []int64
	MB    []int64
	L3Num int
}

func NewResctrlSchemataRaw() *ResctrlSchemataRaw {
	return &ResctrlSchemataRaw{L3Num: 1}
}

func (r *ResctrlSchemataRaw) WithL3Num(l3Num int) *ResctrlSchemataRaw {
	r.L3Num = l3Num
	return r
}

func (r *ResctrlSchemataRaw) WithL3Mask(mask string) *ResctrlSchemataRaw {
	// l3 mask MUST be a valid hex
	maskValue, err := strconv.ParseInt(strings.TrimSpace(mask), 16, 64)
	if err != nil {
		klog.V(5).Infof("failed to parse l3 mask %s, err: %v", mask, err)
	}
	r.L3 = make([]int64, r.L3Num)
	for i := 0; i < r.L3Num; i++ {
		r.L3[i] = maskValue
	}
	return r
}

func (r *ResctrlSchemataRaw) WithMBPercent(percent string) *ResctrlSchemataRaw {
	// mba percent MUST be a valid integer
	percentValue, err := strconv.ParseInt(strings.TrimSpace(percent), 10, 64)
	if err != nil {
		klog.V(5).Infof("failed to parse mba percent %s, err: %v", percent, err)
	}
	r.MB = make([]int64, r.L3Num)
	for i := 0; i < r.L3Num; i++ {
		r.MB[i] = percentValue
	}
	return r
}

func (r *ResctrlSchemataRaw) DeepCopy() *ResctrlSchemataRaw {
	n := NewResctrlSchemataRaw().WithL3Num(r.L3Num)
	for i := range r.L3 {
		n.L3 = append(n.L3, r.L3[i])
	}
	for i := range r.MB {
		n.MB = append(n.MB, r.MB[i])
	}
	return n
}

func (r *ResctrlSchemataRaw) Prefix() string {
	var prefix string
	if len(r.L3) > 0 {
		prefix += L3SchemataPrefix + ":"
	}
	if len(r.MB) > 0 {
		prefix += MbSchemataPrefix + ":"
	}
	return prefix
}

func (r *ResctrlSchemataRaw) L3Number() int {
	return r.L3Num
}

func (r *ResctrlSchemataRaw) L3String() string {
	if len(r.L3) <= 0 {
		return ""
	}
	schemata := L3SchemataPrefix + ":"
	// the last ';' will be auto ignored
	for i := range r.L3 {
		schemata = schemata + strconv.Itoa(i) + "=" + strconv.FormatInt(r.L3[i], 16) + ";"
	}
	// the trailing '\n' is necessary to append
	schemata += "\n"
	return schemata
}

func (r *ResctrlSchemataRaw) MBString() string {
	if len(r.MB) <= 0 {
		return ""
	}
	schemata := MbSchemataPrefix + ":"
	// the last ';' will be auto ignored
	for i := range r.MB {
		schemata = schemata + strconv.Itoa(i) + "=" + strconv.FormatInt(r.MB[i], 10) + ";"
	}
	// the trailing '\n' is necessary to append
	schemata += "\n"
	return schemata
}

func (r *ResctrlSchemataRaw) Equal(a *ResctrlSchemataRaw) (bool, string) {
	if r.L3Num != a.L3Num {
		return false, "l3 number not equal"
	}
	if a.L3 != nil {
		if len(r.L3) != len(a.L3) || r.L3 == nil {
			return false, "the number of l3 masks not equal"
		}
		for i := 0; i < len(r.L3); i++ {
			if r.L3[i] != a.L3[i] {
				return false, "the value of l3 mask not equal"
			}
		}
	}
	if a.MB != nil {
		if len(r.MB) != len(a.MB) || r.MB == nil {
			return false, "the number of mba percent not equal"
		}
		for i := 0; i < len(r.MB); i++ {
			if r.MB[i] != a.MB[i] {
				return false, "the value of mba percent not equal"
			}
		}
	}
	return true, ""
}

// ParseResctrlSchemata parses the resctrl schemata of given cgroup, and returns the l3_cat masks and mba masks.
// @content `L3:0=fff;1=fff\nMB:0=100;1=100\n` (may have additional lines (e.g. ARM MPAM))
// @l3Num 2
// @return { L3: ["fff", "fff"], MB: ["100", "100"] }, nil
func (r *ResctrlSchemataRaw) ParseResctrlSchemata(content string, l3Num int) error {
	schemataMap := ParseResctrlSchemataMap(content)

	for _, t := range []struct {
		prefix string
		base   int
		v      *[]int64
	}{
		{
			prefix: L3SchemataPrefix,
			base:   16,
			v:      &r.L3,
		},
		{
			prefix: MbSchemataPrefix,
			base:   10,
			v:      &r.MB,
		},
	} {
		maskMap := schemataMap[t.prefix]
		if maskMap == nil {
			klog.V(5).Infof("read resctrl schemata of %s aborted, mask not found", t.prefix)
			continue
		}
		if len(maskMap) != l3Num {
			return fmt.Errorf("read resctrl schemata failed, %s masks has invalid count %v",
				t.prefix, len(maskMap))
		}
		for i := 0; i < l3Num; i++ {
			mask, ok := maskMap[i]
			if !ok {
				return fmt.Errorf("read resctrl schemata failed, %s masks of node %v is missing",
					t.prefix, i)
			}
			maskValue, err := strconv.ParseInt(strings.TrimSpace(mask), t.base, 64)
			if err != nil {
				return fmt.Errorf("read resctrl schemata failed, %s masks is invalid, value %s, err: %s",
					t.prefix, mask, err)
			}
			*t.v = append(*t.v, maskValue)
		}
	}

	return nil
}

// @return /sys/fs/resctrl
func GetResctrlSubsystemDirPath() string {
	return filepath.Join(Conf.SysFSRootDir, ResctrlDir)
}

// @groupPath BE
// @return /sys/fs/resctrl/BE
func GetResctrlGroupRootDirPath(groupPath string) string {
	return filepath.Join(Conf.SysFSRootDir, ResctrlDir, groupPath)
}

// @return /sys/fs/resctrl/info/L3/cbm_mask
func GetResctrlL3CbmFilePath() string {
	return ResctrlL3CbmMask.Path("")
}

// @groupPath BE
// @return /sys/fs/resctrl/BE/schemata
func GetResctrlSchemataFilePath(groupPath string) string {
	return ResctrlSchemata.Path(groupPath)
}

// @groupPath BE
// @return /sys/fs/resctrl/BE/tasks
func GetResctrlTasksFilePath(groupPath string) string {
	return ResctrlTasks.Path(groupPath)
}

func ReadResctrlSchemataRaw(schemataFile string, l3Num int) (*ResctrlSchemataRaw, error) {
	content, err := os.ReadFile(schemataFile)
	if err != nil {
		return nil, fmt.Errorf("failed to read schemata file, err: %v", err)
	}

	schemataRaw := NewResctrlSchemataRaw()
	err = schemataRaw.ParseResctrlSchemata(string(content), l3Num)
	if err != nil {
		return nil, fmt.Errorf("failed to parse l3 schemata, content %s, err: %v", string(content), err)
	}

	return schemataRaw, nil
}

// ParseResctrlSchemataMap parses the content of resctrl schemata.
// e.g. schemata=`L3:0=fff;1=fff\nMB:0=100;1=100\n` -> `{"L3": {0: "fff", 1: "fff"}, "MB": {0: "100", 1: "100"}}`
func ParseResctrlSchemataMap(content string) map[string]map[int]string {
	schemataMap := map[string]map[int]string{}
	lines := strings.Split(content, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line) // `L3:0=fff;1=fff`, `MB:0=100;1=100`
		if len(line) <= 0 {
			continue
		}

		pair := strings.Split(line, ":") // {`L3`, `0=fff;1=fff`}, {`MB`, `0=100;1=100`}
		if len(pair) != 2 {
			klog.V(6).Infof("failed to parse resctrl schemata, line %s, err: invalid key value pair", line)
			continue
		}

		masks := strings.Split(pair[1], ";") // {`0=fff`, `1=fff`}, {`0=100`, `1=100`}
		maskMap := map[int]string{}
		for _, mask := range masks {
			maskPair := strings.Split(mask, "=") // {`0`, `fff`}, {`1`, `100`}
			if len(maskPair) != 2 {
				klog.V(6).Infof("failed to parse resctrl schemata, mask %s, err: invalid key value pair", mask)
				continue
			}
			nodeID, err := strconv.ParseInt(maskPair[0], 10, 32)
			if err != nil {
				klog.V(6).Infof("failed to parse resctrl schemata, mask %s, err: %s", mask, err)
				continue
			}
			maskMap[int(nodeID)] = maskPair[1] // {0: `fff`}
		}
		schemataMap[pair[0]] = maskMap // {`L3`: {0: `fff`, 1: `fff`} }
	}

	return schemataMap
}

// ReadCatL3CbmString reads and returns the value of cat l3 cbm_mask
func ReadCatL3CbmString() (string, error) {
	cbmFile := GetResctrlL3CbmFilePath()
	out, err := os.ReadFile(cbmFile)
	if err != nil {
		return "", fmt.Errorf("failed to read l3 cbm, path %s, err: %v", cbmFile, err)
	}
	return strings.TrimSpace(string(out)), nil
}

// ReadResctrlTasksMap reads and returns the map of given resctrl group's task ids
func ReadResctrlTasksMap(groupPath string) (map[int32]struct{}, error) {
	tasksPath := GetResctrlTasksFilePath(groupPath)
	rawContent, err := os.ReadFile(tasksPath)
	if err != nil {
		return nil, err
	}

	tasksMap := map[int32]struct{}{}

	lines := strings.Split(string(rawContent), "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if len(line) <= 0 {
			continue
		}
		task, err := strconv.ParseInt(line, 10, 32)
		if err != nil {
			return nil, err
		}
		tasksMap[int32(task)] = struct{}{}
	}
	return tasksMap, nil
}

// CheckAndTryEnableResctrlCat checks if resctrl and l3_cat are enabled; if not, try to enable the features by mount
// resctrl subsystem; See MountResctrlSubsystem() for the detail.
// It returns whether the resctrl cat is enabled, and the error if failed to enable or to check resctrl interfaces
func CheckAndTryEnableResctrlCat() error {
	// resctrl cat is correctly enabled: l3_cbm path exists
	l3CbmFilePath := GetResctrlL3CbmFilePath()
	_, err := os.Stat(l3CbmFilePath)
	if err == nil {
		return nil
	}
	newMount, err := MountResctrlSubsystem()
	if err != nil {
		return err
	}
	if newMount {
		klog.Infof("mount resctrl successfully, resctrl enabled")
	}
	// double check l3_cbm path to ensure both resctrl and cat are correctly enabled
	l3CbmFilePath = GetResctrlL3CbmFilePath()
	_, err = os.Stat(l3CbmFilePath)
	if err != nil {
		return fmt.Errorf("resctrl cat is not enabled, err: %s", err)
	}
	return nil
}

func InitCatGroupIfNotExist(group string) error {
	path := GetResctrlGroupRootDirPath(group)
	_, err := os.Stat(path)
	if err == nil {
		return nil
	} else if !os.IsNotExist(err) {
		return fmt.Errorf("check dir %v for group %s but got unexpected err: %v", path, group, err)
	}
	err = os.Mkdir(path, 0755)
	if err != nil {
		return fmt.Errorf("create dir %v failed for group %s, err: %v", path, group, err)
	}
	return nil
}

func CalculateCatL3MaskValue(cbm uint, startPercent, endPercent int64) (string, error) {
	// check if the parsed cbm value is valid, eg. 0xff, 0x1, 0x7ff, ...
	// NOTE: (Cache Bit Masks) X86 hardware requires that these masks have all the '1' bits in a contiguous block.
	//       ref: https://www.kernel.org/doc/Documentation/x86/intel_rdt_ui.txt
	// since the input cbm here is the cbm value of the resctrl root, every lower bit is required to be `1` additionally
	if bits.OnesCount(cbm+1) != 1 {
		return "", fmt.Errorf("illegal cbm %v", cbm)
	}

	// check if the startPercent and endPercent are valid
	if startPercent < 0 || endPercent > 100 || endPercent <= startPercent {
		return "", fmt.Errorf("illegal l3 cat percent: start %v, end %v", startPercent, endPercent)
	}

	// calculate a bit mask belonging to interval [startPercent% * ways, endPercent% * ways)
	// eg.
	// cbm 0x3ff ('b1111111111), start 10%, end 80%
	// ways 10, l3Mask 0xfe ('b11111110)
	// cbm 0x7ff ('b11111111111), start 10%, end 50%
	// ways 11, l3Mask 0x3c ('b111100)
	// cbm 0x7ff ('b11111111111), start 0%, end 30%
	// ways 11, l3Mask 0xf ('b1111)
	ways := float64(bits.Len(cbm))
	startWay := uint64(math.Ceil(ways * float64(startPercent) / 100))
	endWay := uint64(math.Ceil(ways * float64(endPercent) / 100))

	var l3Mask uint64 = (1 << endWay) - (1 << startWay)
	return strconv.FormatUint(l3Mask, 16), nil
}
