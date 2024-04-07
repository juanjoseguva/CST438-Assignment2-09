package com.cst438.service;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class GradebookServiceProxy {

    Queue gradebookServiceQueue = new Queue("gradebook_service", true);

    @Bean
    public Queue createQueue() {
        return new Queue("registrar_service", true);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @RabbitListener(queues = "registrar_service")
    public void receiveFromGradebook(String message)  {
        try {
            String[] messageParts = message.split(" ");
            String action = messageParts[0];
            String dto = messageParts[1];

            if (action == "updateGrade") {
                EnrollmentDTO enrollmentDTO = fromJsonString(dto, EnrollmentDTO.class);
                Enrollment e = enrollmentRepository.findEnrollmentByEnrollmentId(enrollmentDTO.enrollmentId());
                if (e != null) {
                    e.setGrade(enrollmentDTO.grade());
                    enrollmentRepository.save(e);
                }
            }
        } catch (Exception e){
            System.out.println("Exception in receiveFromGradebook "+ e.getMessage());
        }
    }


    //course related methods
    public void addCourse(CourseDTO courseDTO){
        sendMessage("addCourse " +asJsonString(courseDTO));
    }
    public void updateCourse(CourseDTO courseDTO){
        sendMessage("updateCourse " + asJsonString(courseDTO) );
    }

    public void deleteCourse(CourseDTO courseDTO) {
        sendMessage("deleteCourse " + asJsonString(courseDTO));
    }

    //section related methods
    public void addSection(SectionDTO sectionDTO){
        sendMessage("addSection " + asJsonString(sectionDTO));
    }
    public void updateSection(SectionDTO sectionDTO){
        sendMessage( "updateSection " + asJsonString(sectionDTO));
    }
    public void deleteSection(SectionDTO sectionDTO) {
        sendMessage("deleteSection " + asJsonString(sectionDTO));
    }

    //student related methods
    public void addCourse(EnrollmentDTO enrollmentDTO){
        sendMessage("addCourseStudent " + asJsonString(enrollmentDTO));
    }
    public void dropCourse(EnrollmentDTO enrollmentDTO){
        sendMessage("dropCourse " + asJsonString(enrollmentDTO));
    }

    //user related methods
    public void createUser(UserDTO userDTO){
        sendMessage("createUser " + asJsonString(userDTO));
    }
    public void updateUser(UserDTO userDTO){
        sendMessage("updateUser " + asJsonString(userDTO));
    }
    public void deleteUser(UserDTO userDTO){
        sendMessage("deleteUser " + asJsonString(userDTO));
    }

    //utility methods
    private void sendMessage(String s) {
        rabbitTemplate.convertAndSend(gradebookServiceQueue.getName(), s);
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
