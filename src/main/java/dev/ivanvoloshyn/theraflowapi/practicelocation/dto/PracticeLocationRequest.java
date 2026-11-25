package dev.ivanvoloshyn.theraflowapi.practicelocation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
@Builder
public record PracticeLocationRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = NONBLANK_TRIMMED_TEXT, message = TEXT_MESSAGE)
        String name,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = NONBLANK_TRIMMED_TEXT, message = TEXT_MESSAGE)
        String city,

        @NotBlank
        @Size(max = 150)
        @Pattern(regexp = NONBLANK_TRIMMED_TEXT, message = TEXT_MESSAGE)
        String street,

        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = NONBLANK_TRIMMED_TEXT, message = TEXT_MESSAGE)
        String buildingNumber,

        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = NONBLANK_TRIMMED_TEXT, message = TEXT_MESSAGE)
        String unitNumber,

        @NotNull
        @Pattern(regexp = "\\d{2}-\\d{3}", message = "must be a Polish postal code (NN-NNN)")
        String postalCode,

        @Pattern(regexp = "\\+[1-9][0-9]{1,14}",
                message = "must be an international phone number starting with + and containing 2 to 15 digits")
        String phoneNumber
) {

    private static final String NONBLANK_TRIMMED_TEXT = "(?s)\\S(?:.*\\S)?";
    private static final String TEXT_MESSAGE = "must not be blank or have leading or trailing spaces";
}
