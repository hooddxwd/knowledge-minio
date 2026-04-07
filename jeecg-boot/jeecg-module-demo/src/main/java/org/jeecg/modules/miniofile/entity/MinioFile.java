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
@ApiModel(value = "minio_file对象", description = "MinIO文件")
@TableName("minio_file")
public class MinioFile extends JeecgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "文件夹ID")
    private String folderId;

    @Excel(name = "文档名称", width = 30)
    @ApiModelProperty(value = "文档名称")
    private String fileName;

    @Excel(name = "文档密级", width = 15)
    @ApiModelProperty(value = "文档密级")
    private String classificationLevel;

    @ApiModelProperty(value = "摘要")
    private String summary;

    @Excel(name = "作者", width = 20)
    @ApiModelProperty(value = "作者")
    private String author;

    @Excel(name = "技术体系", width = 20)
    @ApiModelProperty(value = "技术体系")
    private String techSystem;

    @Excel(name = "文档类型", width = 15)
    @ApiModelProperty(value = "文档类型")
    private String docType;

    @Excel(name = "文档来源", width = 20)
    @ApiModelProperty(value = "文档来源")
    private String docSource;

    @Excel(name = "年度", width = 10)
    @ApiModelProperty(value = "年度")
    private String year;

    @ApiModelProperty(value = "文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty(value = "MIME类型")
    private String fileType;

    @ApiModelProperty(value = "MinIO对象名")
    private String minioObjectName;

    // ========== 科技报告标签 ==========
    @ApiModelProperty(value = "任务来源/渠道")
    private String reportTaskSource;
    @ApiModelProperty(value = "型号")
    private String reportModel;
    @ApiModelProperty(value = "专业")
    private String reportMajor;
    @ApiModelProperty(value = "类别")
    private String reportCategory;

    // ========== 标准规范标签 ==========
    @ApiModelProperty(value = "标准层次")
    private String stdLevel;
    @ApiModelProperty(value = "标准领域")
    private String stdField;
    @ApiModelProperty(value = "军民属性")
    private String stdCivilMilitary;
    @ApiModelProperty(value = "适用航空器类型")
    private String stdAircraftType;
    @ApiModelProperty(value = "标准类型")
    private String stdType;

    // ========== 新闻资讯标签 ==========
    @ApiModelProperty(value = "新闻类型")
    private String newsType;
    @ApiModelProperty(value = "新闻专业")
    private String newsMajor;

    // ========== 规章制度标签 ==========
    @ApiModelProperty(value = "业务域")
    private String ruleDomain;
    @ApiModelProperty(value = "制度等级")
    private String ruleLevel;
    @ApiModelProperty(value = "主责部门")
    private String ruleDept;

    // ========== 期刊文献标签 ==========
    @ApiModelProperty(value = "来源数据库")
    private String journalDb;
    @ApiModelProperty(value = "学科")
    private String journalSubject;
    @ApiModelProperty(value = "论文级别")
    private String journalLevel;
    @ApiModelProperty(value = "行业分类")
    private String journalIndustry;

    // ========== 行政公文标签 ==========
    @ApiModelProperty(value = "来文单位")
    private String docFromUnit;
    @ApiModelProperty(value = "主办单位")
    private String docHostUnit;
    @ApiModelProperty(value = "拟稿人")
    private String docDrafter;
    @ApiModelProperty(value = "拟稿部门")
    private String docDraftDept;

    // ========== 知识产权标签 ==========
    @ApiModelProperty(value = "专利发明人")
    private String ipPatentInventor;
    @ApiModelProperty(value = "专利申请人")
    private String ipPatentApplicant;
    @ApiModelProperty(value = "所级申报人")
    private String ipAchievementDeclarant;
    @ApiModelProperty(value = "所级主要完成人")
    private String ipAchievementCompleter;
    @ApiModelProperty(value = "软著开发人列表")
    private String ipSoftDevList;
    @ApiModelProperty(value = "著作权人代表")
    private String ipSoftRepresentative;
}
