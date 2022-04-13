package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author kkm
 * @date 2019.3.22
 * @brief TOAST 서버 생성용 모델
 */
@Data
public class FlavorDetailInfo implements Serializable {

    private static final long serialVersionUID = -8629918608557087127L;

    @Key private BigInteger disk;
    @Key private String id;
    @Key private String name;
    @Key private BigInteger ram;
    @Key private BigInteger vcpus;
    @Key private Links[] links;
    @Key private ExtraSpecs extra_specs;
    @Key private Float rxtx_factor;




//    @Override
//    public String toString() {
//        return "CreateServerInfo{" +
//                "header=" + createServerInfo +
//                ", instance=" + createServerInfo2 +
//                '}';
//    }
}
