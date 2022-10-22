package cloud.makeronbean.gmall.all.controller;

import cloud.makeronbean.gmall.client.ListFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;


    /**
     * 商品列表
     */
    @GetMapping("/list.html")
    public String list(SearchParam searchParam, Model model){
        Result<Map<String,Object>> result = listFeignClient.list(searchParam);
        model.addAllAttributes(result.getData());
        String urlParam = this.buildParam(searchParam);

        // urlParam
        model.addAttribute("urlParam",urlParam);

        // 数据回显
        model.addAttribute("searchParam",searchParam);

        // 面包屑-品牌
        String trademarkParam = this.buildTrademarkParam(searchParam.getTrademark());
        model.addAttribute("trademarkParam",trademarkParam);

        // 面包屑-平台
        List<Map<String,String>> propsParamList = this.buildPropParamList(searchParam);
        model.addAttribute("propsParamList",propsParamList);

        // 排序
        Map<String,String> orderMap = buildOrderMap(searchParam);
        model.addAttribute("orderMap",orderMap);

        return "list/index";
    }


    /**
     * 排序处理
     */
    private Map<String, String> buildOrderMap(SearchParam searchParam) {
        String order = searchParam.getOrder();
        Map<String,String> orderMap = new HashMap<>(2);

        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            if (split.length == 2){
                orderMap.put("type",split[0]);
                orderMap.put("sort",split[1]);
            }
        } else {
            orderMap.put("type","1");
            orderMap.put("sort","desc");
        }
        return orderMap;
    }


    /**
     * 平台面包屑
     */
    private List<Map<String, String>> buildPropParamList(SearchParam searchParam) {
        List<Map<String,String>> propsParamList = null;
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0){
            propsParamList = Arrays.stream(props).map(item -> {
                String[] split = item.split(":");
                Map<String,String> map = new HashMap<>(3);
                if (split.length == 3){
                    map.put("attrName",split[2]);
                    map.put("attrValue",split[1]);
                    map.put("attrId",split[0]);
                }
                return map;
            }).collect(Collectors.toList());
        }
        return propsParamList;
    }


    /**
     * 品牌面包屑属性构造
     */
    private String buildTrademarkParam(String trademark) {
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split(":");
            if (split != null && split.length == 2){
                return "品牌 : " + split[1];
            }
        }
        return "";
    }


    /**
     * 构建商品列表页所需要的 urlParam 属性
     */
    private String buildParam(SearchParam searchParam){
        StringBuilder builder = new StringBuilder();

        // 入口：从搜索进入
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            builder.append("keyword="+searchParam.getKeyword());
        }

        // 入口：从分类进入
        if (searchParam.getCategory1Id() != null){
            builder.append("category1Id=").append(searchParam.getCategory1Id());
        }
        if (searchParam.getCategory2Id() != null){
            builder.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (searchParam.getCategory3Id() != null){
            builder.append("category3Id=").append(searchParam.getCategory3Id());
        }

        // 品牌
        if (!StringUtils.isEmpty(searchParam.getTrademark())){
            if (builder.length()>0) {
                builder.append("&trademark=").append(searchParam.getTrademark());
            }
        }

        // 平台属性
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0){
            Arrays.stream(props).forEach(item -> {
                if (builder.length() > 0){
                    builder.append("&props=").append(item);
                }
            });
        }

        return "/list.html?"+builder;
    }

}
