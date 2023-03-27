import { DebeziumConfigurator } from './DebeziumConfigurator';

const config = {
  steps: ['Connection', 'Filter definition', 'Data & runtime'],
  Configurator: DebeziumConfigurator,
};

export default config;
