<template>
  <div class="flash-sale-view">
    <section class="page-header">
      <h1>Flash Sale 秒杀专区</h1>
      <p>限时限量，手快有，手慢无。</p>
    </section>

    <el-card v-if="isAdmin" shadow="never" class="create-card">
      <template #header>
        <div class="card-header">
          <span>创建秒杀活动</span>
          <el-tag type="warning" size="small">管理员</el-tag>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" size="small">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="活动标题，例如：限量耳机秒杀" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="活动亮点/规则说明" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="秒杀价" prop="flashPrice">
              <el-input-number v-model="form.flashPrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="库存" prop="totalStock">
          <el-input-number v-model="form.totalStock" :min="1" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="createLoading" @click="handleCreate">
            发布秒杀
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>进行中 / 即将开始的活动</span>
          <el-button type="primary" link @click="loadItems" :loading="loading">
            刷新
          </el-button>
        </div>
      </template>
      <el-empty v-if="!items.length" description="暂时没有秒杀活动" />
      <el-space
        v-else
        direction="vertical"
        :size="16"
        wrap
        style="width: 100%"
      >
        <el-card
          v-for="item in items"
          :key="item.id"
          class="sale-card"
          :class="statusClass(item)"
        >
          <div class="sale-card__header">
            <div>
              <h3>{{ item.title }}</h3>
              <p class="sale-desc" v-if="item.description">{{ item.description }}</p>
            </div>
            <el-tag :type="tagType(item.status)">
              {{ statusLabel(item.status) }}
            </el-tag>
          </div>
          <div class="sale-card__body">
            <div class="price-block">
              <span class="flash-price">¥ {{ item.flashPrice.toFixed(2) }}</span>
              <span class="original-price">原价 ¥ {{ item.originalPrice.toFixed(2) }}</span>
            </div>
            <div class="meta-block">
              <span>库存：{{ item.remainingStock }} / {{ item.totalStock }}</span>
              <span>开始：{{ formatTime(item.startTime) }}</span>
              <span>结束：{{ formatTime(item.endTime) }}</span>
            </div>
            <p class="countdown" v-if="item.status !== 'ENDED'">
              {{ countdownText(item) }}
            </p>
          </div>
          <div class="sale-card__footer">
            <el-button
              type="primary"
              :disabled="!canPurchase(item)"
              :loading="purchaseLoading === item.id"
              @click="handlePurchase(item)"
            >
              马上抢购
            </el-button>
          </div>
        </el-card>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus';
import { useRouter } from 'vue-router';
import {
  createFlashSaleItem,
  fetchFlashSaleItems,
  FlashSaleItem,
  purchaseFlashSaleItem
} from '../apis/flashSale';
import { useUserStore } from '../stores/userStore';

const router = useRouter();
const userStore = useUserStore();

const isAdmin = computed(() => userStore.role === 'ADMIN');
const isLoggedIn = computed(() => userStore.isAuthenticated);

const now = ref(Date.now());
let timer: number | null = null;

onMounted(() => {
  loadItems();
  timer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
  }
});

const items = ref<FlashSaleItem[]>([]);
const loading = ref(false);
const purchaseLoading = ref<number | null>(null);

const loadItems = async () => {
  loading.value = true;
  try {
    const { data } = await fetchFlashSaleItems();
    items.value = data;
  } catch {
    ElMessage.error('加载秒杀活动失败');
  } finally {
    loading.value = false;
  }
};

type CreateForm = {
  title: string;
  description: string;
  originalPrice: number | null;
  flashPrice: number | null;
  totalStock: number | null;
  startTime: string | null;
  endTime: string | null;
};

const defaultForm = (): CreateForm => ({
  title: '',
  description: '',
  originalPrice: null,
  flashPrice: null,
  totalStock: null,
  startTime: null,
  endTime: null
});

