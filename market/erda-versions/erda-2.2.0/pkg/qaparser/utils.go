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
	"archive/tar"
	"bytes"
	"compress/gzip"
	"encoding/xml"
	"html"
	"io"
	"os"
	"strconv"
	"strings"

	"github.com/pkg/errors"
)

//解压 tar.gz
func DeCompress(tarFile, dest string) error {
	srcFile, err := os.Open(tarFile)
	if err != nil {
		return err
	}
	defer srcFile.Close()
	gr, err := gzip.NewReader(srcFile)
	if err != nil {
		return err
	}
	defer gr.Close()
	tr := tar.NewReader(gr)
	for {
		hdr, err := tr.Next()
		if err != nil {
			if err == io.EOF {
				break
			} else {
				return err
			}
		}
		filename := dest + hdr.Name
		file, err := createFile(filename)
		if err != nil {
			return err
		}
		io.Copy(file, tr)
	}
	return nil
}

func createFile(name string) (*os.File, error) {
	err := os.MkdirAll(string([]rune(name)[0:strings.LastIndex(name, "/")]), 0755)
	if err != nil {
		return nil, err
	}
	return os.Create(name)
}

func ParseInt(i interface{}) (int, error) {
	switch i.(type) {
	case int:
		return i.(int), nil
	case string:
		if strings.TrimSpace(i.(string)) == "" {
			return 0, nil
		}
		r, err := strconv.Atoi(i.(string))
		if err != nil {
			return 0, err
		}
		return r, nil
	default:
		return 0, errors.New("not supported type yet")
	}
}

// reparentXML will wrap the given data (which is assumed to be valid XML), in
// a fake root nodeAlias.
//
// This action is useful in the event that the original XML document does not
// have a single root nodeAlias, which is required by the XML specification.
// Additionally, Go's XML parser will silently drop all nodes after the first
// that is encountered, which can lead to data loss from a parser perspective.
// This function also enables the ingestion of blank XML files, which would
// normally cause a parsing error.
func reparentXML(data []byte) []byte {
	return append(append([]byte("<fake-root>"), data...), "</fake-root>"...)
}

// extractContent parses the raw contents from an XML node, and returns it in a
// more consumable form.
//
// This function deals with two distinct classes of node data; Encoded entities
// and CDATA tags. These Encoded entities are normal (html escaped) text that
// you typically find between tags like so:
//   • "Hello, world!"  →  "Hello, world!"
//   • "I &lt;/3 XML"   →  "I </3 XML"
// CDATA tags are a special way to embed data that would normally require
// escaping, without escaping it, like so:
//   • "<![CDATA[Hello, world!]]>"  →  "Hello, world!"
//   • "<![CDATA[I &lt;/3 XML]]>"   →  "I &lt;/3 XML"
//   • "<![CDATA[I </3 XML]]>"      →  "I </3 XML"
//
// This function specifically allows multiple interleaved instances of either
// encoded entities or cdata, and will decode them into one piece of normalized
// text, like so:
//   • "I &lt;/3 XML <![CDATA[a lot]]>. You probably <![CDATA[</3 XML]]> too."  →  "I </3 XML a lot. You probably </3 XML too."
//      └─────┬─────┘         └─┬─┘   └──────┬──────┘         └──┬──┘   └─┬─┘
//      "I </3 XML "            │     ". You probably "          │      " too."
//                          "a lot"                         "</3 XML"
//
// Errors are returned only when there are unmatched CDATA tags, although these
// should cause proper XML unmarshalling errors first, if encountered in an
// actual XML document.
func extractContent(data []byte) ([]byte, error) {
	var (
		cdataStart = []byte("<![CDATA[")
		cdataEnd   = []byte("]]>")
		mode       int
		output     []byte
	)

	for {
		if mode == 0 {
			offset := bytes.Index(data, cdataStart)
			if offset == -1 {
				// The string "<![CDATA[" does not appear in the data. Unescape all remaining data and finish
				if bytes.Contains(data, cdataEnd) {
					// The string "]]>" appears in the data. This is an error!
					return nil, errors.New("unmatched CDATA end tag")
				}

				output = append(output, html.UnescapeString(string(data))...)
				break
			}

			// The string "<![CDATA[" appears at some offset. Unescape up to that offset. Discard "<![CDATA[" prefix.
			output = append(output, html.UnescapeString(string(data[:offset]))...)
			data = data[offset:]
			data = data[9:]
			mode = 1
		} else if mode == 1 {
			offset := bytes.Index(data, cdataEnd)
			if offset == -1 {
				// The string "]]>" does not appear in the data. This is an error!
				return nil, errors.New("unmatched CDATA start tag")
			}

			// The string "]]>" appears at some offset. Read up to that offset. Discard "]]>" prefix.
			output = append(output, data[:offset]...)
			data = data[offset:]
			data = data[3:]
			mode = 0
		}

	}

	return output, nil
}

// parse unmarshalls the given XML data into a graph of nodes, and then returns
// a slice of all top-level nodes.
func NodeParse(data []byte) ([]XmlNode, error) {
	var (
		buf  = bytes.NewBuffer(reparentXML(data))
		dec  = xml.NewDecoder(buf)
		root XmlNode
	)

	if err := dec.Decode(&root); err != nil {
		return nil, err
	}

	return root.Nodes, nil
}
