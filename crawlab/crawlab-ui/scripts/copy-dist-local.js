import fs from 'fs-extra'
import path from 'path'

import {dirname} from 'path';
import {fileURLToPath} from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const files = [
  'dist',
  'public',
  'src',
  'typings',
  'babel.config.js',
  'vue.config.js',
  'README.md',
  'package.json',
]

const sourceDir = path.resolve(path.join(__dirname, '..'))
const targetDir = path.resolve(path.join(__dirname, '..', 'dist_local'))

fs.ensureDirSync(targetDir)
fs.emptyDirSync(targetDir)

files.forEach(fileName => {
  // source file path
  const srcFilePath = path.join(sourceDir, fileName)

  if (fs.statSync(srcFilePath).isDirectory()) {
    // directory
    const tgtDirPath = path.join(targetDir, fileName)
    fs.ensureDirSync(tgtDirPath)
    fs.copySync(srcFilePath, tgtDirPath)
  } else {
    // file
    const tgtFilePath = path.join(targetDir, fileName)
    fs.copySync(srcFilePath, tgtFilePath)
  }
})
