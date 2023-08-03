## Design

每个engine代表一个执行引擎，每个执行引擎都有自己的执行模式，但是它们必须包括以下组件
- datavines-engine-api
  - 定义engine中的核心组件接口
- datavines-engine-config
  - 负责将标准的datavinesConfiguration转换成每个引擎自己特有的Configuration
- datavines-engine-core
  - 封装引擎的执行流程
- datavines-engine-executor
  - 定义引擎的任务类型
- datavines-engine-connector
  - 每个引擎都有自己独有的Connector,需要保证不同的引擎能支撑多种Connector
