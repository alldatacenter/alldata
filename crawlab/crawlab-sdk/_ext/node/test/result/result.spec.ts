import {describe, expect, test} from '@jest/globals';
import {getResultService} from '../../src';
import {ResultItem} from '../../typings';

describe('test-results-service', () => {
  /*
  os.environ['CRAWLAB_TASK_ID'] = ''.join(['0'] * 24)
  os.environ['CRAWLAB_GRPC_ADDRESS'] = 'localhost:9666'
  os.environ['CRAWLAB_GRPC_AUTH_KEY'] = 'Crawlab2021!'
   */

  process.env.CRAWLAB_TASK_ID = '000000000000000000000001';
  process.env.CRAWLAB_GRPC_ADDRESS = 'localhost:9666';
  process.env.CRAWLAB_GRPC_AUTH_KEY = 'Crawlab2021!';

  const basicItem: ResultItem = {hello: 'world'};

  test('should save item', async () => {
    const rs = getResultService();
    await rs.saveItem(basicItem);
  });

  test('should save items', async () => {
    const rs = getResultService();
    await rs.saveItems([basicItem] as ResultItem[]);
  });

  test('should save large-sized items', async () => {
    const rs = getResultService();
    for (let i = 0; i < 1000; i++) {
      await rs.saveItem(basicItem);
    }
  });

  test('should import dist', async () => {
    const {saveItem} = require('../../dist/index.js');
    await saveItem(basicItem);
  });
});
