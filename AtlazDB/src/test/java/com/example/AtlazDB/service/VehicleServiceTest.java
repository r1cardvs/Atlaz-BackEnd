package com.example.AtlazDB.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.AtlazDB.dto.VehicleRequestDTO;
import com.example.AtlazDB.dto.VehicleResponseDTO;
import com.example.AtlazDB.enums.CnhType;
import com.example.AtlazDB.enums.VehicleStatus;
import com.example.AtlazDB.service.BusinessRuleException;
import com.example.AtlazDB.model.Model;
import com.example.AtlazDB.model.Refueling;
import com.example.AtlazDB.model.ServiceOrder;
import com.example.AtlazDB.model.User;
import com.example.AtlazDB.model.Vehicle;
import com.example.AtlazDB.repository.ModelRepository;
import com.example.AtlazDB.repository.RefuelingRepository;
import com.example.AtlazDB.repository.ServiceOrderRepository;
import com.example.AtlazDB.repository.UserRepository;
import com.example.AtlazDB.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository repository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefuelingRepository refuelingRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Model defaultModel;
    private Vehicle defaultVehicle;

    @BeforeEach
    void setUp() {
        defaultModel = new Model();
        defaultModel.setId(1L);
        defaultModel.setModelName("Hilux");
        defaultModel.setBrandName("Toyota");

        defaultVehicle = new Vehicle();
        defaultVehicle.setId(1L);
        defaultVehicle.setPrefix("VTR-01");
        defaultVehicle.setVehicleStatus(VehicleStatus.DISPONIVEL);
        defaultVehicle.setTipoCnhNecessaria(CnhType.B);
        defaultVehicle.setModel(defaultModel);
        defaultVehicle.setKm(1000.0);
        defaultVehicle.setKmTrocaOleo(BigDecimal.valueOf(11000.0));
    }

    @Test
    void shouldFindAvailableVehiclesByUserFilteringByStatusAndCnh() {
        User user = new User();
        user.setId(1L);
        user.setCnhTypes(Set.of(CnhType.B));

        Vehicle availableCompatible = defaultVehicle;

        Vehicle unavailableVehicle = new Vehicle();
        unavailableVehicle.setId(2L);
        unavailableVehicle.setVehicleStatus(VehicleStatus.DESATIVADA);
        unavailableVehicle.setTipoCnhNecessaria(CnhType.B);
        unavailableVehicle.setModel(defaultModel);

        Vehicle incompatibleCnhVehicle = new Vehicle();
        incompatibleCnhVehicle.setId(3L);
        incompatibleCnhVehicle.setVehicleStatus(VehicleStatus.DISPONIVEL);
        incompatibleCnhVehicle.setTipoCnhNecessaria(CnhType.D);
        incompatibleCnhVehicle.setModel(defaultModel);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.findAll()).thenReturn(List.of(availableCompatible, unavailableVehicle, incompatibleCnhVehicle));

        List<VehicleResponseDTO> result = vehicleService.findAvailableVehiclesByUser(1L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundInFindAvailableVehicles() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> vehicleService.findAvailableVehiclesByUser(99L));
    }

    @Test
    void shouldListAllVehicles() {
        when(repository.findAll()).thenReturn(List.of(defaultVehicle));

        List<VehicleResponseDTO> result = vehicleService.listAll();

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindVehicleByIdSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(defaultVehicle));

        VehicleResponseDTO result = vehicleService.findById(1L);

        assertNotNull(result);
    }

    @Test
    void shouldThrowExceptionWhenVehicleNotFoundById() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> vehicleService.findById(99L));
    }

    @Test
    void shouldSaveVehicleWithAllExplicitValues() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setModelId(1L);
        dto.setPrefix("VTR-02");
        dto.setTipoCnhNecessaria(CnhType.C);
        dto.setStatus(VehicleStatus.DESATIVADA);
        dto.setKm(5000.0);
        dto.setKmTrocaOleo(15000.0);

        when(modelRepository.findById(1L)).thenReturn(Optional.of(defaultModel));
        when(repository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponseDTO result = vehicleService.save(dto);

        assertNotNull(result);
        verify(repository).save(any(Vehicle.class));
    }

    @Test
    void shouldSaveVehicleWithDefaultValuesWhenNull() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setModelId(1L);
        dto.setPrefix("VTR-03");
        dto.setTipoCnhNecessaria(null);
        dto.setStatus(null);
        dto.setKm(null);
        dto.setKmTrocaOleo(null);

        when(modelRepository.findById(1L)).thenReturn(Optional.of(defaultModel));
        when(repository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponseDTO result = vehicleService.save(dto);

        assertNotNull(result);
        verify(repository).save(any(Vehicle.class));
    }

    @Test
    void shouldThrowExceptionWhenModelNotFoundOnSave() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setModelId(99L);

        when(modelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> vehicleService.save(dto));
    }

    @Test
    void shouldDeleteVehicleById() {
        vehicleService.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenVehicleNotFoundOnUpdate() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> vehicleService.update(99L, dto));
    }

    @Test
    void shouldThrowExceptionWhenModelNotFoundOnUpdate() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setModelId(99L);

        when(repository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(modelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> vehicleService.update(1L, dto));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenDeactivatingVehicleWithActiveOrder() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setModelId(1L);
        dto.setStatus(VehicleStatus.DESATIVADA);

        when(repository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(modelRepository.findById(1L)).thenReturn(Optional.of(defaultModel));
        when(serviceOrderRepository.existsByVehicleIdAndReturnDateIsNull(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> vehicleService.update(1L, dto));
    }

    @Test
    void shouldUpdateVehicleWhenDeactivatingWithoutActiveOrderAndAllFieldsProvided() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setModelId(1L);
        dto.setStatus(VehicleStatus.DESATIVADA);
        dto.setKm(2500.0);
        dto.setTipoCnhNecessaria(CnhType.D);

        when(repository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(modelRepository.findById(1L)).thenReturn(Optional.of(defaultModel));
        when(serviceOrderRepository.existsByVehicleIdAndReturnDateIsNull(1L)).thenReturn(false);
        when(repository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponseDTO result = vehicleService.update(1L, dto);

        assertNotNull(result);
        verify(repository).save(any(Vehicle.class));
    }

    @Test
    void shouldUpdateVehicleWhenOptionalFieldsAreNull() {
        VehicleRequestDTO dto = new VehicleRequestDTO();
        dto.setModelId(1L);
        dto.setStatus(null);
        dto.setKm(null);
        dto.setTipoCnhNecessaria(null);

        when(repository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(modelRepository.findById(1L)).thenReturn(Optional.of(defaultModel));
        when(repository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponseDTO result = vehicleService.update(1L, dto);

        assertNotNull(result);
    }

    @Test
    void shouldDoNothingInUpdateCurrentKmWhenLastOrderIsNull() {
        when(serviceOrderRepository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(null);

        vehicleService.updateCurrentKm(1L);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDoNothingInUpdateCurrentKmWhenArrivalKmIsNull() {
        ServiceOrder order = new ServiceOrder();
        order.setArrivalKm(null);

        when(serviceOrderRepository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(order);

        vehicleService.updateCurrentKm(1L);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateCurrentKmSuccessfully() {
        ServiceOrder order = new ServiceOrder();
        order.setArrivalKm(BigDecimal.valueOf(3000.0));

        when(serviceOrderRepository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(order);
        when(repository.findById(1L)).thenReturn(Optional.of(defaultVehicle));

        vehicleService.updateCurrentKm(1L);

        assertEquals(3000.0, defaultVehicle.getKm());
        verify(repository).save(defaultVehicle);
    }

    @Test
    void shouldThrowExceptionInUpdateCurrentKmWhenVehicleNotFound() {
        ServiceOrder order = new ServiceOrder();
        order.setArrivalKm(BigDecimal.valueOf(3000.0));

        when(serviceOrderRepository.findTopByVehicle_IdAndReturnDateIsNotNullOrderByReturnDateDesc(99L))
                .thenReturn(order);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> vehicleService.updateCurrentKm(99L));
    }

    @Test
    void shouldCalculateAverageConsumptionCoveringAllFilterBranches() {
        Refueling r1 = new Refueling();
        r1.setServiceOrder(null);

        ServiceOrder soNoArrival = new ServiceOrder();
        soNoArrival.setArrivalKm(null);
        Refueling r2 = new Refueling();
        r2.setServiceOrder(soNoArrival);

        ServiceOrder soNoDeparture = new ServiceOrder();
        soNoDeparture.setArrivalKm(BigDecimal.valueOf(200));
        soNoDeparture.setDepartureKm(null);
        Refueling r3 = new Refueling();
        r3.setServiceOrder(soNoDeparture);

        ServiceOrder soValidKm = new ServiceOrder();
        soValidKm.setDepartureKm(BigDecimal.valueOf(100));
        soValidKm.setArrivalKm(BigDecimal.valueOf(200));
        Refueling r4 = new Refueling();
        r4.setServiceOrder(soValidKm);
        r4.setLiters(null);

        Refueling r5 = new Refueling();
        r5.setServiceOrder(soValidKm);
        r5.setLiters(BigDecimal.ZERO);

        ServiceOrder soNegativeKm = new ServiceOrder();
        soNegativeKm.setDepartureKm(BigDecimal.valueOf(200));
        soNegativeKm.setArrivalKm(BigDecimal.valueOf(100));
        Refueling r6 = new Refueling();
        r6.setServiceOrder(soNegativeKm);
        r6.setLiters(BigDecimal.valueOf(10));

        Refueling r7 = new Refueling();
        r7.setServiceOrder(soValidKm);
        r7.setLiters(BigDecimal.valueOf(10));

        when(refuelingRepository.findByVehicleId(1L))
                .thenReturn(List.of(r1, r2, r3, r4, r5, r6, r7));

        Double average = vehicleService.calculateAverageConsumption(1L);

        assertEquals(10.0, average);
    }

    @Test
    void shouldReturnZeroAverageConsumptionWhenListIsEmpty() {
        when(refuelingRepository.findByVehicleId(1L)).thenReturn(List.of());

        Double average = vehicleService.calculateAverageConsumption(1L);

        assertEquals(0.0, average);
    }

    @Test
    void shouldUpdateOilChangeKmSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(defaultVehicle));
        when(repository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponseDTO result = vehicleService.atualizarKmTrocaOleo(1L, BigDecimal.valueOf(20000));

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(20000), defaultVehicle.getKmTrocaOleo());
    }

    @Test
    void shouldThrowExceptionWhenVehicleNotFoundOnUpdateOilChangeKm() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> vehicleService.atualizarKmTrocaOleo(99L, BigDecimal.valueOf(20000)));
    }
}