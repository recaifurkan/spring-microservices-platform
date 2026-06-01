package com.example.frontend.dto;

import java.math.BigDecimal;
import java.util.List;

/** Sepet özeti yanıtı (cart-service'ten dönen). */
public record CartResponse(List<?> items, BigDecimal total, int itemCount) {}

