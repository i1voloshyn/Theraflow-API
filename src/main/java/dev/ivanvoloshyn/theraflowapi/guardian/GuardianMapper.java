package dev.ivanvoloshyn.theraflowapi.guardian;

import dev.ivanvoloshyn.theraflowapi.guardian.dto.GuardianResponse;
import dev.ivanvoloshyn.theraflowapi.guardian.model.Guardian;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface GuardianMapper {

    GuardianResponse toGuardianResponse(Guardian guardian);
}
