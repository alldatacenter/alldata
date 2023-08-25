import {EntityType} from '../../../types.generated';

// TODO(Gabe): integrate this w/ the theme
export const REDESIGN_COLORS = {
    GREY: '#e5e5e5',
    BLUE: '#1890FF',
};

export const ANTD_GRAY = {
    1: '#FFFFFF',
    2: '#FAFAFA',
    2.5: '#F8F8F8',
    3: '#F5F5F5',
    4: '#F0F0F0',
    4.5: '#E9E9E9',
    5: '#D9D9D9',
    6: '#BFBFBF',
    7: '#8C8C8C',
    8: '#595959',
    9: '#434343',
};

export const EMPTY_MESSAGES = {
    documentation: {
        title: '还没有文档',
        description: '通过添加文档和指向有用资源的链接来分享您的知识。',
    },
    tags: {
        title: '还没有添加标签',
        description: '标记实体以帮助它们更容易被发现，并指出它们最重要的属性。',
    },
    terms: {
        title: '尚未添加任何条款',
        description: '将术语表术语应用于实体以对其数据进行分类。',
    },
    owners: {
        title: '没有负责人',
        description: '添加所有者可以帮助您跟踪谁对这些数据负责。',
    },
    properties: {
        title: '没有属性',
        description: '如果数据源中存在属性，则这些属性将显示在此处。',
    },
    queries: {
        title: '没有查询',
        description: '创建、查看和共享此数据集的常用查询。',
    },
    domain: {
        title: '未设置域',
        description: '通过将相关实体添加到域，使用根据您的组织结构对其进行分组。',
    },
    contains: {
        title: '不包含任何术语',
        description: '术语可以包含其他术语来表示“是A”样式的关系。',
    },
    inherits: {
        title: '不继承任何术语',
        description: '术语可以从其他术语继承来表示“是A”样式的关系。',
    },
    'contained by': {
        title: '未包含在任何术语中',
        description: '术语可以包含在其他术语中，以表示“具有a”样式的关系。',
    },
    'inherited by': {
        title: '未按任何条件继承',
        description: '术语可以由其他术语继承，以表示“是A”样式的关系。',
    },
};

export const ELASTIC_MAX_COUNT = 10000;

export const getElasticCappedTotalValueText = (count: number) => {
    if (count === ELASTIC_MAX_COUNT) {
        return `${ELASTIC_MAX_COUNT}+`;
    }

    return `${count}`;
};

export const ENTITY_TYPES_WITH_MANUAL_LINEAGE = new Set([
    EntityType.Dashboard,
    EntityType.Chart,
    EntityType.Dataset,
    EntityType.DataJob,
]);

export const GLOSSARY_ENTITY_TYPES = [EntityType.GlossaryTerm, EntityType.GlossaryNode];

export const DEFAULT_SYSTEM_ACTOR_URNS = ['urn:li:corpuser:__datahub_system', 'urn:li:corpuser:unknown'];

export const VIEW_ENTITY_PAGE = 'VIEW_ENTITY_PAGE';
