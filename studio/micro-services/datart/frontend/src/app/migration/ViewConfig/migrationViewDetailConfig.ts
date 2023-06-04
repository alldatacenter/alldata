import { APP_VERSION_BETA_2 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

export const beta2 = viewConfig => {
  if (!viewConfig) {
    return viewConfig;
  }

  try {
    if (viewConfig) {
      viewConfig.expensiveQuery = false;
    }

    return viewConfig;
  } catch (error) {
    console.error('Migration ViewConfig Errors | beta.2 | ', error);
    return viewConfig;
  }
};

export const migrateViewConfig = (viewConfig: string) => {
  if (!viewConfig?.trim().length) {
    return viewConfig;
  }

  const config = JSON.parse(viewConfig);
  const event2 = new MigrationEvent(APP_VERSION_BETA_2, beta2);
  const dispatcher = new MigrationEventDispatcher(event2);
  const result = dispatcher.process(config);

  return JSON.stringify(result);
};
