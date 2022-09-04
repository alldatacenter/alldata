import { Spin } from "antd"
import { Suspense } from "react"
// interface Props {
//   lazyChildren:React.LazyExoticComponent<React.FC<{}>>
// }

const SuspenseRoute:React.FC<{lazyChildren:React.LazyExoticComponent<React.FC<{title:string}>>,title:string}> = props => {
  return (
    <Suspense fallback={<Spin tip='loading...'/>}>
      <props.lazyChildren title={props.title}/>
    </Suspense>
  )
}

export default SuspenseRoute