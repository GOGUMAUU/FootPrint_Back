package com.example.footprint.dto;

import com.example.footprint.exception.ApiException;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/** 공통 JSON 형식. 성공 응답에 추가 wrapper를 적용하지 않는다. */
public final class CommonDtos {
    private CommonDtos() {}

    public record ApiError(String code, String message) {}

    public record PageResponse<T>(List<T> content, int page, int size, boolean hasNext) {
        public static PageRequest request(int page, int size) {
            if (page < 0 || size < 1 || size > 100) {
                throw ApiException.bad("INVALID_PAGE", "page는 0 이상, size는 1~100이어야 합니다.");
            }
            return PageRequest.of(page, size);
        }

        public static <S, T> PageResponse<T> from(Slice<S> slice, Function<S, T> mapper) {
            return new PageResponse<>(slice.getContent().stream().map(mapper).toList(),
                    slice.getNumber(), slice.getSize(), slice.hasNext());
        }
    }
}
