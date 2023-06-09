<template>
  <div class="introduce-wrap">
    <h1 class="title">Introduce</h1>
    <p class="desc">
      Arctic is a LakeHouse management system under open architecture, which on top of data lake open formats provides more optimizations for streaming and upsert scenarios, as well as a set of pluggable self-optimizing mechanisms and management services. Using Arctic could help various data platforms, tools and products build out-of-the-box, streaming and batch unified LakeHouses quickly.
    </p>
    <h2 class="sub-title">What is Arctic</h2>
    <p class="desc">
      Currently, Arctic is a LakeHouse management system on top of iceberg format. Benefit from the thriving ecology of Apache Iceberg, Arctic could be used on kinds of data lakes on premise or clouds with varities of engines. Several concepts should be known before your deeper steps:
    </p>
    <img
      src="@/assets/images/introduce_arctic.png"
      alt=""
      class="arctic-introduce"
    />
    <div class="desc">
      <ol>
        <li>AMS and optimizers - Arctic Management Service provides management features including self-optimizing mechanisms running on optimizers, which could be scaled as demand and scheduled on different platforms.</li>
        <li>Mutiple formats — Arctic use formats analogous to MySQL or ClickHouse using storage engines to meet different scenarios. Two formats were available since Arctic v0.4.
          <div>
            <p>
              <span class="dot"></span>Iceberg format — learn more about iceberg format details and usage with different engines:
              <a
                href="https://iceberg.apache.org/docs/latest/"
                target="_blank"
                rel="noopener noreferrer">
                Iceberg Docs
              </a>
            </p>
            <p><span class="dot"></span>Mixed streaming format - if you are interested in advanced features like auto-bucket, logstore, hive compatible, strict PK constraints etc. learn Arctic
              <a
                href="https://arctic.netease.com/ch/concepts/table-formats/#mixed-iceberg-format"
                target="_blank"
                rel="noopener noreferrer">
                Mixed Iceberg format
              </a> and
              <a
                href="https://arctic.netease.com/ch/concepts/table-formats/#mixed-hive-format"
                target="_blank"
                rel="noopener noreferrer">
                Mixed Hive format
              </a>
            </p>
          </div>
        </li>
      </ol>
    </div>
    <h2 class="sub-title">Arctic features</h2>
    <ol>
      <li>Defining keys - supports defining primary key with strict constraints, and more types of keys in future</li>
      <li>Self-optimizing - user-insensitive asynchronous self-optimization mechanisms could keep lakehouse fresh and healthy</li>
      <li>Management features - dashboard UI to support catalog/table management, SQL terminal and all kinds of metrics
      </li>
      <li>Formats compatible - Hive/Iceberg format compatible means writing and reading through native Hive/Iceberg connector</li>
      <li>Better data pipeline SLA - using LogStore like kafka to accelarate streaming data pipeline to ms/s latency</li>
      <li>Better OLAP performace - provides auto-bucket feature for better compaction and merge-on-read performance</li>
      <li>Concurrent conflicts resovling - Flink or Spark could concurrent write data without worring about conflicts</li>
    </ol>
    <h2 class="sub-title">Engines supported</h2>
    <p class="desc">Arctic support multiple processing engines as below:</p>
    <table class="table">
      <thead class="ant-table-thead">
        <tr>
          <th>Processing Engine</th>
          <th>Version</th>
        </tr>
      </thead>
      <tbody class="ant-table-tbody">
        <tr>
          <td>
            <span class="td-val">Flink</span>
          </td>
          <td>
            <span class="td-val">1.12.x,1.14.x and 1.15.x</span>
          </td>
        </tr>
        <tr>
          <td>
            <span class="td-val">Spark</span>
          </td>
          <td>
            <span class="td-val">2.3, 3.1</span>
          </td>
        </tr>
        <tr>
          <td>
            <span class="td-val">Trino</span>
          </td>
          <td>
            <span class="td-val">380</span>
          </td>
        </tr>
      </tbody>
    </table>
    <h2 class="sub-title">Quickstart</h2>
    <p class="desc">
      Visit
      <a
        href="https://arctic.netease.com/ch/quickstart/quick-demo/"
        target="_blank"
        rel="noopener noreferrer">
      https://arctic.netease.com/ch/quickstart/quick-demo/
      </a>
      to quickly explore what arctic can do.
    </p>
    <h2 class="sub-title">Join Community</h2>
    <p class="desc">If you are interested in Lakehouse, Data Lake Format, welcome to join our community, we welcome any organizations, teams and individuals to grow together, and sincerely hope to help users better use Data Lake Format through open source.</p>
    <p class="desc">Join the Arctic WeChat Group: Add " <code>kllnn999</code> " as a friend on WeChat and specify "Arctic lover".</p>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted } from 'vue'

export default defineComponent({
  name: 'Introduce',
  components: {},
  setup() {
    onMounted(() => {})
    return {}
  }
})
</script>

<style lang="less" scoped>
.introduce-wrap {
  font-size: 16px;
  box-sizing: border-box;
  padding: 24px 48px;
  max-width: 1200px;
  .title {
    font-size: 30px;
    font-weight: bold;
    padding-bottom: 7px;
    margin-bottom: 16px;
    border-bottom: 1px solid #e5e5e5;
  }
  .arctic-introduce {
    width: 80%;
    margin: 0 auto 16px;
    display: block;
  }
  .desc {
    margin-bottom: 16px;
  }
  .dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 6px;
    border: 1px solid #000;
    margin-right: 12px;
    vertical-align: middle;
  }
  .sub-title {
    margin: 24px 0 16px;
    font-size: 24px;
    font-weight: bold;
    border-bottom: 1px solid #e5e5e5;
    padding-bottom: 7px;
  }
  code {
    padding: 2px 4px;
    background-color: rgba(175, 184, 193, 0.2);
    border-radius: 4px;
    display: inline-block;
    line-height: 14px;
    font-size: 14px;
  }
  ol li {
    line-height: 24px;
    margin-top: 4px;
    padding-left: 32px;
    position: relative;
    &::after {
      content: "";
      position: absolute;
      width: 6px;
      height: 6px;
      border-radius: 100%;
      background-color: #000;
      left: 14px;
      top: 10px;
    }
  }
}
</style>
