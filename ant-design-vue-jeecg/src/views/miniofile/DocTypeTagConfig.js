/**
 * 文档类型与标签字段配置
 */

/**
 * 密级等级配置
 * 等级顺序：公开(1) → 内部(2) → 秘密(3) → 机密(4) → 绝密(5)
 * 默认只开放 公开、内部 两级，如需开放更多在数据字典 classification_level 中修改
 */
export const CLASSIFICATION_LEVEL_DICT_CODE = 'classification_level'
export const DOC_TYPES = [
  {
    docTypeKey: '科技报告',
    label: '科技报告',
    tagFields: [
      { field: 'reportTaskSource', label: '任务来源/渠道', type: 'select', dictCode: 'doc_report_task_source' },
      { field: 'reportModel', label: '型号', type: 'select', dictCode: 'doc_report_model' },
      { field: 'reportMajor', label: '专业', type: 'select', dictCode: 'doc_report_major' },
      { field: 'reportCategory', label: '类别', type: 'select', dictCode: 'doc_report_category' },
    ]
  },
  {
    docTypeKey: '标准规范',
    label: '标准规范',
    tagFields: [
      { field: 'stdLevel', label: '标准层次', type: 'select', dictCode: 'standard_level' },
      { field: 'stdField', label: '标准领域', type: 'select', dictCode: 'doc_std_field' },
      { field: 'stdCivilMilitary', label: '军民属性', type: 'select', dictCode: 'doc_std_civil_military' },
      { field: 'stdAircraftType', label: '适用航空器类型', type: 'select', dictCode: 'doc_std_aircraft_type' },
      { field: 'stdType', label: '标准类型', type: 'select', dictCode: 'doc_std_type' },
    ]
  },
  {
    docTypeKey: '新闻资讯',
    label: '新闻资讯',
    tagFields: [
      { field: 'newsType', label: '新闻类型', type: 'select', dictCode: 'doc_news_type' },
      { field: 'newsMajor', label: '新闻专业', type: 'select', dictCode: 'doc_news_major' },
    ]
  },
  {
    docTypeKey: '规章制度',
    label: '规章制度',
    tagFields: [
      { field: 'ruleDomain', label: '业务域', type: 'select', dictCode: 'doc_rule_domain' },
      { field: 'ruleLevel', label: '制度等级', type: 'select', dictCode: 'doc_rule_level' },
      { field: 'ruleDept', label: '主责部门', type: 'select', dictCode: 'doc_rule_dept' },
    ]
  },
  {
    docTypeKey: '期刊文献',
    label: '期刊文献',
    tagFields: [
      { field: 'journalDb', label: '来源数据库', type: 'select', dictCode: 'doc_journal_db' },
      { field: 'journalSubject', label: '学科', type: 'select', dictCode: 'doc_journal_subject' },
      { field: 'journalLevel', label: '论文级别', type: 'select', dictCode: 'doc_journal_level' },
      { field: 'journalIndustry', label: '行业分类', type: 'select', dictCode: 'doc_journal_industry' },
    ]
  },
  {
    docTypeKey: '行政公文',
    label: '行政公文',
    tagFields: [
      { field: 'docFromUnit', label: '来文单位', type: 'input' },
      { field: 'docHostUnit', label: '主办单位', type: 'input' },
      { field: 'docDrafter', label: '拟稿人', type: 'input' },
      { field: 'docDraftDept', label: '拟稿部门', type: 'input' },
    ]
  },
]

export const IP_SUB_TYPES = [
  {
    docTypeKey: '知识产权-专利标签',
    label: '专利标签',
    tagFields: [
      { field: 'ipPatentInventor', label: '专利发明人', type: 'input' },
      { field: 'ipPatentApplicant', label: '专利申请人', type: 'input' },
    ]
  },
  {
    docTypeKey: '知识产权-科技成果',
    label: '科技成果',
    tagFields: [
      { field: 'ipAchievementDeclarant', label: '所级申报人', type: 'input' },
      { field: 'ipAchievementCompleter', label: '所级主要完成人', type: 'input' },
    ]
  },
  {
    docTypeKey: '知识产权-软件著作权',
    label: '软件著作权',
    tagFields: [
      { field: 'ipSoftDevList', label: '软著开发人列表', type: 'input' },
      { field: 'ipSoftRepresentative', label: '著作权人代表', type: 'input' },
    ]
  },
]

export function getTagFieldsByDocType(docType) {
  const all = [...DOC_TYPES, ...IP_SUB_TYPES]
  const found = all.find(item => item.docTypeKey === docType)
  return found ? found.tagFields : []
}

export function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}

export function getFileIcon(fileName) {
  if (!fileName) return 'file'
  const ext = fileName.split('.').pop().toLowerCase()
  const iconMap = {
    pdf: 'file-pdf', doc: 'file-word', docx: 'file-word',
    xls: 'file-excel', xlsx: 'file-excel',
    ppt: 'file-ppt', pptx: 'file-ppt',
    jpg: 'file-image', jpeg: 'file-image', png: 'file-image', gif: 'file-image',
    zip: 'file-zip', rar: 'file-zip', '7z': 'file-zip',
    txt: 'file-text',
    mp4: 'video-camera', avi: 'video-camera', mov: 'video-camera',
    mp3: 'audio', wav: 'audio',
  }
  return iconMap[ext] || 'file'
}
