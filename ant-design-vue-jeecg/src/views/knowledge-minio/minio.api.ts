import { defHttp } from '/@/utils/http/axios';

enum Api {
  // 文件管理（数据库驱动）
  fileList = '/minio/file/list',
  upload = '/minio/file/upload',
  download = '/minio/file/download',
  delete = '/minio/file/delete',
  deleteBatch = '/minio/file/deleteBatch',
  edit = '/minio/file/edit',
  tags = '/minio/file/tags',
  // Bucket管理
  bucketList = '/minio/bucket/listAll',
  bucketSync = '/minio/bucket/sync',
}

/**
 * 获取文件列表（分页）
 */
export const getFileList = (params: any) => defHttp.get({ url: Api.fileList, params });

/**
 * 上传文件URL
 */
export const getUploadUrl = () => Api.upload;

/**
 * 删除文件
 */
export const deleteFile = (params: { id: string }) =>
  defHttp.delete({ url: Api.delete, params }, { joinParamsToUrl: true });

/**
 * 批量删除
 */
export const deleteBatch = (params: { ids: string }) =>
  defHttp.delete({ url: Api.deleteBatch, params }, { joinParamsToUrl: true });

/**
 * 编辑文件信息（标签、备注）
 */
export const editFile = (params: any) => defHttp.put({ url: Api.edit, params });

/**
 * 获取所有标签
 */
export const getAllTags = () => defHttp.get({ url: Api.tags });

/**
 * 获取所有启用的Bucket
 */
export const getBucketList = () => defHttp.get({ url: Api.bucketList });

/**
 * 同步MinIO Bucket到数据库
 */
export const syncBuckets = () => defHttp.post({ url: Api.bucketSync });
