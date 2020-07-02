package org.jeecg.modules.activiti.controller;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.jeecg.modules.activiti.base.ResultMapHelper;
import org.jeecg.modules.activiti.entity.Leave;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/level")
public class LeaveController {
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ProcessEngine processEngine;

    /**
     * 1.部署  影响：act_re表  act_re_deployment  act_re_procdef
     * @return
     */
    @RequestMapping(value = "/deploy", method = RequestMethod.GET)
    public Map<String, Object> deploy(@RequestParam(name = "profile") CommonsMultipartFile profile, @RequestParam(name = "name") String name){
        // 流上传  上传至classpath:/processes/下


        // 根据名字部署
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("firstDemo.bpmn20.xml")
                .name("请假")
                .deploy();
        System.out.println("部署ID：" + deploy.getId());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("deployId",deploy.getId());
        return resultMap;
    }

    /**
     * 2.启动流程  影响：act_ru表  act_ru_task   act_ru_execution  act_ru_identitylink
     * @param userId
     * @return
     */
    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public Map<String, Object> start(@RequestParam(name = "userId") String userId){

        //A.启动的时候，从登陆的信息中，获取用户，设置变量，并传递变量
        //登陆时，可以把该用户及用户的上级查出来，在流转流程
        Map<String, Object> vars = new HashMap<>();
        Leave leave = new Leave();
        leave.setUserId(userId);
        vars.put("leave",leave);
        /*
         *  leavel1的值来自act_re_procdef的Key字段值，
         *  每启动一次act_ge_procdef表中相同的Key的值记录就会多一条，但是activiti会自动启动version版本最大的一条记录
         */
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leave1",vars);
        System.out.println("实例ID："+processInstance.getId());
        System.out.println("流程定义ID："+processInstance.getDeploymentId());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("processId",processInstance.getId());
        return resultMap;
    }

    /**
     * 查询用户流程
     * @param userId
     * @return
     */
    @RequestMapping(value = "/find", method = RequestMethod.GET)
    public Map<String, Object> find(@RequestParam("userId")String userId){
        List<Task> taskList = taskService.createTaskQuery().taskAssignee(userId).list();

        List<Leave> resultList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(taskList)){
            for(Task task : taskList){
                Leave leave = (Leave) taskService.getVariable(task.getId(),"leave");
                leave.setTaskId(task.getId());
                leave.setTaskName(task.getName());
                resultList.add(leave);
            }
        }

