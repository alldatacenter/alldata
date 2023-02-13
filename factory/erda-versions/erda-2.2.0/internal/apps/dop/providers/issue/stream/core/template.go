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

package core

import (
	"bytes"
	"fmt"
	"strings"
	"text/template"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
)

type StreamTemplateRequest struct {
	StreamType   string
	StreamParams common.ISTParam
	Locale       string
	Lang         i18n.LanguageCodes
}

// getIssueStreamTemplate get issue stream template
func getIssueStreamTemplate(locale string, ist string) (string, error) {
	if locale != "zh" && locale != "en" {
		return "", errors.Errorf("invalid locale %v", locale)
	}

	v, ok := common.IssueTemplate[locale][ist]
	if !ok {
		return "", errors.Errorf("issue stream template not found")
	}

	return v, nil
}

// getIssueStreamTemplateForMsgSending get issue stream template for msg sending
func getIssueStreamTemplateForMsgSending(locale string, ist string) (string, error) {
	if locale == "" || locale == "zh-CN" {
		locale = "zh"
	}
	if locale == "en-US" {
		locale = "en"
	}
	templateContent, err := getIssueStreamTemplate(locale, ist)
	if err != nil {
		return "", err
	}

	// override template if have for msg sending
	vv, ok := common.IssueTemplateOverrideForMsgSending[locale][ist]
	if ok {
		templateContent = vv
	}

	return templateContent, nil
}

// getDefaultContent get rendered msg
func (p *provider) GetDefaultContent(req StreamTemplateRequest) (string, error) {
	locale := req.Locale
	if locale == "" || strings.Contains(locale, "zh") {
		locale = "zh"
	}
	if strings.Contains(locale, "en") {
		locale = "en"
	}
	// TODO: refactor issue stream template
	ct, err := getIssueStreamTemplate(locale, req.StreamType)
	if err != nil {
		return "", err
	}
	content, err := renderTemplate(ct, req.StreamParams, p.commonTran, req.Lang)
	if err != nil {
		return "", err
	}
	if req.StreamParams.ReasonDetail != "" {
		return fmt.Sprintf("%v %v", content, p.I18n.Text(req.Lang, req.StreamParams.ReasonDetail)), nil
	}
	return content, nil
}

// getDefaultContentForMsgSending get rendered msg for sending
func getDefaultContentForMsgSending(ist string, param common.ISTParam, tran i18n.Translator, locale string) (string, error) {
	ct, err := getIssueStreamTemplateForMsgSending(locale, ist)
	if err != nil {
		return "", err
	}
	langs, _ := i18n.ParseLanguageCode(locale)
	return renderTemplate(ct, param, tran, langs)
}

// renderTemplate render template
func renderTemplate(templateContent string, param common.ISTParam, tran i18n.Translator, lang i18n.LanguageCodes) (string, error) {
	tpl, err := template.New("c").Parse(templateContent)
	if err != nil {
		return "", err
	}

	var content bytes.Buffer
	if err := tpl.Execute(&content, param.Localize(tran, lang)); err != nil {
		return "", err
	}

	return content.String(), nil
}
