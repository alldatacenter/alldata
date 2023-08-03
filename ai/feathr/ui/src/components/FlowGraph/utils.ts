import dagre from 'dagre'
import { Node, Edge, ArrowHeadType, Position, Elements } from 'react-flow-renderer'

import { Feature, FeatureLineage, RelationData } from '@/models/model'
import { FeatureType, getFeatureDetailUrl } from '@/utils/utils'

import { NodeData } from './interface'

const featureTypeColors: Record<string, string> = {
  feathr_source_v1: 'hsl(315, 100%, 50%)',
  feathr_anchor_v1: 'hsl(270, 100%, 50%)',
  feathr_anchor_feature_v1: 'hsl(225, 100%, 50%)',
  feathr_derived_feature_v1: 'hsl(135, 100%, 50%)'
}

const DEFAULT_WIDTH = 20
const DEFAULT_HEIGHT = 36

const generateNode = (project: string, data: Feature): Node<NodeData> => {
  return {
    id: data.guid,
    type: 'custom-node',
    style: {
      border: '2px solid featureTypeColors[data.typeName]'
    },
    position: {
      x: 0,
      y: 0
    },
    data: {
      id: data.guid,
      label: data.displayText,
      subtitle: data.typeName,
      featureId: data.guid,
      version: data.version,
      borderColor: featureTypeColors[data.typeName],
      detialUrl: getFeatureDetailUrl(project, data)
    }
  }
}

const generateEdge = (data: RelationData, entityMap: Record<string, Feature>): Edge => {
  let { fromEntityId: from, toEntityId: to } = data
  const { relationshipType } = data
  if (relationshipType === 'Consumes') {
    ;[from, to] = [to, from]
  }
  const sourceNode = entityMap?.[from]
  const targetNode = entityMap?.[to]

  return {
    id: `e-${from}_${to}`,
    source: from,
    target: to,
    arrowHeadType: ArrowHeadType.ArrowClosed,
    data: {
      sourceTypeName: sourceNode?.typeName,
      targetTypeName: targetNode?.typeName
    }
  }
}

export const getLineageNodes = (
  project: string,
  lineageData: FeatureLineage,
  featureType: FeatureType
): Node<NodeData>[] => {
  const { guidEntityMap } = lineageData
  if (!guidEntityMap) {
    return []
  }

  return Object.values(guidEntityMap).reduce((nodes: Node<NodeData>[], item: Feature) => {
    if (
      item.typeName !== 'feathr_workspace_v1' &&
      (featureType === FeatureType.AllNodes ||
        item.typeName === featureType ||
        (featureType === FeatureType.AnchorFeature && item.typeName === FeatureType.Anchor))
    ) {
      nodes.push(generateNode(project, item))
    }
    return nodes
  }, [] as Node<NodeData>[])
}

export const getLineageEdge = (lineageData: FeatureLineage, featureType: FeatureType): Edge[] => {
  if (!lineageData.relations || !lineageData.guidEntityMap) {
    return []
  }

  return lineageData.relations.reduce((edges: Edge[], item) => {
    if (['Consumes', 'Contains', 'Produces'].includes(item.relationshipType)) {
      const edge = generateEdge(item, lineageData.guidEntityMap!)
      if (
        edges.findIndex((item) => item.id === edge.id) === -1 &&
        edge.data.sourceTypeName !== 'feathr_workspace_v1' &&
        (featureType === FeatureType.AllNodes ||
          (featureType === FeatureType.AnchorFeature &&
            edge.data.sourceTypeName === FeatureType.Anchor &&
            edge.data.targetTypeName === FeatureType.AnchorFeature))
      ) {
        edges.push(edge)
      }
    }

    return edges
  }, [] as Edge[])
}

export const getElements = (
  project: string,
  lineageData: FeatureLineage,
  featureType: FeatureType = FeatureType.AllNodes,
  direction = 'LR'
) => {
  const elements: Elements<NodeData | any> = []

  const dagreGraph = new dagre.graphlib.Graph({ compound: true })

  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({ rankdir: direction })

  const isHorizontal = direction === 'LR'

  const nodes = getLineageNodes(project, lineageData, featureType)
  let edges = getLineageEdge(lineageData, featureType)

  const anchorEdges = edges.filter((item) => {
    return (
      item.data.sourceTypeName === FeatureType.Anchor &&
      item.data.targetTypeName === FeatureType.AnchorFeature
    )
  })

  edges = edges.reduce((data: any, item) => {
    const anchorEdge = anchorEdges.find((i: any) => i.target === item.target)
    if (anchorEdge) {
      if (
        !(
          item.data.sourceTypeName === FeatureType.Source &&
          item.data.targetTypeName === FeatureType.AnchorFeature
        )
      ) {
        data.push(item)
      }
    } else {
      data.push(item)
    }
    return data
  }, [])

  nodes.forEach((item) => {
    dagreGraph.setNode(item.id, {
      label: item.data!.label,
      node: item,
      width: item.data!.label.length * 8 + DEFAULT_WIDTH,
      height: item.style?.height || DEFAULT_HEIGHT
    })
    elements.push(item)
  })

  edges?.forEach((item: any) => {
    dagreGraph.setEdge(item.source, item.target)
    elements.push(item)
  })

  dagre.layout(dagreGraph)

  nodes.forEach((item) => {
    const nodeWithPosition = dagreGraph.node(item.id)
    item.targetPosition = isHorizontal ? Position.Left : Position.Top
    item.sourcePosition = isHorizontal ? Position.Right : Position.Bottom
    item.position.x = nodeWithPosition.x
    item.position.y = nodeWithPosition.y - DEFAULT_HEIGHT / 2
  })

  return elements
}
