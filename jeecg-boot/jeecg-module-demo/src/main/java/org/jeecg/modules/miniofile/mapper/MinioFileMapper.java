package org.jeecg.modules.miniofile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.miniofile.entity.MinioFile;
import java.util.List;
import java.util.Map;

public interface MinioFileMapper extends BaseMapper<MinioFile> {

    @Select("SELECT doc_type AS docType, COUNT(*) AS count FROM minio_file WHERE folder_id = #{folderId} GROUP BY doc_type")
    List<Map<String, Object>> countByDocType(@Param("folderId") String folderId);

    @Select("SELECT ${tagName} AS tagValue, COUNT(*) AS count FROM minio_file WHERE folder_id = #{folderId} AND doc_type = #{docType} AND ${tagName} IS NOT NULL AND ${tagName} != '' GROUP BY ${tagName}")
    List<Map<String, Object>> countByTag(@Param("folderId") String folderId, @Param("docType") String docType, @Param("tagName") String tagName);
}
