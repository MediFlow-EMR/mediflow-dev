package com.mediflow.emr.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class CookieUtilTest {
    private CookieProperties properties(boolean secure, String domain, String sameSite) {
        CookieProperties p = new CookieProperties();
        p.setSecure(secure); p.setDomain(domain); p.setSameSite(sameSite);
        p.setHttpOnly(true); p.setAccessTokenName("ACCESS_TOKEN"); p.setRefreshTokenName("REFRESH_TOKEN");
        p.setAccessMaxAge(900); p.setRefreshMaxAge(604800);
        return p;
    }
    @Test void localCookiesRemainHttpOnlyAndUseHostOnlyLax() {
        MockHttpServletResponse r = new MockHttpServletResponse();
        new CookieUtil(properties(false, "", "Lax")).addAccessTokenCookie(r, "test-token");
        String h = r.getHeader("Set-Cookie");
        assertTrue(h.contains("HttpOnly")); assertTrue(h.contains("SameSite=Lax"));
        assertFalse(h.contains("Secure")); assertFalse(h.contains("Domain="));
    }
    @Test void productionCookiesKeepSecureCrossSiteAttributes() {
        MockHttpServletResponse r = new MockHttpServletResponse();
        new CookieUtil(properties(true, ".mediflow-emr.cloud", "None")).addAccessTokenCookie(r, "test-token");
        String h = r.getHeader("Set-Cookie");
        assertTrue(h.contains("HttpOnly")); assertTrue(h.contains("Secure"));
        assertTrue(h.contains("SameSite=None")); assertTrue(h.contains("Domain=.mediflow-emr.cloud"));
    }
    @Test void logoutDeletesCookiesWithSameScope() {
        MockHttpServletResponse r = new MockHttpServletResponse();
        new CookieUtil(properties(false, "", "Lax")).deleteAccessTokenCookie(r);
        assertTrue(r.getHeader("Set-Cookie").contains("Max-Age=0"));
        assertFalse(r.getHeader("Set-Cookie").contains("Domain="));
    }
}
