package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.practicelocation.model.PracticeLocation;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class TherapistSpecifications {

    public static Specification<Therapist> hasCity(String city) {
        return ((root, query, criteriaBuilder) -> {
            Join<Therapist, PracticeLocation> therapist = root.join("locations");
            return criteriaBuilder.equal(therapist.get("city"), city);
        });
    }
}
