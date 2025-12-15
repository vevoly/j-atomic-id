package io.github.vevoly.id.example.api;

import io.github.vevoly.id.client.core.generator.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class IdController {
    @Autowired
    private IdGenerator idGenerator;

    /**
     * 测试发号
     * http://localhost:8081/test
     */
    @GetMapping("/test")
    public String test() {
        long start = System.nanoTime();

        // 1. 获取订单 ID (号段模式 - 极速)
        long orderId = idGenerator.nextId("order_global");

        // 2. 获取 IM ID (严格模式 - 有网络开销)
        long msgId = idGenerator.nextId("chat_group_1001");

        long cost = System.nanoTime() - start;

        return String.format("""
                {
                    "cost_ns": %d,
                    "order_id": %d,  <-- 本地内存秒出
                    "msg_id": %d     <-- 远程实时请求
                }
                """, cost, orderId, msgId);
    }
}
