package org.jeecg.entity;

import lombok.Data;

/**
 * @Description:
 * @author: LiKun
 * @date: 2021年10月20日 16:54
 */
@Data
public class FieldEntity {

    private String name;

    private String type;

    private String mark;

    // 不传长度时，默认为0
    private int length;

}
