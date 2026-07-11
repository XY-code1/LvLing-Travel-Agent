package com.guido.scenicai.module.route.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("route_spot")
public class RouteSpot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long routeId;
    private Long spotId;
    private Integer sortOrder;
}
