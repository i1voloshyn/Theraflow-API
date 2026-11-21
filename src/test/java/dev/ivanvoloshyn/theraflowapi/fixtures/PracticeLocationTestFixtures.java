package dev.ivanvoloshyn.theraflowapi.fixtures;

import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationRequest;
import dev.ivanvoloshyn.theraflowapi.practicelocation.model.PracticeLocation;

public class PracticeLocationTestFixtures {

    private static final PracticeLocationRequest locationRequest =
            PracticeLocationRequest.builder()
                    .name("Modern Therapy")
                    .city("Krakow")
                    .street("Prokocimska")
                    .buildingNumber("44A")
                    .unitNumber("13")
                    .postalCode("30-392")
                    .phoneNumber("+48432567987")
                    .build();


    public static PracticeLocationRequest validRequest() {
        return locationRequest;
    }

    public static PracticeLocation validModel() {
        PracticeLocation.Builder practiceLocationBuilder = new PracticeLocation.Builder(
                locationRequest.name(),
                locationRequest.city(),
                locationRequest.street(),
                locationRequest.buildingNumber(),
                locationRequest.unitNumber()
        );
        return practiceLocationBuilder.postalCode(locationRequest.postalCode())
                .phoneNumber(locationRequest.phoneNumber())
                .build();

    }
}
