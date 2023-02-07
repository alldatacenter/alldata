import path from 'path'
import fs from 'fs'
import klawSync from 'klaw-sync'
import base64Img from 'base64-img'

import {dirname} from 'path';
import {fileURLToPath} from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

const includedFiles = [
  '.svg',
]
const include = (path) => includedFiles.some(f => path.includes(f))

const genSvg = async () => {
  const root = path.resolve(__dirname, '../src/assets')
  const filePaths = klawSync(root, {
    nodir: true,
  })
    .map(item => item.path)
    .filter(include)

  await Promise.all(
    filePaths.map(async file => {
      if (file.endsWith('.svg')) {
        const data = base64Img.base64Sync(file)

        const fileName = path.basename(file).replace(/\.svg$/, '.js')
        const dirPath = path.resolve(__dirname, `../src/assets/js/svg`)
        const filePath = `${dirPath}/${fileName}`

        if (!fs.existsSync(dirPath)) fs.mkdirSync(dirPath)

        let content = `module.exports = \`${data}\``
        await fs.promises.writeFile(filePath, content, 'utf-8')
      }
    })
  )
}

(async function () {
  await genSvg()
})()
