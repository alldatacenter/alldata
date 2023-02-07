interface LComponentsMetric {
  filters: {
    metricSource: string;
    timeUnit: string;
    timeRange: string;
  };
  empty: {
    noMetricsSelected: string;
  };
  dashboard: {
    columns: {
      name: string;
      type: string;
      status: string;
      metrics: {
        title: string;
        node: {
          cpu: string;
          memory: string;
          disk: string;
          net: string;
        };
        mongo: {};
      };
    };
  };
  type: {
    node: string;
    mongo: string;
    seaweedfs: string;
  };
  progress: {
    detail: {
      name: string;
      value: string;
    };
  };
  snapshot: {
    node: {
      cpu: string;
      mem: string;
      disk: string;
      net: string;
    };
    mongo: {
      fs: string;
      db: string;
    };
  };
  status: {
    danger: string;
    warning: string;
    healthy: string;
    unknown: string;
  };
  metrics: {
    'performance:node:cpu:percent': string;
    'performance:node:mem:total': string;
    'performance:node:mem:available': string;
    'performance:node:mem:used': string;
    'performance:node:mem:used_percent': string;
    'performance:node:disk:total': string;
    'performance:node:disk:free': string;
    'performance:node:disk:used': string;
    'performance:node:disk:used_percent': string;
    'performance:node:disk:io_read_count': string;
    'performance:node:disk:io_read_bytes': string;
    'performance:node:disk:io_read_time': string;
    'performance:node:disk:io_write_count': string;
    'performance:node:disk:io_write_bytes': string;
    'performance:node:disk:io_write_time': string;
    'performance:node:disk:io_read_count_rate': string;
    'performance:node:disk:io_read_bytes_rate': string;
    'performance:node:disk:io_read_time_rate': string;
    'performance:node:disk:io_write_count_rate': string;
    'performance:node:disk:io_write_bytes_rate': string;
    'performance:node:disk:io_write_time_rate': string;
    'performance:node:net:io_bytes_sent': string;
    'performance:node:net:io_bytes_recv': string;
    'performance:node:net:io_packets_sent': string;
    'performance:node:net:io_packets_recv': string;
    'performance:node:net:io_errin': string;
    'performance:node:net:io_errout': string;
    'performance:node:net:io_dropin': string;
    'performance:node:net:io_dropout': string;
    'performance:node:net:io_fifoin': string;
    'performance:node:net:io_fifoout': string;
    'performance:node:net:io_bytes_sent_rate': string;
    'performance:node:net:io_bytes_recv_rate': string;
    'performance:node:net:io_packets_sent_rate': string;
    'performance:node:net:io_packets_recv_rate': string;
    'performance:node:net:io_errin_rate': string;
    'performance:node:net:io_errout_rate': string;
    'performance:node:net:io_dropin_rate': string;
    'performance:node:net:io_dropout_rate': string;
    'performance:node:net:io_fifoin_rate': string;
    'performance:node:net:io_fifoout_rate': string;
    'performance:mongo:size:fs_total_size': string;
    'performance:mongo:size:fs_used_size': string;
    'performance:mongo:size:total_size': string;
    'performance:mongo:size:total_free_storage_size': string;
    'performance:mongo:size:storage_size': string;
    'performance:mongo:size:data_size': string;
    'performance:mongo:size:free_storage_size': string;
    'performance:mongo:size:index_free_storage_size': string;
    'performance:mongo:size:index_size': string;
    'performance:mongo:size:avg_obj_size': string;
    'performance:mongo:size:fs_used_size_percent': string;
    'performance:mongo:size:total_size_percent': string;
    'performance:mongo:size:total_free_storage_size_percent': string;
    'performance:mongo:size:storage_size_percent': string;
    'performance:mongo:size:data_size_percent': string;
    'performance:mongo:size:free_storage_size_percent': string;
    'performance:mongo:size:index_free_storage_size_percent': string;
    'performance:mongo:size:index_size_percent': string;
    'performance:mongo:count:collections': string;
    'performance:mongo:count:objects': string;
    'performance:mongo:count:views': string;
    'performance:mongo:other:scale_factor': string;
  };
}
