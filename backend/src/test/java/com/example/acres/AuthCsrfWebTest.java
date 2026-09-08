package com.example.acres;

import com.example.acres.controller.AuthController;
import com.example.acres.dto.AuthDtos.AuthResponse;
import com.example.acres.dto.AuthDtos.UserResponse;
import com.example.acres.security.AuthCookieService;
import com.example.acres.security.AuthCsrfFilter;
import com.example.acres.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthCsrfWebTest {
    @Mock
    AuthService authService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        AuthCookieService cookies = new AuthCookieService();
        ReflectionTestUtils.setField(cookies, "secure", false);
        ReflectionTestUtils.setField(cookies, "sameSite", "Lax");
        ReflectionTestUtils.setField(cookies, "domain", "");
        ReflectionTestUtils.setField(cookies, "refreshTokenDays", 30L);

        AuthController controller = new AuthController(authService, cookies);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilter(new AuthCsrfFilter())
                .build();
    }

    @Test
    void csrfReturnsTokenInJsonAndSetsCookie() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void loginRejectsMissingCsrfHeader_crossOriginScenario() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrf.getResponse().getCookie("XSRF-TOKEN"))
                        .content("{\"email\":\"a@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginAcceptsMatchingCsrfHeaderAndCookie_crossOriginScenario() throws Exception {
        when(authService.login(anyString(), anyString(), anyString(), any()))
                .thenReturn(new AuthService.Session(
                        new AuthResponse("access-token", new UserResponse(1L, "Admin", "a@example.com", "ADMIN", true, false, null)),
                        "refresh-token"));

        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-XSRF-TOKEN", token)
                        .cookie(csrf.getResponse().getCookie("XSRF-TOKEN"))
                        .content("{\"email\":\"a@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        verify(authService).login(anyString(), anyString(), anyString(), any());
    }
}
