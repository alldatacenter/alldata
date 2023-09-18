package datart.server.base.dto;

import datart.core.entity.BaseEntity;
import datart.core.entity.Storypage;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StorypageDetail extends Storypage {

    BaseEntity vizDetail;

}
