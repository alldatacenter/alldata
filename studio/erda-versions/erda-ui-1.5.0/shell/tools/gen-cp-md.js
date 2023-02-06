// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

const fs = require('fs');
const path = require('path');
const ts = require('typescript');
const { walker } = require('./file-walker');
const { tsquery } = require('@phenomnomnominal/tsquery');

const targetDirPath = path.resolve(__dirname, '../app/config-page/docs');

function fileNameToScriptKind(fileName) {
  if (fileName.endsWith('.ts')) return ts.ScriptKind.TS;
  if (fileName.endsWith('.js')) return ts.ScriptKind.JS;
  if (fileName.endsWith('.tsx')) return ts.ScriptKind.TSX;
  if (fileName.endsWith('.jsx')) return ts.ScriptKind.JSX;
  return ts.ScriptKind.Unknown;
}

function getSourceFile(fileName) {
  return ts.createSourceFile(
    fileName,
    fs.readFileSync(fileName, 'utf-8'),
    ts.ScriptTarget.ESNext,
    true,
    fileNameToScriptKind(fileName),
  );
}

if (!fs.existsSync(targetDirPath)) {
  console.log(`目录 ${targetDirPath} 不存在，开始创建`);
  fs.mkdirSync(targetDirPath, '0777');
}

// AST 查看工具：https://ts-ast-viewer.com/
const dealFile = (content, filePath, isEnd) => {
  const { base } = path.parse(filePath);
  // const sourceFile = getSourceFile(filePath);

  // if (sourceFile.isDeclarationFile) {
  //   ts.forEachChild(sourceFile, visit);

  //   function visit(node) {
  //     const commentRanges = ts.getLeadingCommentRanges(sourceFile.getFullText(), node.getFullStart());
  //     console.log('commentRanges:', node.name, commentRanges);
  //     if (commentRanges && commentRanges.length) {
  //       const commentStrings = commentRanges.map(r => sourceFile.getFullText().slice(r.pos, r.end))
  //       console.log('commentStrings:', commentStrings);
  //     }
  //   }
  // }

  if (base.endsWith('.d.ts')) {
    const ast = tsquery.ast(content);
    const modules = tsquery(ast, 'ModuleDeclaration');
    const data = {};
    modules.map((module) => {
      const infData = [];
      const interfaces = tsquery(module, 'InterfaceDeclaration');
      interfaces.map((iFace) => {
        infData.push({
          name: iFace.name.text,
          props: iFace.members.map((m) => {
            return [m.name && m.name.text, m.type.getText(), !!m.questionToken];
          }),
        });
      });

      const enumData = [];
      const enums = tsquery(module, 'EnumDeclaration');
      enums.map((_enum) => {
        enumData.push({
          name: _enum.name.text,
          props: _enum.members.map((m) => {
            return [m.name && m.name.text, m.initializer.text];
          }),
        });
      });

      const typeData = [];
      const types = tsquery(module, 'TypeAliasDeclaration');
      types.map((type) => {
        typeData.push({
          name: type.name.text,
          value: type.type.getText(),
        });
      });

      data[module.name.text] = {
        infs: infData,
        enums: enumData,
        types: typeData,
      };

      content = `# ${module.name.text}

## 接口
${infData.map((inf) => {
  const text = ['', `### ${inf.name}`, `| 名称 | 类型 | 必填 |`, `| --- | --- | --- |`];
  inf.props.map(([name, type, required]) => text.push(`| ${name} | ${type} | ${required} |`));
  return text.join('\n');
})}

## 枚举

${enumData.map((_enum) => {
  const text = ['', `### ${_enum.name}`, `| 名称 | 值 |`, `| --- | --- |`];
  _enum.props.map(([name, value]) => text.push(`| ${name} | ${value} |`));
  return text.join('\n');
})}

## 类型

| 名称 | 值 |
| --- | --- |
${typeData.map((type) => {
  const text = [`| ${type.name} | ${type.value} |`];
  return text.join('\n');
})}
`;
    });

    const savePath = path.resolve(targetDirPath, `${base.slice(0, -5)}.md`);

    fs.writeFile(savePath, content, 'utf8', (writeErr) => {
      if (writeErr) return console.error(`写入文件：${savePath}错误`, writeErr);
    });
  }

  if (isEnd) {
    console.log('文档生成完毕');
  }
};

walker({
  root: path.resolve(__dirname, '../app/config-page/components/'),
  dealFile,
});
