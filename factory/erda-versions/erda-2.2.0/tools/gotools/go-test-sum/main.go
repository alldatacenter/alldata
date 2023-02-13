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

package main

import (
	"bytes"
	"crypto/md5"
	"encoding/hex"
	"errors"
	"fmt"
	"go/parser"
	"go/token"
	"io"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/mitchellh/go-homedir"

	"github.com/erda-project/erda/tools/gotools/go-test-sum/cover"
)

var (
	codeFileExts = map[string]bool{
		".go":  true,
		".s":   true,
		".c":   true,
		".h":   true,
		".cpp": true,
	}
	homeDir         string
	cachePath       string
	testSumFilename = "erda-go.test.sum"
)

const testNoTestFilesPackage = true
const testAllPackagesTrigger = 0.2

func init() {
	home, err := homedir.Dir()
	if err != nil {
		fmt.Println("failed to get home directory")
		os.Exit(1)
	}
	homeDir = home
	cachePath = filepath.Join(homeDir, ".cache", "go-test")
	err = os.MkdirAll(cachePath, os.ModePerm)
	if err != nil {
		fmt.Println("failed to create go test cache")
		os.Exit(1)
	}
	testSumFilename = filepath.Join(cachePath, testSumFilename)
}

func main() {
	base, err := readBasePath()
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	err = testAllPackages(base)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
		return
	}
}

