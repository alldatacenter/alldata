package com.alibaba.tesla.tkgone.server.domain.dto;

import com.alibaba.tesla.tkgone.server.domain.ConsumerNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

/**
 * @author yangjinghua
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ConsumerNodeDto extends ConsumerNode {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ConsumerNodeDto(ConsumerNode consumerNode) {

        BeanUtils.copyProperties(consumerNode, this);

    }

    public ConsumerNode toConsumerNode() {

        ConsumerNode consumerNode = new ConsumerNode();
        BeanUtils.copyProperties(this, consumerNode);

        return consumerNode;

    }

}