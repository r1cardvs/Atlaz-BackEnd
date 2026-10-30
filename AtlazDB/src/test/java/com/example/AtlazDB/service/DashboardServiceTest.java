package com.example.AtlazDB.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.AtlazDB.dto.DashboardResponseDTO;
import com.example.AtlazDB.enums.Profile;
import com.example.AtlazDB.enums.UserStatus;
import com.example.AtlazDB.enums.VehicleStatus;
import com.example.AtlazDB.model.Refueling;
import com.example.AtlazDB.model.ServiceOrder;
import com.example.AtlazDB.repository.RefuelingRepository;
import com.example.AtlazDB.repository.ServiceOrderRepository;
import com.example.AtlazDB.repository.UserRepository;
import com.example.AtlazDB.repository.VehicleRepository;
import com.example.AtlazDB.repository.projection.AtividadeProjection;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private RefuelingRepository refuelingRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldGetDashboardWithAllConsumptionFilterBranchesAndActivities() {
        when(vehicleRepository.countByVehicleStatus(VehicleStatus.EM_USO)).thenReturn(2);
        when(vehicleRepository.countByVehicleStatus(VehicleStatus.DISPONIVEL)).thenReturn(5);
        when(userRepository.countByProfileAndUserStatus(Profile.TECNICO, UserStatus.EM_CAMPO)).thenReturn(3);

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

        when(refuelingRepository.findAll()).thenReturn(List.of(r1, r2, r3, r4, r5, r6, r7));

        AtividadeProjection proj = mock(AtividadeProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getTipo()).thenReturn("OS");
        when(proj.getPrefixoViatura()).thenReturn("VTR-01");
        when(proj.getNomeTecnico()).thenReturn("Ricardo");
        when(proj.getDescricao()).thenReturn("Atendimento");
        when(proj.getStatus()).thenReturn("CONCLUIDO");
        when(proj.getCorStatus()).thenReturn("GREEN");

        when(serviceOrderRepository.findOrdensDeHoje()).thenReturn(List.of(proj));
        when(refuelingRepository.findAbastecimentosDeHoje()).thenReturn(List.of(proj));

        DashboardResponseDTO response = dashboardService.getDashboard();

        assertNotNull(response);
        assertEquals(2, response.data().todayActivities().size());
    }

    @Test
    void shouldGetDashboardWhenRefuelingsAreEmpty() {
        when(refuelingRepository.findAll()).thenReturn(List.of());
        when(serviceOrderRepository.findOrdensDeHoje()).thenReturn(List.of());
        when(refuelingRepository.findAbastecimentosDeHoje()).thenReturn(List.of());

        DashboardResponseDTO response = dashboardService.getDashboard();

        assertNotNull(response);
        assertNotNull(response.data());
    }
}