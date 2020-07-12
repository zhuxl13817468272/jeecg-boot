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
import java.io.FileOutputStream;
import java.io.OutputStream;
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
     * 0.二进制流上传文件
     * @return
     */
    @PostMapping(path = "write")
    @ApiOperation(value = "上传流文件到classpath的processes目录下",notes = "上传到classpath的processes目录下")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "bytes",value = "流程图文件二进制流",dataType = "byte[]",paramType = "query"),
            @ApiImplicitParam(name = "fileName",value = "二进制文件名字，如bpmnName.bpmn20.xml和bpmnName.png",dataType = "String",paramType = "query")
    })
    public RestMessgae write(@RequestParam("bytes") byte[] bytes, @RequestParam("fileName") String fileName) {
        try {
            // 项目根路径文件夹(相对路径)
            String fileDirPath = new String("src/main/resources/processes");
            // 文件夹绝对路径
            File fileDir  = new File(fileDirPath);
            // 构建真实的文件路径
            File newFile = new File(fileDir.getAbsolutePath() + File.separator + fileName);
            System.out.println(newFile.getAbsolutePath());

            if (!newFile.exists()) {
                newFile.createNewFile();
            }
            OutputStream os = new FileOutputStream(newFile);
            os.write(bytes);
            os.close();

            String[] split = fileName.split("\\.");
            String bpmnName = Arrays.asList(split).get(0);

            // 响应
            Map<String, String> result = new HashMap<>(2);
            result.put("bpmnName", bpmnName);
            result.put("path",newFile.getAbsolutePath());
            return RestMessgae.success("上传成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
            return RestMessgae.fail("上传失败", e.getMessage());
        }

    }



}
