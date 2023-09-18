package datart.server.base.dto;

import datart.core.entity.Dashboard;
import datart.core.entity.Datachart;
import datart.core.entity.Variable;
import datart.core.entity.View;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DashboardDetail extends Dashboard {

    private String parentId;

    private Double index;

    private String config;

    private List<WidgetDetail> widgets;

    private List<View> views;

    private List<Datachart> datacharts;

    private List<Variable> queryVariables;

    private boolean download;
}