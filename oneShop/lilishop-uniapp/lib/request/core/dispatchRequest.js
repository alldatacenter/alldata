import adapter from '../adapters/index'


export default (config) => {
  config.header = config.header || {}
  return adapter(config)
}
