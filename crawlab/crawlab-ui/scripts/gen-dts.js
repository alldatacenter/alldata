import path from 'path'
import fs from 'fs'
import {Project} from 'ts-morph'
import vueCompiler from '@vue/compiler-sfc'
import klawSync from 'klaw-sync'
import chalk from 'chalk'

import {dirname} from 'path';
import {fileURLToPath} from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const TSCONFIG_PATH = path.resolve(__dirname, '../tsconfig.dts.json')
const DEMO_RE = /\/demo\/\w+\.vue$/
const TEST_RE = /__test__|__tests__/
const excludedFiles = [
  'mock',
  'package.json',
  'spec',
  'test',
  'tests',
  'css',
  '.DS_Store',
]
const includedFiles = [
  '.vue',
  '.ts',
]
const exclude = (path) => !excludedFiles.some(f => path.includes(f))
const include = (path) => includedFiles.some(f => path.includes(f))

/**
 * fork = require( https://github.com/egoist/vue-dts-gen/blob/main/src/index.ts
 */
const genVueTypes = async (root, outDir = path.resolve(__dirname, '../typings')) => {
  const options = {
    compilerOptions: {
      allowJs: true,
      declaration: true,
      emitDeclarationOnly: true,
      // noEmitOnError: true,
      noEmitOnError: false,
      outDir,
      paths: {
        '@': [
          path.resolve(__dirname, '../src')
        ],
        '@/*': [
          path.resolve(__dirname, '../src/*')
        ]
      },
      skipLibCheck: true,
    },
    tsConfigFilePath: TSCONFIG_PATH,
    skipAddingFilesFromTsConfig: true,
  }
  const project = new Project(options)

  const sourceFiles = []

  const filePaths = klawSync(root, {
    nodir: true,
  })
    .map(item => item.path)
    .filter(path => !DEMO_RE.test(path))
    .filter(path => !TEST_RE.test(path))
    .filter(exclude)
    .filter(include)

  await Promise.all(
    filePaths.map(async file => {
      const fileName = file.replace(root + '/', '')

      if (file.endsWith('.vue')) {
        // .vue file
        const content = await fs.promises.readFile(file, 'utf-8')
        const sfc = vueCompiler.parse(content)
        const {script, scriptSetup} = sfc.descriptor
        if (script || scriptSetup) {
          let content = ''
          let isTS = false
          if (script && script.content) {
            content += script.content
            if (script.lang === 'ts') isTS = true
          }
          if (scriptSetup) {
            const compiled = vueCompiler.compileScript(sfc.descriptor, {
              id: 'xxx',
            })
            content += compiled.content
            if (scriptSetup.lang === 'ts') isTS = true
          }
          const sourceFile = project.createSourceFile(
            path.relative(process.cwd(), file) + (isTS ? '.ts' : '.js'),
            content,
          )
          sourceFiles.push(sourceFile)
        }
      } else if (file.endsWith('.ts')) {
        // .ts file
        const sourceFile = project.addSourceFileAtPath(file)
        sourceFiles.push(sourceFile)
      }
    }),
  )

  const diagnostics = project.getPreEmitDiagnostics()

  console.log(project.formatDiagnosticsWithColorAndContext(diagnostics))

  await project.emit({
    emitOnlyDtsFiles: true,
  })

  // iterate source files
  for (const sourceFile of sourceFiles) {
    const emitOutput = sourceFile.getEmitOutput()
    const outputFiles = emitOutput.getOutputFiles()
    console.log(chalk.yellow(`Generating definition for file: ${chalk.bold(sourceFile.getBaseName())} (${outputFiles.length})`))

    for (const outputFile of outputFiles) {
      const filepath = outputFile.getFilePath()

      await fs.promises.mkdir(path.dirname(filepath), {
        recursive: true,
      })

      await fs.promises.writeFile(filepath,
        outputFile
          .getText(),
        'utf8')
      console.log(
        chalk.green(
          'Definition for file: ' +
          chalk.bold(
            sourceFile.getBaseName(),
          ) +
          ' generated',
        ),
      )
    }
  }

  // export interfaces in typings/index.d.ts
  const idxFilePath = path.resolve(__dirname, '../typings/index.d.ts')
  if (fs.existsSync(idxFilePath)) {
    let fileContent = fs.readFileSync(idxFilePath)
    const exportInterfacesLine = 'export * from \'./interfaces\';'
    if (!fileContent.includes(exportInterfacesLine)) {
      fileContent = exportInterfacesLine + '\n' + fileContent
    }
    fs.writeFileSync(idxFilePath, fileContent)
  }
}

(async function () {
  await genVueTypes(path.resolve(__dirname, '../src'), path.resolve(__dirname, '../typings'))
})()
