# Swin-Unet 学习笔记：从理论到实践

本文基于综述《A comprehensive review of deep learning for medical image segmentation》中对 Swin-Unet 的解读，结合实践经验总结模型背景、理论机制、代码实现、训练策略及后续行动建议，帮助快速形成体系化认知。

---

## 1. 模型背景与核心问题
- **定位**：Swin-Unet 结合 Transformer 的全局建模能力与 U-Net 的对称编码-解码结构，专注医学图像分割。
- **痛点**：
  - 传统 CNN（如 U-Net）缺乏长程依赖建模能力，难刻画远距离结构关系。
  - 纯 ViT 虽具备全局注意力，但计算复杂度高、缺少层级特征。
- **核心创新**：引入移位窗口（Shifted Window）机制与层次化特征提取，使自注意力计算保持线性复杂度，同时保留 U-Net 的跳跃连接优势。

---

## 2. 关键理论解析

### 2.1 编码器-解码器范式
- **编码器**：依次执行 Patch Partition、Linear Embedding 和多个 Swin Transformer Block，下采样路径通常从 `H/4 → H/32`，通道数逐步增加。
- **解码器**：通过 Patch Expanding 逐级上采样，并与编码器的高分辨率特征做跳跃连接，保留细节。
- **跳跃连接**：每层编码输出与对应解码层拼接，引导精细结构恢复。

### 2.2 移位窗口注意力
- **问题**：标准自注意力复杂度 `O(N^2)`，对高分辨率医学图像不可行。
- **W-MSA**：将特征划分为 `7×7` 等局部窗口，窗口内计算注意力，将复杂度降至线性。
- **SW-MSA**：下一层滑动/移位窗口边界，让不同窗口的信息交互。
- **公式**：`Attention(Q,K,V) = SoftMax(QKᵀ / √d_k + B) V`，其中 `B` 为相对位置编码，保证位置信息对齐。

### 2.3 核心组件
- **Patch Partition**：将输入图像（如 `224×224×3`）拆分为 `4×4` patch，每个 patch 展平成 48 维向量。
- **Linear Embedding**：线性投影至嵌入维度 `C`（常用 96）。
- **Patch Merging**：编码阶段的下采样，拼接后用线性层实现分辨率减半、通道翻倍。
- **Patch Expanding**：解码阶段的上采样，通过通道重组恢复分辨率。

---

## 3. 代码实现指南

### 3.1 关键模块
```python
import torch
import torch.nn as nn
from timm.models.layers import DropPath

class SwinTransformerBlock(nn.Module):
    def __init__(self, dim, num_heads, window_size=7, shift_size=0):
        super().__init__()
        self.window_size = window_size
        self.shift_size = shift_size
        self.norm1 = nn.LayerNorm(dim)
        self.attn = WindowAttention(dim, num_heads, window_size)
        self.norm2 = nn.LayerNorm(dim)
        self.mlp = nn.Sequential(
            nn.Linear(dim, 4 * dim),
            nn.GELU(),
            nn.Linear(4 * dim, dim)
        )

    def forward(self, x):
        if self.shift_size > 0:
            x = torch.roll(x, shifts=(-self.shift_size, -self.shift_size), dims=(1, 2))
        shortcut = x
        x = self.norm1(x)
        x = self.attn(x)
        x = shortcut + x
        x = x + self.mlp(self.norm2(x))
        return x
```
- `WindowAttention` 负责窗口自注意力；`shift_size` 控制是否执行 SW-MSA。

### 3.2 模型集成与硬件适配
- **预训练**：可直接加载官方仓库（如 `HuCaoFighting/Swin-Unet`）的 ImageNet 预训练权重，加速收敛。
- **显存策略**（如 RTX 2070s）：
  - 将输入由 `512×512` 调整为 `128×128` 或 `224×224`。
  - 批次大小 `2-4`，必要时用梯度累积模拟大 batch。
  - 启用 AMP（Automatic Mixed Precision）以降低显存占用。

---

## 4. 训练与调试实践

### 4.1 数据准备与增强
- **数据集**：推荐从 ISIC2018 等中小规模数据集着手。
- **增强策略**：使用 Albumentations 进行 Resize、翻转、归一化等操作。
```python
import albumentations as A
transform = A.Compose([
    A.Resize(224, 224),
    A.HorizontalFlip(p=0.5),
    A.Normalize(mean=(0.485, 0.456, 0.406),
                std=(0.229, 0.224, 0.225))
])
```

### 4.2 损失函数与优化
- **Dice + BCE 组合**：缓解类别不平衡。
```python
class DiceBCELoss(nn.Module):
    def forward(self, pred, target):
        dice = 1 - (2 * (pred * target).sum() + 1e-6) / (pred.sum() + target.sum() + 1e-6)
        bce = nn.BCEWithLogitsLoss()(pred, target)
        return dice + bce
```
- **梯度累积**：分摊显存压力。
```python
optimizer.zero_grad()
for i, (inputs, labels) in enumerate(loader):
    outputs = model(inputs)
    loss = criterion(outputs, labels) / 4
    loss.backward()
    if (i + 1) % 4 == 0:
        optimizer.step()
        optimizer.zero_grad()
```

### 4.3 评估与可视化
- **指标**：Dice、IoU。
```python
def dice_score(pred, target):
    pred = (pred > 0.5).float()
    intersection = (pred * target).sum()
    return (2 * intersection) / (pred.sum() + target.sum() + 1e-6)
```
- **注意力热力图**：提取 Swin Transformer Block 的注意力权重，验证模型聚焦区域。

---

## 5. 挑战与进阶思考

- **显存不足**：启用梯度检查点（Gradient Checkpointing）或进一步降低窗口/层数。
- **训练较慢**：结合 AMP、分布式训练（DDP）或更高效的数据管线。
- **模型对比**：
  - VS CNN-Based：Swin-Unet 通过移位窗口实现全局依赖捕获，优于纯卷积的局部感受野。
  - VS 纯 ViT：层次结构与滑动窗口保持线性复杂度，更适于高分辨率输入。
- **未来方向**：为后续研究状态空间模型（如 Mamba）做铺垫，探索更高效的长序列建模。

---

## 6. 关键收获与行动清单

- **理论掌握**：搞清楚移位窗口和层次结构如何协同解决长程依赖问题。
- **代码能力**：能独立实现 Swin Transformer Block，并将其嵌入 U-Net 架构。
- **实践技能**：熟悉数据增强、损失设计、显存优化、可视化调试。

### 下一步行动
1. 克隆官方实现并跑通 Demo。
2. 在 ISIC2018 等数据集上训练，目标 Dice > 0.85。
3. 可视化注意力图，分析模型决策过程。
4. 调整窗口大小、学习率等超参，观察性能变化趋势。

通过上述学习路径，可全面把握 Swin-Unet 的理论、实现与实战技巧，为后续深入 Transformer 系列医学分割模型打下坚实基础。
