<template>
  <div class="my-goods-view">
    <el-row :gutter="24">
      <el-col :xs="24" :md="10">
        <el-card class="form-card">
          <template #header>
            <div class="card-header">
              <span>Create New Listing</span>
              <el-tag type="info">Practice mode</el-tag>
            </div>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="Title" prop="title">
              <el-input
                v-model="form.title"
                placeholder="Example: Lightly used textbook"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="Category" prop="category">
              <el-select v-model="form.category" placeholder="Select category">
                <el-option label="Electronics" value="Electronics" />
                <el-option label="Books" value="Books" />
                <el-option label="Daily" value="Daily" />
              </el-select>
            </el-form-item>
            <el-form-item label="Price (CNY)" prop="price">
              <el-input-number v-model="form.price" :min="0.01" :step="1" />
            </el-form-item>
            <el-form-item label="Quantity" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :step="1" />
            </el-form-item>
            <el-form-item label="Description" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                placeholder="Add detail such as condition, included accessories, etc."
              />
            </el-form-item>
            <el-form-item label="Cover Image">
              <div class="upload-row">
                <el-upload
                  class="cover-upload"
                  :show-file-list="false"
                  :auto-upload="false"
                  accept="image/*"
                  :before-upload="beforeGoodsImageUpload"
                  :on-change="handleGoodsImageChange"
                  :disabled="goodsImageUploading"
                >
                  <el-button type="primary" :loading="goodsImageUploading">
                    <template #icon>
                      <el-icon><PictureFilled /></el-icon>
                    </template>
                    Upload Image
                  </el-button>
                </el-upload>
                <el-image
                  v-if="form.coverImageUrl"
                  :src="form.coverImageUrl"
                  fit="cover"
                  class="cover-preview"
                />
                <el-button v-if="form.coverImageUrl" text type="danger" @click="removeGoodsImage">
                  Remove
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submitForm">Submit</el-button>
              <el-button @click="resetForm">Reset</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <div class="list-header">
          <h2>My Listings</h2>
          <el-button link @click="refreshList">Refresh</el-button>
        </div>
        <el-empty
          v-if="!goodsStore.myGoods.length"
          description="No listings yet. Use the form on the left to create one."
        />
        <el-row v-else :gutter="16">
          <el-col v-for="item in goodsStore.myGoods" :key="item.id" :xs="24" :sm="12">
            <el-card shadow="hover" class="goods-card">
              <img
                :src="item.coverImageUrl || getFallbackImageByCategory(item.category)"
                alt=""
                class="goods-cover"
              />
              <div class="goods-body">
                <div class="title-line">
                  <h3>{{ item.title }}</h3>
                  <div class="tag-group">
                    <el-tag :type="statusTagType[item.status]" size="small">{{ statusLabel[item.status] }}</el-tag>
                    <el-tag v-if="item.sold" type="warning" size="small">Sold</el-tag>
                  </div>
                </div>
                <p class="price">CNY {{ item.price }}</p>
                <p class="stock">Available: {{ item.quantity }}</p>
                <p class="description">{{ item.description }}</p>
                <p
                  v-if="item.status !== 'APPROVED'"
                  :class="['status-hint', item.status === 'PENDING_REVIEW' ? 'pending' : 'rejected']"
                >
                  {{
                    item.status === 'PENDING_REVIEW'
                      ? 'Waiting for admin review'
                      : 'Rejected by admin. Edit and resubmit.'
                  }}
                </p>
                <el-button link @click="goDetail(item.id)">View detail</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, type UploadFile, type UploadProps } from 'element-plus';
import { PictureFilled } from '@element-plus/icons-vue';
import { useGoodsStore } from '../stores/goodsStore';
import { getFallbackImageByCategory } from '../utils/image';
import type { GoodsStatus } from '../apis/goods';
import { uploadGoodsImage } from '../apis/upload';

const goodsStore = useGoodsStore();
const router = useRouter();

const formRef = ref<FormInstance>();
const submitting = ref(false);
const goodsImageUploading = ref(false);
const GOODS_MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const form = reactive({
  title: '',
  description: '',
  category: '',
  price: 1,
  quantity: 1,
  coverImageUrl: ''
});

const rules: FormRules = {
  title: [{ required: true, message: 'Title is required', trigger: 'blur' }],
  category: [{ required: true, message: 'Select a category', trigger: 'change' }],
  price: [{ required: true, message: 'Price is required', trigger: 'change' }],
  quantity: [{ required: true, message: 'Quantity is required', trigger: 'change' }],
  description: [{ required: true, message: 'Description is required', trigger: 'blur' }]
};

onMounted(() => {
  goodsStore.loadMyGoods();
});

const refreshList = () => {
  goodsStore.loadMyGoods();
};

const beforeGoodsImageUpload: UploadProps['beforeUpload'] = () => false;

const handleGoodsImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const raw = uploadFile.raw;
  if (!raw) {
    return;
  }
  if (!raw.type.startsWith('image/')) {
    ElMessage.error('Only image files are allowed');
    return;
  }
  if (raw.size > GOODS_MAX_IMAGE_SIZE) {
    ElMessage.error('Image size cannot exceed 5MB');
    return;
  }
  goodsImageUploading.value = true;
  try {
    const { data } = await uploadGoodsImage(raw);
    form.coverImageUrl = data.url;
    ElMessage.success('Image uploaded');
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Failed to upload image';
    ElMessage.error(message);
  } finally {
    goodsImageUploading.value = false;
  }
};

const removeGoodsImage = () => {
  form.coverImageUrl = '';
};

const submitForm = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const payload = { ...form };
      if (!payload.coverImageUrl) {
        payload.coverImageUrl = getFallbackImageByCategory(payload.category);
      }
      const goods = await goodsStore.createGoods(payload);
      ElMessage.success('Listing submitted for review');
      resetForm();
      goodsStore.loadMyGoods();
      router.push(`/goods/${goods.id}`);
    } catch (error) {
      ElMessage.error('Failed to create listing');
    } finally {
      submitting.value = false;
    }
  });
};

const resetForm = () => {
  form.title = '';
  form.description = '';
  form.category = '';
  form.price = 1;
  form.quantity = 1;
  form.coverImageUrl = '';
};

const goDetail = (id: number) => router.push(`/goods/${id}`);

const statusLabel: Record<GoodsStatus, string> = {
  PENDING_REVIEW: 'Pending Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected'
};

const statusTagType: Record<GoodsStatus, 'info' | 'success' | 'danger' | 'warning'> = {
  PENDING_REVIEW: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
};
</script>

<style scoped>
.my-goods-view {
  padding: 24px;
}

.form-card {
  border-radius: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-preview {
  width: 96px;
  height: 96px;
  border-radius: 12px;
  object-fit: cover;
}

.goods-card {
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 16px;
}

.goods-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.goods-body {
  margin-top: 12px;
}

.title-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.tag-group {
  display: flex;
  gap: 6px;
  align-items: center;
}

.price {
  color: #1e90ff;
  font-weight: 600;
  margin-bottom: 8px;
}

.stock {
  color: #4b5563;
  margin-bottom: 8px;
}

.description {
  min-height: 40px;
  color: #6b7280;
}

.status-hint {
  margin-bottom: 8px;
  font-size: 12px;
}

.status-hint.pending {
  color: #f59e0b;
}

.status-hint.rejected {
  color: #ef4444;
}
</style>



