package org.jeecg.modules.activiti.base;

import java.util.Map;

public class ResultMapHelper {

    public static Map<String, Object> getSuccessMap() {
        Map<String, Object> resultMap = ResultMapHelper.getSuccessMap();
        resultMap.put("resultCode","0");
        resultMap.put("resultMsg","ok");
        return resultMap;
    }
}
