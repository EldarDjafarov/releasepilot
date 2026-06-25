package com.releasepilot.application.query.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PagedResultDto<T> {
    List<T> items;
    int page;
    int size;
    long totalElements;
}
