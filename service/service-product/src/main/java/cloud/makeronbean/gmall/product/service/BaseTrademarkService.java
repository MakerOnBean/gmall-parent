package cloud.makeronbean.gmall.product.service;

import cloud.makeronbean.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author makeronbean
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    /**
     * 分页查询品牌列表
     */
    Page<BaseTrademark> getBaseTrademarkPage(Page<BaseTrademark> page);


}
