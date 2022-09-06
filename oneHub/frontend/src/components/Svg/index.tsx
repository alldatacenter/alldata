import React from 'react'
import styles from './index.less'

type TProps = {
  path: string
  className?: string
  fontSize?: number
}

interface Props {
  onClick?: () => void
}

const RkSvg = ({ className, path, fontSize, onClick }: TProps & Props) => {
  const newFontSize = fontSize || 'inherit'

  return (
    <svg
      className={`${styles.svg} ${className}`}
      aria-hidden="true"
      onClick={onClick}
      style={{ fontSize: newFontSize }}
    >
      <use xlinkHref={path} />
    </svg>
  )
}
export default RkSvg
