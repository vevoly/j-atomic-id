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
### j-atomic-id 客户端双 Buffer 架构图
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
