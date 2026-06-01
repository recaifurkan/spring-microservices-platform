package com.example.frontend.security;

import com.example.frontend.dto.AddItemRequest;
import com.example.frontend.service.CartService;
import com.example.frontend.service.GuestCartService;
import com.example.frontend.service.GuestCartStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 / OIDC girişi başarıyla tamamlandığında devreye girer.
 *
 * <p>GUEST_CART_ID cookie'si okunur → GuestCartStore'dan sepet alınır →
 * cart-service'e aktarılır → cookie temizlenir → /dashboard'a yönlendirilir.
 *
 * <p>Bu yaklaşımda session bağımlılığı yoktur; GUEST_CART_ID tarayıcı tarafından
 * güvenilir şekilde OAuth2 callback isteğiyle gönderilir.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final CartService cartService;
    private final GuestCartService guestCartService;
    private final GuestCartStore guestCartStore;

    public OAuth2LoginSuccessHandler(CartService cartService,
                                     GuestCartService guestCartService,
                                     GuestCartStore guestCartStore) {
        super("/dashboard");
        this.cartService = cartService;
        this.guestCartService = guestCartService;
        this.guestCartStore = guestCartStore;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        final HttpSession session = request.getSession();

        String guestId = guestCartService.getGuestId(request);
        log.info("[GuestCart] GUEST_CART_ID cookie = {}", guestId);

        if (guestId != null) {
            Map<Long, Integer> guestItems = guestCartStore.remove(guestId);
            log.info("[GuestCart] Items to migrate: {}", guestItems);

            if (!guestItems.isEmpty()) {
                for (Map.Entry<Long, Integer> entry : guestItems.entrySet()) {
                    try {
                        cartService.addItem(new AddItemRequest(entry.getKey(), entry.getValue()));
                        log.info("[GuestCart] ✅ productId={} qty={}", entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        log.warn("[GuestCart] ❌ productId={}: {}", entry.getKey(), e.getMessage());
                    }
                }
                log.info("[GuestCart] Migration complete.");
            } else {
                log.info("[GuestCart] Guest cart was empty.");
            }

            guestCartService.clearGuestIdCookie(response);
        } else {
            log.info("[GuestCart] No GUEST_CART_ID cookie — nothing to migrate.");
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
