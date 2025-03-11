package com.atom.netpaygateway.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 外部接口调用响应码枚举类
 *
 * @author Tom
 * @date 10/3/2025
 */
@Getter
@AllArgsConstructor
public enum EnumRespCode {

    SUCCESS("0000", "成功"),
    TIMEOUT("8888", "超时"),
    FAIL("9999", "失败");

    /**
     * 类型
     */
    private final String code;

    /**
     * 描述
     */
    private final String desc;
}
