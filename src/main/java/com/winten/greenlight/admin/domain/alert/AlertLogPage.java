package com.winten.greenlight.admin.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AlertLogPage {
    private final List<AlertLog> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
