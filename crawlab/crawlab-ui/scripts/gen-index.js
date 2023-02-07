import path from 'path'
import fs from 'fs'
import rd from 'rd'

const INDEX_COMP_NAME = 'index'

const IGNORE_COMPONENTS_SUB_MODULES = [
  // 'node',
  // 'project',
  // 'spider',
  // 'task',
  // 'tag',
  // 'dataCollection',
  // 'schedule',
  // 'user',
  // 'token',
  // 'plugin',
  // 'git',
  // 'file',
]

const EXPORT_MODULES = [
  'components',
  // 'constants',
  'directives',
  'layouts',
  // 'services',
  // 'store',
  // 'utils',
  'views',
]

const COMPONENT_PREFIX = 'Cl'

const genIndex = (moduleName) => {
  // import/export lines
  const importLines = []
  const exportLines = []

  // module path
  const modulePath = path.resolve(`./src/${moduleName}`)

  // read each file
  rd.eachSync(modulePath, (f, s) => {
    // relative path
    const relPath = `.${f.replace(modulePath, '')}`

    // file name
    const fileName = path.basename(f)

    // vue
    if (f.endsWith('.vue')) {
      // component name
      const compName = fileName.replace('.vue', '')

      // skip ignored components sub-modules
      if (moduleName === 'components') {
        const subModuleName = relPath.split('/')[1]
        if (IGNORE_COMPONENTS_SUB_MODULES.includes(subModuleName)) return
      }

      // import line
      const importLine = `import ${compName} from '${relPath}';`

      // export line
      const exportLine = `${compName} as ${COMPONENT_PREFIX}${compName},`

      // add to importLines/exportLines
      importLines.push(importLine)
      exportLines.push(exportLine)
    } else if (f.endsWith('.ts')) {
      // skip components, layouts
      if ([
        'components',
        'layouts',
        'views',
      ].includes(moduleName)) return;

      // component name
      let compName = fileName.replace('.ts', '')

      // skip index
      if (compName === INDEX_COMP_NAME) return

      // add suffix to component name
      if (compName === 'export') {
        compName += '_';
      }

      // relative component name
      const relCompName = relPath.replace('.ts', '')

      // import line
      const importLine = `import ${compName} from '${relCompName}';`

      // export line
      const exportLine = `${compName} as ${compName},`

      // add to importLines/exportLines
      importLines.push(importLine)
      exportLines.push(exportLine)
    }
  })

  // write to index.ts
  let content = ''
  importLines.forEach(l => content += l + '\n')
  content += `
export {
${exportLines.map(l => '  ' + l).join('\n')}
};
`
  fs.writeFileSync(`${modulePath}/index.ts`, content)
}

const genRootIndex = () => {
  const exportLines = EXPORT_MODULES.map(m => `export * from './${m}';`)
  const content = `${exportLines.join('\n')}
export * from './router';
export * from './store';
export * from './i18n';
export * from './package';
export * from './utils';
export * from './constants';
export * from './layouts/content';
export * from './components/form';
export {default as useSpider} from './components/spider/spider';
export {
  ClSpiderDetail,
} from './views';
export {installer as default} from './package';
export {default as useRequest} from './services/request';
`
  fs.writeFileSync('./src/index.ts', content)
}

// gen module index.ts
EXPORT_MODULES.forEach(m => genIndex(m))

// gen root index.ts
genRootIndex()
