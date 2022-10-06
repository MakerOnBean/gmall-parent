package cloud.makeronbean.gmall.product.service.impl;

import cloud.makeronbean.gmall.model.product.BaseTrademark;
import cloud.makeronbean.gmall.product.mapper.BaseTrademarkMapper;
import cloud.makeronbean.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author makeronbean
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper,BaseTrademark> implements BaseTrademarkService {

    /**
     * 分页查询品牌列表
     */
    @Override
    public Page<BaseTrademark> getBaseTrademarkPage(Page<BaseTrademark> page) {
        // 排序
        LambdaQueryWrapper<BaseTrademark> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BaseTrademark::getId);
        return baseMapper.selectPage(page,wrapper);
    }
}
