package cloud.makeronbean.gmall.model.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description="登录对象")
public class LoginVo {

    @ApiModelProperty(value = "手机号-移动端")
    private String phone;

    @ApiModelProperty(value = "密码-移动端")
    private String password;

    @ApiModelProperty(value = "登录名-pc端")
    private String loginName;

    @ApiModelProperty(value = "密码-pc端")
    private String passwd;

    @ApiModelProperty(value = "IP")
    private String ip;
}
