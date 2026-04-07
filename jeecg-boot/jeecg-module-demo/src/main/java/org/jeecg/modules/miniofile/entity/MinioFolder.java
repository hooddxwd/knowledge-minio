package org.jeecg.modules.miniofile.entity;

import java.io.Serializable;
import org.jeecg.common.system.base.entity.JeecgEntity;
import org.jeecgframework.poi.excel.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "minio_folder对象", description = "MinIO文件夹")
@TableName("minio_folder")
public class MinioFolder extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "文件夹名称(Bucket名)", width = 25)
    @ApiModelProperty(value = "文件夹名称（Bucket名，英文/数字/短横线）")
    private String folderName;

    @Excel(name = "显示名称", width = 25)
    @ApiModelProperty(value = "显示名称")
    private String displayName;

    @Excel(name = "文件夹描述", width = 30)
    @ApiModelProperty(value = "文件夹描述")
    private String description;
}
