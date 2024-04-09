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
                    CourseDTO course0 = fromJsonString(dtoString, CourseDTO.class);
                    Course c0 = new Course();
                    c0.setCredits(course0.credits());
                    c0.setTitle(course0.title());
                    c0.setCourseId(course0.courseId());
                    courseRepository.save(c0);

                    break;
                case "updateCourse":
                    CourseDTO course1 = fromJsonString(dtoString, CourseDTO.class);
                    Course c1 = courseRepository.findById(course1.courseId()).orElse(null);
                    if (c1==null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "course not found "+course1.courseId());
                    } else {
                        c1.setTitle(course1.title());
                        c1.setCredits(course1.credits());
                        courseRepository.save(c1);
                    }

                    break;
                case "deleteCourse":
                    CourseDTO course2 = fromJsonString(dtoString, CourseDTO.class);
                    Course c2 = courseRepository.findById(course2.courseId()).orElse(null);
                    // if course does not exist, do nothing.
                    if (c2!=null) {
                        CourseDTO courseDTO = new CourseDTO(
                                c2.getCourseId(),
                                c2.getTitle(),
                                c2.getCredits()
                        );
                    }
                    courseRepository.delete(c2);

                    break;
                case "addSection":
                    SectionDTO section0 = fromJsonString(dtoString, SectionDTO.class);
                    Course c3 = courseRepository.findById(section0.courseId()).orElse(null);
                    if (c3 == null ){
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "course not found "+section0.courseId());
                    }
                    Section s = new Section();
                    s.setCourse(c3);

                    Term term = termRepository.findByYearAndSemester(section0.year(), section0.semester());
                    if (term == null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "year, semester invalid ");
                    }
                    s.setTerm(term);

                    s.setSecId(section0.secId());
                    s.setBuilding(section0.building());
                    s.setRoom(section0.room());
                    s.setTimes(section0.times());

                    User instructor = null;
                    if (section0.instructorEmail()==null || section0.instructorEmail().equals("")) {
                        s.setInstructor_email("");
                    } else {
                        instructor = userRepository.findByEmail(section0.instructorEmail());
                        if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "email not found or not an instructor " + section0.instructorEmail());
                        }
                        s.setInstructor_email(section0.instructorEmail());
                    }
                    sectionRepository.save(s);

                    break;
                case "updateSection":
                    SectionDTO section1 = fromJsonString(dtoString, SectionDTO.class);
                    s = sectionRepository.findById(section1.secNo()).orElse(null);
                    if (s==null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "section not found "+section1.secNo());
                    }
                    s.setSecId(section1.secId());
                    s.setBuilding(section1.building());
                    s.setRoom(section1.room());
                    s.setTimes(section1.times());

                    instructor = null;
                    if (section1.instructorEmail()==null || section1.instructorEmail().equals("")) {
                        s.setInstructor_email("");
                    } else {
                        instructor = userRepository.findByEmail(section1.instructorEmail());
                        if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "email not found or not an instructor " + section1.instructorEmail());
                        }
                        s.setInstructor_email(section1.instructorEmail());
                    }
                    sectionRepository.save(s);

                    break;
                case "deleteSection":
                    SectionDTO section3 = fromJsonString(dtoString, SectionDTO.class);
                    s = sectionRepository.findById(section3.secNo()).orElse(null);
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
