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
const _ = require('lodash');
const { tsquery } = require('@phenomnomnominal/tsquery');

const targetDirPath = path.resolve(__dirname, '../app/config-page/go-spec');

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

// 处理字符串枚举数据：
// a: 'a' | 'b'；1 | 2；
// string | number；Array<string | number>：go中不兼容多种类型定义，这种默认使用第一个定义，故第一个尽量写范围大的数据：string[] > string > number/boolean
const dealEnumStr = (str, type) => {
  const valArr = str.split('|');
  let defineType = '';
  const enumValStr = ['const ('];
  const isArray = str.startsWith('Array<');
  valArr.forEach((val, idx) => {
    const vStr = val.trim();
    if (!defineType) {
      if (isArray) {
        // 取第一个类型
        defineType = vStr.split('Array<')[1].trim();
      } else if (vStr.startsWith("'")) {
        defineType = 'string';
      } else if (!_.isNaN(+vStr)) {
        defineType = 'number';
      }
    }
    if (!isArray && ['string', 'number'].includes(defineType)) {
      const vKey = vStr.startsWith("'") ? vStr.slice(1, vStr.length - 1) : vStr;
      enumValStr.push(`  ${type}${_.upperFirst(vKey)} ${type} = ${defineType === 'string' ? `"${vKey}"` : +vKey}`);
    }
  });
  enumValStr.push(')\n');

  if (!isArray && ['string', 'number'].includes(defineType)) {
    return {
      type,
      define: `type ${type} ${defineType}\n`,
      value: enumValStr.join('\n'),
    };
  }
  let firstVal = valArr[0].trim();
  firstVal.includes('[]') && (firstVal = `[]${firstVal.split('[]')[0]}`);
  return {
    type: isArray ? `[]${defineType}` : firstVal,
  };
};

// 处理普通结构
const dealSpec = (spec) => {
  let codeArr = [];
  let extraCode = [];
  let importCommonCode = '';
  const typeCodeMap = {
    string: 'string',
    boolean: 'bool',
    number: 'int', // 后端分int和float
    any: 'interface{}',
    Obj: 'map[string]interface{}',
  };

  spec.forEach((specItem) => {
    const { name, props } = specItem;
    if (!name.startsWith('_')) {
      codeArr.push(`type ${name} struct {`);
      props.forEach((prop) => {
        const [attrName, attrType, attrRequired] = prop;
        if (!attrName) return;
        const codeItemArr = [`    ${_.upperFirst(attrName)}`];
        if (typeCodeMap[attrType]) {
          codeItemArr.push(typeCodeMap[attrType]);
        } else if (attrType.includes('|')) {
          // a | b | c 或者 string | number | boolean
          const res = dealEnumStr(attrType, `${_.upperFirst(attrName)}Enum`);
          codeItemArr.push(typeCodeMap[res.type] || res.type);
          if (res.define) extraCode.push(res.define);
          if (res.value) extraCode.push(res.value);
        } else if (attrType.endsWith('[]')) {
          // 数组类型
          const t = attrType.split('[]')[0];
          const arrStr = attrType.split(t)[1];
          codeItemArr.push(`${arrStr}${typeCodeMap[t] || t}`);
        } else if (attrType.startsWith('{\n')) {
          // 对象类型
          codeItemArr.push(`map[string]interface{}`);
        } else if (attrType.startsWith('Obj<')) {
          // Obj<T>类型
          const tStr = attrType.split('Obj')[1];
          const t = tStr.slice(1, tStr.length - 1);
          codeItemArr.push(`map[string]${t}`);
        } else if (attrType.startsWith("'")) {
          // 固定字符串
          codeItemArr.push('string');
        } else {
          codeItemArr.push(attrType);
        }
        codeItemArr.push(`\`json:"${attrName}"\``);
        codeArr.push(codeItemArr.join(' '));
      });
      codeArr.push('}\n');
    }
  });
  return [importCommonCode, ...codeArr].concat(extraCode).join('\n');
};

const dealEnumSpec = (spec) => {
  let codeArr = [];
  spec.forEach((specItem) => {
    const { name, props } = specItem;
    if (props.length) {
      codeArr.push(
        `
type ${name} string

const (
  ${props.map((p) => `${_.upperFirst(p[0])} ${name} = "${p[1]}"`)}
)

`,
      );
    }
  });
  return codeArr.join('\n');
};

// 处理type类型
const dealTypeSpec = (spec) => {
  let codeArr = [];
  spec.forEach((specItem) => {
    const { name, value } = specItem;
    if (name !== 'Props') {
      // 过滤MakeProps<Spec>
      if (value.startsWith('{')) {
        // 当Obj处理
        codeArr.push(`type ${name} map[string]interface{}\n`);
      } else if (value.includes('|')) {
        // 当枚举处理
        const res = dealEnumStr(value, name);
        if (res.define) codeArr.push(res.define);
        if (res.value) codeArr.push(res.value);
      }
    }
  });
  return codeArr.join('\n');
};

// AST 查看工具：https://ts-ast-viewer.com/
const dealFile = (content, filePath, isEnd) => {
  const { base } = path.parse(filePath);
  let importCode = '';
  if (base.endsWith('.d.ts')) {
    if (content.includes('CP_COMMON.')) {
      content = content.replace(/CP_COMMON./g, 'common.');
      importCode = 'import "terminus.io/dice/dice-cp/spec/go-spec/common"';
    }
    const fileName = base.split('.spec.d.ts')[0];
    const curFilePath = `${targetDirPath}/${fileName}`;
    if (!fs.existsSync(curFilePath)) {
      console.log(`创建文件夹: ${curFilePath}`);
      fs.mkdirSync(curFilePath, '0777');
    }

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
      if (!fs.existsSync(targetDirPath)) {
        console.log(`目录 ${targetDirPath} 不存在，开始创建`);
        fs.mkdirSync(targetDirPath, '0777');
      }

      content = `package ${fileName.replace(/-/g, '_')}

${importCode}

${dealSpec(infData)}

${dealEnumSpec(enumData)}

${dealTypeSpec(typeData)}

`;
    });

    const savePath = path.resolve(curFilePath, `${fileName}.go`);

    fs.writeFile(savePath, content, 'utf8', (writeErr) => {
      if (writeErr) return console.error(`写入文件：${savePath}错误`, writeErr);
    });
  }

  if (isEnd) {
    console.log('go定义文件生成完毕');
  }
};

walker({
  root: path.resolve(__dirname, '../app/config-page/components'),
  dealFile,
});
