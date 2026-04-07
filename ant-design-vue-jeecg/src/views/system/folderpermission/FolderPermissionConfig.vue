<template>
  <a-card :bordered="false">
    <!-- 用户选择区域 -->
    <div class="query-area">
      <div class="query-row">
        <span class="query-label">选择用户：</span>
        <j-select-user-by-dep
          ref="userSelect"
          v-model="selectedUserIdsStr"
          :multi="true"
          store="id"
          text="realname"
          @change="onUserChange"
        />
      </div>
    </div>

    <!-- 已选用户回显 -->
    <div v-if="selectedUsers.length > 0" style="margin-bottom:16px">
      <span class="query-label">已选用户：</span>
      <a-tag
        v-for="user in selectedUsers"
        :key="user.value"
        color="blue"
        closable
        @close="removeUser(user.value)"
        style="margin-bottom:4px"
      >
        {{ user.text }}
      </a-tag>
      <span style="color:#999;margin-left:8px;font-size:12px">（共 {{ selectedUsers.length }} 人）</span>
    </div>

    <!-- 分隔线 -->
    <a-divider v-if="selectedUsers.length > 0" />

    <!-- 文件夹授权区域 -->
    <div v-if="selectedUsers.length > 0">
      <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
        <span class="section-title">授权文件夹（{{ selectedFolderIds.length }}）</span>
        <div>
          <a-button type="primary" icon="plus" size="small" @click="showFolderSelect">添加文件夹</a-button>
          <a-button size="small" style="margin-left:8px" @click="removeSelectedFolders" :disabled="selectedRows.length === 0">
            批量移除
          </a-button>
        </div>
      </div>

      <!-- 已选文件夹表格 -->
      <a-table
        :columns="folderColumns"
        :dataSource="authorizedFolders"
        :loading="loadingPerms"
        rowKey="id"
        size="middle"
        :rowSelection="{ selectedRowKeys: selectedRows, onChange: onSelectChange }"
        :pagination="false"
      >
        <template slot="action" slot-scope="text, record">
          <a @click="removeFolder(record.id)">移除</a>
        </template>
      </a-table>

      <!-- 添加文件夹弹窗 -->
      <a-modal
        title="选择文件夹"
        :visible="folderSelectVisible"
        @ok="confirmFolderSelect"
        @cancel="folderSelectVisible = false"
        width="500px"
      >
        <div style="margin-bottom:8px">
          <a-input-search placeholder="搜索文件夹名称" v-model="folderSearchText" />
        </div>
        <a-checkbox-group v-model="tempSelectedFolderIds" style="width:100%">
          <div style="max-height:300px;overflow-y:auto">
            <a-checkbox
              v-for="folder in availableFolders"
              :key="folder.id"
              :value="folder.id"
              style="display:block;margin-bottom:8px"
            >
              <span v-if="folder.displayName">{{ folder.displayName }}</span>
              <span v-else>{{ folder.folderName }}</span>
              <span v-if="folder.description" style="color:#999;margin-left:4px">- {{ folder.description }}</span>
            </a-checkbox>
          </div>
          <a-empty v-if="availableFolders.length === 0" description="没有可添加的文件夹" />
        </a-checkbox-group>
      </a-modal>
    </div>

    <!-- 未选择用户时的提示 -->
    <a-empty v-else description="请先选择用户" />
  </a-card>
</template>

<script>
import { getAction, postAction } from '@/api/manage'
import JSelectUserByDep from '@/components/jeecgbiz/JSelectUserByDep'

