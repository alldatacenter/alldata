// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import app from 'app/images/resources/app.png';
import mysql from 'app/images/resources/mysql.png';
import nodejs from 'app/images/resources/nodejs.png';
import redis from 'app/images/resources/redis.png';
import memcached from 'app/images/resources/memcached.png';
import mongodb from 'app/images/resources/mongodb.png';
import zookeeper from 'app/images/resources/zookeeper.png';
import elasticsearch from 'app/images/resources/elasticsearch.png';
import custom from 'app/images/resources/custom.png';
import spring from 'app/images/resources/spring.png';
import dubbo from 'app/images/resources/dubbo.png';
import endUser from 'app/images/resources/end-user.png';
import externalService from 'app/images/resources/external-service.png';
import addon from 'app/images/resources/addon.png';
import sidecar from 'app/images/resources/sidecar.png';
import registercenter from 'app/images/resources/registercenter.png';
import noticecenter from 'app/images/resources/noticecenter.png';
import services from 'app/images/resources/services.png';
import instance from 'app/images/resources/instance.png';
import canal from 'app/images/resources/canal.png';
import gateway from 'app/images/resources/gateway.png';
import rocketmq from 'app/images/resources/rocketmq.png';
import kafka from 'app/images/resources/kafka.png';
import minio from 'app/images/resources/minio.png';
import ons from 'app/images/resources/ons.png';
import rabbitmq from 'app/images/resources/rabbitmq.png';
import rds from 'app/images/resources/rds.png';

const color = {
  black: '#2f4554',
  gray: '#6e7074',
  blue: '#2AB6F6',
  purple: '#4D66FD',
  red: '#E8366C',
  orange: '#FFBC07',
  green: '#84BE44',
  green_blue: '#61a0a8',
  coffee: '#bda29a',
  deep_green: '#749f83',
  deep_orange: '#ca8622',
  light_green: '#91c7ae',
  light_blue: '#1296db',
};

const defaultSize = [0.6, 0.6];

const SVGICONS = {
  my: {
    img: mysql,
    color: color.blue,
    size: [1, 0.9],
  },
  mysql: {
    img: mysql,
    color: color.blue,
    size: defaultSize,
  },
  nodejs: {
    img: nodejs,
    color: color.green,
    size: [1, 0.9],
  },
  redis: {
    img: redis,
    color: color.purple,
    size: defaultSize,
  },
  memcached: {
    img: memcached,
    color: color.red,
    size: defaultSize,
  },
  mongodb: {
    img: mongodb,
    color: color.orange,
    size: defaultSize,
  },
  zookeeper: {
    img: zookeeper,
    color: color.deep_orange,
    size: defaultSize,
  },
  elasticsearch: {
    img: elasticsearch,
    color: color.green_blue,
    size: [1, 1],
  },
  custom: {
    img: custom,
    color: color.coffee,
    size: defaultSize,
  },
  // h2: {
  //   img: 'image:///images/resources/h2.png',
  //   color: color.deep_green,
  //   size: [1, 7 / 8],
  // },
  // database: {
  //   img: 'image:///images/resources/database.png',
  //   color: color.green,
  //   size: defaultSize,
  // },
  spring: {
    img: spring,
    color: color.green,
    size: defaultSize,
  },
  // postgresql: {
  //   img: 'image:///images/resources/postgresql.png',
  //   color: color.black,
  //   size: defaultSize,
  // },
  dubbo: {
    img: dubbo,
    color: color.light_blue,
    size: defaultSize,
  },
  'end-user': {
    img: endUser,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  'external-service': {
    img: externalService,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  addon: {
    img: addon,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  sidecar: {
    img: sidecar,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  registercenter: {
    img: registercenter,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  noticecenter: {
    img: noticecenter,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  configcenter: {
    img: services,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  service: {
    img: app,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  app: {
    img: app,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  instance: {
    img: instance,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  canal: {
    img: canal,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  kafka: {
    img: kafka,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  minio: {
    img: minio,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  ons: {
    img: ons,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  rabbitmq: {
    img: rabbitmq,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  rds: {
    img: rds,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  rocketmq: {
    img: rocketmq,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  // noticecenter: {
  //   img: 'image:///images/resources/notice.png',
  //   color: color.light_blue,
  //   size: [0.6, 0.6],
  // },
  gateway: {
    img: gateway,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
  apigateway: {
    img: gateway,
    color: color.light_blue,
    size: [0.6, 0.6],
  },
};

export { SVGICONS };
