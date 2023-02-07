import path from 'path'
import fs from 'fs'
import rd from 'rd'
import chalk from 'chalk'

import {dirname} from 'path';
import {fileURLToPath} from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const genInterfaces = (moduleName) => {
  if (!moduleName) {
    moduleName = 'interfaces';
  }

  // module path
  const modulePath = path.resolve(`./src/${moduleName}`)

  // output directory path
  const outputDirPath = path.resolve(__dirname, '..', 'typings', moduleName)

  // read each file
  rd.eachSync(modulePath, (f, s) => {
    // skip non-definition files
    if (!f.endsWith('.d.ts')) return;

    // output file path
    const outputFilePath = f.replace(modulePath, outputDirPath)

    // output file directory path
    const outputFileDirPath = path.dirname(outputFilePath)

    // create directory if not exists
    if (!fs.existsSync(outputFileDirPath)) {
      fs.mkdirSync(outputFileDirPath, {
        recursive: true,
      });
    }

    // copy file
    fs.copyFileSync(f, outputFilePath);
    console.log(chalk.yellow(`Generating definition for file: ${chalk.bold(path.basename(f))}`))
  });
};

genInterfaces();
