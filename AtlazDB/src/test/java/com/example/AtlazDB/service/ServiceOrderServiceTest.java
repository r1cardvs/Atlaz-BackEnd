package com.example.AtlazDB.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.AtlazDB.dto.ServiceOrderRequestDTO;
import com.example.AtlazDB.enums.CnhType;
import com.example.AtlazDB.enums.UserStatus;
import com.example.AtlazDB.enums.VehicleStatus;
import com.example.AtlazDB.model.ServiceOrder;
import com.example.AtlazDB.model.User;
import com.example.AtlazDB.model.Vehicle;
import com.example.AtlazDB.repository.ServiceOrderRepository;
import com.example.AtlazDB.repository.UserRepository;
import com.example.AtlazDB.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private ServiceOrderService serviceOrderService;

    private User defaultUser;
    private Vehicle defaultVehicle;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultUser.setId(1L);
        defaultUser.setCnhTypes(Set.of(CnhType.B));

        defaultVehicle = new Vehicle();
        defaultVehicle.setId(1L);
        defaultVehicle.setTipoCnhNecessaria(CnhType.B);
        defaultVehicle.setKm(1000.0);
    }

    @Test
    void shouldListAllServiceOrders() {
        when(repository.findAll()).thenReturn(List.of(new ServiceOrder()));
        assertEquals(1, serviceOrderService.listAll().size());
    }

    @Test
    void shouldFindServiceOrderById() {
        when(repository.findById(1L)).thenReturn(Optional.of(new ServiceOrder()));
        assertTrue(serviceOrderService.findById(1L).isPresent());
    }

    @Test
    void shouldFindServiceOrdersByUserId() {
        when(repository.findByUser_IdOrderByDepartureDateDesc(1L)).thenReturn(List.of(new ServiceOrder()));
        assertEquals(1, serviceOrderService.findByUserId(1L).size());
    }

    @Test
    void shouldFindServiceOrdersByMonthAndYear() {
        when(repository.findByPeriod(any(), any())).thenReturn(List.of(new ServiceOrder()));
        assertEquals(1, serviceOrderService.findByMonthAndYear(5, 2026).size());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundOnSave() {
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> serviceOrderService.save(dto));
    }

    @Test
    void shouldThrowExceptionWhenVehicleNotFoundOnSave() {
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(99L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> serviceOrderService.save(dto));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotHaveRequiredCnhOnSave() {
        defaultVehicle.setTipoCnhNecessaria(CnhType.D);
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));

        assertThrows(RuntimeException.class, () -> serviceOrderService.save(dto));
    }

    @Test
    void shouldThrowExceptionWhenArrivalKmIsLessThanLastRecordedOnSave() {
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);
        dto.setArrivalKm(BigDecimal.valueOf(900));

        ServiceOrder lastOrder = new ServiceOrder();
        lastOrder.setArrivalKm(BigDecimal.valueOf(1000));

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(repository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(lastOrder);

        assertThrows(RuntimeException.class, () -> serviceOrderService.save(dto));
    }

    @Test
    void shouldSaveServiceOrderWithoutArrivalKmAndWhenLastIsNull() {
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);
        dto.setArrivalKm(null);
        dto.setReturnDate(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(repository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(null);
        when(repository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceOrder result = serviceOrderService.save(dto);

        assertNotNull(result);
        assertEquals(VehicleStatus.EM_USO, defaultVehicle.getVehicleStatus());
        assertEquals(UserStatus.EM_CAMPO, defaultUser.getUserStatus());
        verify(vehicleService, never()).updateCurrentKm(any());
    }

    @Test
    void shouldSaveServiceOrderWhenLastExistsButArrivalKmIsNull() {
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);
        dto.setArrivalKm(null);
        dto.setReturnDate(LocalDateTime.now());

        ServiceOrder lastOrder = new ServiceOrder();
        lastOrder.setArrivalKm(BigDecimal.valueOf(1000));

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(repository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(lastOrder);
        when(repository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceOrder result = serviceOrderService.save(dto);

        assertNotNull(result);
        verify(vehicleService, never()).updateCurrentKm(any());
    }

    @Test
    void shouldSaveServiceOrderWithArrivalKmButNullReturnDate() {
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);
        dto.setArrivalKm(BigDecimal.valueOf(1200));
        dto.setReturnDate(null);

        ServiceOrder lastOrder = new ServiceOrder();
        lastOrder.setArrivalKm(BigDecimal.valueOf(1000));

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(repository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(lastOrder);
        when(repository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceOrder result = serviceOrderService.save(dto);

        assertNotNull(result);
        verify(vehicleService, never()).updateCurrentKm(any());
    }

    @Test
    void shouldSaveServiceOrderWithValidArrivalKmAndReturnDate() {
        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setUserId(1L);
        dto.setVehicleId(1L);
        dto.setArrivalKm(BigDecimal.valueOf(1500));
        dto.setReturnDate(LocalDateTime.now());

        ServiceOrder lastOrder = new ServiceOrder();
        lastOrder.setArrivalKm(BigDecimal.valueOf(1000));

        when(userRepository.findById(1L)).thenReturn(Optional.of(defaultUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(repository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(lastOrder);
        when(repository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceOrder result = serviceOrderService.save(dto);

        assertNotNull(result);
        verify(vehicleService).updateCurrentKm(1L);
    }

    @Test
    void shouldDeleteServiceOrder() {
        serviceOrderService.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenServiceOrderNotFoundOnUpdate() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> serviceOrderService.update(99L, new ServiceOrderRequestDTO()));
    }

    @Test
    void shouldThrowExceptionWhenReducingVehicleKmOnUpdate() {
        ServiceOrder existing = new ServiceOrder();
        existing.setVehicle(defaultVehicle);
        existing.setUser(defaultUser);

        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setArrivalKm(BigDecimal.valueOf(800));

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class, () -> serviceOrderService.update(1L, dto));
    }

    @Test
    void shouldUpdateServiceOrderWhenVehicleCurrentKmIsNullAndReturnDateProvided() {
        defaultVehicle.setKm(null);
        ServiceOrder existing = new ServiceOrder();
        existing.setVehicle(defaultVehicle);
        existing.setUser(defaultUser);

        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        dto.setArrivalKm(BigDecimal.valueOf(500));
        dto.setReturnDate(LocalDateTime.now());

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceOrder updated = serviceOrderService.update(1L, dto);

        assertNotNull(updated);
        assertEquals(BigDecimal.valueOf(500), updated.getArrivalKm());
        verify(vehicleService, times(2)).updateCurrentKm(1L);
    }

    @Test
    void shouldUpdateServiceOrderWhenArrivalKmIsNullOrReturnDateIsNull() {
        ServiceOrder existing = new ServiceOrder();
        existing.setVehicle(defaultVehicle);
        existing.setUser(defaultUser);

        ServiceOrderRequestDTO dtoNullArrival = new ServiceOrderRequestDTO();
        dtoNullArrival.setArrivalKm(null);
        dtoNullArrival.setReturnDate(null);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        serviceOrderService.update(1L, dtoNullArrival);

        ServiceOrderRequestDTO dtoNullReturnDate = new ServiceOrderRequestDTO();
        dtoNullReturnDate.setArrivalKm(BigDecimal.valueOf(1200));
        dtoNullReturnDate.setReturnDate(null);

        ServiceOrder updated = serviceOrderService.update(1L, dtoNullReturnDate);
        assertNotNull(updated);
    }

    @Test
    void shouldCountServiceOrdersByDateInterval() {
        when(repository.countByDepartureDateBetween(any(), any())).thenReturn(5L);
        Long count = serviceOrderService.countServiceOrdersByDateInterval(LocalDate.now(), LocalDate.now());
        assertEquals(5L, count);
    }

    @Test
    void shouldFindServiceOrdersByInterval() {
        when(repository.findByPeriod(any(), any())).thenReturn(List.of(new ServiceOrder()));
        List<ServiceOrder> list = serviceOrderService.findByInterval(LocalDate.now(), LocalDate.now());
        assertEquals(1, list.size());
    }
}