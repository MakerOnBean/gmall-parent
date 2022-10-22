package cloud.makeronbean.gmall.all.controller;

import cloud.makeronbean.gmall.client.ProductFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author makeronbean
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    @GetMapping({"/", "/index.html"})
    public String index(Model model) {
        Result<List<JSONObject>> baseCategoryList = productFeignClient.getBaseCategoryList();
        model.addAttribute("list",baseCategoryList.getData());
        return "index/index";
    }

    @GetMapping("/createIndex")
    @ResponseBody
    public Result<List<JSONObject>> createIndex(){
        // 数据获取
        Result<List<JSONObject>> result = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        context.setVariable("list",result.getData());
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("/Users/makeronbean/Desktop/gmall/index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        templateEngine.process("index/index.html",context,fileWriter);
        return Result.ok();
    }
}
