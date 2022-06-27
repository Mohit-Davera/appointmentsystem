package com.simformsolutions.appointment.controller;

import com.simformsolutions.appointment.dto.doctor.DoctorDetailsDto;
import com.simformsolutions.appointment.service.DoctorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping("/")
    public DoctorDetailsDto registerDoctor(@Valid @RequestBody DoctorDetailsDto doctorDetailsDto){
        return doctorService.saveDoctor(doctorDetailsDto);
    }

}
