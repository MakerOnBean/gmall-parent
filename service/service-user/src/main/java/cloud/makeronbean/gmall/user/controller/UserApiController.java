package cloud.makeronbean.gmall.user.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.common.util.AuthContextHolder;
import cloud.makeronbean.gmall.model.user.UserAddress;
import cloud.makeronbean.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Autowired
    private UserService userService;

    @GetMapping("/userAddress/auth/findUserAddressList")
    public Result findUserAddressList(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userService.findUserAddressByUserId(Long.parseLong(userId));
        return Result.ok(userAddressList);
    }


    /**
     * 查询用户地址列表
     */
    @GetMapping("/inner/findUserAddressByUserId/{userId}")
    public List<UserAddress> findUserAddressByUserId(@PathVariable Long userId){
        return userService.findUserAddressByUserId(userId);
    }

}
