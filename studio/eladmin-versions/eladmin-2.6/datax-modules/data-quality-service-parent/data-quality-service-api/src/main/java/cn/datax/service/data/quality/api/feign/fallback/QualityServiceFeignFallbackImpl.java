package cn.datax.service.data.quality.api.feign.fallback;


import cn.datax.service.data.quality.api.entity.CheckRuleEntity;
import cn.datax.service.data.quality.api.feign.QualityServiceFeign;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QualityServiceFeignFallbackImpl implements QualityServiceFeign {

	@Setter
	private Throwable cause;

	@Override
	public CheckRuleEntity getBySourceId(String sourceId) {
		log.error("feign 调用数据质量管理查询出错", cause);
		throw new RuntimeException("feign 调用数据质量管理查询出错", cause);
	}
}
