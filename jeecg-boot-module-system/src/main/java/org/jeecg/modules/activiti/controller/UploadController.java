package org.jeecg.modules.activiti.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.jeecg.modules.activiti.base.RestMessgae;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *  上次流程
 */
@RestController
@Api(tags = "上传流程")
public class UploadController {

    /**
     * 0.上传流程
     * @return
     */
    @PostMapping(path = "upload")
    @ApiOperation(value = "上传到classpath的processes目录下",notes = "上传到classpath的processes目录下")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file",value = "流程图文件bpmnName.bpmn20.xml和bpmnName.png",dataType = "MultipartFile",paramType = "query")
    })
    public RestMessgae upload(@RequestParam("file") MultipartFile file){
        String filename = file.getOriginalFilename(); //uploadTest.bpmn20.xml  uploadTest.png
        String[] split = filename.split("\\.");
        String bpmnName = Arrays.asList(split).get(0);
        try {
            // 与jar包同级的目录(绝对路径)
            // String classpath = ResourceUtils.getURL("classpath:").getPath();
            // 项目根路径文件夹(相对路径)
            String fileDirPath = new String("src/main/resources/processes");

            // 文件夹绝对路径
            File fileDir  = new File(fileDirPath);
            System.out.println(fileDir.getAbsolutePath());
            // 构建真实的文件路径
            File newFile = new File(fileDir.getAbsolutePath() + File.separator + filename);
            System.out.println(newFile.getAbsolutePath());

            // 上传文件到 -》 “绝对路径”
            file.transferTo(newFile);

            // 响应
            Map<String, String> result = new HashMap<>(2);
            result.put("bpmnName", bpmnName);
            result.put("path",newFile.getAbsolutePath());
            return RestMessgae.success("文件写入成功", result);
        }catch (Exception e){
            System.out.println("文件写入失败");
            return RestMessgae.fail("文件写入失败", e.getMessage());
        }

    }


}
