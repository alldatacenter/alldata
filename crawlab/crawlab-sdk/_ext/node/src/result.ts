import {ResultItem, ResultService} from '../typings';
import {credentials, Metadata} from '@grpc/grpc-js';
import {getTaskId} from './config';
import {getAuthToken} from './authToken';
import {ClientWritableStream} from '@grpc/grpc-js/src/call';

const {TaskServiceClient} = require('./grpc/services/task_service_grpc_pb.js');
const {StreamMessage} = require('./grpc/entity/stream_message_pb.js');
const {StreamMessageCode} = require('./grpc/entity/stream_message_code_pb.js');

export class ResultServiceClass implements ResultService {
  private readonly client: typeof TaskServiceClient;
  private sub: ClientWritableStream<typeof StreamMessage> | undefined;
  private readonly host: string = 'localhost';
  private readonly port: string = '9666';

  constructor() {
    if (process.env.CRAWLAB_GRPC_ADDRESS) {
      const parts = process.env.CRAWLAB_GRPC_ADDRESS.split(':');
      this.host = parts[0];
      this.port = parts[1];
    } else {
      if (process.env.CRAWLAB_GRPC_HOST) {
        this.host = process.env.CRAWLAB_GRPC_HOST;
      }
      if (process.env.CRAWLAB_GRPC_PORT) {
        this.port = process.env.CRAWLAB_GRPC_PORT;
      }
    }
    this.client = new TaskServiceClient(
      this.getAddress(),
      credentials.createInsecure(),
    );
    this.subscribe();
  }

  private subscribe() {
    const metadata = new Metadata();
    metadata.add('authorization', getAuthToken());
    const options = {};
    const callback = (value: any) => {
    };
    this.sub = this.client.subscribe(metadata, options, callback);
  }

  private unsubscribe() {
    this.sub?.end();
    this.sub = undefined;
  }

  private getAddress(): string {
    return `${this.host}:${this.port}`;
  }

  async save(items: ResultItem[]) {
    let _items: any[] = [];
    for (let i = 0; i < items.length; i++) {
      _items.push(items[i]);
      if (i > 0 && i % 50 === 0) {
        await this._save(_items);
        _items = [];
      }
    }
    if (_items.length > 0) {
      await this._save(_items);
    }
  }

  async saveItem(...items: ResultItem[]) {
    return this.save(items);
  }

  async saveItems(items: ResultItem[]) {
    return this.save(items);
  }

  private async _save(items: Record<string, any>[]) {
    const tid = getTaskId();
    if (tid === null) {
      return;
    }

    if (!this.sub) {
      this.subscribe();
    }

    const records = items.map(item => {
      item['_tid'] = tid;
      return item;
    });

    const code = StreamMessageCode.INSERT_DATA;
    const data = Buffer.from(JSON.stringify({
      task_id: tid,
      data: records,
    }));

    const msg = new StreamMessage();
    msg.setCode(code);
    msg.setData(data);

    try {
      this.sub?.write(msg);
    } catch (e) {
      console.error(e);
      throw e;
    } finally {
      this.unsubscribe();
    }
  }
}

export const saveItem = (...items: ResultItem[]) => {
  return getResultService().saveItem(...items);
};

export const saveItems = (items: ResultItem[]) => {
  return getResultService().saveItems(items);
};

let RS: ResultService;

export const getResultService = (): ResultService => {
  if (RS) {
    return RS;
  }
  RS = new ResultServiceClass();
  return RS;
};
