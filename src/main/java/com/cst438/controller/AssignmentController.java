package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.GradeDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.util.ArrayList;
import java.util.List;
import java.sql.Date;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class AssignmentController {

    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    SectionRepository sectionRepository;
    @Autowired
    EnrollmentRepository enrollmentRepository;
    @Autowired
    GradeRepository gradeRepository;

    // instructor lists assignments for a section.  Assignments ordered by due date.
    // logged in user must be the instructor for the section
    @GetMapping("/sections/{secNo}/assignments")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo) {

        List<Assignment> assignments = assignmentRepository.findBySectionNoOrderByDueDate(secNo);
	if (assignments.isEmpty()) {
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "section not found ");
        }
        List<AssignmentDTO> dto_list = new ArrayList<>();
        for(Assignment a:assignments){
            dto_list.add(new AssignmentDTO(
                    a.getAssignmentId(),
                    a.getTitle(),
                    a.getDueDate().toString(),
                    a.getSection().getCourse().getCourseId(),
                    a.getSection().getSecId(),
                    a.getSection().getSectionNo()
            ));
        }
		
		// hint: use the assignment repository method 
		//  findBySectionNoOrderByDueDate to return 
		//  a list of assignments

        return dto_list;
    }

    // add assignment
    // user must be instructor of the section
    // return AssignmentDTO with assignmentID generated by database
    @PostMapping("/assignments")
    public AssignmentDTO createAssignment(@RequestBody AssignmentDTO dto) {

        Section s = sectionRepository.findById(dto.secId()).orElse(null);
        if (s==null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "section not found " + dto.secId());
        }

        // TODO remove the following line when done
        Assignment a = new Assignment();
        a.setAssignmentId(dto.id());
        a.setTitle(dto.title());
        //convert string -> date
        Date date = Date.valueOf(dto.dueDate());
        a.setDueDate(date);
        a.setSection(s);
        assignmentRepository.save(a);

        return new AssignmentDTO(
                a.getAssignmentId(),
                a.getTitle(),
                a.getDueDate().toString(),
                a.getSection().getCourse().getCourseId(),
                a.getSection().getSecId(),
                a.getSection().getSectionNo()
        );
    }

    // update assignment for a section.  Only title and dueDate may be changed.
    // user must be instructor of the section
    // return updated AssignmentDTO
    @PutMapping("/assignments")
    public AssignmentDTO updateAssignment(@RequestBody AssignmentDTO dto) {

        Assignment a = assignmentRepository.findById(dto.id()).orElse(null);
        if(a==null){
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Assignment not found "+dto.id());
        }

        a.setTitle(dto.title());
        //convert string -> date
        Date date = Date.valueOf(dto.dueDate());
        a.setDueDate(date);
        assignmentRepository.save(a);
        return new AssignmentDTO(
                a.getAssignmentId(),
                a.getTitle(),
                a.getDueDate().toString(),
                a.getSection().getCourse().getCourseId(),
                a.getSection().getSecId(),
                a.getSection().getSectionNo()
        );
    }

    // delete assignment for a section
    // logged in user must be instructor of the section
    @DeleteMapping("/assignments/{assignmentId}")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId) {

        Assignment a = assignmentRepository.findById(assignmentId).orElse(null);
        //if assignment doesn't exist, do nothing
        if(a!=null){
            assignmentRepository.delete(a);
        }
    }

    // instructor gets grades for assignment ordered by student name
    // user must be instructor for the section
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(@PathVariable("assignmentId") int assignmentId) {

        // TODO remove the following line when done
        Assignment a = assignmentRepository.findById(assignmentId).orElse(null);
        if (a==null) {
            throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "assignment not found "+assignmentId);
        }

        // get the list of enrollments for the section related to this assignment.
		// hint: use the enrollment repository method findEnrollmentsBySectionOrderByStudentName.
        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(a.getSection().getSectionNo());

        // for each enrollment, get the grade related to the assignment and enrollment
		//   hint: use the gradeRepository findByEnrollmentIdAndAssignmentId method.
        List<GradeDTO> gradeDTOList = new ArrayList<>();
        for(Enrollment e:enrollments){
            //   if the grade does not exist, create a grade entity and set the score to NULL
            Grade g = gradeRepository.findByEnrollmentIdAndAssignmentId(e.getEnrollmentId(), assignmentId);
            if(g==null){
                g = new Grade();
                g.setScore(null);
            }
            //   and then save the new entity
            gradeRepository.save(g);

            gradeDTOList.add(new GradeDTO(
                    g.getGradeId(),
                    e.getStudent().getName(),
                    e.getStudent().getEmail(),
                    a.getTitle(),
                    e.getSection().getCourse().getCourseId(),
                    e.getSection().getSecId(),
                    g.getScore()
            ));
        }
        return gradeDTOList;
    }

    // instructor uploads grades for assignment
    // user must be instructor for the section
    @PutMapping("/grades")
    public void updateGrades(@RequestBody List<GradeDTO> dlist) {

        // for each grade in the GradeDTO list, retrieve the grade entity
        for(GradeDTO dto:dlist){
            Grade g = gradeRepository.findById(dto.gradeId()).orElse(null);
            if(g==null){
                throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Grade not found "+dto.gradeId());
            }
            // update the score and save the entity
            g.setScore(dto.score());
            gradeRepository.save(g);
        }
    }



    // student lists their assignments/grades for an enrollment ordered by due date
    // student must be enrolled in the section
    @GetMapping("/assignments")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("studentId") int studentId,
            @RequestParam("year") int year,
            @RequestParam("semester") String semester) {

        // TODO remove the following line when done
        List<AssignmentStudentDTO> returnDtos = new ArrayList<>();
        List<Enrollment> enrollments = enrollmentRepository.findByYearAndSemesterOrderByCourseId(year, semester, studentId);
        List<Assignment> assignments = new ArrayList<>();
        for (Enrollment e:enrollments){
            assignments = assignmentRepository.findBySectionNoOrderByDueDate(e.getSection().getSectionNo());
            for(Assignment a:assignments){
                Grade grade = gradeRepository.findByEnrollmentIdAndAssignmentId(e.getEnrollmentId(), a.getAssignmentId());
                returnDtos.add(new AssignmentStudentDTO(
                        a.getAssignmentId(),
                        a.getTitle(),
                        a.getDueDate(),
                        e.getSection().getCourse().getCourseId(),
                        e.getSection().getSecId(),
                        (grade!=null) ? grade.getScore() : null
                ));
            }
        }


        // return a list of assignments and (if they exist) the assignment grade
        //  for all sections that the student is enrolled for the given year and semester
        //  hint: use the assignment repository method findByStudentIdAndYearAndSemesterOrderByDueDate

        return returnDtos;
    }
}
