package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.config.SecurityConfiguration;
import dev.ivanvoloshyn.theraflowapi.fixtures.SignUpTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthenticationController.class)
@Import({SecurityConfiguration.class})
class AuthenticationControllerTest {

    private static final String SIGN_UP_ENDPOINT = "/api/v1/auth/password/sign-up";

    @MockitoBean
    private PasswordAuthenticationService passwordAuthenticationService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @DisplayName("""
            signUp
            SHOULD successfully create new account
            RETURN SignUpResponse
            FOR valid request
            """)
    @Test
    void signUp_success() throws Exception {
        PasswordSignUpRequest request = SignUpTestBuilder.defaultTherapistSignUpRequest();

        SignUpResponse expectedResponse = SignUpTestBuilder.defaultTherapistSignUpResponse();

        when(passwordAuthenticationService.signUp(request)).thenReturn(expectedResponse);

        MvcResult mvcResult = mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        SignUpResponse actualResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SignUpResponse.class);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(passwordAuthenticationService).signUp(request);
    }

}