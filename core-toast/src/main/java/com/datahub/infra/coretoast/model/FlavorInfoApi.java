package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

/**
 * @author kkm
 * @date 2019.3.22
        * @brief TOAST 서버 생성용 모델
        */
@Data
public class FlavorInfoApi implements Serializable {

    private static final long serialVersionUID = -8629918608557087127L;

    @Key private FlavorDetailInfo[] flavors;

}