export default {
  name: 'FolderPermissionConfig',
  components: { JSelectUserByDep },
  data() {
    return {
      allFolders: [],
      selectedUserIdsStr: '',
      selectedUsers: [],       // [{value: userId, text: realname}]
      authorizedFolders: [],
      selectedFolderIds: [],
      selectedRows: [],
      loadingPerms: false,
      folderSelectVisible: false,
      tempSelectedFolderIds: [],
      folderSearchText: '',
      folderColumns: [
        { title: '文件夹名称', dataIndex: 'folderName', width: 180 },
        { title: '显示名称', dataIndex: 'displayName' },
        { title: '描述', dataIndex: 'description', ellipsis: true },
        { title: '操作', width: 80, scopedSlots: { customRender: 'action' } },
      ],
    }
  },
  computed: {
    availableFolders() {
      let folders = this.allFolders.filter(f => !this.selectedFolderIds.includes(f.id))
      if (this.folderSearchText) {
        let keyword = this.folderSearchText.toLowerCase()
        folders = folders.filter(f =>
          (f.displayName && f.displayName.toLowerCase().includes(keyword)) ||
          (f.folderName && f.folderName.toLowerCase().includes(keyword)) ||
          (f.description && f.description.toLowerCase().includes(keyword))
        )
      }
      return folders
    },
  },
  created() {
    this.loadAllFolders()
  },
  methods: {
    loadAllFolders() {
      getAction('/minio/folder/list', { pageNo: 1, pageSize: 9999 }).then(res => {
        if (res.success) this.allFolders = (res.result.records || [])
      })
    },
    // JSelectUserByDep change 事件，返回逗号分隔的 userId
    onUserChange(idsStr) {
      if (!idsStr) {
        this.selectedUsers = []
        this.selectedFolderIds = []
        this.authorizedFolders = []
        return
      }
      // 从组件内部读取 textVals（对应的 realname）
      let textVals = ''
      if (this.$refs.userSelect) {
        textVals = this.$refs.userSelect.textVals || ''
      }
      let ids = idsStr.split(',')
      let texts = textVals ? textVals.split(',') : []
      // 如果 texts 数量不足，用 ID 补充
      this.selectedUsers = ids.map((id, i) => ({
        value: id,
        text: texts[i] || id
      }))
      this.loadCommonFolders()
    },
    // 从已选用户中移除某个
    removeUser(userId) {
      this.selectedUsers = this.selectedUsers.filter(u => u.value !== userId)
      // 同步更新 idsStr
      this.selectedUserIdsStr = this.selectedUsers.map(u => u.value).join(',')
      if (this.selectedUsers.length === 0) {
        this.selectedFolderIds = []
        this.authorizedFolders = []
      } else {
        this.loadCommonFolders()
      }
    },
    // 加载已选用户共有的文件夹（取交集）
    loadCommonFolders() {
      if (this.selectedUsers.length === 0) return
      this.loadingPerms = true
      let userIds = this.selectedUsers.map(u => u.value)
      // 逐个查询每个用户的权限，取交集
      let promises = userIds.map(uid =>
        getAction('/minio/folderPermission/list', { userId: uid }).then(res => {
          if (res.success && res.result) return res.result.map(item => item.folderId)
          return []
        })
      )
      Promise.all(promises).then(results => {
        // 取交集：所有用户都有的文件夹
        let common = results[0] || []
        for (let i = 1; i < results.length; i++) {
          common = common.filter(id => results[i].includes(id))
        }
        this.selectedFolderIds = common
        this.authorizedFolders = this.allFolders.filter(f => common.includes(f.id))
      }).finally(() => { this.loadingPerms = false })
    },
    onSelectChange(keys) {
      this.selectedRows = keys
    },
    showFolderSelect() {
      this.tempSelectedFolderIds = []
      this.folderSearchText = ''
      this.folderSelectVisible = true
    },
    confirmFolderSelect() {
      if (this.tempSelectedFolderIds.length === 0) {
        this.folderSelectVisible = false
        return
      }
      let newIds = [...this.selectedFolderIds, ...this.tempSelectedFolderIds]
      this.savePermissions(newIds)
      this.folderSelectVisible = false
    },
    removeFolder(folderId) {
      let newIds = this.selectedFolderIds.filter(id => id !== folderId)
      this.savePermissions(newIds)
    },
    removeSelectedFolders() {
      let newIds = this.selectedFolderIds.filter(id => !this.selectedRows.includes(id))
      this.savePermissions(newIds)
      this.selectedRows = []
    },
    // 批量保存：串行为所有已选用户设置相同的文件夹权限（避免死锁）
    async savePermissions(folderIds) {
      let userIds = this.selectedUsers.map(u => u.value)
      let failCount = 0
      for (let uid of userIds) {
        try {
          let res = await postAction('/minio/folderPermission/save', { userId: uid, folderIds })
          if (!res.success) failCount++
        } catch (e) {
          failCount++
        }
      }
      if (failCount === 0) {
        this.$message.success('保存成功（' + userIds.length + '个用户）')
        this.selectedFolderIds = folderIds
        this.authorizedFolders = this.allFolders.filter(f => folderIds.includes(f.id))
      } else {
        this.$message.error(failCount + '个用户保存失败')
      }
    },
  },
}
</script>

<style scoped>
.query-area {
  margin-bottom: 16px;
}
.query-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.query-label {
  font-weight: 500;
  margin-right: 8px;
  white-space: nowrap;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
}
</style>
