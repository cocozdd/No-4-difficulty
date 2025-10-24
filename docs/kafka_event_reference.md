YU# Kafka 浜嬩欢鍙傝€冩墜鍐?

## 1. 鏂囨。鑼冨洿

鏈墜鍐屾⒊鐞?Campus Market 椤圭洰涓?Kafka 鐨勪娇鐢ㄦ儏鍐碉紝娑电洊锛?

- 宸蹭娇鐢ㄧ殑 Topic銆佷簨浠舵ā鍨嬩笌鑱岃矗鍒掑垎锛?
- 鐢熶骇鑰呫€佹秷璐硅€呯殑瀹炵幇瑕佺偣锛?
- 鎵嬪姩/鑷姩鍖栭獙璇佹楠わ紱
- 鐩戞帶銆佹帓闅滃強鍚庣画瑙勫垝銆?

鏂囨。涓?UTF-8 缂栫爜锛屽彲鍦?VS Code / Typora / Obsidian 绛夌紪杈戝櫒涓煡鐪嬨€?

## 2. Topic 娓呭崟

| Topic            | 鍒嗗尯 | 淇濈暀绛栫暐 | 鐢熶骇鑰咃紙褰撳墠瀹炵幇锛?                                    | 娑堣垂鑰咃紙褰撳墠瀹炵幇锛?                                  | 涓昏鐢ㄩ€?                              |
|------------------|------|----------|--------------------------------------------------------|------------------------------------------------------|----------------------------------------|
| `order-events`   | 3    | 榛樿     | `OrderService`銆乣KafkaDiagnosticsService`              | `OrderEventListener`                                 | 璁㈠崟鐢熷懡鍛ㄦ湡鍙樻洿銆佽瘖鏂啋鐑熼獙璇?        |
| `goods-events`   | 3    | 榛樿     | `GoodsServiceImpl`锛堝垱寤?鏇存柊/鍒犻櫎/瀹℃牳/娴忚绛夋搷浣滐級   | `GoodsEventListener` + `GoodsEventProcessor`         | 鍟嗗搧琛屼负閲囬泦銆佺紦瀛樺埛鏂般€佺儹姒滄洿鏂?      |
| `chat-events`    | 3    | 榛樿     | `ChatMessageServiceImpl`锛堟秷鎭叆搴撳悗瑙﹀彂锛?            | `ChatEventListener` + `ChatEventProcessor`           | 鑱婂ぉ娑堟伅鎺ㄩ€併€佸璁°€佹秷鎭鏁?          |

> 鎵€鏈?Topic 鍦?`KafkaTopicConfig`锛坄backend/src/main/java/com/campusmarket/config/KafkaTopicConfig.java`锛変腑鑷姩澹版槑锛屽彲瑙嗛渶瑕佽皟鏁村垎鍖烘暟銆佷繚鐣欑瓥鐣ョ瓑閰嶇疆銆?

## 3. 浜嬩欢缁撴瀯

### 3.1 `OrderEvent`

```json
{
  "eventType": "ORDER_CREATED | ORDER_STATUS_CHANGED | TEST_EVENT",
  "orderId": 12345,
  "goodsId": 67890,
  "buyerId": 24680,
  "currentStatus": "CREATED | PAID | SHIPPED | COMPLETED | TEST",
  "previousStatus": "CREATED | PAID | ... | null",
  "note": "鍙€夌殑涓氬姟鎴栬瘖鏂鏄?,
  "eventTime": "2024-01-30T12:34:56.789"
}
```

- **娑堟伅 key**锛氫娇鐢?`orderId`锛堣瘖鏂簨浠舵棤璁㈠崟鍙锋椂閫€鍖栦负 `diag-<UUID>`锛夈€?
- **Schema 婕旇繘**锛氭柊澧炲瓧娈典繚鎸佸彲閫夛紝閬垮厤鍒犻櫎/閲嶅懡鍚嶅凡瀛樺湪瀛楁銆?
- **鏍￠獙**锛氱敓浜ц€呭湪鍙戦€佸墠瀹屾垚棰嗗煙鏍￠獙锛涙秷璐硅€呭鍙┖瀛楁鍋氬ソ瀹归敊銆?

### 3.2 `GoodsEvent`

```json
{
  "eventType": "GOODS_CREATED | GOODS_UPDATED | GOODS_DELETED | GOODS_REVIEWED | GOODS_MARKED_SOLD | GOODS_VIEWED",
  "goodsId": 1001,
  "sellerId": 2001,
  "actorId": 3001,
  "currentStatus": "APPROVED | PENDING_REVIEW | ...",
  "quantity": 5,
  "note": "Goods updated by seller",
  "eventTime": "2024-01-30T12:34:56.789"
}
```

