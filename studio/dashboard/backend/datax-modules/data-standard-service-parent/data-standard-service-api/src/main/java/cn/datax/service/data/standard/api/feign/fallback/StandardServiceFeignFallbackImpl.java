package cn.datax.service.data.standard.api.feign.fallback;


import cn.datax.common.core.R;
import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.feign.StandardServiceFeign;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StandardServiceFeignFallbackImpl implements StandardServiceFeign {

	@Setter
	private Throwable cause;

	@Override
	public ContrastEntity getBySourceId(String sourceId) {
		log.error("feign 调用对照表查询出错", cause);
		throw new RuntimeException("feign 调用对照表查询出错", cause);
	}
}
