import { getAction, postAction, deleteAction, putAction } from '@/api/manage';

const minioApi = {
  // 文件管理（数据库驱动）
  fileList: '/minio/file/list',
  upload: '/minio/file/upload',
  download: '/minio/file/download',
  delete: '/minio/file/delete',
  deleteBatch: '/minio/file/deleteBatch',
  edit: '/minio/file/edit',
  tags: '/minio/file/tags',
  // Bucket管理
  bucketList: '/minio/bucket/listAll',
  bucketSync: '/minio/bucket/sync',
};

/**
 * 获取文件列表（分页）
 */
export function getFileList(params) {
  return getAction(minioApi.fileList, params);
}

/**
 * 上传文件URL
 */
export function getUploadUrl() {
  return minioApi.upload;
}

/**
 * 删除文件
 */
export function deleteFile(params) {
  return deleteAction(minioApi.delete, params);
}

/**
 * 批量删除
 */
export function deleteBatch(params) {
  return deleteAction(minioApi.deleteBatch, params);
}

/**
 * 编辑文件信息（标签、备注）
 */
export function editFile(params) {
  return putAction(minioApi.edit, params);
}

/**
 * 获取所有标签
 */
export function getAllTags() {
  return getAction(minioApi.tags, null);
}

/**
 * 获取所有启用的Bucket
 */
export function getBucketList() {
  return getAction(minioApi.bucketList, null);
}

/**
 * 同步MinIO Bucket到数据库
 */
export function syncBuckets() {
  return postAction(minioApi.bucketSync, null);
}

export default {
  getFileList,
  getUploadUrl,
  deleteFile,
  deleteBatch,
  editFile,
  getAllTags,
  getBucketList,
  syncBuckets,
};
