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

package qaparser

import (
	"encoding/xml"
)

type XmlNode struct {
	XMLName xml.Name
	Attrs   map[string]string `xml:"-"`
	Content []byte            `xml:",innerxml"`
	Nodes   []XmlNode         `xml:",any"`
}

func (n *XmlNode) Attr(name string) string {
	return n.Attrs[name]
}

func (n *XmlNode) UnmarshalXML(d *xml.Decoder, start xml.StartElement) error {
	type nodeAlias XmlNode
	if err := d.DecodeElement((*nodeAlias)(n), &start); err != nil {
		return err
	}

	content, err := extractContent(n.Content)
	if err != nil {
		return err
	}

	n.Content = content

	n.Attrs = attrMap(start.Attr)
	return nil
}

func attrMap(attrs []xml.Attr) map[string]string {
	if len(attrs) == 0 {
		return nil
	}

	attributes := make(map[string]string, len(attrs))
	for _, attr := range attrs {
		attributes[attr.Name.Local] = attr.Value
	}
	return attributes
}
