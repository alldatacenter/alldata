package cn.datax.service.data.visual.api.feign.fallback;


import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.feign.VisualServiceFeign;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VisualServiceFeignFallbackImpl implements VisualServiceFeign {

	@Setter
	private Throwable cause;

	@Override
	public DataSetEntity getBySourceId(String sourceId) {
		log.error("feign 调用可视化管理查询出错", cause);
		throw new RuntimeException("feign 调用可视化管理查询出错", cause);
	}
}
