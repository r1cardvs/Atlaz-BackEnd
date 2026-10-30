package com.example.AtlazDB.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.AtlazDB.model.Refueling;
import com.example.AtlazDB.model.ServiceOrder;
import com.example.AtlazDB.model.Vehicle;

class GenerateCsvTest {

    private final GenerateCsv generateCsv = new GenerateCsv();

    @Test
    void shouldGenerateCsvCoveringAllBranchesAndEscapingRules() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setPrefix("VTR-01");

        ServiceOrder orderWithRefueling = new ServiceOrder();
        orderWithRefueling.setId(10L);
        orderWithRefueling.setVehicle(vehicle);
        orderWithRefueling.setJustification("Text, with comma");
        orderWithRefueling.setRequester("Name \"with quotes\"");
        orderWithRefueling.setDestinationLocation("Line1\nLine2");

        ServiceOrder orderWithoutRefueling = new ServiceOrder();
        orderWithoutRefueling.setId(20L);
        orderWithoutRefueling.setVehicle(vehicle);
        orderWithoutRefueling.setJustification("SimpleText");

        Refueling linkedRefueling = new Refueling();
        linkedRefueling.setServiceOrder(orderWithRefueling);
        linkedRefueling.setLiters(BigDecimal.valueOf(30));
        linkedRefueling.setTotalValue(BigDecimal.valueOf(180));
        linkedRefueling.setReceiptNumber("REC-01");

        ServiceOrder otherOrder = new ServiceOrder();
        otherOrder.setId(99L);
        Refueling otherOrderRefueling = new Refueling();
        otherOrderRefueling.setServiceOrder(otherOrder);

        Refueling standaloneWithVehicle = new Refueling();
        standaloneWithVehicle.setServiceOrder(null);
        standaloneWithVehicle.setVehicle(vehicle);
        standaloneWithVehicle.setReceiptNumber("REC-02");

        Refueling standaloneWithoutVehicle = new Refueling();
        standaloneWithoutVehicle.setServiceOrder(null);
        standaloneWithoutVehicle.setVehicle(null);
        standaloneWithoutVehicle.setReceiptNumber("REC-03");

        byte[] result = generateCsv.generateCSV(
                List.of(orderWithRefueling, orderWithoutRefueling),
                List.of(linkedRefueling, otherOrderRefueling, standaloneWithVehicle, standaloneWithoutVehicle)
        );

        String csvContent = new String(result, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("\"Text, with comma\""));
        assertTrue(csvContent.contains("\"Name \"\"with quotes\"\"\""));
        assertTrue(csvContent.contains("\"Line1\nLine2\""));
        assertTrue(csvContent.contains("ABASTECIMENTO"));
    }
}