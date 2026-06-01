package com.example.cart.dto;

import com.example.cart.model.CartItem;

import java.math.BigDecimal;
import java.util.List;

/** Sepet özeti yanıtı. */
public record CartResponse(List<CartItem> items, BigDecimal total, int itemCount) {}

