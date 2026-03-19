package com.springmart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "商品名は必須です")
    private String name;

    private String description;

    @NotNull(message = "価格は必須です")
    @Min(value = 0, message = "価格は0以上である必要があります")
    private Integer price;

    @Min(value = 0, message = "初期在庫数は0以上である必要があります")
    private Integer initialStock;

    private Long version;
}
