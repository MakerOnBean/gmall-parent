package cloud.makeronbean.gmall.all.controller;

import cloud.makeronbean.gmall.client.ItemFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author makeronbean
 */
@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;


    /**
     * item详情页
     */
    @GetMapping("/{skuId}.html")
    public String item(@PathVariable Long skuId, Model model){

        Result<Map<String,Object>> item = itemFeignClient.getItem(skuId);
        model.addAllAttributes(item.getData());
        return "item/item";
    }
}
