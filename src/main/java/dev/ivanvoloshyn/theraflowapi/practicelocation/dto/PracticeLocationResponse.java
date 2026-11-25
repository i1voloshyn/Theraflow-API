package dev.ivanvoloshyn.theraflowapi.practicelocation.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PracticeLocationResponse(
        UUID id,
        String name,
        String city,
        String street,
        String buildingNumber,
        String unitNumber,
        String postalCode,
        String phoneNumber
) {
}
