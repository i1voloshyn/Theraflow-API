package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface TherapistMapper {

    Therapist toTherapist(TherapistRequest request);
}
