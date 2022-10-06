package cloud.makeronbean.gmall.product.service.impl;

import cloud.makeronbean.gmall.model.product.BaseCategoryTrademark;
import cloud.makeronbean.gmall.model.product.BaseTrademark;
import cloud.makeronbean.gmall.model.product.CategoryTrademarkVo;
import cloud.makeronbean.gmall.product.mapper.BaseCategoryTrademarkMapper;
import cloud.makeronbean.gmall.product.mapper.BaseTrademarkMapper;
import cloud.makeronbean.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@Service
public class BaseCategoryTrademarkServiceImpl
        extends ServiceImpl<BaseCategoryTrademarkMapper,BaseCategoryTrademark>
        implements BaseCategoryTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;


    /**
     * 根据三级分类id查询所有品牌信息
     */
    @Override
    public List<BaseTrademark> findTrademarkList(Integer category3Id) {
        return baseTrademarkMapper.findTrademarkList(category3Id);
    }


    /**
     * 删除三级分类和品牌的关联关系
     */
    @Override
    public void removeByCategory3IdAndTrademarkId(Long category3Id, Long trademarkId) {
        LambdaQueryWrapper<BaseCategoryTrademark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCategoryTrademark::getCategory3Id,category3Id).eq(BaseCategoryTrademark::getTrademarkId,trademarkId);
        baseCategoryTrademarkMapper.delete(wrapper);
    }


    /**
     * 添加品牌时，获取可选择的品牌列表
     */
    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        // 查询已经有哪些品牌
        LambdaQueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategoryTrademarkLambdaQueryWrapper.eq(BaseCategoryTrademark::getCategory3Id,category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkLambdaQueryWrapper);

        // 排除已有的品牌，查询未有的品牌
        LambdaQueryWrapper<BaseTrademark> baseTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            List<Long> collect = baseCategoryTrademarkList.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());
            baseTrademarkLambdaQueryWrapper.notIn(BaseTrademark::getId,collect);
        }
        return baseTrademarkMapper.selectList(baseTrademarkLambdaQueryWrapper);
    }


    /**
     * 保存添加的品牌与三级分类对应关系
     */
    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        Long category3Id = categoryTrademarkVo.getCategory3Id();
        List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream().map(trademarkId -> {
            BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
            baseCategoryTrademark.setTrademarkId(trademarkId);
            baseCategoryTrademark.setCategory3Id(category3Id);
            return baseCategoryTrademark;
        }).collect(Collectors.toList());
        this.saveBatch(baseCategoryTrademarkList);
    }
}
