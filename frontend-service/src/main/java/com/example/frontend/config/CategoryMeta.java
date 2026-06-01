package com.example.frontend.config;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kategori adlarına görsel metadata (emoji + gradient renkleri) eşler.
 * <p>
 * Yeni bir kategori DB'ye eklendiğinde buraya da eklenmeli ya da
 * {@link #DEFAULT_EMOJI} / {@link #DEFAULT_GRADIENT} fallback'i kullanılır.
 * Thymeleaf şablonlarında {@code @catMeta.emoji(cat)} gibi erişilir.
 */
@Component("catMeta")
public class CategoryMeta {

    public static final String DEFAULT_EMOJI    = "📦";
    public static final String DEFAULT_GRADIENT = "linear-gradient(135deg,#374151,#6b7280)";

    /** Kategori adı içeriğine göre emoji eşlemesi (alt-string match, sıralı). */
    private static final LinkedHashMap<String, String> EMOJI_MAP = new LinkedHashMap<>();

    /** Kategori adı içeriğine göre CSS gradient eşlemesi. */
    private static final LinkedHashMap<String, String[]> GRADIENT_MAP = new LinkedHashMap<>();

    static {
        // ── Emoji map ──────────────────────────────────────────────
        EMOJI_MAP.put("Elektronik",  "💻");
        EMOJI_MAP.put("Telefon",     "📱");
        EMOJI_MAP.put("Bilgisayar",  "🖥️");
        EMOJI_MAP.put("Giyim",       "👕");
        EMOJI_MAP.put("Moda",        "👗");
        EMOJI_MAP.put("Ayakkabı",    "👟");
        EMOJI_MAP.put("Çanta",       "👜");
        EMOJI_MAP.put("Aksesuar",    "🎒");
        EMOJI_MAP.put("Kitap",       "📚");
        EMOJI_MAP.put("Spor",        "⚽");
        EMOJI_MAP.put("Outdoor",     "🏕️");
        EMOJI_MAP.put("Ev",          "🏠");
        EMOJI_MAP.put("Mobilya",     "🛋️");
        EMOJI_MAP.put("Mutfak",      "🍳");
        EMOJI_MAP.put("Kozmetik",    "💄");
        EMOJI_MAP.put("Kişisel",     "🧴");
        EMOJI_MAP.put("Oyun",        "🎮");
        EMOJI_MAP.put("Bebek",       "🍼");
        EMOJI_MAP.put("Gıda",        "🥗");
        EMOJI_MAP.put("Oto",         "🚗");
        EMOJI_MAP.put("Bahçe",       "🌱");
        EMOJI_MAP.put("Evcil",       "🐾");
        EMOJI_MAP.put("Sağlık",      "💊");
        EMOJI_MAP.put("Müzik",       "🎵");

        // ── Gradient map ───────────────────────────────────────────
        GRADIENT_MAP.put("Elektronik",  new String[]{"#1e3a8a", "#3b82f6"});
        GRADIENT_MAP.put("Telefon",     new String[]{"#1e3a8a", "#3b82f6"});
        GRADIENT_MAP.put("Bilgisayar",  new String[]{"#1e3a8a", "#3b82f6"});
        GRADIENT_MAP.put("Giyim",       new String[]{"#4c1d95", "#7c3aed"});
        GRADIENT_MAP.put("Moda",        new String[]{"#4c1d95", "#7c3aed"});
        GRADIENT_MAP.put("Aksesuar",    new String[]{"#713f12", "#ca8a04"});
        GRADIENT_MAP.put("Kitap",       new String[]{"#14532d", "#16a34a"});
        GRADIENT_MAP.put("Spor",        new String[]{"#7c2d12", "#ea580c"});
        GRADIENT_MAP.put("Outdoor",     new String[]{"#7c2d12", "#ea580c"});
        GRADIENT_MAP.put("Ev",          new String[]{"#164e63", "#0891b2"});
        GRADIENT_MAP.put("Mobilya",     new String[]{"#164e63", "#0891b2"});
        GRADIENT_MAP.put("Mutfak",      new String[]{"#164e63", "#0891b2"});
        GRADIENT_MAP.put("Kozmetik",    new String[]{"#831843", "#db2777"});
        GRADIENT_MAP.put("Kişisel",     new String[]{"#831843", "#db2777"});
        GRADIENT_MAP.put("Oyun",        new String[]{"#1e3a8a", "#7c3aed"});
        GRADIENT_MAP.put("Bebek",       new String[]{"#065f46", "#22c55e"});
        GRADIENT_MAP.put("Gıda",        new String[]{"#065f46", "#22c55e"});
        GRADIENT_MAP.put("Sağlık",      new String[]{"#065f46", "#22c55e"});
    }

    /**
     * Kategori adına uygun emojiyi döner.
     * Eşleşme bulunamazsa {@link #DEFAULT_EMOJI} döner.
     */
    public String emoji(Object categoryObj) {
        if (categoryObj == null) return DEFAULT_EMOJI;
        String cat = categoryObj.toString();
        for (Map.Entry<String, String> e : EMOJI_MAP.entrySet()) {
            if (cat.contains(e.getKey())) return e.getValue();
        }
        return DEFAULT_EMOJI;
    }

    /**
     * Kategori adına uygun CSS gradient string'ini döner.
     * Örn: {@code "linear-gradient(135deg,#1e3a8a,#3b82f6)"}
     */
    public String gradient(Object categoryObj) {
        if (categoryObj == null) return DEFAULT_GRADIENT;
        String cat = categoryObj.toString();
        for (Map.Entry<String, String[]> e : GRADIENT_MAP.entrySet()) {
            if (cat.contains(e.getKey())) {
                String[] c = e.getValue();
                return "linear-gradient(135deg," + c[0] + "," + c[1] + ")";
            }
        }
        return DEFAULT_GRADIENT;
    }

    /**
     * JS catColors objesi için JSON üretir.
     * base.html script bloğunda {@code th:inline="javascript"} ile kullanılır.
     */
    public String catColorsJson() {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, String[]> e : GRADIENT_MAP.entrySet()) {
            String[] c = e.getValue();
            sb.append("'").append(e.getKey()).append("':['")
              .append(c[0]).append("','").append(c[1]).append("'],");
        }
        if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}

