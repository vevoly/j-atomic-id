# j-atomic-id
一款基于 j-atomic-ledger 核心引擎的高性能分布式 ID 生成器。支持 IM 消息严格递增 与 订单号段 双模式，单机千万级 TPS。解决雪花算法时钟回拨痛点，提供纳秒级发号能力。  
A high-performance distributed ID generator built on j-atomic-ledger. Supports Strict Sequential (IM) and Segment (Order) modes with million-level TPS. Solves Snowflake clock rollback issues with nanosecond-level latency.


### j-atomic-id 服务器端架构图
```mermaid
graph TD
    %% 客户端层
    Client_IM[IM Server<br> 严格递增模式] -->|HTTP/RPC<br>count=1| ID_Server
    Client_Order[Order Service<br>号段模式] -->|HTTP/RPC<br>count=1000| ID_Server

    %% 服务端层 (j-atomic-id)
    subgraph "j-atomic-id Server"
        direction TB
        Controller[IdController]
        
        %% 核心引擎 (直接复用 ledger starter)
        subgraph "Ledger Engine"
            WAL[(Chronicle WAL)]
            Disruptor{Disruptor}
            Processor[IdProcessor]
            State[IdState<br>Map<Tag, MaxId>]
        end
        
        Controller --> Disruptor
        Disruptor --> Processor
        Processor --> State
        Processor --> WAL
    end
    
    %% 持久化层 (可选，仅作管理后台展示)
    Processor -.->|Async| MySQL[(MySQL<br>id_generator_info)]
```
### j-atomic-id 客户端 SDK 架构图
```mermaid
graph TD
    subgraph "Client Side (SDK)"
        App[业务应用]
        SDK[j-atomic-id-client]
        Buffer[双 Buffer 缓冲池]
        Router[一致性哈希路由]
        
        App -->|nextId（order）| SDK
        SDK -->|Get from Mem| Buffer
        Buffer -.->|Async Fetch| Router
    end

    subgraph "Server Cluster"
        LB[Nginx / Gateway]
        
        subgraph "Node A"
            EngineA[Ledger Engine]
            WALA[WAL]
        end
        
        subgraph "Node B"
            EngineB[Ledger Engine]
            WALB[WAL]
        end
    end

    Router --> LB
    LB -->|Hash（tag）| EngineA
    LB -->|Hash（tag）| EngineB
```
### j-atomic-id Server 集群部署架构图
```mermaid
graph TD
    Client[业务客户端] --> ServiceDiscovery[服务发现 Nacos/Eureka]
    ServiceDiscovery -- (查询ID Server实例) --> LoadBalancer[客户端负载均衡 Spring Cloud LoadBalancer]
    
    subgraph "ID Server Cluster (3个实例)"
        NodeA[ID Server Node A]
        NodeB[ID Server Node B]
        NodeC[ID Server Node C]
    end

    LoadBalancer -- Hash(bizTag) --> NodeA
    LoadBalancer -- Hash(bizTag) --> NodeB
    LoadBalancer -- Hash(bizTag) --> NodeC

    NodeA -- Internal Sharding --> P_A0[Partition A0]
    NodeA -- Internal Sharding --> P_A1[Partition A1]
    
    NodeB -- Internal Sharding --> P_B0[Partition B0]
    NodeB -- Internal Sharding --> P_B1[Partition B1]

    subgraph "Ledger Engine"
        P_A0 --> Disruptor_A0
        P_A0 --> WAL_A0
        P_A1 --> Disruptor_A1
        P_A1 --> WAL_A1
        
        P_B0 --> Disruptor_B0
        P_B0 --> WAL_B0
        P_B1 --> Disruptor_B1
        P_B1 --> WAL_B1
    end
```
### j-atomic-id-client 号段模式双Buffer时序图
```mermaid
sequenceDiagram
    participant ClientApp
    participant SDK_BufferA
    participant SDK_BufferB
    participant IDServer

    ClientApp->>SDK_BufferA: nextId() #1
    ClientApp->>SDK_BufferA: nextId #2
    
    ClientApp->>SDK_BufferA: nextId() #799 (80% used)

    SDK_BufferA-->>IDServer: Async Request for new Segment (to fill Buffer B)

    ClientApp->>SDK_BufferA: nextId() #800
    
    ClientApp->>SDK_BufferA: nextId() #1000 (Buffer A exhausted)
    
    SDK_BufferA->>SDK_BufferB: Switch to Buffer B (seamlessly)
    SDK_BufferB-->>IDServer: Async Request for new Segment (to fill Buffer A)
    ClientApp->>SDK_BufferB: nextId() #1
```
### j-atomic-id-client IM模式时序图
```mermaid
sequenceDiagram
    participant M1 as 消息服务器 A
    participant M2 as 消息服务器 B
    participant Ring as ID Server (RingBuffer)
    participant Core as ID Server (内存线程)

    Note over M1, M2: 并发时刻：两个群成员同时在群里发消息

    par 并发请求
        M1->>Ring: 请求(tag="Group1", count=1)
        M2->>Ring: 请求(tag="Group1", count=1)
    end

    Note over Ring: Disruptor 自动将并发请求排序放入槽位

    loop 单线程处理
        Ring->>Core: 取出 M1 的请求
        Core->>Core: 内存 current = 100 -> 101
        Core-->>M1: 返回 ID: 101
        
        Ring->>Core: 取出 M2 的请求
        Core->>Core: 内存 current = 101 -> 102
        Core-->>M2: 返回 ID: 102
    end

    Note over M1, M2: M1 拿到 101，M2 拿到 102，绝对不重复，且连续
```
```mermaid
graph TD
    Start[开始压测 ID=100万] --> T0[线程0]
    Start --> T1[线程1]
    Start --> T49[线程49...]
    
    T0 --处理2万个--> Finish0[线程0 完成!]
    T1 --处理2万个--> Finish1[线程1 完成!]
    T49 --处理2万个--> Finish49[线程49 完成!]
    
    Finish0 --打印日志--> Log["最后一条: ORD-...-1190127"]
    
    Finish1 -.-> GlobalID
    Finish49 -.-> GlobalID
    
    GlobalID --所有人跑完--> Final[最终 ID: 200万]
```
## 🛠️ Prerequisites / 部署前置要求

Before starting the server, you must initialize the MySQL database.
启动服务前，请务必初始化 MySQL 数据库。

1.  **Execute SQL Script / 执行 SQL 脚本**:
    Run `scripts/schema.sql` in your MySQL instance to create the database and table.
    在 MySQL 中执行 `scripts/schema.sql` 以创建库表。

2.  **Configure DB Connection / 配置数据库连接**:
    Update `spring.datasource` settings in `application.yml`.
    修改 `application.yml` 中的数据库连接信息。