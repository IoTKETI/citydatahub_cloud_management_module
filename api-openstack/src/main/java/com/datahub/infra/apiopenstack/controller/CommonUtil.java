package com.datahub.infra.apiopenstack.controller;

import com.datahub.infra.core.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public final class CommonUtil {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public final static int MAX_RETRY_COUNT = 10;
    public final static int SLEEP_TIME = 1000;

    public static int sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        return ms;
    }

    public static BigDecimal getBigDecimal(Object value) {
        BigDecimal ret = null;
        if(value != null) {
            if(value instanceof BigDecimal) {
                ret = (BigDecimal) value;
            } else if(value instanceof String) {
                ret = new BigDecimal((String) value);
            } else if(value instanceof BigInteger) {
                ret = new BigDecimal((BigInteger) value);
            } else if(value instanceof Number) {
                ret = new BigDecimal(((Number)value).doubleValue());
            } else {
                throw new ClassCastException("Not possible to coerce ["+value+"] from class "+value.getClass()+" into a BigDecimal.");
            }
        }
        return ret;
    }

    public  static Double getListAverage(List<Double> list) {
        if(list.size() == 0) return new Double(0);

        Double result = new Double(0);
        for(Double num : list) {
            result += num;
        }
        result = result / list.size();
        return result;
    }

    public static String getUserUUID() {
        UserInfo userInfo = null;

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            HttpSession session = request.getSession();
            userInfo = (UserInfo) session.getAttribute("userInfo");
        } catch (Exception e) {
        }

        String userUUID = "";
        if(userInfo != null) {
            userUUID = userInfo.getId();
        }
        return userUUID;
    }


    public static HttpHeaders getAuthHeaders(String credentialInfo, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("credential", credentialInfo);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8));
        headers.set("Authorization", "Bearer " + token);

        return headers;
    }
}