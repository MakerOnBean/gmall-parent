package cloud.makeronbean.gmall.user.controller;

import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.common.util.IpUtil;
import cloud.makeronbean.gmall.model.user.LoginVo;
import cloud.makeronbean.gmall.model.user.UserInfo;
import cloud.makeronbean.gmall.user.service.UserService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {
    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 退出登陆页面
     */
    @GetMapping("/logout")
    public Result logout(@RequestHeader String token){
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX+token);
        return Result.ok();
    }


    /**
     * 登陆
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginVo loginVo, HttpServletRequest request){
        // 登陆认证
        UserInfo info = userService.login(loginVo);

        // 判断是否存在该用户
        if (info != null) {
            // 生成token
            String token = UUID.randomUUID().toString().replaceAll("-", "");

            // 获取请求的ip
            String ip = IpUtil.getIpAddress(request);

            // 封装redis中作为值存储的数据
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId",info.getId());
            jsonObject.put("ip",ip);

            // 缓存到 redis 中
            redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX +token,jsonObject.toJSONString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            // 返回页面需要的数据
            Map<String,Object> resultMap = new HashMap<>(2);
            resultMap.put("nickName",info.getNickName());
            resultMap.put("token",token);
            return Result.ok(resultMap);


        } else {
            return Result.fail().message("用户名或密码不正确");
        }
    }
}
