package cloud.makeronbean.gmall.product.service;

import cloud.makeronbean.gmall.model.product.BaseCategoryTrademark;
import cloud.makeronbean.gmall.model.product.BaseTrademark;
import cloud.makeronbean.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryTrademarkService extends IService<BaseCategoryTrademark> {
    /**
     * 根据三级分类id查询所有品牌信息
     */
    List<BaseTrademark> findTrademarkList(Integer category3Id);


    /**
     * 删除三级分类和品牌的关联关系
     */
    void removeByCategory3IdAndTrademarkId(Long category3Id, Long trademarkId);

    /**
     * 添加品牌时，获取可选择的品牌列表
     */
    List<BaseTrademark> findCurrentTrademarkList(Long category3Id);

    /**
     * 保存添加的品牌与三级分类对应关系
     */
    void save(CategoryTrademarkVo categoryTrademarkVo);
}