func testAllPackages(base string) error {
	packages := make(map[string]*packageInfo)
	fset := token.NewFileSet()
	err := filepath.Walk(".", func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			name := info.Name()
			if name != "." {
				// Skip directories like ".git".
				if strings.HasPrefix(name, ".") {
					return filepath.SkipDir
				}
				// Skip directories wasn't included in the base package, i.e. proto-go
				if module, err := readBasePathFromDir(path); err == nil && !strings.HasPrefix(module, base+"/") {
					return filepath.SkipDir
				}
			}

			// parse package
			pkgs, err := parser.ParseDir(fset, path, nil, parser.ImportsOnly)
			if err != nil {
				return err
			}
			pkgPath := filepath.Join(base, path)
			pkgi := packages[pkgPath]
			if pkgi == nil {
				pkgi = &packageInfo{
					path:         path,
					dependencies: make(map[string]struct{}),
				}
				packages[pkgPath] = pkgi
			}
			for _, pkg := range pkgs {
				for _, info := range pkg.Files {
					for _, imp := range info.Imports {
						impPath, _ := strconv.Unquote(imp.Path.Value)
						pkgi.dependencies[impPath] = struct{}{}
					}
				}
			}
			return nil
		}
		pkgPath := filepath.Join(base, filepath.Dir(path))
		pkgi := packages[pkgPath]
		if pkgi == nil {
			return nil
		}
		// Only check code files.
		ext := filepath.Ext(path)
		if !codeFileExts[ext] {
			return nil
		}
		if strings.HasSuffix(path, "_test.go") || strings.HasSuffix(path, "_mock.go") {
			pkgi.hasTestFile = true
		}
		pkgi.files = append(pkgi.files, &fileInfo{
			path:       path,
			updateTime: info.ModTime(),
		})
		return nil
	})
	if err != nil {
		return err
	}

	// hash sum
	now := time.Now()
	pkgSum := make(map[string]*testSumItem)
	incoming := make(map[string]map[string]struct{})
	for pkg, info := range packages {
		if len(info.files) <= 0 {
			continue
		}
		sort.Sort(fileInfos(info.files))
		buf := make([]byte, 8)
		hash := md5.New()
		for _, file := range info.files {
			byts, err := ioutil.ReadFile(file.path)
			if err != nil {
				return err
			}
			hash.Write(byts)
			hash.Write(buf)
		}
		byts := hash.Sum(nil)
		pkgSum[pkg] = &testSumItem{
			pkg:      pkg,
			hash:     hex.EncodeToString(byts),
			testTime: now,
			info:     info,
		}

		for d := range info.dependencies {
			m := incoming[d]
			if m == nil {
				m = make(map[string]struct{})
				incoming[d] = m
			}
			m[pkg] = struct{}{}
		}
	}

	const doTestCheck = true
	if doTestCheck {
		preSum := readTestSum()
		cachedCoverage := filepath.Join(cachePath, "coverage.txt")
		profiles, err := cover.ParseProfiles(cachedCoverage)
		if err != nil {
			fmt.Printf("fail to read %s : %s\n", cachedCoverage, err)
		}
		if preSum == nil || err != nil {
			_, err := runTest("./...")
			if err != nil {
				return err
			}
		} else {
			var needTestPackages int
			for pkg, sum := range pkgSum {
				if sum.tested {
					continue
				}
				pre, ok := preSum[pkg]
				if ok && pre.hash == sum.hash {
					sum.testTime = pre.testTime
					continue
				}
				needTestPackages += countNeedTestPackages(sum, pkgSum, incoming)
			}
			if needTestPackages > int(float64(len(pkgSum))*testAllPackagesTrigger) {
				fmt.Println("too many package to test, all packages will be tested")
				_, err := runTest("./...")
				if err != nil {
					return err
				}
			} else {
				coverage := make(map[string][]*cover.Profile)
				for pkg, sum := range pkgSum {
					if sum.tested {
						continue
					}
					pre, ok := preSum[pkg]
					if ok && pre.hash == sum.hash {
						sum.testTime = pre.testTime
						continue
					}
					err := recursiveTest(sum, pkgSum, incoming, coverage)
					if err != nil {
						return err
					}
				}
				var newProfiles []*cover.Profile
				for _, p := range profiles {
					dir := filepath.Dir(p.FileName)
					if _, ok := coverage[dir]; !ok {
						newProfiles = append(newProfiles, p)
					}
				}
				for _, ps := range coverage {
					newProfiles = append(newProfiles, ps...)
				}

				err = cover.Write("coverage.txt.tmp", "atomic", newProfiles)
				if err != nil {
					return err
				}
				os.Rename("coverage.txt.tmp", "coverage.txt")
			}
		}
		absCachedCoverageFilePath, _ := filepath.Abs(cachedCoverage)
		absCoverageFilePath, _ := filepath.Abs("coverage.txt")
		if absCachedCoverageFilePath != absCoverageFilePath {
			cacheCoverageTmp := filepath.Join(cachePath, "coverage.txt.tmp")
			_, err = copyFile(cacheCoverageTmp, "coverage.txt")
			if err != nil {
				fmt.Println("failed to copy coverage.txt:", err)
			} else {
				os.Rename(cacheCoverageTmp, cachedCoverage)
				fmt.Println("save coverage.txt ->", cachedCoverage)
			}
		}
	}
	return writeTestSum(pkgSum)
}

type fileInfo struct {
	path       string
	updateTime time.Time
}

type fileInfos []*fileInfo

func (fs fileInfos) Len() int           { return len(fs) }
func (fs fileInfos) Less(i, j int) bool { return fs[i].path < fs[j].path }
func (fs fileInfos) Swap(i, j int) {
	fs[i], fs[j] = fs[j], fs[i]
}

type packageInfo struct {
	path         string
	files        []*fileInfo
	dependencies map[string]struct{}
	hasTestFile  bool
}

type testSumItem struct {
	pkg      string
	hash     string
	testTime time.Time
	info     *packageInfo
	tested   bool
	checked  bool
}

func readTestSum() map[string]*testSumItem {
	byts, err := ioutil.ReadFile(testSumFilename)
	if err != nil {
		return nil
	}
	sum := make(map[string]*testSumItem)
	lines := strings.Split(string(byts), "\n")
	for _, line := range lines {
		if len(line) <= 0 {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) != 3 {
			if len(fields) == 0 {
				continue
			}
			return nil
		}
		t, err := time.Parse(time.RFC3339Nano, fields[2])
		if err != nil {
			return nil
		}
		pkg := fields[0]
		sum[pkg] = &testSumItem{
			pkg:      pkg,
			hash:     fields[1],
			testTime: t,
		}
	}
	fmt.Println("read test sum:", testSumFilename)
	return sum
}

