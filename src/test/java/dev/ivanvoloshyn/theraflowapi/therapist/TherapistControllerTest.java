package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.config.SecurityConfiguration;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorResponse;
import dev.ivanvoloshyn.theraflowapi.jwt.JwtAuthTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.SecurityErrorHandler;
import dev.ivanvoloshyn.theraflowapi.security.UserDetailsServiceImpl;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistRequest;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.ACCOUNT_ID;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TherapistController.class)
@Import({SecurityConfiguration.class, SecurityErrorHandler.class})
class TherapistControllerTest {

    private static final String THERAPISTS_ENDPOINT = "/api/v1/therapists";

    @MockitoBean
    private TherapistService therapistService;
    @MockitoBean
    private JwtAuthTokenProvider jwtAuthTokenProvider;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @DisplayName("""
            saveTherapist
            SHOULD return CREATED with the therapist profile
            FOR a valid request from an authenticated therapist
            """)
    @Test
    void saveTherapist_success() throws Exception {
        TherapistRequest request = TherapistTestBuilder.defaultTherapistRequest();
        TherapistResponse expectedResponse = new TherapistResponse(
                ACCOUNT_ID, request.firstName(), request.lastName(), request.licenseNumber(), request.professionalTitle());

        when(therapistService.saveProfile(request, ACCOUNT_ID)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(post(THERAPISTS_ENDPOINT)
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        TherapistResponse actualResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TherapistResponse.class);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(therapistService).saveProfile(request, ACCOUNT_ID);
    }

    @DisplayName("""
            saveTherapist
            SHOULD return FORBIDDEN with ACCESS_DENIED
            WHEN the service denies profile creation
            """)
    @Test
    void saveTherapist_error() throws Exception {
        TherapistRequest request = TherapistTestBuilder.defaultTherapistRequest();

        when(therapistService.saveProfile(request, ACCOUNT_ID))
                .thenThrow(new PermissionException(ErrorCode.ACCESS_DENIED));

        MvcResult result = mockMvc.perform(post(THERAPISTS_ENDPOINT)
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(therapistService).saveProfile(request, ACCOUNT_ID);
    }

    @DisplayName("""
            saveTherapist
            SHOULD return BAD_REQUEST with a required-field error without calling the service
            WHEN a required field is null, empty, or whitespace-only
            """)
    @ParameterizedTest(name = "[{index}] {0}: {1}")
    @MethodSource("invalidTherapistRequests")
    void saveTherapist_whenRequiredFieldIsBlank_returnsBadRequest(
            String field, String invalidValueDescription, TherapistRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(THERAPISTS_ENDPOINT)
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(error.errors()).singleElement().satisfies(validationError -> {
            assertThat(validationError.field()).isEqualTo(field);
            assertThat(validationError.code()).isEqualTo(ErrorCode.REQUIRED_FIELD.name());
        });
        verifyNoInteractions(therapistService);
    }

    @DisplayName("getTherapist SHOULD return the therapist WHEN the id exists")
    @Test
    void getTherapist_success() throws Exception {
        UUID therapistId = UUID.randomUUID();
        TherapistResponse expectedResponse = new TherapistResponse(
                therapistId, "Joe", "Toronto", "LICENSE-123", "Therapist");

        when(therapistService.findTherapistById(therapistId)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(get(THERAPISTS_ENDPOINT + "/{id}", therapistId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        TherapistResponse actualResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), TherapistResponse.class);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(therapistService).findTherapistById(therapistId);
    }

    @DisplayName("getTherapist SHOULD return NOT_FOUND WHEN the id does not exist")
    @Test
    void getTherapist_whenIdDoesNotExist_returnsNotFound() throws Exception {
        UUID therapistId = UUID.randomUUID();
        when(therapistService.findTherapistById(therapistId))
                .thenThrow(new EntityNotFoundException(ErrorCode.THERAPIST_NOT_FOUND));

        MvcResult result = mockMvc.perform(get(THERAPISTS_ENDPOINT + "/{id}", therapistId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.THERAPIST_NOT_FOUND);
        verify(therapistService).findTherapistById(therapistId);
    }

    @DisplayName("""
            findAll
            SHOULD return a page of therapists
            FOR the requested city and pagination
            """)
    @Test
    void findAll_success() throws Exception {
        String city = "Kraków";
        PageRequest pageable = PageRequest.of(1, 2);
        TherapistResponse therapist = new TherapistResponse(
                UUID.randomUUID(), "Joe", "Toronto", "LICENSE-123", "Therapist");

        when(therapistService.getAll(city, pageable))
                .thenReturn(new PageImpl<>(List.of(therapist), pageable, 3));

        MvcResult result = mockMvc.perform(get(THERAPISTS_ENDPOINT)
                        .with(user(authenticatedUser()))
                        .param("city", city)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        TherapistPageResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TherapistPageResponse.class);

        assertThat(response.content()).containsExactly(therapist);
        assertThat(response.page()).isEqualTo(new PageMetadata(2, 1, 3));

        verify(therapistService).getAll(city, pageable);
    }

    @DisplayName("""
            findAll
            SHOULD return BAD_REQUEST without calling the service
            WHEN city is missing
            """)
    @Test
    void findAll_whenCityIsMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(get(THERAPISTS_ENDPOINT)
                        .with(user(authenticatedUser())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(therapistService);
    }

    private static Stream<Arguments> invalidTherapistRequests() {
        TherapistRequest valid = TherapistTestBuilder.defaultTherapistRequest();
        return Stream.of(null, "", " \t\n")
                .flatMap(value -> {
                    String description = value == null ? "null" : value.isEmpty() ? "empty" : "whitespace-only";
                    return Stream.of(
                            Arguments.of("firstName", description, new TherapistRequest(
                                    value, valid.lastName(), valid.licenseNumber(), valid.professionalTitle())),
                            Arguments.of("lastName", description, new TherapistRequest(
                                    valid.firstName(), value, valid.licenseNumber(), valid.professionalTitle())),
                            Arguments.of("licenseNumber", description, new TherapistRequest(
                                    valid.firstName(), valid.lastName(), value, valid.professionalTitle())),
                            Arguments.of("professionalTitle", description, new TherapistRequest(
                                    valid.firstName(), valid.lastName(), valid.licenseNumber(), value))
                    );
                });
    }

    private TheraflowUser authenticatedUser() {
        return new TheraflowUser(ACCOUNT_ID, DEFAULT_EMAIL, null, AccountType.THERAPIST, AccountStatus.ACTIVE);
    }

    private record TherapistPageResponse(List<TherapistResponse> content, PageMetadata page) {
    }

    private record PageMetadata(int size, int number, long totalElements) {
    }
}
