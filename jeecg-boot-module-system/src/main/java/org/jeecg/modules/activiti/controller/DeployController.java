package org.jeecg.modules.activiti.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.activiti.base.Constant;
import org.jeecg.modules.activiti.base.RestMessgae;
import org.jeecg.modules.activiti.entity.ActReProcDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * @author xugj<br>
 * @version 1.0<br>
 * @createDate 2019/05/29 17:34 <br>
 * @Description <p> 部署流程、删除流程 </p>
 */

@RestController
@Api(tags="部署流程、删除流程")
public class DeployController {
    @Autowired
    private RuntimeService runtimeService;

    private final RepositoryService repositoryService;

    public DeployController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * 1.部署流程
     *      act_re_deployment  act_re_procdef   act_ge_bytearray  act_ge_property
     * @param bpmnName
     * @return
     */
    @PostMapping(path = "deploy")
    @ApiOperation(value = "根据bpmnName部署流程",notes = "根据bpmnName部署流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bpmnName",value = "设计的流程图名称",dataType = "String",paramType = "query",example = "myProcess")
    })
    public RestMessgae deploy(@RequestParam("bpmnName") String bpmnName){

        RestMessgae restMessgae = new RestMessgae();
        //创建一个部署对象
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name("请假流程");
        Deployment deployment = null;
        try {
            deployment = deploymentBuilder
                    .addClasspathResource("processes/"+ bpmnName +".bpmn20.xml")
                    .addClasspathResource("processes/"+ bpmnName +".png")
                    .deploy();
        } catch (Exception e) {
            restMessgae = RestMessgae.fail("部署失败", e.getMessage());
            e.printStackTrace();
        }

        if (deployment != null) {
            Map<String, String> result = new HashMap<>(2);
            result.put("deployID", deployment.getId());
            result.put("deployName", deployment.getName());
            restMessgae = RestMessgae.success("部署成功", result);
        }
        return restMessgae;
    }


    /**
     * 1.1 分页查询
     *
     * @return ResultDTO  act_re_procdef
     */
    @GetMapping(path = "list")
    @ApiOperation(value = "分页查询",notes = "分页查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page",value = "页码，默认值为1",dataType = "String",paramType = "query",required = false,example = "myProcess"),
            @ApiImplicitParam(name = "pageSize",value = "每页条数，默认值为10",dataType = "String",paramType = "query",required = false,example = "myProcess"),
            @ApiImplicitParam(name = "category",value = "按目录查找分类，可不填",dataType = "String",paramType = "query",required = false,example = "myProcess"),
            @ApiImplicitParam(name = "key",value = "按key查找分类，可不填",dataType = "HttpServletResponse",paramType = "query",required = false,example = "请假流程")
    })
    public RestMessgae list(@RequestParam(value = "page",defaultValue = "1") String page, @RequestParam(value = "pageSize",defaultValue = "10") String pageSize,
                          @RequestParam(value = "category",required = false) String category, @RequestParam(value = "key",required = false) String key) {
        int page1 = Integer.parseInt(page);
        int pageSize1 = Integer.parseInt(pageSize);
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery().latestVersion().orderByProcessDefinitionKey().asc();

        // 按目录查找分类
        if (StringUtils.isNotBlank(category)) {
            processDefinitionQuery.processDefinitionCategory(category);
        }
        // 按key查找分类
        if (StringUtils.isNotBlank(key)) {
            processDefinitionQuery.processDefinitionKey(key);
        }
        List<ProcessDefinition> processDefinitionList = processDefinitionQuery.listPage((page1 - 1) * pageSize1, pageSize1);

        List<ActReProcDef> list = new ArrayList<>();

        for (ProcessDefinition processDefinition : processDefinitionList) {
            ActReProcDef  entity = new ActReProcDef();
            String deploymentId = processDefinition.getDeploymentId();
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
            entity.setId(processDefinition.getId());
            entity.setKey(processDefinition.getKey());
            entity.setName(deployment == null ? "" : StringUtils.isBlank(deployment.getName()) ? processDefinition.getName() : deployment.getName());
            entity.setDeployTime(deployment == null ? null : deployment.getDeploymentTime());
            entity.setDeploymentId(processDefinition.getDeploymentId());
            entity.setSuspensionState(processDefinition.isSuspended() ? Constant.TWO : Constant.ONE);
            entity.setResourceName(processDefinition.getResourceName());
            entity.setDgrmResourceName(processDefinition.getDiagramResourceName());
            entity.setCategory(processDefinition.getCategory());
            entity.setVersion(processDefinition.getVersion());
            entity.setDescription(processDefinition.getDescription());
            entity.setEngineVersion(processDefinition.getEngineVersion());
            entity.setTenantId(processDefinition.getTenantId());
            list.add(entity);
        }
        return RestMessgae.success("查询成功", list);
    }


    /**
     * 1.2 读取资源，通过部署ID
     *
     * @param id       流程定义ID  act_ru_task  proc_def_id   myProcess_1:5:f9d714fa-bf51-11ea-b3c3-e0d55eab2bc1   list接口中的id
     * @param proInsId 流程实例ID  act_ru_task  proc_inst_id  f462fecf-c24d-11ea-97e2-e0d55eab2bc1
     * @param resType  资源类型(xml|image)
     * @param response 响应
     * @throws Exception 读写流异常
     */
    @GetMapping(path = "read")
    @ApiOperation(value = "读取流程资源",notes = "读取流程xml或image资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "流程定义ID,list接口中的id，可不填 ,但id和proInsId必须填一个",dataType = "String",paramType = "query",required = false,example = "myProcess"),
            @ApiImplicitParam(name = "proInsId",value = "流程实例ID，可不填,但id和proInsId必须填一个",dataType = "String",paramType = "query",required = false,example = "myProcess"),
            @ApiImplicitParam(name = "resType",value = "资源类型(xml|image)",dataType = "String",paramType = "query",example = "myProcess"),
            @ApiImplicitParam(name = "response",value = "响应",dataType = "HttpServletResponse",paramType = "query",example = "请假流程")
    })
    public void resourceRead(@RequestParam(value = "id",required = false) String id, @RequestParam(value = "proInsId",required = false) String proInsId, @RequestParam("resType") String resType, HttpServletResponse response) throws Exception {
        if (StringUtils.isBlank(id)) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(proInsId).singleResult();
            id = processInstance.getProcessDefinitionId();
        }
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(id).singleResult();

        String resourceName = "";
        if (Constant.IMAGE.equals(resType)) {
            resourceName = processDefinition.getDiagramResourceName();
        } else if (Constant.XML.equals(resType)) {
            resourceName = processDefinition.getResourceName();
        }

        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);

        byte[] b = new byte[1024];
        int len = -1;
        int lenEnd = 1024;
        while ((len = resourceAsStream.read(b, 0, lenEnd)) != -1) {
            response.getOutputStream().write(b, 0, len);
        }
    }


    @PostMapping(path = "deployZIP")
    @ApiOperation(value = "根据ZIP压缩包部署流程",notes = "根据ZIP压缩包部署流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "zipName",value = "设计的流程图和图片的压缩包名称",dataType = "String",paramType = "query",example = "myProcess")
    })
    public RestMessgae deployZIP(@RequestParam("zipName") String zipName){
        RestMessgae restMessgae = new RestMessgae();
        Deployment deployment = null;
        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("processes/leaveProcess.zip");
            ZipInputStream zipInputStream = new ZipInputStream(in);
            deployment = repositoryService.createDeployment()
                    .name("请假流程2")
                    //指定zip格式的文件完成部署
                    .addZipInputStream(zipInputStream)
                    .deploy();//完成部署
            zipInputStream.close();
        } catch (Exception e) {
            restMessgae = RestMessgae.fail("部署失败", e.getMessage());
            // TODO 上线时删除
            e.printStackTrace();
        }
        if (deployment != null) {
            Map<String, String> result = new HashMap<>(2);
            result.put("deployID", deployment.getId());
            result.put("deployName", deployment.getName());
            restMessgae = RestMessgae.success("部署成功", result);
        }
        return restMessgae;
    }

    @PostMapping(path = "deleteProcess")
    @ApiOperation(value = "根据部署ID删除流程",notes = "根据部署ID删除流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deploymentId",value = "部署ID",dataType = "String",paramType = "query",example = "")
    })
    public RestMessgae deleteProcess(@RequestParam("deploymentId") String deploymentId){
        RestMessgae restMessgae = new RestMessgae();
        /**不带级联的删除：只能删除没有启动的流程，如果流程启动，就会抛出异常*/
        try {
            repositoryService.deleteDeployment(deploymentId);
        } catch (Exception e) {
            restMessgae = RestMessgae.fail("删除失败", e.getMessage());
            // TODO 上线时删除
            e.printStackTrace();
        }

        /**级联删除：不管流程是否启动，都能可以删除（emmm大概是一锅端）*/
//        repositoryService.deleteDeployment(deploymentId, true);
        restMessgae = RestMessgae.success("删除成功", null);
        return  restMessgae;
    }
}
