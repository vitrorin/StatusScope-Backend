package com.itesm.application.usecase;

import com.itesm.application.dto.HospitalGeoContextDto;
import com.itesm.domain.models.Hospital;
import com.itesm.domain.models.Municipality;
import com.itesm.domain.repository.MunicipalityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class HospitalGeoContextService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Inject
    MunicipalityRepository municipalityRepository;

    @ConfigProperty(name = "geo.assistant-radius-km", defaultValue = "75")
    double defaultAssistantRadiusKm;

    public HospitalGeoContextDto resolve(Hospital hospital) {
        return resolve(hospital, defaultAssistantRadiusKm);
    }

    public HospitalGeoContextDto resolve(Hospital hospital, double radiusKm) {
        HospitalGeoContextDto context = new HospitalGeoContextDto();
        context.setHospitalId(hospital.getId());
        context.setMunicipalityId(hospital.getMunicipalityId());
        context.setMunicipalityName(hospital.getMunicipalityName());
        context.setStateId(hospital.getStateId());
        context.setStateName(hospital.getStateName());
        context.setLatitude(hospital.getLatitude());
        context.setLongitude(hospital.getLongitude());
        context.setRadiusKm(radiusKm);
        context.setIncludedMunicipalityIds(resolveMunicipalityIdsWithinRadius(hospital, radiusKm));
        context.setNearbyStates(List.of());
        return context;
    }

    private List<UUID> resolveMunicipalityIdsWithinRadius(Hospital hospital, double radiusKm) {
        if (hospital.getLatitude() == null || hospital.getLongitude() == null) {
            return hospital.getMunicipalityId() == null ? List.of() : List.of(hospital.getMunicipalityId());
        }

        double hospitalLat = hospital.getLatitude().doubleValue();
        double hospitalLon = hospital.getLongitude().doubleValue();

        List<UUID> ids = municipalityRepository.listAllDomain().stream()
                .filter(municipality -> municipality.getLatitude() != null && municipality.getLongitude() != null)
                .filter(municipality -> distanceKm(
                        hospitalLat,
                        hospitalLon,
                        municipality.getLatitude().doubleValue(),
                        municipality.getLongitude().doubleValue()) <= radiusKm)
                .map(Municipality::getId)
                .toList();

        if (!ids.isEmpty() || hospital.getMunicipalityId() == null) {
            return ids;
        }

        return List.of(hospital.getMunicipalityId());
    }

    private double distanceKm(double originLat, double originLon, double targetLat, double targetLon) {
        double latDistance = Math.toRadians(targetLat - originLat);
        double lonDistance = Math.toRadians(targetLon - originLon);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(originLat)) * Math.cos(Math.toRadians(targetLat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
