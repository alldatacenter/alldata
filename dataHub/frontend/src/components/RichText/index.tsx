import React from 'react'
import BraftEditor, { ControlType } from 'braft-editor'
import 'braft-editor/dist/index.css'

import styles from './index.less'

const controls: ControlType[] = [
  'bold',
  'italic',
  'underline',
  'text-color',
  'separator',
  'link',
  'separator',
  'media',
]

interface Interface {
  className?: string
  content?: string | JSON
}

const RichText: React.FunctionComponent<Interface> = ({
  className,
  content = `<p>Hello <b>World!</b></p>`,
}) => {
  const value = BraftEditor.createEditorState(content)

  return (
    <main className={`${styles.main} ${className}`}>
      <BraftEditor
        className={styles.editor}
        controls={controls}
        placeholder="请填写正文"
        // @ts-ignore
        value={value}
      />
    </main>
  )
}

export default RichText
