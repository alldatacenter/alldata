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

//go:build !codeanalysis
// +build !codeanalysis

package gitmodule

import (
	"errors"

	git "github.com/libgit2/git2go/v30"
)

// DiffLineType represents the type of a line in diff.
type DiffLineType string

const (
	DIFF_LINE_CONTEXT = "context"
	DIFF_LINE_ADD     = "add"
	DIFF_LINE_DEL     = "delete"
	DIFF_LINE_SECTION = "section"
)

// DiffFileType represents the file status in diff.
type DiffFileType string

const (
	DIFF_FILE_ADD      DiffFileType = "add"
	DIFF_FILE_MODIFIED DiffFileType = "modified"
	DIFF_FILE_DEL      DiffFileType = "delete"
	DIFF_FILE_RENAME   DiffFileType = "rename"
)

type DiffOptions struct {
	Pathspec              []string //指定路径格式diff
	ShowAll               bool     //是否全量返回
	MaxDiffLineCharacters int      //最大diff一行内容,如果设置ShowAll忽略此值
	MaxDiffLines          int
	MaxDiffFiles          int
	ContextLineCount      int
	MaxSize               int
}

func NewDefaultDiffOptions() *DiffOptions {
	return &DiffOptions{
		Pathspec:              []string{},
		ShowAll:               false,
		MaxDiffLineCharacters: Setting.MaxGitDiffLineCharacters,
		MaxDiffLines:          Setting.MaxGitDiffLines,
		MaxDiffFiles:          Setting.MaxGitDiffFiles,
		ContextLineCount:      Setting.ContextLineCount,
		MaxSize:               Setting.MaxGitDiffSize,
	}
}

func (d *DiffOptions) SetShowAll(showAll bool) *DiffOptions {
	d.ShowAll = showAll
	return d
}

// DiffLine represents a line in diff.
type DiffLine struct {
	OldLineNo int          `json:"oldLineNo"`
	NewLineNo int          `json:"newLineNo"`
	Type      DiffLineType `json:"type"`
	Content   string       `json:"content"`
}

// DiffSection represents a section in diff.
type DiffSection struct {
	Lines []*DiffLine `json:"lines"`
}

// Line returns a specific line by type (add or del) and file line number from a section.
func (diffSection *DiffSection) Line(lineType DiffLineType, idx int) *DiffLine {
	var (
		difference    = 0
		addCount      = 0
		delCount      = 0
		matchDiffLine *DiffLine
	)

LOOP:
	for _, diffLine := range diffSection.Lines {
		switch diffLine.Type {
		case DIFF_LINE_ADD:
			addCount++
		case DIFF_LINE_DEL:
			delCount++
		default:
			if matchDiffLine != nil {
				break LOOP
			}
			difference = diffLine.NewLineNo - diffLine.OldLineNo
			addCount = 0
			delCount = 0
		}

		switch lineType {
		case DIFF_LINE_DEL:
			if diffLine.NewLineNo == 0 && diffLine.OldLineNo == idx-difference {
				matchDiffLine = diffLine
			}
		case DIFF_LINE_ADD:
			if diffLine.OldLineNo == 0 && diffLine.NewLineNo == idx+difference {
				matchDiffLine = diffLine
			}
		}
	}

	if addCount == delCount {
		return matchDiffLine
	}
	return nil
}

// DiffFile represents a file in diff.
type DiffFile struct {
	Name        string         `json:"name"`
	OldName     string         `json:"oldName"`
	Index       string         `json:"index"` // 40-byte SHA, Changed/New: new SHA; Deleted: old SHA
	Addition    int            `json:"addition"`
	Deletion    int            `json:"deletion"`
	Type        DiffFileType   `json:"type"`
	IsBin       bool           `json:"isBin"`
	IsSubmodule bool           `json:"isSubmodule"`
	Sections    []*DiffSection `json:"sections"`
	IsFinish    bool           `json:"-"`
	HasMore     bool           `json:"hasMore"`
	OldMode     string         `json:"oldMode"`
	NewMode     string         `json:"newMode"`
}

func (diffFile *DiffFile) NumSections() int {
	return len(diffFile.Sections)
}

// Diff contains all information of a specific diff output.
type Diff struct {
	FilesChanged  int         `json:"filesChanged"`
	TotalAddition int         `json:"totalAddition"`
	TotalDeletion int         `json:"totalDeletion"`
	Files         []*DiffFile `json:"files"`
	IsFinish      bool        `json:"isFinish"`
}

func (diff *Diff) NumFiles() int {
	return len(diff.Files)
}

func (repo *Repository) GetDiffFile(newCommit *Commit, oldCommit *Commit, oldPath, newPath string) (*DiffFile, error) {

	baseCommit, err := repo.GetMergeBase(newCommit, oldCommit)
	if err != nil {
		return nil, err
	}

	diffOptions := NewDefaultDiffOptions()
	diffOptions.Pathspec = []string{oldPath, newPath}
	// 单文件尽量不做限制，获取全量diff数据
	diffOptions.ShowAll = true
	diffOptions.ContextLineCount = 1000
	diff, err := repo.GetDiffWithOptions(newCommit, baseCommit, diffOptions)
	if err != nil {
		return nil, err
	}
	for _, file := range diff.Files {
		if file.Name == newPath {
			return file, nil
		}
	}
	return nil, errors.New("path error " + newPath)

}

func (repo *Repository) GetDiff(newCommit *Commit, oldCommit *Commit) (*Diff, error) {
	return repo.GetDiffWithOptions(newCommit, oldCommit, NewDefaultDiffOptions().SetShowAll(true))
}

