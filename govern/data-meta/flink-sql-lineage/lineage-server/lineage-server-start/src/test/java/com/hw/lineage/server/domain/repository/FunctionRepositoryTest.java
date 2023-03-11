package com.hw.lineage.server.domain.repository;

import com.hw.lineage.server.AbstractSpringBootTest;
import com.hw.lineage.server.domain.entity.Function;
import com.hw.lineage.server.domain.query.function.FunctionEntry;
import com.hw.lineage.server.domain.vo.FunctionId;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


/**
 * @description: FunctionRepositoryTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class FunctionRepositoryTest extends AbstractSpringBootTest {

    private static final Logger LOG = LoggerFactory.getLogger(FunctionRepositoryTest.class);


    @Resource
    private FunctionRepository functionRepository;

    @Test
    public void testFindEntry() {
        FunctionId functionId = new FunctionId(1L);
        FunctionEntry entry = functionRepository.findEntry(functionId);

        assertThat(entry).isNotNull();
        assertThat(entry.getPluginCode()).isEqualTo("flink1.16.x");
        assertThat(entry.getCatalogName()).isEqualTo("Flink1_16_Memory");
        assertThat(entry.getDatabase()).isEqualTo("default");
        assertThat(entry.getFunctionId()).isEqualTo(1L);
        assertThat(entry.getFunctionName()).isEqualTo("flink_suffix_udf");
    }


    @Test
    public void testFindMemory() {
        List<Function> functionList = functionRepository.findMemory();
        assertThat(functionList).isNotNull();
        functionList.forEach(function -> LOG.info("memory function: {}",function.toString()));
        assertThat(functionList).asList().hasSize(3);
    }
}
