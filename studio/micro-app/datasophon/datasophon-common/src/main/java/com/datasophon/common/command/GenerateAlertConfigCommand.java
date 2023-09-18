package com.datasophon.common.command;

import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@Data
public class GenerateAlertConfigCommand implements Serializable {

    HashMap<Generators, List<AlertItem>> configFileMap;
    Integer clusterId;
}
