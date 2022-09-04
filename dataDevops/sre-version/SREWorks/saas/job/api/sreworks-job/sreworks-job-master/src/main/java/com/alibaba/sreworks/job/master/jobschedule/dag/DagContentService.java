package com.alibaba.sreworks.job.master.jobschedule.dag;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.jobschedule.parallel.ParallelJobScheduleConf;
import com.alibaba.sreworks.job.master.jobschedule.serial.SerialJobScheduleConf;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.tesla.dag.model.repository.TcDagNodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DagContentService {

    @Autowired
    TcDagNodeRepository dagNodeRepository;

    private JSONObject postNode() {
        Long defId = dagNodeRepository.findFirstByAppIdAndName("tesla", "dagJobEndNode").getId();
        return JsonUtil.map(
            "id", "__post_node__",
            "data", JsonUtil.map(
                "defId", defId,
                "type", "NODE"
            )
        );
    }

    public String dagContent(DagJobScheduleConf conf) {

        Long taskDefId = dagNodeRepository.findFirstByAppIdAndName("tesla", "dagJobTaskNode").getId();
        return JsonUtil.map(
            "__post_node__", postNode(),
            "nodes", conf.getNodes().stream().map(node -> JsonUtil.map(
                "id", node.getId(),
                "data", JsonUtil.map(
                    "defId", taskDefId,
                    "type", "NODE",
                    "inputParams", JsonUtil.list(JsonUtil.map(
                        "isOverWrite", true,
                        "fromType", "CONSTANT",
                        "name", "taskId",
                        "alias", "taskId",
                        "type", "LONG",
                        "value", node.getTaskId()
                    ))
                ),
                "options", node.getOptions()
            )).collect(Collectors.toList()),
            "edges", conf.getEdges().stream().map(edge -> JsonUtil.map(
                "id", edge.getId(),
                "source", edge.getSource(),
                "target", edge.getTarget(),
                "data", JsonUtil.map(
                    "expression", edge.getExpression()
                ),
                "options", edge.getOptions()
            )).collect(Collectors.toList())
        ).toJSONString();

    }

    public DagJobScheduleConf dagJobScheduleConf(JSONObject dagContentJSONObject) {
        return DagJobScheduleConf.builder()
            .nodes(dagContentJSONObject.getJSONArray("nodes").toJavaList(JSONObject.class).stream()
                .map(node -> new DagJobScheduleConfNode(
                    node.getString("id"),
                    node.getJSONObject("data").getJSONArray("inputParams").getJSONObject(0).getLongValue("value"),
                    node.getJSONObject("options")
                ))
                .collect(Collectors.toList()))
            .edges(dagContentJSONObject.getJSONArray("edges").toJavaList(JSONObject.class).stream()
                .map(edge -> new DagJobScheduleConfEdge(
                    edge.getString("id"),
                    edge.getString("source"),
                    edge.getString("target"),
                    edge.getJSONObject("data").getString("expression"),
                    edge.getJSONObject("options")
                ))
                .collect(Collectors.toList()))
            .build();

    }

    public String dagContent(ParallelJobScheduleConf conf) {

        Long defId = dagNodeRepository.findFirstByAppIdAndName("tesla", "dagJobTaskNode").getId();
        return JsonUtil.map(
            "__post_node__", postNode(),
            "nodes", conf.getTaskIdList().stream().map(taskId -> JsonUtil.map(
                "id", UUID.randomUUID().toString().replace("-", ""),
                "data", JsonUtil.map(
                    "defId", defId,
                    "type", "NODE",
                    "inputParams", JsonUtil.list(JsonUtil.map(
                        "isOverWrite", true,
                        "fromType", "CONSTANT",
                        "name", "taskId",
                        "alias", "taskId",
                        "type", "LONG",
                        "value", taskId
                    ))
                ),
                "options", JsonUtil.map()
            )).collect(Collectors.toList()),
            "edges", JsonUtil.list()
        ).toJSONString();

    }

    public ParallelJobScheduleConf parallelJobScheduleConf(JSONObject dagContentJSONObject) {
        return ParallelJobScheduleConf.builder()
            .taskIdList(dagContentJSONObject.getJSONArray("nodes").toJavaList(JSONObject.class).stream()
                .map(node ->
                    node.getJSONObject("data").getJSONArray("inputParams").getJSONObject(0).getLongValue("value"))
                .collect(Collectors.toList()))
            .build();

    }

    public String dagContent(SerialJobScheduleConf conf) {

        List<JSONObject> edges = new ArrayList<>();
        List<JSONObject> nodes = new ArrayList<>();
        List<Long> taskIdList = conf.getTaskIdList();
        if (taskIdList.size() >= 2) {
            for (int index = 0; index < taskIdList.size() - 1; index++) {
                edges.add(JsonUtil.map(
                    //"id", index,
                    "source", Integer.toString(index),
                    "target", Integer.toString(index + 1),
                    "data", JsonUtil.map(
                        "expression", ""
                    ),
                    "options", JsonUtil.map()
                ));
            }
        }

        Long defId = dagNodeRepository.findFirstByAppIdAndName("tesla", "dagJobTaskNode").getId();
        for (int index = 0; index < taskIdList.size(); index++) {
            nodes.add(JsonUtil.map(
                "id", Integer.toString(index),
                "data", JsonUtil.map(
                    "defId", defId,
                    "type", "NODE",
                    "inputParams", JsonUtil.list(JsonUtil.map(
                        "fromType", "CONSTANT",
                        "name", "taskId",
                        "alias", "taskId",
                        "type", "LONG",
                        "isOverWrite", true,
                        "value", taskIdList.get(index)
                    ))
                ),
                "options", JsonUtil.map()
            ));
        }

        return JsonUtil.map(
            "__post_node__", postNode(),
            "nodes", nodes,
            "edges", edges
        ).toJSONString();

    }

    public SerialJobScheduleConf serialJobScheduleConf(JSONObject dagContentJSONObject) {
        return SerialJobScheduleConf.builder()
            .taskIdList(dagContentJSONObject.getJSONArray("nodes").toJavaList(JSONObject.class).stream()
                .map(node ->
                    node.getJSONObject("data").getJSONArray("inputParams").getJSONObject(0).getLongValue("value"))
                .collect(Collectors.toList()))
            .build();

    }
}
