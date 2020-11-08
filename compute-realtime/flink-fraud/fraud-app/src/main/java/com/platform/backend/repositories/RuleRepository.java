package com.platform.backend.repositories;

import com.platform.backend.entities.Rule;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface RuleRepository extends CrudRepository<Rule, Integer> {

  @Override
  List<Rule> findAll();
}
