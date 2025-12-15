package io.github.vevoly.id.server.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 对应数据库表: t_id_generator
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_id_generator")
public class IdGeneratorEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务标识 (Primary Key) */
    @TableId(type = IdType.INPUT)
    private String bizTag;

    /** 当前最大 ID */
    private Long maxId;

    /** 步长 **/
    private Integer step;

    /** 描述 **/
    private String description;

    /** 创建时间 **/
    private Date createTime;

    /** 更新时间 (可选) */
    private Date updateTime;
}
