// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"strings"
	"text/tabwriter"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"

	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	_ "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/influxql"
)

func getClient() *elastic.Client {
	options := []elastic.ClientOptionFunc{
		elastic.SetURL(strings.Split("http://addon-elasticsearch.default.svc.cluster.local:9200", ",")...),
		elastic.SetSniff(false),
	}
	client, err := elastic.NewClient(options...)
	if err != nil {
		panic(fmt.Errorf("fail to create elasticsearch client: %s", err))
	}
	return client
}

func Query(text string, params map[string]interface{}) error {
	fmt.Println(text)
	end := time.Now().Add(-5 * time.Minute)
	start := end.Add(-1 * time.Hour)
	parser := tsql.New(start.UnixNano(), end.UnixNano(), "influxql", text).SetParams(params) //.SetTimeKey("@timestamp").SetOriginalTimeUnit(tsql.Millisecond).SetTargetTimeUnit(tsql.Nanosecond)
	querys, err := parser.ParseQuery()
	if err != nil {
		return err
	}
	client := getClient()
	for _, query := range querys {
		err := doQuery(client, query)
		if err != nil {
			fmt.Println(err)
			return err
		}
	}
	return nil
}

func doQuery(client *elastic.Client, query tsql.Query) error {
	searchSource := query.SearchSource()
	var resp *elastic.SearchResult
	if searchSource != nil {
		source, _ := searchSource.Source()
		fmt.Println(jsonx.MarshalAndIndent(source))
		// return nil
		context, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		r, err := client.Search(getSources(query)...).
			IgnoreUnavailable(true).AllowNoIndices(true).
			SearchSource(searchSource).Do(context)
		if err != nil {
			return err
		}
		resp = r
	}
	rs, err := query.ParseResult(resp)
	if err != nil {
		return err
	}
	w := tabwriter.NewWriter(os.Stdout, 0, 0, 1, ' ', tabwriter.Debug)
	var cols []string
	for _, c := range rs.Columns {
		cols = append(cols, strings.ReplaceAll(c.Name+"("+c.Flag.String()+")", "\t", "    "))
	}
	fmt.Println("rows:", len(rs.Rows), "total:", rs.Total)
	fmt.Fprintln(w, strings.Join(cols, "\t"))
	for _, r := range rs.Rows {
		row := cols[0:0]
		for _, v := range r {
			switch v := v.(type) {
			case string:
				row = append(row, strconv.Quote(v))
				continue
			case fmt.Stringer:
				row = append(row, strconv.Quote(v.String()))
				continue
			case error:
				row = append(row, strconv.Quote(v.Error()))
				continue
			}
			row = append(row, strings.ReplaceAll(fmt.Sprint(v), "\t", "    "))
		}
		fmt.Fprintln(w, strings.Join(row, "\t"))
	}
	w.Flush()
	return nil
}

func getSources(query tsql.Query) []string {
	var sources []string
	for _, source := range query.Sources() {
		if len(source.Name) > 0 {
			db := "*"
			if len(source.Database) > 0 {
				db = strings.ReplaceAll(source.Database, "-", "_")
			}
			sources = append(sources, "spot-"+source.Name+"-"+db+"-*")
		}
	}
	fmt.Println(sources)
	return sources
}

func main() {
	err := test24()
	if err != nil {
		panic(err)
	}
}

func test24() error {
	return Query(`
				SELECT time(),round_float(elapsed_count::field, 2) 
				from application_http 
				GROUP BY time(5m)
				`, map[string]interface{}{})
}

func test23() error {
	return Query(
		`
		SELECT time(),round_float(avg(max::field), 2) 
		FROM jvm_memory 
		WHERE service_id::tag='9_feature/auto-test1_apm-demo-dubbo' 
		GROUP BY time()
		`, map[string]interface{}{})
}

func test22() error {
	return Query(`
		SELECT service_instance_id::tag,min(rx_bytes::field),round_float(diff(rx_bytes::field), 2),round_float(diffps(rx_bytes::field), 2)
		FROM docker_container_summary
		GROUP BY time(10m),service_instance_id::tag LIMIT 3
		`, map[string]interface{}{})
}

func test21() error {
	return Query(`
		SELECT min(rx_bytes::field),round_float(diff(rx_bytes::field), 2),round_float(diffps(rx_bytes::field), 2)
		FROM docker_container_summary
		WHERE service_id::tag='9_feature/auto-test1_apm-demo-dubbo'
		GROUP BY time();
		`, map[string]interface{}{})
}

func test20() error {
	return Query(`
    SELECT 
    format_duration(223299999.999,'',5)
    FROM application_http
	LIMIT 1;
	`, map[string]interface{}{})
}

func test19() error {
	return Query(`
	SELECT 
    neq(1,2),
	lt(1,3),
	lte(1,1),
	gt(1,1),
	gte(1,1),
	andf(gt(1,2),gt(1,1),gt(1,3)),
	orf(gt(1,2),gt(1,1),gt(3,1))
	FROM application_http
	LIMIT 1;
	`,
		map[string]interface{}{})
}

func test18() error {
	return Query(`
	SELECT 
	round_float(percentiles(cpu_usage_active::field,70.777),2)
FROM 
host_summary
GROUP BY 
	time(20m),host_ip::tag
LIMIT 5;
	`,
		map[string]interface{}{})
}

