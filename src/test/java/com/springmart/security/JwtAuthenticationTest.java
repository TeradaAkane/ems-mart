package com.springmart.security;
 
import com.springmart.controller.ProductController;
import com.springmart.service.ProductService;
import com.springmart.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
 
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
 
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class})
@AutoConfigureMockMvc(addFilters = true)
public class JwtAuthenticationTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
 
    @MockBean
    private ProductService productService;
 
    @MockBean
    private UserRepository userRepository;
 
    @Test
    void testRequestWithExpiredToken_ShouldReturn401() throws Exception {
        // Given: validateToken が false（期限切れ）を返すように設定
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);
 
        // When & Then
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer expired-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("\u8a8d\u8a3c\u304c\u5fc5\u8981\u3067\u3059"));
    }
 
    @Test
    void testRequestWithInvalidSignature_ShouldReturn401() throws Exception {
        // Given: validateToken が false（署名不正）を返すように設定
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);
 
        // When & Then
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer invalid-signature-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
 
    @Test
    void testRequestWithMalformedHeader_ShouldReturn401() throws Exception {
        // When & Then: "Bearer " プレフィックスがない場合
        mockMvc.perform(get("/api/products")
                .header("Authorization", "invalid-format-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
 
    @Test
    void testRequestWithoutToken_ShouldReturn401() throws Exception {
        // When & Then: トークンなし
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
