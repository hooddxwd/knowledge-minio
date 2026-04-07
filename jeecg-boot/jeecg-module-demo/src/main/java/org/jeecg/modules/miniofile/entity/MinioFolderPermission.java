package org.jeecg.modules.miniofile.entity;

import java.io.Serializable;
import org.jeecg.common.system.base.entity.JeecgEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "minio_folder_permission对象", description = "MinIO文件夹权限")
@TableName("minio_folder_permission")
public class MinioFolderPermission extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "文件夹ID")
    private String folderId;
}
