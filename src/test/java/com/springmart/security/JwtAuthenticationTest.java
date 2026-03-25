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
                .andExpect(jsonPath("$.message").value("\u6050\u308c\u5165\u308a\u307e\u3059\u304c\u3001\u518d\u5ea6\u30ed\u30b0\u30a4\u30f3\u3092\u304a\u9858\u3044\u3044\u305f\u3057\u307e\u3059\u3002"));
    }
 
    @Test
    void testRequestWithInvalidSignature_ShouldReturn401() throws Exception {
        // Given: validateToken が false（署名不正）を返すように設定
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);
 
        // When & Then
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer invalid-signature-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("\u6050\u308c\u5165\u308a\u307e\u3059\u304c\u3001\u518d\u5ea6\u30ed\u30b0\u30a4\u30f3\u3092\u304a\u9858\u3044\u3044\u305f\u3057\u307e\u3059\u3002"));
    }
 
    @Test
    void testRequestWithMalformedHeader_ShouldReturn401() throws Exception {
        // When & Then: "Bearer " プレフィックスがない場合
        mockMvc.perform(get("/api/products")
                .header("Authorization", "invalid-format-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("\u6050\u308c\u5165\u308a\u307e\u3059\u304c\u3001\u518d\u5ea6\u30ed\u30b0\u30a4\u30f3\u3092\u304a\u9858\u3044\u3044\u305f\u3057\u307e\u3059\u3002"));
    }
 
    @Test
    void testAdminRequestToDeleteProduct_ShouldSucceed() throws Exception {
        // Given: ROLE_ADMIN を持つ有効なトークンをシミュレート
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn("admin");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("ROLE_ADMIN");
 
        // When & Then: 商品削除リクエスト（DELETE）
        mockMvc.perform(get("/api/products/1") // 取得は認証済ならOK (今回の実装では role 指定なし)
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testUserRequestToDeleteProduct_ShouldReturn403Forbidden() throws Exception {
        // Given: ROLE_USER を持つ有効なトークンをシミュレート
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("ROLE_USER");
 
        // When & Then: 管理者専用の「削除」リクエストを一般ユーザーが行う
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/products/1")
                .header("Authorization", "Bearer user-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("\u7533\u3057\u8a33\u3042\u308a\u307e\u305b\u3093\u304c\u3001\u3053\u306e\u64cd\u4f5c\u3092\u884c\u3046\u6a29\u9650\u3092\u304a\u6301\u3061\u3067\u306f\u306a\u3044\u3088\u3046\u3067\u3059\u3002"));
    }

    @Test
    void testRequestWithoutToken_ShouldReturn401() throws Exception {
        // When & Then: トークンなし
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("\u6050\u308c\u5165\u308a\u307e\u3059\u304c\u3001\u518d\u5ea6\u30ed\u30b0\u30a4\u30f3\u3092\u304a\u9858\u3044\u3044\u305f\u3057\u307e\u3059\u3002"));
    }
}
