package com.simformsolutions.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.simformsolutions.appointment.dto.AppointmentDoctorDto;
import com.simformsolutions.appointment.dto.appointment.AppointmentDetailsDto;
import com.simformsolutions.appointment.dto.user.UserDetailsDto;
import com.simformsolutions.appointment.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AppointmentControllerTest {

    @MockBean
    private AppointmentService appointmentService;

    @Autowired
    private MockMvc mockMvc;

    static final String BASE_URL = "/appointment";
    static final AppointmentDetailsDto APPOINTMENT_DETAILS_DTO= new AppointmentDetailsDto("ayurveda","random issue", LocalDate.parse("17/12/2022", DateTimeFormatter.ofPattern("dd/MM/yyyy")),"random user");
    static final AppointmentDoctorDto APPOINTMENT_DOCTOR_DTO = new AppointmentDoctorDto(1, 1, "Ravi D", 1, "ayurveda", LocalTime.parse("10:00", DateTimeFormatter.ofPattern("HH:mm")), LocalDate.parse("17/12/2022", DateTimeFormatter.ofPattern("dd/MM/yyyy")), "BOOKED");

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    ObjectWriter objectWriter = objectMapper.writer();

    @Test
    void bookAppointmentSuccess() throws Exception {
        String content = objectWriter.writeValueAsString(APPOINTMENT_DETAILS_DTO);

        Mockito.when(appointmentService.saveAppointment(APPOINTMENT_DETAILS_DTO,1)).thenReturn(APPOINTMENT_DOCTOR_DTO);

        MockHttpServletRequestBuilder mockRequest = post(BASE_URL + "/book")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(content).param("userId", String.valueOf(1));
        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string(objectWriter.writeValueAsString(APPOINTMENT_DOCTOR_DTO)));
    }
}