const form = reactive<CreateForm>(defaultForm());
const formRef = ref<FormInstance>();
const createLoading = ref(false);

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  originalPrice: [{ required: true, message: '请输入原价', trigger: 'change' }],
  flashPrice: [{ required: true, message: '请输入秒杀价', trigger: 'change' }],
  totalStock: [{ required: true, message: '请输入库存', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
};

const resetForm = () => {
  Object.assign(form, defaultForm());
};

const handleCreate = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    if (!form.startTime || !form.endTime) {
      ElMessage.warning('请选择开始和结束时间');
      return;
    }
    if (new Date(form.endTime) <= new Date(form.startTime)) {
      ElMessage.warning('结束时间必须晚于开始时间');
      return;
    }
    createLoading.value = true;
    try {
      await createFlashSaleItem({
        title: form.title,
        description: form.description,
        originalPrice: Number(form.originalPrice),
        flashPrice: Number(form.flashPrice),
        totalStock: Number(form.totalStock),
        startTime: form.startTime,
        endTime: form.endTime
      });
      ElMessage.success('已发布秒杀活动');
      resetForm();
      loadItems();
    } catch (error: any) {
      const message = error?.response?.data?.message ?? '创建秒杀活动失败';
      ElMessage.error(message);
    } finally {
      createLoading.value = false;
    }
  });
};

const canPurchase = (item: FlashSaleItem) => {
  if (!isLoggedIn.value) {
    return false;
  }
  if (item.status !== 'RUNNING') {
    return false;
  }
  return item.remainingStock > 0;
};

const handlePurchase = async (item: FlashSaleItem) => {
  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: '/flash-sale' } });
    return;
  }
  purchaseLoading.value = item.id;
  try {
    const { data } = await purchaseFlashSaleItem(item.id);
    ElMessage.success(`抢购成功，订单号：${data.orderId}`);
    await loadItems();
  } catch (error: any) {
    const message = error?.response?.data?.message ?? '抢购失败，请稍后重试';
    ElMessageBox.alert(message, '抢购结果', { type: 'warning' });
  } finally {
    purchaseLoading.value = null;
  }
};

const tagType = (status: FlashSaleItem['status']) => {
  switch (status) {
    case 'RUNNING':
      return 'success';
    case 'SCHEDULED':
      return 'warning';
    default:
      return 'info';
  }
};

const statusLabel = (status: FlashSaleItem['status']) => {
  switch (status) {
    case 'RUNNING':
      return '进行中';
    case 'SCHEDULED':
      return '未开始';
    default:
      return '已结束';
  }
};

const statusClass = (item: FlashSaleItem) => {
  return {
    'is-running': item.status === 'RUNNING',
    'is-ended': item.status === 'ENDED'
  };
};

const formatTime = (value: string) => {
  const time = new Date(value);
  return Number.isNaN(time.getTime()) ? value : time.toLocaleString();
};

const countdownText = (item: FlashSaleItem) => {
  const start = new Date(item.startTime).getTime();
  const end = new Date(item.endTime).getTime();
  const current = now.value;

  if (item.status === 'SCHEDULED') {
    return `距离开始还有 ${formatDelta(start - current)}`;
  }
  if (item.status === 'RUNNING') {
    return `距离结束还有 ${formatDelta(end - current)}`;
  }
  return '活动已结束';
};

const formatDelta = (delta: number) => {
  if (delta <= 0) {
    return '0秒';
  }
  const seconds = Math.floor(delta / 1000);
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const parts: string[] = [];
  if (d) parts.push(`${d}天`);
  if (h) parts.push(`${h}小时`);
  if (m) parts.push(`${m}分钟`);
  parts.push(`${s}秒`);
  return parts.join('');
};
</script>

<style scoped>
.flash-sale-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px;
}

.page-header {
  background: linear-gradient(135deg, #f97316 0%, #ef4444 100%);
  color: #fff;
  padding: 32px;
  border-radius: 16px;
  text-align: center;
}

.page-header h1 {
  margin-bottom: 8px;
}

.create-card {
  border: 1px solid #fed7aa;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sale-card {
  border-radius: 14px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.sale-card.is-running {
  border: 1px solid #f97316;
  box-shadow: 0 10px 30px rgba(249, 115, 22, 0.15);
}

.sale-card.is-ended {
  filter: grayscale(0.5);
}

.sale-card:hover {
  transform: translateY(-4px);
}

.sale-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.sale-card__header h3 {
  margin: 0;
}

.sale-desc {
  color: #6b7280;
  margin-top: 4px;
}

.sale-card__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.price-block {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.flash-price {
  font-size: 24px;
  font-weight: 700;
  color: #ef4444;
}

.original-price {
  text-decoration: line-through;
  color: #94a3b8;
}

.meta-block {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #475569;
  font-size: 14px;
}

.countdown {
  color: #dc2626;
  font-weight: 600;
}

.sale-card__footer {
  display: flex;
  justify-content: flex-end;
}

.refresh-icon {
  margin-right: 4px;
}
</style>
