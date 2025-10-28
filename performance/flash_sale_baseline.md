# Flash Sale Baseline Load Test

This document captures the steps to produce a baseline for `/api/flash-sale/purchase` before introducing load balancing and multi-instance deployment.

## 1. Prerequisites
- 秒杀数据表已初始化（参见 `docs/flash_sale_feature.md`），Redis / MySQL 正常运行。
- 至少有一个处于 RUNNING 状态的秒杀活动并记录其 `id`。
- 已创建多名测试用户，将账号/密码填入 `performance/data/flash_users.csv`（示例已提供）。
- 安装 Apache JMeter 5.6+ 并配置环境变量 `JMETER_HOME`，确保命令行 `jmeter` 可用。

## 2. JMeter 脚本说明
脚本路径：`performance/jmeter/flash-sale-purchase.jmx`

线程逻辑：
1. 从 CSV 读取一组账号。
2. 调用 `/api/auth/login` 获取 `token`。
3. 200ms 思考时间。
4. 向 `/api/flash-sale/purchase` 发送请求。
5. 断言响应中包含 `orderId` 字段。

默认设置：50 线程，30s 匀速拉起，每线程请求 1 次。

## 3. 命令行运行
```powershell
jmeter -n `
  -t performance/jmeter/flash-sale-purchase.jmx `
  -l performance/results/flash_sale_baseline.jtl `
  -e -o performance/results/flash_sale_baseline_report `
  -JflashSaleItemId=<实际活动ID> `
  -JusersCsv=performance/data/flash_users.csv
```

说明：
- `flashSaleItemId` 替换为当前活动 ID；
- `flash_users.csv` 中用户数量最好 ≥ 线程数，防止重复抢购导致业务失败；
- 报告生成在 `performance/results/flash_sale_baseline_report/index.html`。

## 4. 记录基线
重点关注：
- 吞吐量 (Requests/sec)
- 平均/95th 响应时间
- 错误率（若库存不足会出现业务错误，应记录并说明）

将指标记录下来，作为单实例对照。后续在引入 Nginx + 多实例时重复执行并对比新结果。

> 小贴士：若需快速验证脚本，可改为 GUI 模式或减少线程：`jmeter -t performance/jmeter/flash-sale-purchase.jmx`。
