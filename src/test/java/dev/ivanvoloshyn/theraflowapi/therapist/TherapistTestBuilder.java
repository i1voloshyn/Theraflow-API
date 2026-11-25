package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistRequest;

public class TherapistTestBuilder {

    public static TherapistRequest defaultTherapistRequest() {
        return new TherapistRequest("Joe", "Toronto", "LICENSE-123", "Therapist");
    }

    public static TherapistRequest therapistRequestWithWhitespace() {
        return new TherapistRequest(" Joe ", " Toronto ", " LICENSE-123 ", " Therapist ");
    }
    public static Therapist defaultTestTherapist() {
        return new Therapist(
                "Joe",
                "Toronto",
                "PL 1234",
                "Just Doc"
        );
    }
}
