package io.github.vevoly.id.example.service;

import io.github.vevoly.id.client.core.generator.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderIdService {

    @Autowired
    private IdGenerator idGenerator;

    private static final String BIZ_TAG = "bench_order";
    private static final String PREFIX = "ORD-";

    // 缓存当天的日期字符串，避免每次都格式化时间 (性能优化关键点)
    private final AtomicReference<String> currentDateStr = new AtomicReference<>();
    private volatile LocalDate lastDate = null;

    /**
     * 获取下一个订单号
     * 格式: ORD-20251216-100001
     */
    public String nextOrderNo() {
        // 1. 获取高性能 Raw ID (耗时 < 10ns)
        long rawId = idGenerator.nextId(BIZ_TAG);

        // 2. 拼接字符串 (这是主要耗时点)
        // 使用 StringBuilder 减少内存分配
        // 预估长度: 4(ORD-) + 8(日期) + 1(-) + 10(ID) = 23
        StringBuilder sb = new StringBuilder(24);

        sb.append(PREFIX);
        sb.append(getDateStr());
        sb.append('-');
        sb.append(rawId);

        return sb.toString();
    }

    /**
     * 获取日期字符串 (带缓存)
     * 只有跨天的时候才会重新计算，平时直接读内存，极快
     */
    private String getDateStr() {
        LocalDate now = LocalDate.now();
        if (!now.equals(lastDate)) {
            String str = now.format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
            currentDateStr.set(str);
            lastDate = now;
            return str;
        }
        return currentDateStr.get();
    }
}