- **娑堟伅 key**锛氫娇鐢?`goodsId`锛屼繚璇佸悓涓€鍟嗗搧浜嬩欢钀藉湪鍚屼竴鍒嗗尯銆?
- **actorId**锛氭爣璇嗚Е鍙戞搷浣滅殑鐢ㄦ埛锛堝崠瀹?绠＄悊鍛?娴忚鑰咃級锛屾祻瑙堜簨浠跺彲鑳戒负绌恒€?
- **currentStatus / quantity**锛氭彁渚涚儹搴︽銆佹悳绱€佽繍钀ョ粺璁℃墍闇€涓婁笅鏂囥€?
- **note**锛氱敤浜庢棩蹇椾笌 BI 瑙ｈ銆?

### 3.3 `ChatEvent`
### 3.4 Schema 瀛樺偍

- JSON Schema 鏂囦欢浣嶄簬 `backend/src/main/resources/schemas/`锛屽垎鍒鐩栬鍗曘€佸晢鍝併€佽亰澶╀簨浠躲€?
- CI 鐨?`./mvnw test` 浼氳繍琛?`SchemaTest` 鐢ㄤ緥锛岃嫢缁撴瀯琚牬鍧忎細绔嬪嵆澶辫触銆?


```json
{
  "eventType": "CHAT_MESSAGE_CREATED",
  "messageId": 555,
  "senderId": 101,
  "receiverId": 202,
  "messageType": "TEXT | IMAGE",
  "contentPreview": "Hello ...",
  "eventTime": "2024-01-30T12:34:56.789"
}
```

- **娑堟伅 key**锛氫娇鐢?`messageId` 纭繚骞傜瓑澶勭悊銆?
- **contentPreview**锛氭彁渚涙秷鎭憳瑕侊紝鏂逛究 Push銆佸璁℃垨 BI 寮曠敤銆?

## 4. 鐢熶骇鑰呮敞鎰忎簨椤?

1. **缁熶竴灏佽**锛氳鍗曠浉鍏充簨浠堕€氳繃 `OrderEventPublisher`銆乣KafkaDiagnosticsService` 鍙戦€侊紝淇濇寔涓€鑷寸殑鍙戝竷妯℃澘銆?
2. **鍟嗗搧浜嬩欢鍏ㄩ潰瑕嗙洊**锛歚GoodsServiceImpl` 鍦ㄥ垱寤恒€佹洿鏂般€佸垹闄ゃ€佸鏍搞€佸簱瀛樺彉鍔ㄣ€佹祻瑙堢瓑鑺傜偣璋冪敤 `GoodsEventPublisher`锛屼繚璇佷簨浠朵笌鏁版嵁搴撳啓鎿嶄綔鍚屼簨鍔℃彁浜ゃ€?
3. **鑱婂ぉ浜嬩欢閾捐矾**锛歚ChatMessageServiceImpl` 淇濆瓨娑堟伅鍚庣珛鍗宠皟鐢?`ChatEventPublisher`锛岃Е鍙?`CHAT_MESSAGE_CREATED` 浜嬩欢锛屼緵鎺ㄩ€佸拰瀹¤浣跨敤銆?
4. **閬垮厤闃诲**锛氱敓浜ц€呯姝㈢洿鎺?`CompletableFuture#join()`锛涜嫢闇€鍚屾纭锛岄渶璁剧疆瓒呮椂锛堣瘖鏂帴鍙ｇず渚嬶細5 绉掞級銆?
5. **鏃ュ織杩借釜**锛氬彂甯冪粨鏋滀互 `INFO`/`WARN` 璁板綍锛屽苟闄勫甫涓氬姟鏍囪瘑锛屼究浜庤法鏈嶅姟鎺掓煡銆?

### 璇婃柇鎺ュ彛

- `POST /api/diagnostics/kafka/order-events`锛氬彂閫?`TEST_EVENT`锛屽唴缃?5 绉掕秴鏃躲€?
- 鍓嶇寮€鍙戞ā寮忥紙`npm run dev`锛夐椤垫彁渚涒€淜afka 蹇€熸祴璇曗€濆崱鐗囷紝鍙洿鎺ヨ皟璇ユ帴鍙ｃ€?

## 5. 娑堣垂鑰呮敞鎰忎簨椤?

### 5.1 閫氱敤閰嶇疆