        Map<String, Object> resultMap = ResultMapHelper.getSuccessMap();
        resultMap.put("datas", resultList);
        return resultMap;
    }

    /**
     * 3.填写请假单
     *      任务表act_ru_task找ID
     * @param leave 传递数据必须序列化
     * @return
     */
    @RequestMapping(value="/apply", method = RequestMethod.POST)
    public Map<String, Object> apply(@RequestBody Leave leave){
        Task task = taskService.createTaskQuery().taskId(leave.getTaskId()).singleResult();
        String taskId = leave.getTaskId();

        //A.得到上个节点信息（启动节点信息），并进一步设置变量（请假信息）
        Leave origin = (Leave) taskService.getVariable(taskId, "leave"); //
        origin.setDesc(leave.getDesc());
        origin.setStartDate(leave.getStartDate());
        origin.setEndDate(leave.getEndDate());
        origin.setTotalDay(leave.getTotalDay());
        //根据员工查询自己的主管是谁
        origin.setApprover1(leave.getApprover1());
        origin.setApprover2(leave.getApprover2());
        origin.setSubmit(leave.getSubmit());

        Map<String, Object> vars = new HashMap<>();
        vars.put("leave", origin);
        //B.完成请假，并传递变量
        taskService.complete(taskId, vars);

        Map<String, Object> resultMap = ResultMapHelper.getSuccessMap();
        return resultMap;
    }

    /**
     * 4.直接主管审批
     * @param leave
     * @return
     */
    @RequestMapping(value = "/approve1", method = RequestMethod.POST)
    public Map<String, Object> approve1(@RequestBody Leave leave){
        Task task = taskService.createTaskQuery().taskId(leave.getTaskId()).singleResult();
        String taskId = leave.getTaskId();

        //A.得到上个节点信息（请假节点信息），并进一步设置变量（审批意见）
        Leave origin = (Leave) taskService.getVariable(taskId, "leave");
        origin.setApproveDesc1(leave.getApproveDesc1());
        origin.setAgree1(leave.getAgree1());

        Map<String, Object> vars = new HashMap<>();
        vars.put("leave", origin);
        //B.完成审批，并传递变量
        taskService.complete(taskId,vars);
        Map<String, Object> resultMap = ResultMapHelper.getSuccessMap();
        return resultMap;
    }

    /**
     * 5.部门主管审批
     * @param leave
     * @return
     */
    @RequestMapping(value = "/approve2", method = RequestMethod.POST)
    public Map<String, Object> approve2(@RequestBody Leave leave){
        Task task = taskService.createTaskQuery().taskId(leave.getTaskId()).singleResult();
        String taskId = leave.getTaskId();

        //A.得到上个节点信息（直接主管审批节点信息），并进一步设置变量（部门主管审批意见）
        Leave origin = (Leave) taskService.getVariable(taskId, "leave");

        Map<String, Object> vars = new HashMap<>();
        origin.setApproveDesc2(leave.getApproveDesc2());
        origin.setAgree2(leave.getAgree2());
        vars.put("leave", origin);
        //B.完成审批，并传递变量
        taskService.complete(leave.getTaskId(),vars); //完成任务清空act_ru_task表数据，增加act_hi表数据

        Map<String, Object> resultMap = ResultMapHelper.getSuccessMap();
        return resultMap;
    }



    /**
     * 查看历史记录
     * @param userId
     * @return
     */
    @RequestMapping(value="/findClosed", method = RequestMethod.GET)
    public Map<String, Object> findClosed(String userId){
        HistoryService historyService = processEngine.getHistoryService();

        List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("leave1").variableValueEquals("leave.userId",userId).list();
        List<Leave> leaves = new ArrayList<>();
        for(HistoricProcessInstance pi : list){
            leaves.add((Leave) pi.getProcessVariables().get("leave"));
        }

        Map<String, Object> resultMap = ResultMapHelper.getSuccessMap();
        resultMap.put("datas", leaves);
        return resultMap;
    }



//*********************************************************************************************************************************************************
    /**
     * 查询任务  影响：act_ru表  act_ru_task   act_ru_execution  act_ru_identitylink
     * @param userName
     * @return
     */
    @RequestMapping(value = "/findTask", method = RequestMethod.GET)
    public Map<String, Object> findTask(@RequestParam String userName){
        //查询任务
        TaskService taskService = processEngine.getTaskService();
        TaskQuery taskQuery = taskService.createTaskQuery().taskAssignee(userName);//根据条件查询

        List<Task> list = taskQuery.list();
        for(Task task:list){
            System.out.println("任务ID："+task.getId());
            System.out.println("任务名称："+task.getName());
        }

        Map<String, Object> resultMap = new HashMap<>();
        return resultMap;
    }

    /**
     * 完成任务  影响：act_hi表 act_hi_taskinst  act_hi_procinst act_hi_indentitylink  act_hi_actinst历史活动节点表（流程图多少节点多条数据，查看任务的完成时间等）
     * @param taskId
     * @return
     */
    @RequestMapping(value = "/completeTask", method = RequestMethod.GET)
    public Map<String, Object> completeTask(@RequestParam String taskId){
        //完成任务
        TaskService taskService = processEngine.getTaskService();
        taskService.complete(taskId); //完成任务清空act_ru_task表数据，增加act_hi表数据

        Map<String, Object> resultMap = new HashMap<>();
        return resultMap;
    }


}
