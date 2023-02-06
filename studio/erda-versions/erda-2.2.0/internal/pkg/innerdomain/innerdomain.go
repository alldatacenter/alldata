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

// Package innerdomain , interconversion between k8s & marathon internal domain
//
// 旧版本的内部地址格式
//
// <servicename>.<servicegroup-name>.<servicegroup-namespace>.<suffix1>.<suffix2>.marathon.l4lb.thisdcos.directory
//
// e.g.
// (marathon) prototype.prod-6056.services.v1.runtimes.marathon.l4lb.thisdcos.directory
// (k8s)      不存在旧版本k8s地址
// (marathon) consul.consul-afdb5eb0327848e19f3d414eb345dfdd.addons-2126.v1.runtimes.marathon.l4lb.thisdcos.directory
// (k8s)      不存在旧版本k8s地址
//
// 新版本的内部地址格式
//
// marathon:
//   <servicename>.<namespace>.marathon.l4lb.thisdcos.directory
// k8s:
//   <servicename>.<namespace>.svc.cluster.local
//
// _LIMIT_: <namespace> 最长63位, <namespace> 必须保持唯一
//
// e.g.
// (marathon) blog-service.<namespace>.marathon.l4lb.thisdcos.directory
// (k8s)      blog-service.<namespace>.svc.cluster.local
package innerdomain

import (
	"fmt"
	"net/url"
	"regexp"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/strutil"
)

var (
	// ErrInvalidAddr 输入地址不规范
	ErrInvalidAddr = errors.New("invalid input address")
	// ErrMarathonLegacyAddr marathon _旧版_ 内部域名不满足规则
	ErrMarathonLegacyAddr = fmt.Errorf("invalid legacy marathon addr, not satisfy regexp: %v",
		marathonLegacyRegex)
	// ErrMarathonAddr marathon 内部域名不满足规则
	ErrMarathonAddr = fmt.Errorf("invalid marathon addr, not satisfy regexp: %v", marathonRegex)
	// ErrK8SAddr k8s 内部域名不满足规则
	ErrK8SAddr = fmt.Errorf("invalid k8s addr: not satisfy regexp: %v", k8sRegex)
	// ErrTooLongNamespace 内部域名的 namespace 部分过长
	ErrTooLongNamespace = errors.New("internal addr's namespace length > 63")
	// ErrNoLegacyK8SAddr 不存在 k8s _旧版_ 内部域名
	ErrNoLegacyK8SAddr = errors.New("can't generate legacy k8s internal addr")
)

var (
	marathonLegacyRegex = regexp.MustCompile(`^((?:[\w-]+?\.)+?)marathon\.l4lb\.thisdcos\.directory`)
	marathonRegex       = regexp.MustCompile(
		`^(?P<servicename>[\w-]+?)\.(?P<namespace>[\w-]+?)\.marathon\.l4lb\.thisdcos\.directory`)
	k8sRegex = regexp.MustCompile(
		`^(?P<servicename>[\w-]+?)\.(?P<namespace>[\w-]+?)\.svc\.cluster\.local`)
)

const (
	// 不同与普通的 marathon 地址，dice 组件的内部地址形如:
	// eventbox.marathon.l4lb.thisdcos.directory
	// 转换成 k8s 内部地址时，指定 namespace 为 "dice"
	// e.g.   eventbox.dice.svc.cluster.local
	namespaceForDice = "default"

	k8sSuffix      = "svc.cluster.local"
	marathonSuffix = "marathon.l4lb.thisdcos.directory"
)

// InnerDomain 代表内部地址
type InnerDomain struct {
	originAddr string
	host       string
	domaininfo domainGenerator
}

func MustParseWithEnv(originAddr string, k8s bool) string {
	r, err := ParseWithEnv(originAddr, k8s)
	if err != nil {
		panic(err)
	}
	return r
}

// ParseWithEnv
func ParseWithEnv(originAddr string, k8s bool) (string, error) {
	inner, err := Parse(originAddr)
	if err != nil {
		return "", err
	}
	if k8s {
		return inner.K8S()
	}
	return inner.Marathon()
}

// Parse 从 host 解析出 InnerDomain
func Parse(originAddr string) (InnerDomain, error) {
	addr := strutil.Concat("http://", strutil.TrimPrefixes(originAddr, "http://", "https://"))
	u, err := url.Parse(addr)
	if err != nil {
		return InnerDomain{}, errors.Wrap(ErrInvalidAddr, err.Error())
	}
	if u.Host == "" {
		return InnerDomain{}, errors.Wrap(ErrInvalidAddr, fmt.Sprintf("empty host, originaddr: %v", originAddr))
	}
	domaininfo, err := parse(u.Host)
	if err != nil {
		return InnerDomain{}, err
	}
	return InnerDomain{
		originAddr: originAddr,
		host:       u.Host,
		domaininfo: domaininfo,
	}, nil
}

// New 根据 servicename, namespace 构建 InnerDomain
func New(servicename, namespace string) (InnerDomain, error) {
	domaininfo := domaininfo{serviceName: servicename, namespace: namespace}
	k8s, err := domaininfo.k8s()
	if err != nil {
		return InnerDomain{}, err
	}
	domaininfo1, err := parseK8S(k8s)
	if err != nil {
		return InnerDomain{}, err
	}
	marathon, err := domaininfo.marathon()
	if err != nil {
		return InnerDomain{}, err
	}
	domaininfo2, err := parseMarathon(marathon)
	if err != nil {
		return InnerDomain{}, err
	}
	if domaininfo != domaininfo1 || domaininfo != domaininfo2 {
		return InnerDomain{},
			fmt.Errorf("[BUG] domaininfo: %v, domaininfo1: %v, domaininfo2: %v", domaininfo, domaininfo1, domaininfo2)
	}
	return InnerDomain{domaininfo: &domaininfo}, nil
}