- `KafkaListenerConfig` 注册 `DefaultErrorHandler(FixedBackOff(1s, 2))`，失败两次后会把消息投递到 `<topic>.DLT`（已预创建 `goods-events.DLT`），同时记录 `WARN` 日志。
- 娑堣垂鑰呴渶淇濇寔骞傜瓑鎬э紙鍙€氳繃 Redis Set銆佹暟鎹簱鍞竴閿€佷笟鍔′富閿瓑瀹炵幇锛夈€?
- 濡傞渶杩涗竴姝ュ鐏撅紝鍙帴鍏?Retry Topic銆佹淇￠槦鍒楃瓑鏈哄埗銆?

### 5.2 璁㈠崟浜嬩欢鐩戝惉鍣?

- `OrderEventListener` 鎵撳嵃 `type/orderId/status/note`锛屼究浜庨€氱煡銆佺紦瀛樺埛鏂般€佺粺璁＄瓑涓嬫父娑堣垂銆?

### 5.3 鍟嗗搧浜嬩欢鐩戝惉鍣?

- `GoodsEventListener` 缁撳悎 `GoodsEventProcessor` 寮傛澶勭悊锛?
  - `GOODS_VIEWED`锛氫氦鐢?`GoodsEventProcessor` 璋冪敤 `GoodsMetricsService.recordView`锛?
  - `GOODS_DELETED`锛氱Щ闄ゅ搴旂儹搴?璁℃暟鏁版嵁锛?
  - 鍏朵粬鍙樻洿锛堝垱寤恒€佹洿鏂般€佸鏍搞€佸敭缃勭瓑锛夛細瑙﹀彂 `HotGoodsService.evictHotCache()`銆?
- 鐩戝惉鍣ㄤ娇鐢ㄧ嫭绔?groupId锛坄${spring.kafka.consumer.group-id}-goods`锛夛紝渚夸簬姘村钩鎵╁睍銆?

### 5.4 鑱婂ぉ浜嬩欢鐩戝惉鍣?

- `ChatEventListener` 搭配 `ChatEventProcessor`，消费 `CHAT_MESSAGE_CREATED` 时会调用 `ChatMetricsService` 计数，并通过 `ChatCacheService` 更新未读缓存（具备消息 ID 幂等校验）。
- 鐩戝惉鍣ㄤ娇鐢?`${spring.kafka.consumer.group-id}-chat`锛屼究浜庡拰璁㈠崟銆佸晢鍝佺洃鍚嫭绔嬫墿瀹广€?

## 6. 楠岃瘉鎵嬪唽

鎸夐噸瑕佹€ч『搴忔墽琛屼互涓嬮獙璇侊細

1. **CLI 鍐掔儫**
   ```powershell
   "smoke-test" | docker exec -i campus-kafka /opt/kafka/bin/kafka-console-producer.sh `
     --bootstrap-server localhost:9092 --topic order-events

   docker exec campus-kafka /opt/kafka/bin/kafka-console-consumer.sh `
     --bootstrap-server localhost:9092 --topic order-events --from-beginning --timeout-ms 5000
   ```
2. **鍚庣璇婃柇鎺ュ彛**
   ```powershell
   Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/diagnostics/kafka/order-events `
     -Body '{"message":"Hello Kafka","orderId":9001}' -ContentType 'application/json'
   ```
   鈫?鏈熸湜 HTTP 200锛屾棩蹇楄緭鍑?`Kafka diagnostics event published`銆?
3. **鍓嶇娴嬭瘯闈㈡澘**
   - 杩愯 `npm run dev`锛岃闂?`http://localhost:5173`锛屼娇鐢ㄢ€淜afka 蹇€熸祴璇曗€濆崱鐗囨彁浜ゆ秷鎭紝纭鐣岄潰鍥炴樉鍙婂悗鍙版棩蹇椼€?
4. **娑堣垂缁?Lag**
   ```powershell
   docker exec campus-kafka /opt/kafka/bin/kafka-consumer-groups.sh `
     --bootstrap-server localhost:9092 --describe --group quick-check
   ```
5. **寮傚父娑堟伅鍥炴斁锛堣鍗曪級**
   ```powershell
   docker exec -i campus-kafka /opt/kafka/bin/kafka-console-producer.sh `
     --bootstrap-server localhost:9092 --topic order-events <<< "invalid"
   ```
   鈫?妫€鏌?`DefaultErrorHandler` 鏄惁鎵撳嵃閲嶈瘯鍛婅銆?
