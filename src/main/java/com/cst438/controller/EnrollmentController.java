package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.dto.EnrollmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class EnrollmentController {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // instructor downloads student enrollments for a section, ordered by student name
    // user must be instructor for the section
    @GetMapping("/sections/{sectionNo}/enrollments")
    public List<EnrollmentDTO> getEnrollments(@PathVariable("sectionNo") int sectionNo) {
        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo);

        return enrollments.stream()
                .map(enrollment -> new EnrollmentDTO(
                        enrollment.getEnrollmentId(),
                        enrollment.getGrade(),
                        enrollment.getUser().getId(),
                        enrollment.getUser().getName(),
                        enrollment.getUser().getEmail(),
                        enrollment.getSection().getCourse().getCourseId(),
                        enrollment.getSection().getSecId(),
                        enrollment.getSection().getSectionNo(),
                        enrollment.getSection().getBuilding(),
                        enrollment.getSection().getRoom(),
                        enrollment.getSection().getTimes(),
                        enrollment.getSection().getCourse().getCredits(),
                        enrollment.getSection().getTerm().getYear(),
                        enrollment.getSection().getTerm().getSemester()))
                .collect(Collectors.toList());
    }

    // instructor uploads enrollments with the final grades for the section
    // user must be instructor for the section
    @PutMapping("/enrollments")
    public void updateEnrollmentGrade(@RequestBody List<EnrollmentDTO> dtoList) {
        for (EnrollmentDTO dto : dtoList) {
            Enrollment enrollment = enrollmentRepository.findById(dto.enrollmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

            // Update the grade and save back to the database
            enrollment.setGrade(dto.grade());
            enrollmentRepository.save(enrollment);
        }
    }
}