// K8S 生成 k8s 版本的内部地址
func (i *InnerDomain) K8S() (string, error) {
	return i.domaininfo.k8s()
}

// Marathon 生成 marathon 版本的内部地址
func (i *InnerDomain) Marathon() (string, error) {
	return i.domaininfo.marathon()
}

// MustK8S 类似 K8S，如果出错则panic
func (i *InnerDomain) MustK8S() string {
	addr, err := i.domaininfo.k8s()
	if err != nil {
		panic(err)
	}
	return addr
}

// MustMarathon 类似 Marathon，如果出错则 panic
func (i *InnerDomain) MustMarathon() string {
	addr, err := i.domaininfo.marathon()
	if err != nil {
		panic(err)
	}
	return addr
}

func parse(addr string) (domainGenerator, error) {
	var (
		err1, err2, err3 error
		domainil         domaininfoLegacy
		domaini          domaininfo
	)
	if domaini, err1 = parseMarathon(addr); err1 == nil {
		return &domaini, nil
	}
	if err1 != ErrMarathonAddr {
		return nil, err1
	}
	if domaini, err2 = parseK8S(addr); err2 == nil {
		return &domaini, nil
	}
	if err2 != ErrK8SAddr {
		return nil, err2
	}
	if domainil, err3 = parseMarathonLegacy(addr); err3 == nil {
		return &domainil, nil
	}
	if err3 != ErrMarathonLegacyAddr {
		return nil, err3
	}

	return nil, errors.Wrap(err1, errors.Wrap(err2, err3.Error()).Error())
}

type domainGenerator interface {
	k8s() (string, error)
	marathon() (string, error)
}

// (marathon) prototype.prod-6056.services.v1.runtimes.marathon.l4lb.thisdcos.directory
// {
//  serviceName: prototype,
//  servicegroupName: prod-6056,
//  servicegroupNamespace: services,
//  suffix: []string{"v1", "runtimes"},
// }
// (marathon) consul.consul-afdb5eb0327848e19f3d414eb345dfdd.addons-2126.v1.runtimes.marathon.l4lb.thisdcos.directory
// {
//  serviceName: consul,
//  servicegroupName: consul-afdb5eb0327848e19f3d414eb345dfdd,
//  servicegroupNamespace: addons-2126
//  suffix: []string{"v1", "runtimes"},
// }
type domaininfoLegacy struct {
	servicegroupName      string
	servicegroupNamespace string
	serviceName           string
	suffix                []string
}

// 对于 普通服务 的旧版marathon地址，返回 ErrNoLegacyK8SAddr
// 对于 dice组件 的旧版marathon地址，如下处理:
// eventbox.marathon.l4lb.thisdcos.directory => eventbox.dice.svc.cluster.local
// 区分 dice组件 和 普通服务 的方式：
// dice组件: domaininfoLegacy.servicegroupName == "" && domaininfoLegacy.servicegroupNamespace == ""
// 普通服务: 其他情况
func (a *domaininfoLegacy) k8s() (string, error) {
	if a.servicegroupName != "" || a.servicegroupNamespace != "" {
		return "", ErrNoLegacyK8SAddr
	}
	return a.serviceName, nil
}

func (a *domaininfoLegacy) marathon() (string, error) {
	components := append([]string{a.serviceName, a.servicegroupName, a.servicegroupNamespace}, a.suffix...)
	components = append(components, marathonSuffix)
	return strutil.Join(components, ".", true), nil
}

// marathon:
//   <servicename>.<namespace>.marathon.l4lb.thisdcos.directory
// k8s:
//   <servicename>.<namespace>.svc.cluster.local
type domaininfo struct {
	serviceName string
	namespace   string
}

func (a *domaininfo) k8s() (string, error) {
	return strutil.Join([]string{a.serviceName, a.namespace, k8sSuffix}, "."), nil
}
func (a *domaininfo) marathon() (string, error) {
	return strutil.Join([]string{a.serviceName, a.namespace, marathonSuffix}, "."), nil
}

func parseMarathonLegacy(addr string) (domaininfoLegacy, error) {
	r := marathonLegacyRegex.FindStringSubmatch(addr)
	if len(r) < 2 {
		return domaininfoLegacy{}, ErrMarathonLegacyAddr
	}
	splitted := strutil.Split(r[1], ".", true)

	info := domaininfoLegacy{}
	if len(splitted) >= 1 {
		info.serviceName = splitted[0]
	}
	if len(splitted) >= 2 {
		info.servicegroupName = splitted[1]
	}
	if len(splitted) >= 3 {
		info.servicegroupNamespace = splitted[2]
	}
	if len(splitted) >= 4 {
		info.suffix = splitted[3:]
	}
	return info, nil
}
func parseMarathon(addr string) (domaininfo, error) {
	r := marathonRegex.FindStringSubmatch(addr)
	if len(r) < 3 {
		return domaininfo{}, ErrMarathonAddr
	}
	if len(r[2]) > 63 {
		return domaininfo{}, ErrTooLongNamespace
	}
	return domaininfo{serviceName: r[1], namespace: r[2]}, nil
}

func parseK8S(addr string) (domaininfo, error) {
	r := k8sRegex.FindStringSubmatch(addr)
	if len(r) < 3 {
		return domaininfo{}, ErrK8SAddr
	}
	if len(r[2]) > 63 {
		return domaininfo{}, ErrTooLongNamespace
	}
	return domaininfo{
		serviceName: r[1],
		namespace:   r[2],
	}, nil
}
