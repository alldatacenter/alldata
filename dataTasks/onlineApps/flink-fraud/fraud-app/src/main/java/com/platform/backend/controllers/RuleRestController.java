package com.platform.backend.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.backend.entities.Rule;
import com.platform.backend.exceptions.RuleNotFoundException;
import com.platform.backend.model.RulePayload;
import com.platform.backend.repositories.RuleRepository;
import com.platform.backend.services.FlinkRulesService;
import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class RuleRestController {

  private final RuleRepository repository;
  private final FlinkRulesService flinkRulesService;

  RuleRestController(RuleRepository repository, FlinkRulesService flinkRulesService) {
    this.repository = repository;
    this.flinkRulesService = flinkRulesService;
  }

  private final ObjectMapper mapper = new ObjectMapper();

  @GetMapping("/rules")
  List<Rule> all() {
    return repository.findAll();
  }

  @PostMapping("/rules")
  Rule newRule(@RequestBody Rule newRule) throws IOException {
    Rule savedRule = repository.save(newRule);
    Integer id = savedRule.getId();
    RulePayload payload = mapper.readValue(savedRule.getRulePayload(), RulePayload.class);
    payload.setRuleId(id);
    String payloadJson = mapper.writeValueAsString(payload);
    savedRule.setRulePayload(payloadJson);
    Rule result = repository.save(savedRule);
    flinkRulesService.addRule(result);
    return result;
  }

  @GetMapping("/rules/pushToFlink")
  void pushToFlink() {
    List<Rule> rules = repository.findAll();
    for (Rule rule : rules) {
      flinkRulesService.addRule(rule);
    }
  }

  @GetMapping("/rules/{id}")
  Rule one(@PathVariable Integer id) {
    return repository.findById(id).orElseThrow(() -> new RuleNotFoundException(id));
  }

  @DeleteMapping("/rules/{id}")
  void deleteRule(@PathVariable Integer id) throws JsonProcessingException {
    repository.deleteById(id);
    flinkRulesService.deleteRule(id);
  }

  @DeleteMapping("/rules")
  void deleteAllRules() throws JsonProcessingException {
    List<Rule> rules = repository.findAll();
    for (Rule rule : rules) {
      repository.deleteById(rule.getId());
      flinkRulesService.deleteRule(rule.getId());
    }
  }
}
