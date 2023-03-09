import { useEffect } from 'react'

import nprogress from 'nprogress'
import 'nprogress/nprogress.css'

nprogress.configure({
  easing: 'ease',
  speed: 500,
  showSpinner: false,
  trickleSpeed: 200,
  minimum: 0.3
})

const Loading = () => {
  useEffect(() => {
    nprogress.start()

    return () => {
      nprogress.done()
    }
  }, [])

  return null
}

export default Loading