func (repo *Repository) GetDiffWithOptions(newCommit *Commit, oldCommit *Commit, diffOptions *DiffOptions) (*Diff, error) {

	modeMap := map[uint16]string{
		0:     "0",
		16384: "040000",  //tree
		33188: "0100644", //blob
		33261: "0100755", //exec
		40960: "0120000", //sym
		57344: "0160000", //commit
	}
	rawRepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}

	var treeOld *git.Tree

	if oldCommit == nil {
		treeOld = nil
	} else {
		oidOld, _ := git.NewOid(oldCommit.TreeSha)
		treeOld, err = rawRepo.LookupTree(oidOld)
		if err != nil {
			return nil, err
		}
	}

	oidNew, _ := git.NewOid(newCommit.TreeSha)
	treeNew, err := rawRepo.LookupTree(oidNew)
	if err != nil {
		return nil, err
	}

	options, _ := git.DefaultDiffOptions()
	options.Pathspec = diffOptions.Pathspec
	options.ContextLines = uint32(diffOptions.ContextLineCount)
	options.MaxSize = diffOptions.MaxSize
	diff, err := rawRepo.DiffTreeToTree(treeOld, treeNew, &options)
	if err != nil {
		return nil, err
	}
	findOptions, err := git.DefaultDiffFindOptions()
	if err != nil {
		return nil, err
	}
	err = diff.FindSimilar(&findOptions)
	if err != nil {
		return nil, err
	}
	diffStats, err := diff.Stats()
	if err != nil {
		return nil, err
	}
	diffData := &Diff{
		FilesChanged:  diffStats.FilesChanged(),
		TotalDeletion: diffStats.Deletions(),
		TotalAddition: diffStats.Insertions(),
		IsFinish:      false,
	}

	fileCount := 0
	err = diff.ForEach(func(delta git.DiffDelta, f float64) (git.DiffForEachHunkCallback, error) {
		diffFile := &DiffFile{
			Name:     delta.NewFile.Path,
			OldName:  delta.OldFile.Path,
			IsFinish: false,
			HasMore:  false,
			Index:    "",
			OldMode:  modeMap[delta.OldFile.Mode],
			NewMode:  modeMap[delta.NewFile.Mode],
		}

		if fileCount > diffOptions.MaxDiffFiles {
			return func(hunk git.DiffHunk) (git.DiffForEachLineCallback, error) {
				return func(line git.DiffLine) error {
					return nil
				}, nil
			}, nil
		}
		fileCount += 1

		if delta.Flags == git.DiffFlagBinary {
			diffFile.IsBin = true
		}

		switch delta.Status {
		case git.DeltaAdded:
			diffFile.Type = DIFF_FILE_ADD
			diffFile.Index = newCommit.ID
		case git.DeltaModified:
			diffFile.Type = DIFF_FILE_MODIFIED
			diffFile.Index = newCommit.ID
		case git.DeltaDeleted:
			diffFile.Type = DIFF_FILE_DEL
			diffFile.Index = oldCommit.ID
		case git.DeltaRenamed:
			diffFile.Type = DIFF_FILE_RENAME
			diffFile.Index = newCommit.ID
		}

		diffData.Files = append(diffData.Files, diffFile)
		return func(hunk git.DiffHunk) (git.DiffForEachLineCallback, error) {
			section := &DiffSection{}
			diffFile.Sections = append(diffFile.Sections, section)
			diffLine := &DiffLine{
				OldLineNo: -1,
				NewLineNo: -1,
				Content:   hunk.Header,
				Type:      DIFF_LINE_SECTION,
			}

			section.Lines = []*DiffLine{}
			if hunk.OldStart > 1 || hunk.NewStart > 1 {
				section.Lines = append(section.Lines, diffLine)
			}

			//	Header:   hunk.Header,
			return func(line git.DiffLine) error {
				diffLine := &DiffLine{
					OldLineNo: line.OldLineno,
					NewLineNo: line.NewLineno,
				}

				switch line.Origin {
				case git.DiffLineContext:
					diffLine.Type = DIFF_LINE_CONTEXT
				case git.DiffLineAddition:
					diffLine.Type = DIFF_LINE_ADD
					diffFile.Addition += 1
				case git.DiffLineDeletion:
					diffLine.Type = DIFF_LINE_DEL
					diffFile.Deletion += 1
				}

				//大文件不做处理显示
				if (len(line.Content) > diffOptions.MaxDiffLineCharacters || diffFile.IsFinish == true) && !diffOptions.ShowAll {
					diffFile.HasMore = true
					diffFile.IsFinish = true
					return nil
				}

				lineContent := line.Content

				if len(lineContent) > 0 && lineContent[len(lineContent)-1] == '\n' {
					// Remove line break.
					lineContent = lineContent[:len(lineContent)-1]
				}

				diffLine.Content = lineContent

				section.Lines = append(section.Lines, diffLine)
				return nil
			}, nil
		}, nil
	}, git.DiffDetailLines)

	for _, file := range diffData.Files {
		if len(file.Sections) > 0 {
			section := file.Sections[len(file.Sections)-1]
			if ButtonContextLineCount(section.Lines) >= diffOptions.ContextLineCount {
				file.Sections = append(file.Sections, &DiffSection{
					Lines: []*DiffLine{
						{
							OldLineNo: -1,
							NewLineNo: -1,
							Content:   "",
							Type:      DIFF_LINE_SECTION,
						},
					},
				})
			}
		}
	}
	return diffData, nil
}

func ButtonContextLineCount(lines []*DiffLine) int {
	lineNum := len(lines)
	contextLineCount := 0
	lineNum -= 1
	for lineNum >= 0 {
		if lines[lineNum].Type == DIFF_LINE_CONTEXT {
			contextLineCount += 1
		} else {
			return contextLineCount
		}
		lineNum -= 1
	}
	return contextLineCount
}
