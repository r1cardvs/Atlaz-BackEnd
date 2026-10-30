package com.example.AtlazDB.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.example.AtlazDB.dto.ServiceOrderRequestDTO;
import com.example.AtlazDB.model.Refueling;
import com.example.AtlazDB.model.ServiceOrder;
import com.example.AtlazDB.service.GenerateCsv;
import com.example.AtlazDB.service.RefuelingService;
import com.example.AtlazDB.service.ServiceOrderService;

@ExtendWith(MockitoExtension.class)
class ServiceOrderControllerTest {

    @Mock
    private ServiceOrderService service;

    @Mock
    private RefuelingService refuelingService;

    @Mock
    private GenerateCsv generateCsv;

    @InjectMocks
    private ServiceOrderController controller;

    @Test
    void shouldCoverBasicCrudAndQueryEndpoints() {
        when(service.listAll()).thenReturn(List.of(new ServiceOrder()));
        assertEquals(1, controller.list().size());

        assertNotNull(controller.listTypes());

        when(service.findById(1L)).thenReturn(Optional.of(new ServiceOrder()));
        assertNotNull(controller.findById(1L));

        when(service.findById(2L)).thenReturn(Optional.empty());
        assertNull(controller.findById(2L));

        when(service.findByUserId(1L)).thenReturn(List.of(new ServiceOrder()));
        assertEquals(200, controller.findByUserId(1L).getStatusCode().value());

        ServiceOrderRequestDTO dto = new ServiceOrderRequestDTO();
        when(service.save(dto)).thenReturn(new ServiceOrder());
        assertNotNull(controller.create(dto));

        when(service.update(1L, dto)).thenReturn(new ServiceOrder());
        assertNotNull(controller.update(1L, dto));

        controller.delete(1L);
        verify(service).delete(1L);

        when(service.findByMonthAndYear(5, 2026)).thenReturn(List.of(new ServiceOrder()));
        assertEquals(200, controller.findByMonth(5, 2026).getStatusCode().value());

        LocalDate today = LocalDate.now();
        when(service.countServiceOrdersByDateInterval(today, today)).thenReturn(4L);
        assertEquals(4L, controller.countByInterval(today, today).getBody());
    }

    @Test
    void shouldDownloadCsvCoveringAllDateMonthAndCategoryBranches() {
        LocalDate d1 = LocalDate.of(2026, 1, 1);
        LocalDate d2 = LocalDate.of(2026, 1, 31);

        when(service.findByInterval(d1, d2)).thenReturn(List.of(new ServiceOrder()));
        when(refuelingService.findByInterval(d1, d2)).thenReturn(List.of(new Refueling()));
        when(service.findByMonthAndYear(5, 2026)).thenReturn(List.of(new ServiceOrder()));
        when(refuelingService.findByMonthAndYear(5, 2026)).thenReturn(List.of(new Refueling()));
        when(service.listAll()).thenReturn(List.of(new ServiceOrder()));
        when(refuelingService.listAll()).thenReturn(List.of(new Refueling()));
        when(generateCsv.generateCSV(any(), any())).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> res1 = controller.downloadCSV(d1, d2, null, null, "abastecimento");
        assertEquals(200, res1.getStatusCode().value());

        ResponseEntity<byte[]> res2 = controller.downloadCSV(d1, null, 5, 2026, "os");
        assertEquals(200, res2.getStatusCode().value());

        ResponseEntity<byte[]> res3 = controller.downloadCSV(null, null, 5, null, null);
        assertEquals(200, res3.getStatusCode().value());

        ResponseEntity<byte[]> res4 = controller.downloadCSV(null, null, null, null, "todos");
        assertEquals(200, res4.getStatusCode().value());
    }
}