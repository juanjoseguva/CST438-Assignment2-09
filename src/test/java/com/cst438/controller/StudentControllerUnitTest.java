package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.SectionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest
public class StudentControllerUnitTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Test
    public void enrollInSection() throws Exception {
        MockHttpServletResponse response;

        // Create DTO with data for new assignment
        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                "A",
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst363",
                "Introduction to Database Systems",
                9,
                9,
                "052",
                "104",
                "M W 10:00-11:50",
                4,
                2024,
                "Spring"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                               .post("/enrollments/sections/"+enrollment.sectionNo()+"?studentId="+enrollment.studentId())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();

        // check the response code for 200 meaning OK
        assertEquals(200, response.getStatus());

        // return data converted from String to DTO
        EnrollmentDTO result = fromJsonString(response.getContentAsString(), EnrollmentDTO.class);

        // primary key should have a non zero value from the database
        assertNotEquals(0, result.enrollmentId());
        // check other fields of the DTO for expected values
        assertEquals("cst363", result.courseId());

        // check the database
        Enrollment e = enrollmentRepository.findById(result.enrollmentId()).orElse(null);
        assertNotNull(e);
        assertEquals("cst363", e.getSection().getCourse().getCourseId());

        // clean up after test. issue http DELETE request for section
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .delete("/enrollments/"+result.enrollmentId()))
                .andReturn()
                .getResponse();

        assertEquals(200, response.getStatus());

        // check database for delete
        e = enrollmentRepository.findById(result.enrollmentId()).orElse(null);
        //s = sectionRepository.findById(result.secNo()).orElse(null);
        assertNull(e);  // section should not be found after delete
    }

    @Test
    public void enrollInSectionButAlreadyEnrolled() throws Exception {
        MockHttpServletResponse response;

        // Create DTO with data for new assignment
        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                "A",
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst363",
                "Introduction to Database Systems",
                1,
                8,
                "052",
                "104",
                "M W 10:00-11:50",
                4,
                2024,
                "Spring"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/"+enrollment.sectionNo()+"?studentId="+enrollment.studentId())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();

        // check the response code for 200 meaning OK
        assertEquals(400, response.getStatus());

        // check the expected error message
        String message = response.getErrorMessage();
        assertEquals("already enrolled in this section", message);
    }

    @Test
    public void enrollinSectionButInvalidSectionNo() throws Exception{
        MockHttpServletResponse response;

        // Create DTO with data for new assignment
        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                "A",
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst363",
                "Introduction to Database Systems",
                1,
                58,
                "052",
                "104",
                "M W 10:00-11:50",
                4,
                2024,
                "Spring"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/"+enrollment.sectionNo()+"?studentId="+enrollment.studentId())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();

        // We should get a NOT_FOUND 404 error
        assertEquals(404, response.getStatus());

        // check the expected error message
        String message = response.getErrorMessage();
        assertEquals("section not found 58", message);

    }

    @Test
    public void enrollinSectionButPastDeadline() throws Exception{
        MockHttpServletResponse response;

        // Create DTO with data for new assignment
        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                "A",
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst338",
                "Introduction to Database Systems",
                1,
                1,
                "052",
                "100",
                "M W 10:00-11:50",
                4,
                2024,
                "Spring"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/"+enrollment.sectionNo()+"?studentId="+enrollment.studentId())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();

        // check the response code for 200 meaning OK
        assertEquals(400, response.getStatus());

        // check the expected error message
        String message = response.getErrorMessage();
        assertEquals("Invalid date", message);
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
