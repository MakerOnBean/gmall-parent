package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.UserDegradeFeignClientImpl;
import cloud.makeronbean.gmall.model.user.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author makeronbean
 */
@Component
@FeignClient(value = "service-user",fallback = UserDegradeFeignClientImpl.class)
public interface UserFeignClient {
    /**
     * 查询用户地址列表
     */
    @GetMapping("/api/user/inner/findUserAddressByUserId/{userId}")
    List<UserAddress> findUserAddressByUserId(@PathVariable Long userId);
}
