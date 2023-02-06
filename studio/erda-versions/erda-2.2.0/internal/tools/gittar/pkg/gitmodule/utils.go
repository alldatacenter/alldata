// Copyright 2015 The Gogs Authors. All rights reserved.
// Use of this source code is governed by a MIT-style
// license that can be found in the LICENSE file.

package gitmodule

import (
	"fmt"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
	"sync"

	"github.com/sirupsen/logrus"
)

// objectCache provides thread-safe cache opeations.
type objectCache struct {
	lock  sync.RWMutex
	cache map[string]interface{}
}

func newObjectCache() *objectCache {
	return &objectCache{
		cache: make(map[string]interface{}, 10),
	}
}

func (oc *objectCache) Set(id string, obj interface{}) {
	oc.lock.Lock()
	defer oc.lock.Unlock()

	oc.cache[id] = obj
}

func (oc *objectCache) Get(id string) (interface{}, bool) {
	oc.lock.RLock()
	defer oc.lock.RUnlock()

	obj, has := oc.cache[id]
	return obj, has
}

// isDir returns true if given path is a directory,
// or returns false when it's a file or does not exist.
func isDir(dir string) bool {
	f, e := os.Stat(dir)
	if e != nil {
		return false
	}
	return f.IsDir()
}

// isFile returns true if given path is a file,
// or returns false when it's a directory or does not exist.
func isFile(filePath string) bool {
	f, e := os.Stat(filePath)
	if e != nil {
		return false
	}
	return !f.IsDir()
}

// isExist checks whether a file or directory exists.
// It returns false when the file or directory does not exist.
func isExist(path string) bool {
	_, err := os.Stat(path)
	return err == nil || os.IsExist(err)
}

func concatenateError(err error, stderr string) error {
	if len(stderr) == 0 {
		return err
	}
	return fmt.Errorf("%v - %s", err, stderr)
}

// If the object is stored in its own file (i.e not in a pack file),
// this function returns the full path to the object file.
// It does not test if the file exists.
func filepathFromSHA1(rootdir, sha1 string) string {
	return filepath.Join(rootdir, "objects", sha1[:2], sha1[2:])
}

func RefEndName(refStr string) string {
	if strings.HasPrefix(refStr, BRANCH_PREFIX) {
		return refStr[len(BRANCH_PREFIX):]
	}

	if strings.HasPrefix(refStr, TAG_PREFIX) {
		return refStr[len(TAG_PREFIX):]
	}

	return refStr
}

func GetMirrorRepoUrl(repoPath string) string {
	re := regexp.MustCompile(`\r?\n`)
	remoteCmd := exec.Command("git", "remote", "get-url", "origin")
	remoteCmd.Dir = repoPath
	out, err := remoteCmd.CombinedOutput()
	if err != nil {
		logrus.Errorf("fail to GetMirrorRepoUrl,err: %s", err.Error())
		return ""
	}
	return re.ReplaceAllString(string(out), "")
}

func GetUrlWithBasicAuth(repoUrl string, username string, password string) (string, error) {
	parseRepoUrl, err := url.Parse(repoUrl)
	if err != nil {
		return "", err
	}
	parseRepoUrl.User = url.UserPassword(username, password)
	return parseRepoUrl.String(), nil
}
func CheckRemoteHttpRepo(repoUrl string, username string, password string) error {
	urlWithAuth, err := GetUrlWithBasicAuth(repoUrl, username, password)
	if err != nil {
		return err
	}
	cmd := exec.Command("git", "ls-remote", "--refs", urlWithAuth)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("err:%s output:%s", err, RemoveAuthInfo(string(output), urlWithAuth))
	}
	return nil
}

func RemoveAuthInfo(input string, originUrl string) string {
	parseRepoUrl, err := url.Parse(originUrl)
	if err != nil {
		return input
	}
	parseRepoUrl.User = &url.Userinfo{}
	newUrl := parseRepoUrl.String()
	result := strings.Replace(input, originUrl, newUrl, -1)
	return strings.Replace(result, "@", "", -1)
}
