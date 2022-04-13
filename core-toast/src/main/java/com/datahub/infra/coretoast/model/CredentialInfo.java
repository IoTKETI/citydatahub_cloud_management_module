package com.datahub.infra.coretoast.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kkm
 * @date 2019.3.18
 * @brief 클라우드별 API 접속 정보를 담는 클래스
 */
@Data
public class CredentialInfo implements Serializable {


    private static final long serialVersionUID = -4468599970527324982L;
    private String id;
    private String name;
    private String region;
    private String tenant;
    private String accessId;
    private String accessToken;
}
