package com.example.frontend.security;

import com.example.frontend.service.GuestSessionService;
import com.example.frontend.service.GuestSessionStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2 yetkilendirme isteğini HTTP session yerine cookie'de saklar.
 * Bu sayede auth-server'dan geri redirect geldiğinde session kaybı yaşanmaz.
 */
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final GuestSessionStore guestSessionStore;
    private final GuestSessionService guestSessionService;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        final String sessionId = guestSessionService.getSessionId(request);
        return guestSessionStore.get(sessionId);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {

        final HttpSession session = request.getSession();
        session.setAttribute("deneme", authorizationRequest);

        final String orCreateGuestId = guestSessionService.getOrCreateGuestId(request, response);

        guestSessionStore.save(orCreateGuestId, authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest req = loadAuthorizationRequest(request);
        guestSessionService.clearGuestIdCookie(response);
        return req;
    }
}

