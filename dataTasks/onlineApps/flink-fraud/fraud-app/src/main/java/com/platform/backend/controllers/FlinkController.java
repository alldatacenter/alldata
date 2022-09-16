package com.platform.backend.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.backend.entities.Rule;
import com.platform.backend.model.RulePayload;
import com.platform.backend.model.RulePayload.ControlType;
import com.platform.backend.model.RulePayload.RuleState;
import com.platform.backend.services.FlinkRulesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FlinkController {

  private final FlinkRulesService flinkRulesService;

  // Currently rules channel is also (mis)used for control messages. This has to do with how control
  // channels are set up in Flink Job.
  FlinkController(FlinkRulesService flinkRulesService) {
    this.flinkRulesService = flinkRulesService;
  }

  private final ObjectMapper mapper = new ObjectMapper();

  @GetMapping("/syncRules")
  void syncRules() throws JsonProcessingException {
    Rule command = createControllCommand(ControlType.EXPORT_RULES_CURRENT);
    flinkRulesService.addRule(command);
  }

  @GetMapping("/clearState")
  void clearState() throws JsonProcessingException {
    Rule command = createControllCommand(ControlType.CLEAR_STATE_ALL);
    flinkRulesService.addRule(command);
  }

  private Rule createControllCommand(ControlType clearStateAll) throws JsonProcessingException {
    RulePayload payload = new RulePayload();
    payload.setRuleState(RuleState.CONTROL);
    payload.setControlType(clearStateAll);
    Rule rule = new Rule();
    rule.setRulePayload(mapper.writeValueAsString(payload));
    return rule;
  }
}