6. **鍟嗗搧浜嬩欢楠岃瘉**
   - 娴忚锛歚POST /api/goods/{id}/view`锛堢‘淇濆凡瀹℃牳閫氳繃锛夛紝纭 `GoodsEventListener` 鏃ュ織鍑虹幇 `GOODS_VIEWED` 涓?Redis 璁℃暟澧炲姞銆?
   - 鍒涘缓/鏇存柊/瀹℃牳锛氭搷浣滃晢鍝佸悗锛岀‘璁ゆ棩蹇楀寘鍚搴斾簨浠讹紝鐑缂撳瓨琚埛鏂般€?
7. **鑱婂ぉ浜嬩欢楠岃瘉**
   ```powershell
   Invoke-RestMethod -Method Post `
     -Uri http://localhost:8080/api/chat/messages `
     -Headers @{ Authorization = 'Bearer <token>' } `
     -Body '{"receiverId":2,"content":"ping","messageType":"TEXT"}' `
     -ContentType 'application/json'
   ```
   - 检查 `ChatEventListener` 日志、Redis 指标（`chat:metrics:*`）以及未读缓存键 `chat:unread:{receiver}`。
8. **Topic 鍒楄〃纭**
   ```powershell
   docker exec campus-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
   ```
   鈫?搴斿垪鍑?`order-events`銆乣goods-events`銆乣chat-events`銆?


### 鑷姩鍖栨祴璇?

- 渚濊禆 Docker 鐜锛圱estcontainers 浼氳嚜鍔ㄦ媺璧峰鍣級銆?
- 杩愯 `./mvnw test` 鍙墽琛?order/goods/chat 浜嬩欢鐨勯泦鎴愭祴璇曘€?

## 7. 鐩戞帶涓庡憡璀?

### 7.1 Prometheus / Grafana 涓€閿惎鍔?

- 鍦?`monitoring/` 鐩綍鎵ц `docker-compose up -d`锛屼細鎷夎捣 Prometheus (9090) 涓?Grafana (3000)銆?
- Prometheus 宸查厤缃姄鍙?`host.docker.internal:8080/actuator/prometheus`锛岀‘淇濆悗绔湇鍔″凡鍚姩骞跺厑璁歌闂€?
Grafana 榛樿璐﹀彿瀵嗙爜涓?`admin / YULIN011210`锛岄娆＄櫥褰曞悗鍙湪鏁版嵁婧愪腑鐪嬪埌宸插噯澶囩殑 Prometheus锛堝闇€鑷缓锛屽彲鎵嬪姩娣诲姞 `http://prometheus:9090`锛夈€?
- 鍦?Grafana 鍒涘缓 Dashboard 鏃讹紝鍙洿鎺ユ悳绱?`kafka_`銆乣jvm_` 鎸囨爣锛屾垨瀵煎叆瀹樻柟 Spring/Kafka 妯℃澘杩涜灞曠ず銆?

### 7.2 鎸囨爣璇存槑



- **Lag / 鍚炲悙**锛氱粨鍚?Prometheus + Kafka JMX Exporter 閲囬泦鎸囨爣锛岃缃?Lag 闃堝€煎憡璀︺€?
- **Actuator / Prometheus**锛氬簲鐢ㄥ紑鍚簡 `spring-boot-starter-actuator` 涓?`micrometer-registry-prometheus`锛屽彲鍦?`/actuator/prometheus` 鏆撮湶鎸囨爣锛孏rafana 鍙洿鎺ユ姄鍙栥€?
- **搴旂敤鎸囨爣**锛氬紑鍚?Spring Boot Actuator锛坄/actuator/prometheus`锛夛紝鍏虫敞鍙戝竷鎴愬姛/澶辫触娆℃暟銆佹秷璐硅€楁椂銆?
- **鏃ュ織涓績**锛氬鎺?ELK/EFK锛岄噸鐐瑰叧娉?`GoodsEventProcessor`銆乣ChatEventListener`銆乣KafkaDiagnosticsService` 鐨?WARN/ERROR銆?
- **鍙敤鎬ф鏌?*锛氬彲閫氳繃瀹氭椂浠诲姟鑷姩璋冪敤璇婃柇鎺ュ彛鎴栨帹閫佹祴璇曟秷鎭紝鎻愬墠鍙戠幇 Broker/鐩戝惉鍣ㄥ紓甯搞€?

## 8. 鎺掗殰閫熸煡

