package cloud.makeronbean.gmall.product.mapper;

import cloud.makeronbean.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author makeronbean
 */
@Mapper
public interface BaseTrademarkMapper extends BaseMapper<BaseTrademark> {

    /**
     * 根据三级分类id查询所有品牌信息
     */
    List<BaseTrademark> findTrademarkList(Integer category3Id);
}
