package com.cst438.service;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

//TODO: file copied from RegistarServiceProxy
// some items may need to be adjusted
@Service
public class RegistrarServiceProxy {

    Queue registrarServiceQueue = new Queue("registrar_service", true);

    @Bean
    public Queue createQueue() {
        return new Queue("gradebook_service", true);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    UserRepository userRepository;

    @RabbitListener(queues = "gradebook_service")
    public void receiveFromRegistrar(String message)  {
        try {
            String[] messageParts = message.split(" ", 2);
            String action = messageParts[0];
            String dtoString = messageParts[1];

            //TODO: add remainder of actions
            switch(action){

                case "addCourse":
                    CourseDTO course = fromJsonString(dtoString, CourseDTO.class);
                    Course c = new Course();
                    c.setCredits(course.credits());
                    c.setTitle(course.title());
                    c.setCourseId(course.courseId());
                    courseRepository.save(c);

                    break;
                case "updateCourse":
                    course = fromJsonString(dtoString, CourseDTO.class);
                    c = courseRepository.findById(course.courseId()).orElse(null);
                    if (c==null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "course not found "+course.courseId());
                    } else {
                        c.setTitle(course.title());
                        c.setCredits(course.credits());
                        courseRepository.save(c);
                    }

                    break;
                case "deleteCourse":
                    course = fromJsonString(dtoString, CourseDTO.class);
                    c = courseRepository.findById(course.courseId()).orElse(null);
                    // if course does not exist, do nothing.
                    if (c!=null) {
                        CourseDTO courseDTO = new CourseDTO(
                                c.getCourseId(),
                                c.getTitle(),
                                c.getCredits()
                        );
                    }
                    courseRepository.delete(c);

                    break;
                case "addSection":
                    SectionDTO section = fromJsonString(dtoString, SectionDTO.class);
                    c = courseRepository.findById(section.courseId()).orElse(null);
                    if (c == null ){
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "course not found "+section.courseId());
                    }
                    Section s = new Section();
                    s.setCourse(c);

                    Term term = termRepository.findByYearAndSemester(section.year(), section.semester());
                    if (term == null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "year, semester invalid ");
                    }
                    s.setTerm(term);

                    s.setSecId(section.secId());
                    s.setBuilding(section.building());
                    s.setRoom(section.room());
                    s.setTimes(section.times());

                    User instructor = null;
                    if (section.instructorEmail()==null || section.instructorEmail().equals("")) {
                        s.setInstructor_email("");
                    } else {
                        instructor = userRepository.findByEmail(section.instructorEmail());
                        if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "email not found or not an instructor " + section.instructorEmail());
                        }
                        s.setInstructor_email(section.instructorEmail());
                    }
                    sectionRepository.save(s);

                    break;
                case "updateSection":
                    section = fromJsonString(dtoString, SectionDTO.class);
                    s = sectionRepository.findById(section.secNo()).orElse(null);
                    if (s==null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "section not found "+section.secNo());
                    }
                    s.setSecId(section.secId());
                    s.setBuilding(section.building());
                    s.setRoom(section.room());
                    s.setTimes(section.times());

                    instructor = null;
                    if (section.instructorEmail()==null || section.instructorEmail().equals("")) {
                        s.setInstructor_email("");
                    } else {
                        instructor = userRepository.findByEmail(section.instructorEmail());
                        if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "email not found or not an instructor " + section.instructorEmail());
                        }
                        s.setInstructor_email(section.instructorEmail());
                    }
                    sectionRepository.save(s);

                    break;
                case "deleteSection":
                    section = fromJsonString(dtoString, SectionDTO.class);
                    s = sectionRepository.findById(section.secNo()).orElse(null);
                    if (s != null) {
                        sectionRepository.delete(s);
                    }

                    break;
            }

        } catch (Exception e){
            System.out.println("Exception in receiveFromRegistrar "+ e.getMessage());
        }
    }

    //enrollment methods
    public void updateEnrollment(EnrollmentDTO enrollmentDTO){
        sendMessage("updateGrade" + asJsonString(enrollmentDTO));
    }

    //the following methods correspond to controller methods but may not be necessary
//    public void updateGrades(GradeDTO gradeDTO){
//        sendMessage("updateGrades " + asJsonString(gradeDTO));
//    }
//    public void createAssignment(AssignmentDTO assignmentDTO) {
//        sendMessage("addAssignment " + asJsonString(assignmentDTO));
//    }
//    public void updateAssignment(AssignmentDTO assignmentDTO) {
//        sendMessage("updateAssignment " + asJsonString(assignmentDTO));
//    }
//    public void deleteAssignment(AssignmentDTO assignmentDTO) {
//        sendMessage("deleteAssignment " + asJsonString(assignmentDTO));
//    }



    private void sendMessage(String s) {
        rabbitTemplate.convertAndSend(registrarServiceQueue.getName(), s);
    }
    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static <T> T  fromJsonString(String str, Class<T> valueType ) {
        try {
            return new ObjectMapper().readValue(str, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
