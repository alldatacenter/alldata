import React, {
  MouseEvent as ReactMouseEvent,
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState
} from 'react'

import { LoadingOutlined } from '@ant-design/icons'
import { Spin } from 'antd'
import cs from 'classnames'
import ReactFlow, {
  ConnectionLineType,
  Controls,
  Edge,
  Node,
  Elements,
  getIncomers,
  getOutgoers,
  ReactFlowProvider,
  isNode,
  OnLoadParams
} from 'react-flow-renderer'
import { useSearchParams } from 'react-router-dom'

import { FeatureLineage } from '@/models/model'
import { isFeature, FeatureType } from '@/utils/utils'

import { NodeData, FlowGraphProps } from './interface'
import LineageNode from './LineageNode'
import { getElements } from './utils'

import styles from './index.module.less'

const FlowGraphNodeTypes = {
  'custom-node': LineageNode
}

const defaultProps: FlowGraphProps = {
  project: '',
  snapGrid: [15, 15],
  featureType: FeatureType.AllNodes
}

const FlowGraph = (props: FlowGraphProps, ref: any) => {
  const {
    className,
    style,
    data,
    loading,
    height,
    minHeight,
    project,
    nodeId,
    featureType,
    snapGrid
  } = {
    ...defaultProps,
    ...props
  }
  const [, setURLSearchParams] = useSearchParams()
  const flowRef = useRef<OnLoadParams>()
  const hasReadRef = useRef<boolean>(false)
  const elementRef = useRef<Elements<NodeData | any>>()
  const hasHighlight = useRef<boolean>(false)
  const [elements, setElements] = useState<Elements<NodeData | any>>([])

  // Reset all node highlight status
  const resetHighlight = useCallback(() => {
    if (elementRef.current && elementRef.current.length > 0 && hasHighlight.current) {
      hasHighlight.current = false
      setElements((state) => {
        return state.map((element) => {
          if (isNode(element)) {
            element.style = {
              ...element.style,
              opacity: 1
            }
            element.data!.active = false
          } else {
            element.animated = false
          }
          return element
        })
      })
    }
  }, [setElements])

  // Highlight path of selected node, including all linked up and down stream nodes
  const highlightPath = useCallback(
    (node: Node<NodeData>) => {
      if (elementRef.current && elementRef.current.length > 0) {
        hasHighlight.current = true
        setElements((elements) => {
          const incomerIds = new Set(getIncomers(node, elements).map((item) => item.id))
          const outgoerIds = new Set(getOutgoers(node, elements).map((item) => item.id))

          return elements.map((element) => {
            if (isNode(element)) {
              const highlight =
                element.id === node.id || incomerIds.has(element.id) || outgoerIds.has(element.id)
              element.style = {
                ...element.style,
                opacity: highlight ? 1 : 0.25
              }
              element.data = {
                ...element.data,
                active: element.id === node.id && isFeature(element.data!.subtitle)
              }
            } else {
              const highlight = element.source === node.id || element.target === node.id
              const animated =
                incomerIds.has(element.source) &&
                (incomerIds.has(element.target) || node.id === element.target)

              element.animated = highlight || animated
            }
            return element
          })
        })
      }
    },
    [setElements]
  )

  // Fired when panel is clicked, reset all highlighted path, and remove the nodeId query string in url path.
  const onPaneClick = useCallback(() => {
    resetHighlight()
    setURLSearchParams({})
  }, [resetHighlight, setURLSearchParams])

  const onElementClick = useCallback(
    (e: ReactMouseEvent, element: Node<NodeData> | Edge) => {
      e.stopPropagation()
      if (isNode(element)) {
        setURLSearchParams({
          nodeId: element.id,
          featureType: element.data!.subtitle
        })
        setTimeout(() => {
          highlightPath(element)
        }, 0)
      }
    },
    [highlightPath, setURLSearchParams]
  )

  const handleInit = useCallback(
    (project: string, data: FeatureLineage, featureType?: FeatureType, nodeId?: string) => {
      const elements = (elementRef.current = getElements(project, data, featureType))
      setElements(elements)
      if (nodeId) {
        const node = elements?.find((item) => item.id === nodeId) as Node<NodeData>
        if (node) {
          highlightPath(node)
        }
      }
    },
    [setElements, highlightPath]
  )

  // Fit the graph to the center of layout view when graph is initialized
  const onLoad = (reactFlowInstance: OnLoadParams) => {
    flowRef.current = reactFlowInstance
    flowRef.current?.fitView()
  }

  useEffect(() => {
    if (data) {
      const type = hasHighlight.current ? FeatureType.AllNodes : featureType
      handleInit(project!, data, type, nodeId)
    }
  }, [data, project, nodeId, featureType, handleInit])

  useEffect(() => {
    if (elements.length > 0 && !hasReadRef.current) {
      hasReadRef.current = true
      setTimeout(() => {
        flowRef.current?.fitView()
      }, 100)
    }
  }, [elements])

  return (
    <Spin spinning={loading} indicator={<LoadingOutlined spin style={{ fontSize: 48 }} />}>
      <ReactFlowProvider>
        <ReactFlow
          snapToGrid
          className={cs(styles.flowGraph, className)}
          style={{ ...style, height, minHeight }}
          elements={elements}
          snapGrid={snapGrid}
          zoomOnScroll={false}
          preventScrolling={false}
          nodeTypes={FlowGraphNodeTypes}
          connectionLineType={ConnectionLineType.SmoothStep}
          onLoad={onLoad}
          onPaneClick={onPaneClick}
          onElementClick={onElementClick}
        >
          <Controls />
        </ReactFlow>
      </ReactFlowProvider>
    </Spin>
  )
}

const FlowGraphComponent = forwardRef<unknown, FlowGraphProps>(FlowGraph)

FlowGraphComponent.displayName = 'FlowGraph'

export default FlowGraphComponent
