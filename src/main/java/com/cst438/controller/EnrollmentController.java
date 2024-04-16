package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.dto.EnrollmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class EnrollmentController {

    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private SectionRepository sectionRepository;

    // instructor downloads student enrollments for a section, ordered by student name
    // user must be instructor for the section
    @GetMapping("/sections/{sectionNo}/enrollments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<EnrollmentDTO> getEnrollments(
            @PathVariable("sectionNo") int sectionNo,
            Principal principal) {
        Section section = sectionRepository.findSectionBySectionNo(sectionNo);
        //Check that the section exists
        if(section == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Section number is invalid" );
        }
        //Check that the logged in professor is the professor of the requested section
        if(!(section.getInstructorEmail().equals(principal.getName()))){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the listed instructor of this course");
        }
        //Get the list of enrollments based on sectionNo
        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo);
        //Check that enrollments !=0
        if(enrollments.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There are no current enrollments for that section");
        }

        return enrollments.stream()
                .map(enrollment -> new EnrollmentDTO(
                        enrollment.getEnrollmentId(),
                        enrollment.getGrade(),
                        enrollment.getStudent().getId(),
                        enrollment.getStudent().getName(),
                        enrollment.getStudent().getEmail(),
                        enrollment.getSection().getCourse().getCourseId(),
                        enrollment.getSection().getCourse().getTitle(),
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
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateEnrollmentGrade(
            @RequestBody List<EnrollmentDTO> dtoList,
            Principal principal) {
        for (EnrollmentDTO dto : dtoList) {
            Enrollment enrollment = enrollmentRepository.findById(dto.enrollmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

            if(!(principal.getName().equals(enrollment.getSection().getInstructorEmail()))){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the listed instructor of this course");
            }

            // Update the grade and save back to the database
            enrollment.setGrade(dto.grade());
            enrollmentRepository.save(enrollment);
        }
    }
}