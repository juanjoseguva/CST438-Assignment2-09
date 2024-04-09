package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.GradebookServiceProxy;
import com.sun.tools.jconsole.JConsoleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    GradebookServiceProxy gradebookServiceProxy;


   // student gets transcript showing list of all enrollments
   // studentId will be temporary until Login security is implemented
   //example URL  /transcript?studentId=19803
   @GetMapping("/transcripts")
   public List<EnrollmentDTO> getTranscript(@RequestParam("studentId") int studentId) {

       // list course_id, sec_id, title, credit, grade in chronological order
       // user must be a student
	   // hint: use enrollment repository method findEnrollmentByStudentIdOrderByTermId

      List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(studentId);
      List<EnrollmentDTO> dto_list = new ArrayList<>();
      boolean studentFound = false;
      for (Enrollment e : enrollments) {
          if (e.getStudent().getId() == studentId) {
              studentFound = true;
              break;
          }
      }
      if (!studentFound) {
          throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Student not found "+ studentId);
      } else {
          for (Enrollment e : enrollments) {
              dto_list.add(new EnrollmentDTO(
                      e.getEnrollmentId(),
                      e.getGrade(),
                      studentId,
                      e.getStudent().getName(),
                      e.getStudent().getEmail(),
                      e.getSection().getCourse().getCourseId(),
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
   }

    // student gets a list of their enrollments for the given year, semester
    // user must be student
    // studentId will be temporary until Login security is implemented
   @GetMapping("/enrollments")
   public List<EnrollmentDTO> getSchedule(
           @RequestParam("year") int year,
           @RequestParam("semester") String semester,
           @RequestParam("studentId") int studentId) {

     // TODO
	 //  hint: use enrollment repository method findByYearAndSemesterOrderByCourseId
       List<Enrollment> enrollments = enrollmentRepository.findByYearAndSemesterOrderByCourseId(year, semester, studentId);
       if (enrollments.isEmpty()) {
           throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Enrollment not found");
       }
       List<EnrollmentDTO> dto_list = new ArrayList<>();
       for (Enrollment e : enrollments) {
           dto_list.add(new EnrollmentDTO(
                   e.getEnrollmentId(),
                   e.getGrade(),
                   studentId,
                   e.getStudent().getName(),
                   e.getStudent().getEmail(),
                   e.getSection().getCourse().getCourseId(),
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
    public EnrollmentDTO addCourse(
		    @PathVariable int sectionNo,
            @RequestParam("studentId") int studentId ) {

        // TODO
        // check that the Section entity with primary key sectionNo exists
        Section s = sectionRepository.findById(sectionNo).orElse(null);
        if (s==null){
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "section not found " + sectionNo);
        }

        // check that today is between addDate and addDeadline for the section
        long millis = System.currentTimeMillis();
        java.sql.Date today = new java.sql.Date(millis);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd");
        Date addDate = s.getTerm().getAddDate();
        Date addDeadline = s.getTerm().getAddDeadline();
        if (today.before(addDate) || today.after(addDeadline)) {
            throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid date");
        }

        // check that student is not already enrolled into this section
        Enrollment e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, studentId);
        Section section = sectionRepository.findSectionBySectionNo(sectionNo);
        List<User> users = userRepository.findAllByOrderByIdAsc();
        User student = new User();
        for (User u : users) {
            if (u.getId() == studentId) {
                student = u;
                break;
            }
        }

        // create a new enrollment entity and save.  The enrollment grade will
        // be NULL until instructor enters final grades for the course.
        if (e != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already enrolled in this section");
        } else {
            e = new Enrollment();
            e.setSection(section);
            e.setUser(student);
            enrollmentRepository.save(e);
            EnrollmentDTO enrollmentDTO = new EnrollmentDTO(
                    e.getEnrollmentId(),
                    "",
                    studentId,
                    e.getStudent().getName(),
                    e.getStudent().getEmail(),
                    e.getSection().getCourse().getCourseId(),
                    e.getSection().getSecId(),
                    e.getSection().getSectionNo(),
                    e.getSection().getBuilding(),
                    e.getSection().getRoom(),
                    e.getSection().getTimes(),
                    e.getSection().getCourse().getCredits(),
                    e.getSection().getTerm().getYear(),
                    e.getSection().getTerm().getSemester()
            );
            gradebookServiceProxy.addCourse(enrollmentDTO);
            return enrollmentDTO;
        }

    }

    // student drops a course
   @DeleteMapping("/enrollments/{enrollmentId}")
   public void dropCourse(@PathVariable("enrollmentId") int enrollmentId) {

       Enrollment e = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentId);
       if (e == null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment does not exist");
       }

       // user must be student
       if (!e.getStudent().getType().equals("STUDENT")) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be student");
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
           EnrollmentDTO enrollmentDTO = new EnrollmentDTO(
                   e.getEnrollmentId(),
                   e.getGrade(),
                   e.getStudent().getId(),
                   e.getStudent().getName(),
                   e.getStudent().getEmail(),
                   e.getSection().getCourse().getCourseId(),
                   e.getSection().getSecId(),
                   e.getSection().getSectionNo(),
                   e.getSection().getBuilding(),
                   e.getSection().getRoom(),
                   e.getSection().getTimes(),
                   e.getSection().getCourse().getCredits(),
                   e.getSection().getTerm().getYear(),
                   e.getSection().getTerm().getSemester()

           );
           gradebookServiceProxy.dropCourse(enrollmentDTO);
       }

   }
}
