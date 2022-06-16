package org.jeecg.generator.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.entity.BaseEntity;

/**
* <p>
* 
* </p>
*
* @author system
* @since 2021-08-04
*/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("atg_user_infos")
@ApiModel(value="UserInfos对象", description="")
public class UserInfos extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户角色")
    private String userRole;

    @ApiModelProperty(value = "用户状态")
    private String userStatus;

    @ApiModelProperty(value = "用户密码")
    private String userPassword;

    @ApiModelProperty(value = "用户邮箱")
    private String userEmail;

    @ApiModelProperty(value = "用户名")
    private String userName;

    @ApiModelProperty(value = "手机号")
    private String userPhone;

    @ApiModelProperty(value = "用户头像")
    private String userImg;


}
