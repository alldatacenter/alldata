import makeInstaller from './make-installer';
import {mapElements} from './utils';
import * as components from '../components';
import * as layouts from '../layouts';
import * as views from '../views';

export const installer = makeInstaller([
  ...mapElements(layouts),
  ...mapElements(components),
  ...mapElements(views),
]);

export {default as createApp} from './create-app';