func testFormatStatus() error {
	return Query(`SELECT map(max(health_status::field),0,'健康',1,'警告',2,'部分故障',3,'严重故障'), component_name::tag,message::tag
FROM leaf_component_status 
WHERE component_group::tag='dice_addon'
GROUP BY component_name::tag
ORDER BY max(health_status::field)`, map[string]interface{}{})
}

func test17() error {
	return Query(`
	SELECT 
	quantile(cpu_usage_active::field)
FROM 
host_summary
GROUP BY 
	time(10m), host_ip::tag
LIMIT 10;
	`,
		map[string]interface{}{})
}

func test16() error {
	return Query(`
SELECT
mem_used_percent,round_float(mem_used_percent,2)
FROM host_summary
`,
		map[string]interface{}{})
}

func test15() error {
	return Query(`
SELECT
	value(host_ip::tag)
FROM host_summary
`,
		map[string]interface{}{})
}

func test14() error {
	return Query(`
SELECT
	mem_used, mem_used/scope(max(mem_total), 'global')
FROM host_summary
`,
		map[string]interface{}{})
}

func test13() error {
	return Query(`
SELECT
	row_num()+1, host_ip::tag
FROM host_summary
GROUP BY host_ip::tag
`,
		map[string]interface{}{})
}

func test12() error {
	return Query(`
SELECT
	max(name::field), name()
FROM host_summary;`,
		map[string]interface{}{})
}

func test11() error {
	return Query(`
SELECT
	max(mem_used), scope(max(mem_total), 'global'), max(mem_used)/scope(max(mem_total), 'global')
FROM host_summary
GROUP BY host_ip
`,
		map[string]interface{}{})
}

func test10() error {
	return Query(`
SELECT
	max(mem_used), scope(max(mem_total), 'terms'), max(mem_used)/scope(max(mem_total), 'terms')
FROM host_summary
GROUP BY time(5m), host_ip
`,
		map[string]interface{}{})
}

func test9() error {
	return Query(`SELECT host_ip::tag AS host_ip, max(mem_used) FROM "host_summary" GROUP BY time(1m), host_ip ORDER BY max(mem_used::field) DESC LIMIT 3`, map[string]interface{}{})
}

func test1() error {
	return Query(`SELECT host_ip::tag, timestamp() AS t, max(mem_used)/1024/1024 AS mem, format_bytes(max(mem_used)) 
FROM "host_summary" 
GROUP BY time(10m), host_ip 
LIMIT 2`, map[string]interface{}{})
}

func test6() error {
	return Query(`SELECT host_ip::tag, mem_total AS mem FROM "host_summary" GROUP BY host_ip, range(mem_total, 0, 16401833984, 16401833984, 40401833984) LIMIT 1000`, map[string]interface{}{})
}

func test5() error {
	return Query(`SELECT host_ip::tag, mem_used, if(include(host_ip::tag, '10.167.0.70', '10.167.0.39'), host_ip::tag, 'other') AS host FROM "host_summary" GROUP BY if(include(host_ip::tag, '10.167.0.70', '10.167.0.39'), host_ip::tag, 'other')`, map[string]interface{}{})
}

func test4() error {
	return Query(`SELECT host_ip::tag, mem_used, if(eq(host_ip::tag,'10.167.0.70'),host_ip::tag,'other') AS host FROM "host_summary" GROUP BY if(eq(host_ip,'10.167.0.70'),host_ip,'other')`, map[string]interface{}{})
}

func test3() error {
	return Query(`SELECT host_ip::tag + 'x' AS host_ip, mem_used AS host_ip, (substring(host_ip::tag+'xx', 0, 10)) AS host_ip  FROM "host_summary" GROUP BY (host_ip+'x'),substring(host_ip+'xx', 0, 10) LIMIT 50 OFFSET 50`, map[string]interface{}{})
}

func test2() error {
	return Query(`SELECT host_ip::tag+'xxx', mem_used FROM "host_summary" GROUP BY time(1m),host_ip+'xxx'`, map[string]interface{}{})
}

func scrollTest() error {
	client := getClient()
	id, err := scrollOne(client, "")
	if err != nil {
		return err
	}
	for {
		if len(id) <= 0 {
			return nil
		}
		id, err = scrollOne(client, id)
		if err != nil {
			return err
		}
		time.Sleep(10 * time.Second)
	}
}
func scrollOne(client *elastic.Client, sid string) (string, error) {
	context, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	req := client.Scroll("spot-host_summary-*-1606780800000").IgnoreUnavailable(true).AllowNoIndices(true)
	if len(sid) > 0 {
		req = req.Scroll("1m").ScrollId(sid)
	} else {
		searchSource := elastic.NewSearchSource()
		query := elastic.NewBoolQuery().
			Filter(elastic.NewRangeQuery("timestamp").Gte(1606805580000000000).Lte(1606805820000000000))
		searchSource.Query(query)
		req = req.Scroll("1m").SearchSource(searchSource)
	}
	resp, err := req.Do(context)
	if err != nil {
		return "", nil
	}
	fmt.Println(resp.ScrollId)
	if resp.Hits != nil {
		hits := *resp.Hits
		fmt.Println(hits.TotalHits)
		for _, hit := range hits.Hits {
			fmt.Println(hit.Uid, string(*hit.Source))
		}
	}
	return resp.ScrollId, nil
}