| 鐜拌薄                           | 鍙兘鍘熷洜                     | 澶勭悊寤鸿                                                                 |
|--------------------------------|------------------------------|--------------------------------------------------------------------------|
| `UNKNOWN_TOPIC_OR_PARTITION`   | Topic 鏈垱寤烘垨 Broker 閲嶅惎   | 鍙傜収 `SETUP_GUIDE` 鎵嬪姩鍒涘缓锛涘繀瑕佹椂寮€鍚嚜鍔ㄥ缓 Topic 鎴栬皟鏁村惎鍔ㄩ『搴?       |
| 璇婃柇鎺ュ彛瓒呮椂锛? 绉掞級           | Kafka Broker 涓嶅彲杈?         | 妫€鏌?Docker 瀹瑰櫒鐘舵€併€佺綉缁滆繛閫氬強 9092 绔彛鏄犲皠                           |
| 娑堣垂绔棤鏃ュ織/闃诲              | 鍙嶅簭鍒楀寲寮傚父鎴栫洃鍚櫒寮傚父閫€鍑?| 鏌ョ湅鏃ュ織锛岃皟鏁?Schema/Trusted Packages锛屽繀瑕佹椂寮曞叆姝讳俊闃熷垪               |
| Lag 鎸佺画澧為暱                   | 娑堣垂鑰呭紓甯告垨澶勭悊杩囨參         | 澧炲姞瀹炰緥銆佷紭鍖栨秷璐归€昏緫銆佹鏌?Offset 鎻愪氦绛栫暐                             |
| `goods-events` 鏈Е鍙?         | 鏈嶅姟鏈皟鐢ㄥ彂甯冨櫒鎴栦簨鍔″洖婊?  | 瀵圭収 `GoodsServiceImpl` 瀵瑰簲浠ｇ爜涓庢暟鎹簱鎿嶄綔璁板綍锛岀‘淇濅簨鍔℃垚鍔熸彁浜?      |
| `chat-events` 鏈Е鍙?          | 娑堟伅淇濆瓨澶辫触鎴栧彂甯冨櫒寮傚父     | 鏌ョ湅 `ChatMessageServiceImpl` 鏃ュ織锛岀‘璁ゆ秷鎭啓鍏ュ強浜嬩欢鍙戝竷鎴愬姛           |
| Redis 璁℃暟鏃犲彉鍖?              | Redis 涓嶅彲鐢ㄦ垨鏉冮檺涓嶈冻       | 妫€鏌?`StringRedisTemplate` 渚濊禆锛岀‘璁?Redis 鏈嶅姟銆佽璇併€佺綉缁滈厤缃?        |
| 401/403                        | JWT 杩囨湡/缂哄け                | 閲嶆柊鐧诲綍鑾峰彇 Token锛屽苟纭璇锋眰澶?`Authorization` 璁剧疆姝ｇ‘                |

## 9. 鍚庣画瑙勫垝

0. 宸叉柊澧?GitHub Actions 娴佺▼锛坄.github/workflows/ci.yml`锛夛紝鍦?push/PR 鏃惰嚜鍔ㄨ繍琛?backend `./mvnw test` 涓?frontend `npm run build`锛屽悗缁彲鎵╁睍閫氱煡銆佸苟琛岀紦瀛樼瓑閰嶇疆銆?
1. 涓?`goods-events`銆乣chat-events` 澧炲姞鏇村瀹炵敤娑堣垂鑰咃細鐑害姒溿€佹悳绱㈢储寮曘€佹帹閫佹湇鍔°€佽繍钀ュ璁＄瓑銆?
2. 浣跨敤 Testcontainers/Embedded Kafka 缂栧啓鍙戝竷-娑堣垂闆嗘垚娴嬭瘯锛屽苟绾冲叆 CI銆?
3. 寮曞叆 Schema Registry锛岀粺涓€绠＄悊浜嬩欢缁撴瀯涓庣増鏈吋瀹圭瓥鐣ャ€?
4. 灏?Kafka 鎸囨爣绾冲叆 Grafana锛屽苟璁剧疆 Lag銆佸け璐ョ巼銆丅roker 鍋ュ悍绛夊憡璀︺€?
5. 瑙勫垝浜嬩欢绾?SLA 涓?Topic 淇濈暀绛栫暐锛屽楂樹环鍊间簨浠惰缃洿闀夸繚鐣欍€佹洿澶氬壇鏈€?

---

濡?Topic銆佷簨浠跺瓧娈点€佹秷璐圭瓥鐣ュ彂鐢熻皟鏁达紝璇风涓€鏃堕棿鏇存柊鏈墜鍐岋紝骞跺悓姝ユ鏌ョ浉鍏虫簮鐮佸紩鐢ㄦ槸鍚︿竴鑷淬€?**



