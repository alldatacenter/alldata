import { View } from '../../types/View';
import { APP_VERSION_BETA_4 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

export const beta4 = view => {
  if (!view) {
    return view;
  }

  try {
    if (!view.type) {
      view.type = 'SQL';
    }

    return view;
  } catch (error) {
    console.error('Migration view Errors | beta.4 | ', error);
    return view;
  }
};

const migrationViewConfig = (view: View): View => {
  if (!view) {
    return view;
  }

  const event2 = new MigrationEvent(APP_VERSION_BETA_4, beta4);
  const dispatcher = new MigrationEventDispatcher(event2);
  const result = dispatcher.process(view);

  return result;
};

export default migrationViewConfig;
