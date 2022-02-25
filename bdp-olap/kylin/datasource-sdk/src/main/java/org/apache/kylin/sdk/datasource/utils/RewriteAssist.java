public class RewriteAssist {
    private static final Logger logger = LoggerFactory.getLogger(RewriteAssist.class);

    private static final String partitionFormat = "yyyyMM";

    private static final String columnFormat = "%s.%s.%s";

    private static final Set<String> conditions1 = new HashSet<String>() {{
        add("=");
    }};

    private static final Set<String> conditions2 = new HashSet<String>() {{
        add(">");
        add("<");
        add("<=");
        add(">=");
    }};

    private static final String[] dataFormat = {
            "yyyy-MM-dd hh:mm:ss",
            "yyyy/MM/dd hh:mm:ss",
            "yyyy-MM-dd hh:mm",
            "yyyy/MM/dd hh:mm",
            "yyyyMMddhhmmss",
            "yyyyMMddhhmm",
            "yyyy-MM-dd hh",
            "yyyy/MM/dd hh",
            "yyyyMMdd HH",
            "yyyyMMddhh",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyyMMdd",
            "yyyyMM",
            "yyyy-MM",
            "yyyy"
    };

    private SqlDialect.DatabaseProduct database;

    private PushDownModelDesc desc;

    private CalciteSqlParser sqlParser;

    public RewriteAssist(SqlDialect.DatabaseProduct database, PushDownModelDesc desc) {
        this.database = database;
        this.desc = desc;
        this.sqlParser = CalciteSqlParser.create();
    }

    /**
     * 对SQL进行重写
     *
     * @param sql
     * @return
     * @throws SqlParseException
     */
    public String rewrite(String sql) throws SqlParseException {
        SqlNode sqlNode = sqlParser.parseQuery(sql);

        // 从 sql 中提取出 select 字段
        SqlSelect select = (SqlSelect) ((SqlOrderBy) sqlNode).query;

        /**
         * SELECT * FROM table 重写为 SELECT SANGFOR_ALL_COL FROM table
         * 但是因为 SELECT *,col FROM table 这种结果返回的列名处理起来有难度，所以暂时不考虑处理，故存在 select.getSelectList().size() == 1 这个逻辑
         */
        for (int i = 0; i < select.getSelectList().size() && select.getSelectList().size() == 1; i++) {
            SqlNode node = select.getSelectList().get(i);
            if (SqlKind.IDENTIFIER.equals(node.getKind())) {
                SqlIdentifier id = ((SqlIdentifier) node);
                if (id.names.size() == 1 && StringUtils.isEmpty(id.names.get(0))) {
                    id = new SqlIdentifier(ImmutableList.of(desc.getRewriteDesc().getAllCol()), SqlParserPos.ZERO);
                    select.getSelectList().set(i, id);
                    desc.setRewriteAsterisk(true);
                }
            }
        }

        if (select.getWhere() != null) {
            SqlBasicCall where = (SqlBasicCall) select.getWhere();
            Map<String, List<SqlNode>> mapToSqlNode = Maps.newHashMap();
            Map<String, PushDownModelDesc.ColumnDesc> mapToColumnDesc = Maps.newHashMap();
            // 使用引用传值的方式对 mapToSqlNode 和 mapToColumnDesc 赋值
            String col = parseWhere(where, mapToSqlNode, mapToColumnDesc);
            if (StringUtils.isNotEmpty(col)) {
                // 对 where 中的条件进行重写
                where = rewriteWhere(where, col, mapToSqlNode.get(col), mapToColumnDesc.get(col));
                // 将重写后的 where 重写设置进 select 中
                select.setWhere(where);
            }
        }

        sql = sqlNode.toSqlString(database.getDialect(), false).getSql();
        return sql;
    }

    /**
     * 注意，这里使用引用传值的方式对两个map进行赋值，使用时务必需要小心
     *
     * @param where
     * @param mapToSqlNode    <db.tab.col, [col=1, tab.col=2, db.tab.col=3]>
     * @param mapToColumnDesc 和mapToColumnDesc 中的元素个数完全相同，key值完全相等
     * @return db.tab.col 需要追加的条件的字段名
     */
    private String parseWhere(SqlBasicCall where,
                              Map<String, List<SqlNode>> mapToSqlNode,
                              Map<String, PushDownModelDesc.ColumnDesc> mapToColumnDesc
    ) {
        /*
         * col 仅仅存储主索引字段
         * col[0] 存储时间类型的主索引字段
         * col[1] 存储非时间类型的主索引字段
         */
        String[] col = {"", ""};

        for (SqlNode node : parseBasicCall(where)) {
            node.accept(new SqlBasicVisitor<SqlNode>() {
                @Override
                public SqlNode visit(SqlCall call) {
                    if (call.getOperandList().get(0).getKind().equals(SqlKind.IDENTIFIER)) {
                        SqlIdentifier id = (SqlIdentifier) call.getOperandList().get(0);
                        String operate = call.getOperator().getName();
                        // 字段是主索引字段，且条件是 >,<,<=,>=,=,!= 中的一种
                        if (conditions1.contains(operate) || conditions2.contains(operate)) {
                            // map 中最多只会有一个元素，因为只传了一个 id，而 map 的 key 值本身是对 id 的 db.tab.col 补全
                            Map<String, PushDownModelDesc.ColumnDesc> map = mapIdToFullName(id, desc.getPartitionFromFlat());
                            if (!map.isEmpty()) {
                                Map.Entry<String, PushDownModelDesc.ColumnDesc> next = map.entrySet().iterator().next();
                                if (!mapToSqlNode.containsKey(next.getKey())) {
                                    mapToColumnDesc.put(next.getKey(), next.getValue());
                                    mapToSqlNode.put(next.getKey(), new ArrayList<>());

                                    if (PushDownModelDesc.ColumnDesc.IndexType.PRI.equals(next.getValue().getIndex())) {
                                        if (next.getValue().getType().startsWith("DATE")) {
                                            col[0] = next.getKey();
                                        } else {
                                            if (StringUtils.isEmpty(col[1])) {
                                                col[1] = next.getKey();
                                            } else {
                                                // 这里会存在潜在bug，如果传过来的list顺序和UI上的主索引字段顺序对不上，那么这里会取错值
                                                PushDownModelDesc.TableDesc tmpTable = desc.getPartitionFromFlat().get(0);
                                                PushDownModelDesc.ColumnDesc tmpColumn = tmpTable.getColumns().get(0);
                                                String columnName = String.format(columnFormat, tmpTable.getDb(), tmpTable.getName(), tmpColumn.getName());
                                                col[1] = mapToColumnDesc.containsKey(columnName) ? columnName : col[1];
                                            }
                                        }
                                    }
                                }
                                mapToSqlNode.get(next.getKey()).add(node);
                            }
                        }
                    }

                    return call;
                }
            });
        }

        return StringUtils.isNotEmpty(col[0]) ? col[0] : col[1];
    }


    /**
     * @param where      where条件
     * @param col        db.tab.col
     * @param nodes      [col=1, tab.col=2, db.tab.col=3]
     * @param columnDesc <name, type, PRI/AUX/NONE>
     */
    private SqlBasicCall rewriteWhere(SqlBasicCall where,
                                      String col,
                                      List<SqlNode> nodes,
                                      PushDownModelDesc.ColumnDesc columnDesc
    ) throws SqlParseException {
        if (nodes.size() != 1) {
            return where;
        }

        // 因为上述 entry.getValue().size() 的判断，决定了 entry.getValue() 可以取到唯一的一个元素
        SqlBasicCall call = (SqlBasicCall) nodes.get(0);

        String value = call.operand(1).accept(new SqlBasicVisitor<String>() {
            @Override
            public String visit(SqlLiteral literal) {
                return literal.toValue();
            }
        });

        // 该主索引字段是时间类型
        String[] cols = col.split("\\.");
        SqlNode condition = null;
        if (columnDesc.getType().toUpperCase().startsWith("DATE")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(partitionFormat);
            String format = "\"%s\".\"%s\".\"%s\" = '%s'";
            try {
                String tmp = dateFormat.format(DateUtils.parseDateStrictly(value, Locale.getDefault(), dataFormat));
                condition = sqlParser.parseExpression(String.format(format, cols[0], cols[1], desc.getRewriteDesc().gettPartition(), tmp));
            } catch (ParseException e) {
                // 按照时间戳进行解析
                if (StringUtils.isNumeric(value)) {
                    String tmp = StringUtils.rightPad(value, 13, "0");
                    tmp = DateFormatUtils.format(Long.valueOf(tmp), partitionFormat);
                    condition = sqlParser.parseExpression(String.format(format, cols[0], cols[1], desc.getRewriteDesc().gettPartition(), tmp));
                } else {
                    logger.warn("{} 不能被解析", call.toSqlString(database.getDialect()));
                }
            }
        } else {
            if (conditions1.contains(call.getOperator().getName())) {
                String format = "\"%s\".\"%s\".\"%s\" = '%s'";
                String cond = "";
                switch (database) {
                    case CLICKHOUSE:
                        cond = String.format(format, cols[0], cols[1],
                                desc.getRewriteDesc().gethPartition(),
                                bucket(hash(value), desc.getRewriteDesc().gethCkPartitionCnt())
                        );
                        break;
                    case PRESTO:
                        cond = String.format(format, cols[0], cols[1],
                                desc.getRewriteDesc().gethPartition(),
                                bucket(hash(value), desc.getRewriteDesc().gethFlatPartitionCnt())
                        );
                        break;
                    default:
                        cond = String.format(format, cols[0], cols[1], desc.getRewriteDesc().gethPartition(), hash(value));
                        break;

                }
                condition = sqlParser.parseExpression(cond);
            }
        }

        if (condition != null) {
            where = (SqlBasicCall) SqlStdOperatorTable.AND.createCall(SqlParserPos.ZERO, where, condition);
        }

        return where;
    }

    private List<SqlNode> parseBasicCall(SqlBasicCall call) {
        List<SqlNode> list = new ArrayList<>(16);

        Stack<SqlNode> stack = new Stack<>();
        SqlNode node = call;

        while (true) {
            if (node instanceof SqlBasicCall) {
                stack.push(node);
                node = ((SqlBasicCall) node).operand(0);
            } else {
                SqlNode tmp = stack.pop();

                if (tmp.getKind().equals(SqlKind.AND) || tmp.getKind().equals(SqlKind.OR)) {
                    node = ((SqlBasicCall) tmp).operand(1);
                } else {
                    list.add(tmp);
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return list;
    }

    /**
     * id 字段是索引表中的一个字段，然后返回 id 的全名称 db.tab.col
     *
     * @param id
     * @param indexTables
     * @return
     */
    private Map<String, PushDownModelDesc.ColumnDesc> mapIdToFullName(SqlIdentifier id, List<PushDownModelDesc.TableDesc> indexTables) {
        Map<String, PushDownModelDesc.ColumnDesc> map = Maps.newHashMap();
        for (PushDownModelDesc.TableDesc indexTable : indexTables) {
            boolean cond = false;
            for (PushDownModelDesc.ColumnDesc column : indexTable.getColumns()) {
                if (id.isSimple()) {
                    cond = id.getSimple().equals(column.getName());
                } else if (id.names.size() == 2) {
                    cond = id.names.get(0).equals(indexTable.getName()) && id.names.get(1).equals(column.getName());
                } else if (id.names.size() == 3) {
                    cond = id.names.get(0).equals(indexTable.getDb()) && id.names.get(1).equals(indexTable.getName()) && id.names.get(2).equals(column.getName());
                }

                if (cond) {
                    PushDownModelDesc.ColumnDesc columnDesc = column;
                    String columnName = String.format(columnFormat, indexTable.getDb(), indexTable.getName(), column.getName());
                    map.put(columnName, columnDesc);
                    return map;
                }
            }
        }

        return map;
    }

    /**
     * 对原始字符串按照 num 分桶，大概类似于 str % num
     *
     * @param num
     * @param bucket
     * @return
     */
    protected long bucket(long num, int bucket) {
        return Math.abs(num) % bucket;
    }

    /**
     * 将字符串经过hash计算形成long的数字
     *
     * @param str
     * @return
     * @see <a href="https://stackoverflow.com/questions/63112959/scala-murmurhash3-library-not-matching-spark-hash-function"/a>
     */
    protected int hash(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return Murmur3.hashUnsafeBytes(bytes, 16, bytes.length, 42);
    }
}
