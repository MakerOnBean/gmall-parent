package cloud.makeronbean.gmall.product.mapper;

import cloud.makeronbean.gmall.model.product.SkuInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author makeronbean
 */
@Mapper
public interface SkuInfoMapper extends BaseMapper<SkuInfo> {
    List<Long> selectIdList();
}
