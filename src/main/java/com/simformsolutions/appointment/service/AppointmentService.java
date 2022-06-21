package com.simformsolutions.appointment.service;

import com.simformsolutions.appointment.converter.AppointmentDoctorDtoConverter;
import com.simformsolutions.appointment.dto.AppointmentDoctorDto;
import com.simformsolutions.appointment.dto.appointment.AppointmentDetailsDto;
import com.simformsolutions.appointment.enums.AppointmentStatus;
import com.simformsolutions.appointment.excepetion.*;
import com.simformsolutions.appointment.model.Appointment;
import com.simformsolutions.appointment.model.Doctor;
import com.simformsolutions.appointment.model.Schedule;
import com.simformsolutions.appointment.model.User;
import com.simformsolutions.appointment.projection.DoctorView;
import com.simformsolutions.appointment.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    private final UserRepository userRepository;

    private final AppointmentDoctorDtoConverter appointmentDoctorDtoConverter;
    private final SpecialityRepository specialityRepository;
    private final AppointmentRepository appointmentRepository;

    private final ModelMapper modelMapper;

    public AppointmentService(ScheduleRepository scheduleRepository, DoctorRepository doctorRepository, UserRepository userRepository, AppointmentDoctorDtoConverter appointmentDoctorDtoConverter, SpecialityRepository specialityRepository, AppointmentRepository appointmentRepository,ModelMapper modelMapper) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.appointmentDoctorDtoConverter = appointmentDoctorDtoConverter;
        this.specialityRepository = specialityRepository;
        this.appointmentRepository = appointmentRepository;
        this.modelMapper = modelMapper;
    }

    public List<AppointmentDoctorDto> checkSchedule(ArrayList<Doctor> doctors, Appointment userAppointment) {

        LocalTime currentTime = getCurrentLocalTime();
        LocalDateTime currentDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        //Predicates
        Predicate<Appointment> sameDateFilter = appointment -> appointment.getDate().equals(userAppointment.getDate());
        Predicate<Appointment> statusFilter = appointment -> appointment.getStatus().equals(AppointmentStatus.BOOKED);
        Predicate<Doctor> appointmentNullFilter = doctor -> doctor.getAppointments().size() == 0;


        //Add Doctor Who Don't Have Any Appointments
        ArrayList<Doctor> freeDoctors = new ArrayList<>(doctors.stream().filter(appointmentNullFilter).toList());
        List<AppointmentDoctorDto> availableDoctors = new ArrayList<>(appointmentDoctorDtoConverter.freeDoctorToBookedDoctorConverter(freeDoctors, userAppointment, currentTime));

        if (freeDoctors.size() != 0) {
            doctors.removeAll(freeDoctors);
        }
        if (doctors.size() != 0) {

            AppointmentDoctorDto appointmentDoctorDto;
            LocalTime doctorBookedTillTime;
            LocalDateTime doctorBookedTillDateTime;

            for (Doctor d : doctors) {
                Optional<LocalDateTime> optionalDoctorBookedTillDateTime = d.getAppointments().stream().filter(sameDateFilter.and(statusFilter)).map(
                        appointment -> LocalDateTime.of(appointment.getDate(),appointment.getEndTime())
                ).max(LocalDateTime::compareTo);
                if (optionalDoctorBookedTillDateTime.isEmpty()) {
                    doctorBookedTillTime = userAppointment.getDate().getDayOfMonth() <= LocalDate.now().getDayOfMonth()?currentTime:d.getEntryTime();
                    appointmentDoctorDto = new AppointmentDoctorDto(d.getDoctorId(), d.getFirstName() + " " + d.getLastName(), d.getExperience(), userAppointment.getSpeciality(), (doctorBookedTillTime), userAppointment.getDate());
                    availableDoctors.add(appointmentDoctorDto);
                }
                if (optionalDoctorBookedTillDateTime.isPresent()) {
                    doctorBookedTillDateTime = optionalDoctorBookedTillDateTime.get();
                    if ((doctorBookedTillDateTime.equals(currentDateTime) || doctorBookedTillDateTime.isBefore(currentDateTime)) && currentDateTime.plusMinutes(60).getHour() + 1 < d.getExitTime().getHour()) {
                        appointmentDoctorDto = new AppointmentDoctorDto(d.getDoctorId(), d.getFirstName() + " " + d.getLastName(), d.getExperience(), userAppointment.getSpeciality(), currentTime, userAppointment.getDate());
                        availableDoctors.add(appointmentDoctorDto);
                    } else if (doctorBookedTillDateTime.isAfter(currentDateTime) && doctorBookedTillDateTime.plusMinutes(60).getHour() + 1 < d.getExitTime().getHour()) {
                        String pattern = "HH:m";
                        doctorBookedTillTime = LocalTime.parse(doctorBookedTillDateTime.getHour() +":"+doctorBookedTillDateTime.getMinute(), DateTimeFormatter.ofPattern(pattern));
                        appointmentDoctorDto = new AppointmentDoctorDto(d.getDoctorId(), d.getFirstName() + " " + d.getLastName(), d.getExperience(), userAppointment.getSpeciality(),doctorBookedTillTime, userAppointment.getDate());
                        availableDoctors.add(appointmentDoctorDto);
                    }
                }
            }
        }
        if (availableDoctors.size() == 0) {
            throw new NoDoctorAvailableExcepetion();
        }
        availableDoctors.sort(Comparator.comparingInt(AppointmentDoctorDto::retrieveBookingTimeInHour));
        return availableDoctors;
    }

    public AppointmentDoctorDto saveAppointment(AppointmentDetailsDto appointmentDetailsDto, int userId) {
        Appointment appointment = modelMapper.map(appointmentDetailsDto,Appointment.class);

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) throw new UserNotFoundException();

        String title = appointment.getSpeciality().toLowerCase();
        if (!specialityRepository.existsByTitle(title)) {
            throw new SpecialityException();
        }
        List<DoctorView> doctorViewList = doctorRepository.findDoctorsIdWithSpeciality(specialityRepository.findByTitle(title).getSpecialityId());
        if (doctorViewList.size() == 0) {
            throw new NoSpecialistFoundExcpetion();
        }
        List<Doctor> doctors = doctorViewList.stream().map(DoctorView::getDoctorId).map(doctorRepository::findById).filter(Optional::isPresent).map(Optional::get).toList();
        AppointmentDoctorDto appointmentDoctorDto = checkSchedule(new ArrayList<>(doctors), appointment).get(0);
        Optional<Doctor> d = doctorRepository.findById(appointmentDoctorDto.getDoctorId());

        appointment.setDate(appointmentDoctorDto.getBookedDate());
        appointment.setEndTime(appointmentDoctorDto.getBookingTime().plusHours(1));
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointmentDoctorDto.setStatus(AppointmentStatus.BOOKED.label);

        User user = optionalUser.get();
        user.setAppointment(appointment);
        if (d.isPresent()) {
            d.get().addAppointment(appointment);
            Schedule s = new Schedule(appointment.getEndTime(), appointment.getDate(), d.get(), user, appointment);
            Schedule schedule = scheduleRepository.save(s);
            appointmentDoctorDto.setAppointmentId(schedule.getAppointment().getAppointmentId());
            return appointmentDoctorDto;
        } else {
            throw new NoDoctorFoundException();
        }
    }

    public List<AppointmentDoctorDto> bookAppointmentAgain(Appointment appointment, int userId) {

        List<Doctor> doctors = doctorRepository.findDoctorsWithSpeciality(specialityRepository.findByTitle(appointment.getSpeciality()).getSpecialityId());
        List<AppointmentDoctorDto> availableDoctors = checkSchedule(new ArrayList<>(doctors),appointment);
        int doctorId = appointmentRepository.findDoctorByAppointmentId(appointment.getAppointmentId());
        availableDoctors.forEach(appointmentDoctorDto -> appointmentDoctorDto.setAppointmentId(appointment.getAppointmentId()));
        return availableDoctors.stream().filter(appointmentDoctorDto -> appointmentDoctorDto.getDoctorId() != doctorId).collect(Collectors.toList());
    }

    public LocalTime getCurrentLocalTime() {
        LocalTime currentTime = LocalTime.now();
        int minutes = currentTime.getMinute();
        if (minutes >= 30) {
            currentTime = currentTime.plusHours(1);
            currentTime = currentTime.truncatedTo(ChronoUnit.HOURS);
        } else {
            currentTime = currentTime.plusMinutes(30 - minutes).truncatedTo(ChronoUnit.MINUTES);
        }
        return currentTime;
    }
}