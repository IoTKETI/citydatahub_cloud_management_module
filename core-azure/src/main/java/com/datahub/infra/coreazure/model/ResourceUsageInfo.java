package com.datahub.infra.coreazure.model;

import com.datahub.infra.coreazure.util.CommonUtil;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ResourceUsageInfo implements Serializable {
    private static final long serialVersionUID = 3075701636330880165L;
    private String nextLink;
    private List<ResourceUsageDetailInfo> value;

    public ResourceUsageInfo() {

    }

    public BigDecimal getTotalCost() {
        BigDecimal totalCost = new BigDecimal(0);

        for (ResourceUsageDetailInfo usageDetailInfo : value) {
            Map<String, Object> usageProperty = usageDetailInfo.getProperties();
            BigDecimal pretaxCost = CommonUtil.getBigDecimal(usageProperty.get("pretaxCost"));
            if(pretaxCost.equals(0)) continue;
            totalCost = totalCost.add(pretaxCost);
        }

        if(!totalCost.equals(0)) totalCost = totalCost.setScale(2, BigDecimal.ROUND_HALF_UP);
        return totalCost;
    }
}