func writeTestSum(testSum map[string]*testSumItem) error {
	var keys []string
	for pkg := range testSum {
		keys = append(keys, pkg)
	}
	sort.Strings(keys)
	buf := bytes.Buffer{}
	for _, key := range keys {
		sum := testSum[key]
		buf.WriteString(fmt.Sprintf("%s %s %s\n", sum.pkg, sum.hash, sum.testTime.Format(time.RFC3339Nano)))
	}
	return ioutil.WriteFile(testSumFilename, buf.Bytes(), os.ModePerm)
}

func readBasePath() (string, error) {
	return readBasePathFromDir(".")
}

func readBasePathFromDir(dir string) (string, error) {
	mod, err := ioutil.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		return "", err
	}
	lines := strings.Split(string(mod), "\n")
	for _, line := range lines {
		if strings.HasPrefix(line, "module ") {
			return strings.TrimSpace(line[len("module "):]), nil
		}
	}
	return "", errors.New("not found module path")
}

func copyFile(dstName, srcName string) (written int64, err error) {
	src, err := os.Open(srcName)
	if err != nil {
		return
	}
	defer src.Close()
	dst, err := os.OpenFile(dstName, os.O_WRONLY|os.O_CREATE, os.ModePerm)
	if err != nil {
		return
	}
	defer dst.Close()
	return io.Copy(dst, src)
}

func runTest(file string) (profiles []*cover.Profile, err error) {
	var coverage = "coverage.txt"
	if file != "./..." {
		coverage = filepath.Join(file, "pkg.coverage.txt")
		defer func() {
			profiles, _ = cover.ParseProfiles(coverage)
			os.Remove(coverage)
		}()
	}
	args := append([]string{"test", "-tags=musl", "-gcflags=-l", "-work", "-cpu=2", "-timeout=30s", "-failfast", "-race", "-coverprofile=" + coverage, "-covermode=atomic"})
	args = append(args, []string{"-ldflags", "-X google.golang.org/protobuf/reflect/protoregistry.conflictPolicy=warn"}...)
	args = append(args, []string{"-gcflags", "-N -l"}...)
	args = append(args, file)
	fmt.Printf("exec: go %s\n", strings.Join(args, " "))
	cmd := exec.Command("go", args...)
	cmd.Stdin = os.Stdin
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err = cmd.Run()
	return
}

func recursiveTest(entry *testSumItem, pkgSum map[string]*testSumItem, incoming map[string]map[string]struct{}, coverage map[string][]*cover.Profile) error {
	if entry.tested {
		return nil
	}
	entry.tested = true
	if testNoTestFilesPackage || entry.info.hasTestFile {
		profiles, err := runTest(string([]rune{'.', os.PathSeparator}) + entry.info.path)
		if err != nil {
			return err
		}
		entry.testTime = time.Now()
		coverage[entry.info.path] = profiles
	}
	for pkg := range incoming[entry.pkg] {
		t := pkgSum[pkg]
		if t != nil {
			err := recursiveTest(t, pkgSum, incoming, coverage)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func countNeedTestPackages(entry *testSumItem, pkgSum map[string]*testSumItem, incoming map[string]map[string]struct{}) (testPackages int) {
	if entry.checked {
		return 0
	}
	entry.checked = true
	if testNoTestFilesPackage || entry.info.hasTestFile {
		testPackages++
	}
	for pkg := range incoming[entry.pkg] {
		t := pkgSum[pkg]
		if t != nil {
			testPackages += countNeedTestPackages(t, pkgSum, incoming)
		}
	}
	return testPackages
}
