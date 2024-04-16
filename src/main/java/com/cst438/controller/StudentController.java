package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.EnrollmentDTO;
import com.sun.tools.jconsole.JConsoleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class StudentController {

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;


   // student gets transcript showing list of all enrollments
   // studentId will be temporary until Login security is implemented
   //example URL  /transcript?studentId=19803
   @GetMapping("/transcripts")
   @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
   public List<EnrollmentDTO> getTranscript(Principal principal) {


       // list course_id, sec_id, title, credit, grade in chronological order
       // user must be a student
       // hint: use enrollment repository method findEnrollmentByStudentIdOrderByTermId
       //Get user by email
       User student = userRepository.findByEmail(principal.getName());
       //Make sure user is not null
       if(student == null){
           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student record not found for " + principal.getName());
       }
       //Check that user type is student
       if(!(student.getType().equals("STUDENT"))){
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be a student to get a transcript");
       }
       int studentId = student.getId();
       List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(studentId);
       List<EnrollmentDTO> dto_list = new ArrayList<>();

       dto_list.add(new EnrollmentDTO(
               0,
               null,
               studentId,
               student.getName(),
               null,
               null,
               null,
               69,
               69,
               null,
               null,
               null,
               10,
               2099,
               null
       ));

       for (Enrollment e : enrollments) {
           dto_list.add(new EnrollmentDTO(
                   e.getEnrollmentId(),
                   e.getGrade(),
                   studentId,
                   e.getStudent().getName(),
                   e.getStudent().getEmail(),
                   e.getSection().getCourse().getCourseId(),
                   e.getSection().getCourse().getTitle(),
                   e.getSection().getSecId(),
                   e.getSection().getSectionNo(),
                   e.getSection().getBuilding(),
                   e.getSection().getRoom(),
                   e.getSection().getTimes(),
                   e.getSection().getCourse().getCredits(),
                   e.getSection().getTerm().getYear(),
                   e.getSection().getTerm().getSemester()));
       }
       return dto_list;
   }

    // student gets a list of their enrollments for the given year, semester
    // user must be student
    // studentId will be temporary until Login security is implemented
   @GetMapping("/enrollments")
   @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
   public List<EnrollmentDTO> getSchedule(
           @RequestParam("year") int year,
           @RequestParam("semester") String semester,
           Principal principal) {

     // TODO
	 //  hint: use enrollment repository method findByYearAndSemesterOrderByCourseId
       User student = userRepository.findByEmail(principal.getName());
       if(student == null){
           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student record not found for " +principal.getName());
       }
       int studentId = student.getId();
       List<Enrollment> enrollments = enrollmentRepository.findByYearAndSemesterOrderByCourseId(year, semester, studentId);
       List<EnrollmentDTO> dto_list = new ArrayList<>();
       for (Enrollment e : enrollments) {
           dto_list.add(new EnrollmentDTO(
                   e.getEnrollmentId(),
                   e.getGrade(),
                   studentId,
                   e.getStudent().getName(),
                   e.getStudent().getEmail(),
                   e.getSection().getCourse().getCourseId(),
                   e.getSection().getCourse().getTitle(),
                   e.getSection().getSecId(),
                   e.getSection().getSectionNo(),
                   e.getSection().getBuilding(),
                   e.getSection().getRoom(),
                   e.getSection().getTimes(),
                   e.getSection().getCourse().getCredits(),
                   e.getSection().getTerm().getYear(),
                   e.getSection().getTerm().getSemester()));
       }

      return dto_list;

     //  return null;
   }

    // student adds enrollment into a section
    // user must be student
    // return EnrollmentDTO with enrollmentId generated by database
    @PostMapping("/enrollments/sections/{sectionNo}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public EnrollmentDTO addCourse(
            @PathVariable int sectionNo,
            Principal principal) {

        // TODO
        // check that the Section entity with primary key sectionNo exists
        Section section = sectionRepository.findSectionBySectionNo(sectionNo);
        if (section==null){
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "section not found " + sectionNo);
        }
        //These variable names are to not conflict with the later variables already written
        User student = userRepository.findByEmail(principal.getName());
        if (student == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student record not found for " + principal.getName());
        }
        int studentId = student.getId();
        //Check that student is not already enrolled
        Enrollment enrollment = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, studentId);
        if(enrollment != null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are already enrolled in section " + sectionNo);
        }

        // check that today is between addDate and addDeadline for the section
        long millis = System.currentTimeMillis();
        java.sql.Date today = new java.sql.Date(millis);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd");
        Date addDate = section.getTerm().getAddDate();
        Date addDeadline = section.getTerm().getAddDeadline();
        if (today.before(addDate) || today.after(addDeadline)) {
            throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid date");
        }

        // create a new enrollment entity and save.  The enrollment grade will
        // be NULL until instructor enters final grades for the course.
        Enrollment e = new Enrollment();
        e.setSection(section);
        e.setUser(student);
        enrollmentRepository.save(e);
        return new EnrollmentDTO(
                e.getEnrollmentId(),
                "",
                studentId,
                e.getStudent().getName(),
                e.getStudent().getEmail(),
                e.getSection().getCourse().getCourseId(),
                e.getSection().getCourse().getTitle(),
                e.getSection().getSecId(),
                e.getSection().getSectionNo(),
                e.getSection().getBuilding(),
                e.getSection().getRoom(),
                e.getSection().getTimes(),
                e.getSection().getCourse().getCredits(),
                e.getSection().getTerm().getYear(),
                e.getSection().getTerm().getSemester()
        );
    }

    // student drops a course
   @DeleteMapping("/enrollments/{enrollmentId}")
   @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
   public void dropCourse(
           @PathVariable("enrollmentId") int enrollmentId,
           Principal principal) {

       Enrollment e = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentId);
       if (e == null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment does not exist");
       }
       if (!(e.getStudent().getEmail().equals(principal.getName()))){
           throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the student associated with this enrollment");
       }
	   
       // check that today is not after the dropDeadline for section
       long millis = System.currentTimeMillis();
       java.sql.Date today = new java.sql.Date(millis);
       SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd");
       Date dropDate = e.getSection().getTerm().getDropDeadline();
       if (today.after(dropDate)) {
           throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Too late to drop. Sorry, the money is ours");
       } else {
           enrollmentRepository.delete(e);
       }

   }
}
