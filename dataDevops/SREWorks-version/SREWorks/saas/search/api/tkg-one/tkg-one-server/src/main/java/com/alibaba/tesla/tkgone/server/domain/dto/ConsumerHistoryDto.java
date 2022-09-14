package com.alibaba.tesla.tkgone.server.domain.dto;

import com.alibaba.tesla.tkgone.server.domain.ConsumerHistory;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.util.Date;

/**
 * @author yangjinghua
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ConsumerHistoryDto extends ConsumerHistory {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private Long consumerId;

    private String detail;

    private String states;

    public ConsumerHistoryDto(ConsumerHistory consumerHistory) {

        BeanUtils.copyProperties(consumerHistory, this);

    }

    public ConsumerHistory toConsumerHistory() {

        ConsumerHistory consumerHistory = new ConsumerHistory();
        BeanUtils.copyProperties(this, consumerHistory);

        return consumerHistory;

    }

}