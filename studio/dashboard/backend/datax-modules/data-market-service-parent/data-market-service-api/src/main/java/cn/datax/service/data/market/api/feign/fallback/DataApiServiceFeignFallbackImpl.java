package cn.datax.service.data.market.api.feign.fallback;

import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.feign.DataApiServiceFeign;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataApiServiceFeignFallbackImpl implements DataApiServiceFeign {

	@Setter
	private Throwable cause;

	@Override
	public DataApiEntity getDataApiById(String id) {
		log.error("feign 调用{}出错", id, cause);
		return null;
	}

	@Override
	public DataApiEntity getBySourceId(String sourceId) {
		log.error("feign 调用数据集市关联出错", cause);
		throw new RuntimeException("feign 调用数据集市关联出错", cause);
	}

	@Override
	public List<DataApiEntity> getReleaseDataApiList() {
		log.error("feign 调用出错", cause);
		return null;
	}
}
