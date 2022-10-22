package cloud.makeronbean.gmall.product.mapper;

import cloud.makeronbean.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author makeronbean
 */
@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {

    /**
     * 根据分类查询平台属性
     */
    List<BaseAttrInfo> selectAttrInfoList(@Param("category1Id") Long category1Id, @Param("category2Id") Long category2Id, @Param("category3Id") Long category3Id);

    /**
     * 根据skuId查询平台属性
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
