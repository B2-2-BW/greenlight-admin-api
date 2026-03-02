package com.winten.greenlight.admin.db.repository.redis.room;

import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

public class JsonFilters {
    public static final PropertyFilter roomFilter = SimpleBeanPropertyFilter.serializeAllExcept(
            "userRole",
            "description",
            "createdBy",
            "createdAt",
            "createdIp",
            "updatedBy",
            "updatedAt",
            "updatedIp"
    );
    public static final PropertyFilter roomRuleFilter = SimpleBeanPropertyFilter.serializeAllExcept(
            "siteId",
            "roomId",
            "ruleSeq",
            "userRole",
            "description",
            "createdBy",
            "createdAt",
            "createdIp",
            "updatedBy",
            "updatedAt",
            "updatedIp"
    );
}