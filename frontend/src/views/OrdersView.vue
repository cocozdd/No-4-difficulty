<template>
  <div class="orders-view">
    <h1>Order Collaboration Hub</h1>
    <el-empty v-if="!orderStore.orders.length && !orderStore.loading" description="No related orders yet" />
    <el-skeleton :loading="orderStore.loading" animated>
      <template #template>
        <el-skeleton-item variant="rect" style="width: 100%; height: 120px; margin-bottom: 16px" />
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="order in orderStore.orders"
          :key="order.id"
          :timestamp="formatDateTime(order.createdAt)"
          placement="top"
        >
          <el-card class="order-card">
            <div class="card-header">
              <div>
                <span>Order #{{ order.id }}</span>
                <el-tag class="role-tag" v-if="isBuyer(order)" type="primary">Buyer</el-tag>
                <el-tag class="role-tag" v-if="isSeller(order)" type="success">Seller</el-tag>
              </div>
              <el-tag :type="statusTagType(order.status)">{{ statusLabel(order.status) }}</el-tag>
            </div>
            <div class="order-body">
              <img :src="order.goodsCoverImageUrl || fallbackImg" alt="" class="goods-cover" />
              <div class="info">
                <h3>{{ order.goodsTitle }}</h3>
                <p>Buyer: {{ order.buyerNickname }}</p>
                <p>Seller: {{ order.sellerNickname }}</p>
              </div>
            </div>
            <div class="card-actions">
              <el-button size="small" type="primary" v-if="canPay(order)" @click="openPaymentDialog(order.id)">
                Simulate Payment
              </el-button>
              <el-button size="small" type="danger" v-if="canCancel(order)" @click="updateStatus(order.id, 'CANCELED')">
                Cancel Order
              </el-button>
              <el-button size="small" type="warning" v-if="canShip(order)" @click="updateStatus(order.id, 'PENDING_RECEIVE')">
                Confirm Shipment
              </el-button>
              <el-button size="small" type="success" v-if="canConfirm(order)" @click="updateStatus(order.id, 'COMPLETED')">
                Confirm Receipt
              </el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-skeleton>

    <el-dialog v-model="paymentDialog.visible" title="Payment Simulation" width="360px">
      <p>Select the payment outcome to continue the practice flow.</p>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="paymentDialog.visible = false">Cancel</el-button>
          <el-button type="danger" @click="simulatePaymentFailure">Payment Failed</el-button>
          <el-button type="primary" @click="simulatePaymentSuccess">Payment Succeeded</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { useOrderStore } from '../stores/orderStore';
import { useUserStore } from '../stores/userStore';
import { formatDateTime } from '../utils/date';

const orderStore = useOrderStore();
const userStore = useUserStore();

const fallbackImg =
  'https://dummyimage.com/400x240/f2f6ff/8ca8ff.png&text=No+Image';

const paymentDialog = reactive<{ visible: boolean; orderId: number | null }>({
  visible: false,
  orderId: null
});

onMounted(() => {
  orderStore.loadOrders();
});

const isBuyer = (order: typeof orderStore.orders[number]) => order.buyerId === userStore.userId;
const isSeller = (order: typeof orderStore.orders[number]) => order.sellerId === userStore.userId;

const canPay = (order: typeof orderStore.orders[number]) =>
  isBuyer(order) && order.status === 'PENDING_PAYMENT';

const canCancel = (order: typeof orderStore.orders[number]) =>
  isBuyer(order) && order.status === 'PENDING_PAYMENT';

const canShip = (order: typeof orderStore.orders[number]) =>
  isSeller(order) && order.status === 'PENDING_SHIPMENT';

const canConfirm = (order: typeof orderStore.orders[number]) =>
  isBuyer(order) && order.status === 'PENDING_RECEIVE';

const statusLabel = (status: string) => {
  const mapping: Record<string, string> = {
    PENDING_PAYMENT: 'Pending Payment',
    PENDING_SHIPMENT: 'Pending Shipment',
    PENDING_RECEIVE: 'Pending Receipt',
    COMPLETED: 'Completed',
    CANCELED: 'Canceled'
  };
  return mapping[status] || status;
};

const statusTagType = (status: string) => {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'warning';
    case 'PENDING_SHIPMENT':
      return 'info';
    case 'PENDING_RECEIVE':
      return 'warning';
    case 'COMPLETED':
      return 'success';
    case 'CANCELED':
      return 'danger';
    default:
      return 'info';
  }
};

const updateStatus = async (orderId: number, status: string) => {
  try {
    await orderStore.changeOrderStatus(orderId, { status });
    ElMessage.success('Order status updated');
  } catch (error: any) {
    const message = error?.response?.data?.message || 'Update failed';
    ElMessage.error(message);
  }
};

const openPaymentDialog = (orderId: number) => {
  paymentDialog.orderId = orderId;
  paymentDialog.visible = true;
};

const simulatePaymentSuccess = async () => {
  if (paymentDialog.orderId == null) return;
  await updateStatus(paymentDialog.orderId, 'PENDING_SHIPMENT');
  paymentDialog.visible = false;
};

const simulatePaymentFailure = async () => {
  if (paymentDialog.orderId == null) return;
  await updateStatus(paymentDialog.orderId, 'CANCELED');
  paymentDialog.visible = false;
};
</script>

<style scoped>
.orders-view {
  padding: 24px;
}

.order-card {
  border-radius: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.role-tag {
  margin-left: 8px;
}

.order-body {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
}

.goods-cover {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.info h3 {
  margin: 0 0 8px;
}

.card-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
