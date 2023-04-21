package org.dromara.cloudeon.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 服务实例序号表
 */
@Entity
@Table(name = "ce_service_instance_seq")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInstanceSeqEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;



    /**
     * 框架服务id
     */
    private Integer stackServiceId;

    /**
     * 最大序号
     */
    private Integer maxSeq;



}