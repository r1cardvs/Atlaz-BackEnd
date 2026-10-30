package com.example.AtlazDB.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.AtlazDB.dto.RefuelingRequestDTO;
import com.example.AtlazDB.enums.FuelType;
import com.example.AtlazDB.model.City;
import com.example.AtlazDB.model.Refueling;
import com.example.AtlazDB.model.ServiceOrder;
import com.example.AtlazDB.model.User;
import com.example.AtlazDB.model.Vehicle;
import com.example.AtlazDB.repository.CityRepository;
import com.example.AtlazDB.repository.RefuelingRepository;
import com.example.AtlazDB.repository.ServiceOrderRepository;
import com.example.AtlazDB.repository.UserRepository;
import com.example.AtlazDB.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class RefuelingServiceTest {

    @Mock
    private RefuelingRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @InjectMocks
    private RefuelingService refuelingService;

    private User defaultUser;
    private Vehicle defaultVehicle;
    private City defaultCity;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultUser.setId(1L);

        defaultVehicle = new Vehicle();
        defaultVehicle.setId(1L);

        defaultCity = new City();
        defaultCity.setId(1L);
    }

    @Test
    void shouldListAllRefuelings() {
        when(repository.findAll()).thenReturn(List.of(new Refueling()));
        assertEquals(1, refuelingService.listAll().size());
    }

    @Test
    void shouldFindRefuelingById() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Refueling()));
        assertTrue(refuelingService.findById(1L).isPresent());
    }

    @Test
    void shouldThrowExceptionsOnSaveWhenEntitiesNotFound() {
        RefuelingRequestDTO dto = new RefuelingRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);
        dto.setCityId(1L);
        dto.setServiceOrderId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.save(dto));

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.save(dto));

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(cityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.save(dto));

        when(cityRepository.findById(1L)).thenReturn(Optional.of(defaultCity));
        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.save(dto));
    }

    @Test
    void shouldSaveRefuelingCoveringAllServiceOrderAndArrivalKmBranches() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(cityRepository.findById(1L)).thenReturn(Optional.of(defaultCity));
        when(repository.save(any(Refueling.class))).thenAnswer(inv -> inv.getArgument(0));

        RefuelingRequestDTO dtoNoSo = new RefuelingRequestDTO();
        dtoNoSo.setUserId(1L);
        dtoNoSo.setVehicleId(1L);
        dtoNoSo.setCityId(1L);
        dtoNoSo.setServiceOrderId(null);

        Refueling saved1 = refuelingService.save(dtoNoSo);
        assertNull(saved1.getCurrentKm());

        ServiceOrder soNoArrival = new ServiceOrder();
        soNoArrival.setArrivalKm(null);
        when(serviceOrderRepository.findById(2L)).thenReturn(Optional.of(soNoArrival));

        RefuelingRequestDTO dtoSoNoArrival = new RefuelingRequestDTO();
        dtoSoNoArrival.setUserId(1L);
        dtoSoNoArrival.setVehicleId(1L);
        dtoSoNoArrival.setCityId(1L);
        dtoSoNoArrival.setServiceOrderId(2L);

        Refueling saved2 = refuelingService.save(dtoSoNoArrival);
        assertNull(saved2.getCurrentKm());

        ServiceOrder soWithArrival = new ServiceOrder();
        soWithArrival.setArrivalKm(BigDecimal.valueOf(1500));
        when(serviceOrderRepository.findById(3L)).thenReturn(Optional.of(soWithArrival));

        RefuelingRequestDTO dtoSoWithArrival = new RefuelingRequestDTO();
        dtoSoWithArrival.setUserId(1L);
        dtoSoWithArrival.setVehicleId(1L);
        dtoSoWithArrival.setCityId(1L);
        dtoSoWithArrival.setServiceOrderId(3L);

        Refueling saved3 = refuelingService.save(dtoSoWithArrival);
        assertEquals(BigDecimal.valueOf(1500), saved3.getCurrentKm());
    }

    @Test
    void shouldDeleteRefueling() {
        refuelingService.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void shouldSearchWithFiltersCoveringAllBranches() {
        when(repository.findByFuelType(FuelType.DIESEL)).thenReturn(List.of(new Refueling()));
        when(repository.findByTotalValueGreaterThan(100.0)).thenReturn(List.of(new Refueling()));
        when(repository.findByCurrentKmGreaterThan(500.0)).thenReturn(List.of(new Refueling()));
        when(repository.findAll()).thenReturn(List.of(new Refueling()));

        assertEquals(1, refuelingService.searchWithFilters(FuelType.DIESEL, null, null).size());
        assertEquals(1, refuelingService.searchWithFilters(null, 100.0, null).size());
        assertEquals(1, refuelingService.searchWithFilters(null, null, 500.0).size());
        assertEquals(1, refuelingService.searchWithFilters(null, null, null).size());
    }

    @Test
    void shouldThrowExceptionsOnUpdateWhenEntitiesNotFound() {
        RefuelingRequestDTO dto = new RefuelingRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);
        dto.setCityId(1L);
        dto.setServiceOrderId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.update(1L, dto));

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.update(1L, dto));

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(cityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.update(1L, dto));

        when(cityRepository.findById(1L)).thenReturn(Optional.of(defaultCity));
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.update(1L, dto));

        when(repository.findById(1L)).thenReturn(Optional.of(new Refueling()));
        when(serviceOrderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> refuelingService.update(1L, dto));
    }

    @Test
    void shouldUpdateRefuelingCoveringAllServiceOrderAndArrivalKmBranches() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(cityRepository.findById(1L)).thenReturn(Optional.of(defaultCity));
        when(repository.findById(1L)).thenReturn(Optional.of(new Refueling()));
        when(repository.save(any(Refueling.class))).thenAnswer(inv -> inv.getArgument(0));

        RefuelingRequestDTO dtoNoSo = new RefuelingRequestDTO();
        dtoNoSo.setUserId(1L);
        dtoNoSo.setVehicleId(1L);
        dtoNoSo.setCityId(1L);
        dtoNoSo.setServiceOrderId(null);

        Refueling updated1 = refuelingService.update(1L, dtoNoSo);
        assertNull(updated1.getCurrentKm());

        ServiceOrder soNoArrival = new ServiceOrder();
        soNoArrival.setArrivalKm(null);
        when(serviceOrderRepository.findById(2L)).thenReturn(Optional.of(soNoArrival));

        RefuelingRequestDTO dtoSoNoArrival = new RefuelingRequestDTO();
        dtoSoNoArrival.setUserId(1L);
        dtoSoNoArrival.setVehicleId(1L);
        dtoSoNoArrival.setCityId(1L);
        dtoSoNoArrival.setServiceOrderId(2L);

        Refueling updated2 = refuelingService.update(1L, dtoSoNoArrival);
        assertNull(updated2.getCurrentKm());

        ServiceOrder soWithArrival = new ServiceOrder();
        soWithArrival.setArrivalKm(BigDecimal.valueOf(2200));
        when(serviceOrderRepository.findById(3L)).thenReturn(Optional.of(soWithArrival));

        RefuelingRequestDTO dtoSoWithArrival = new RefuelingRequestDTO();
        dtoSoWithArrival.setUserId(1L);
        dtoSoWithArrival.setVehicleId(1L);
        dtoSoWithArrival.setCityId(1L);
        dtoSoWithArrival.setServiceOrderId(3L);

        Refueling updated3 = refuelingService.update(1L, dtoSoWithArrival);
        assertEquals(BigDecimal.valueOf(2200), updated3.getCurrentKm());
    }

    @Test
    void shouldFindAndCountRefuelingsByDates() {
        when(repository.findByPeriod(any(), any())).thenReturn(List.of(new Refueling()));
        when(repository.countByDateTimeBetween(any(), any())).thenReturn(3L);

        assertNotNull(refuelingService.findByMonthAndYear(5, 2026));
        assertEquals(3L, refuelingService.countRefuelingsByDateInterval(LocalDate.now(), LocalDate.now()));
        assertEquals(1, refuelingService.findByInterval(LocalDate.now(), LocalDate.now()).size());
    }
}