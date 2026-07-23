package com.lfgtavora.poketcapi.poketcg_api.web.dto;

import java.util.List;

public record PagedApiResponse<T>(
        List<T> data,
        int page,
        int pageSize,
        int count,
        long totalCount) {
}
