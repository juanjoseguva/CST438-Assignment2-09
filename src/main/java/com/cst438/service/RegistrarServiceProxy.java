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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
                    addCourse(fromJsonString(dtoString, CourseDTO.class));
                    break;
                case "updateCourse":
                    updateCourse(fromJsonString(dtoString, CourseDTO.class));
                    break;
                case "deleteCourse":
                    deleteCourse(fromJsonString(dtoString, CourseDTO.class));
                    break;
                case "addSection":
                    addSection(fromJsonString(dtoString, SectionDTO.class));
                    break;
                case "updateSection":
                    updateSection(fromJsonString(dtoString, SectionDTO.class));
                    break;
                case "deleteSection":
                    deleteSection(fromJsonString(dtoString, SectionDTO.class));
                    break;
                case "createUser":
                    createUser(fromJsonString(dtoString, UserDTO.class));
                    break;
                case "updateUser":
                    updateUser(fromJsonString(dtoString, UserDTO.class));
                    break;
                case "deleteUser":
                    deleteUser(fromJsonString(dtoString, UserDTO.class));
                    break;
                case "addCourseStudent":
                    addCourseStudent(fromJsonString(dtoString, EnrollmentDTO.class));
                    break;
                case "dropCourse":
                    dropCourse(fromJsonString(dtoString, EnrollmentDTO.class));
                    break;
            }

        } catch (Exception e){
            System.out.println("Exception in receiveFromRegistrar "+ e.getMessage());
        }
    }

    //enrollment methods
    public void updateEnrollmentGrade(EnrollmentDTO enrollmentDTO){
        sendMessage("updateEnrollmentGrade " + asJsonString(enrollmentDTO));
    }

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


    // ADMIN actions
    public void addCourse(CourseDTO course){
        Course c = new Course();
        c.setCredits(course.credits());
        c.setTitle(course.title());
        c.setCourseId(course.courseId());
        courseRepository.save(c);
    }

    public void updateCourse(CourseDTO course){
        Course c = courseRepository.findById(course.courseId()).orElse(null);
        if (c==null) {
            throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "course not found "+course.courseId());
        } else {
            c.setTitle(course.title());
            c.setCredits(course.credits());
            courseRepository.save(c);
        }
    }

    public void deleteCourse(CourseDTO course){
        Course c = courseRepository.findById(course.courseId()).orElse(null);
        // if course does not exist, do nothing.
        if (c!=null) {
            CourseDTO courseDTO = new CourseDTO(
                    c.getCourseId(),
                    c.getTitle(),
                    c.getCredits()
            );
        }
        courseRepository.delete(c);
    }

    public void addSection(SectionDTO section){
        Course c = courseRepository.findById(section.courseId()).orElse(null);
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

        s.setSectionNo(section.secNo());
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
    }

    public void updateSection(SectionDTO section){
        Section s = sectionRepository.findById(section.secNo()).orElse(null);
        if (s==null) {
            throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "section not found "+section.secNo());
        }
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
    }

    public void deleteSection(SectionDTO section){
        Section s = sectionRepository.findById(section.secNo()).orElse(null);
        if (s != null) {
            sectionRepository.delete(s);
        }
    }

    public void createUser(UserDTO user){
        User u = new User();
        u.setId(user.id());
        u.setType(user.type());
        u.setPassword("temp");
        u.setEmail(user.email());
        u.setName(user.name());
        userRepository.save(u);
    }

    public void updateUser(UserDTO user){
        User u = userRepository.findById(user.id()).orElse(null);
        if (u==null) {
            throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "uer not found " + user.id());
        } else {
            u.setName(user.name());
            u.setType(user.type());
            u.setEmail(user.email());
            userRepository.save(u);
        }
    }

    public void deleteUser(UserDTO user){
        User u = userRepository.findById(user.id()).orElse(null);
        if (u!=null){
            userRepository.delete(u);
        }
    }

    // STUDENT actions
    public void addCourseStudent(EnrollmentDTO enrollmentDTO){
        User student = userRepository.findById(3).orElse(null);
        Section section = sectionRepository.findSectionBySectionNo(enrollmentDTO.sectionNo());
        Enrollment e = new Enrollment();
        e.setEnrollmentId(enrollmentDTO.enrollmentId());
        e.setUser(student);
        e.setSection(section);
        enrollmentRepository.save(e);
    }

    public void dropCourse(EnrollmentDTO enrollmentDTO){
        Enrollment e = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentDTO.enrollmentId());
        try{
            enrollmentRepository.delete(e);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "course cannot be dropped");
        }
    }
}
