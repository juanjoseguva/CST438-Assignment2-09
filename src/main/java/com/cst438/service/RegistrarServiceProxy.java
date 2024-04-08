package com.cst438.service;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.GradeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

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

    @RabbitListener(queues = "gradebook_service")
    public void receiveFromRegistrar(String message)  {
        try {
            String[] messageParts = message.split(" ");
            String action = messageParts[0];
            String dto = messageParts[1];

            //TODO: add remainder of actions
            if (action == "updateGrade") {
                EnrollmentDTO enrollmentDTO = fromJsonString(dto, EnrollmentDTO.class);
                Enrollment e = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentDTO.enrollmentId());
                if (e != null) {
                    e.setGrade(enrollmentDTO.grade());
                    enrollmentRepository.save(e);
                }
            } else if (action == "addCourse") {
                CourseDTO courseDTO = fromJsonString(dto, CourseDTO.class);
                Course c = new Course();
                c.setTitle(courseDTO.title());
                c.setCourseId(courseDTO.courseId());
                c.setCredits(courseDTO.credits());
                courseRepository.save(c);

            }
        } catch (Exception e){
            System.out.println("Exception in receiveFromRegistrar "+ e.getMessage());
        }
    }

    //enrollment methods
    public void updateEnrollment(EnrollmentDTO enrollmentDTO){
        sendMessage("updateEnrollment " + asJsonString(enrollmentDTO));
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
