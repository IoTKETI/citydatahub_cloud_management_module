package com.datahub.infra.coredb.service;

import com.datahub.infra.core.Constants;
import com.datahub.infra.core.model.ActionInfo;

import java.util.List;
import java.util.Map;

public interface ActionService {
    public List<ActionInfo> actions(Map<String, Object> params);
    public int getActionsTotal(Map<String, Object> params);
    public ActionInfo action(Map<String, Object> params);
    public int updateAction(ActionInfo info);
    public int createAction(ActionInfo info);
    public int getIDCount(String id);
    String initAction(String groupId, String userId, String content, String targetId, String targetName, Constants.ACTION_CODE actionCode, Constants.HISTORY_TYPE type);
    void setActionResult(String actionId, Constants.ACTION_RESULT opResult);
    void setActionResult(String actionId, Constants.ACTION_RESULT opResult, String resultDetail);
}
