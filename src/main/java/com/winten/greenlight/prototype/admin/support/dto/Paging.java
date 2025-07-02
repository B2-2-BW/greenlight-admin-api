package com.winten.greenlight.prototype.admin.support.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@SuperBuilder
public class Paging {
    private int offset;
    private int limit;
    private String sortKey; // 정렬 기준 필드 (예: "createdAt")
    private String sort; // 정렬 방향 (예: "asc", "desc")

    // 기본값 설정을 위한 생성자
    public Paging() {
        this.offset = 0;
        this.limit = 10;
        this.sortKey = "id";
        this.sort = "asc";
    }
}