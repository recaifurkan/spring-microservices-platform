package com.example.frontend.model;

/**
 * Tüm sayfa view-model record'larının implement ettiği temel arayüz.
 * Navbar/catbar fragment'larının eriştiği ortak alanları (cartCount,
 * unreadCount, category) için default değerler sağlar; sayfalar kendi
 * record bileşenlerinde bu değerleri override edebilir.
 */
public interface PageModel {
    int cartCount();
    default long unreadCount() { return 0L; }
    default String category()  { return null; }
}

