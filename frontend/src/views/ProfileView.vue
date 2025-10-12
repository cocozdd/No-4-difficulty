<template>
  <div class="profile-view">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <el-button type="primary" @click="refreshProfile">刷新</el-button>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="昵称">
          {{ userStore.nickname || '未填写' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag>{{ userStore.role || '游客' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ userStore.phone || '未绑定' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/userStore';

const userStore = useUserStore();

const refreshProfile = async () => {
  try {
    await userStore.loadProfile();
    ElMessage.success('已刷新');
  } catch (error) {
    ElMessage.error('刷新失败');
  }
};
</script>

<style scoped>
.profile-view {
  padding: 24px;
  display: flex;
  justify-content: center;
}

.profile-card {
  width: 420px;
  border-radius: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
