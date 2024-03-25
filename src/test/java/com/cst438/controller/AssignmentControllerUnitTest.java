package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.GradeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest
public class AssignmentControllerUnitTest {

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

    @Test
    public void addAssignment() throws Exception {
        MockHttpServletResponse response;

        // Create DTO with data for new assignment
        AssignmentDTO assignment = new AssignmentDTO(
                0,
                "Final Project",
                "2024-05-01",
                "cst363",
                1,
                9
        );

        // Issue an HTTP POST request to add the assignment
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/assignments")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(assignment)))
                .andReturn()
                .getResponse();

        // Check the response code for 200 meaning OK
        assertEquals(200, response.getStatus());

        // Return data converted from String to DTO
        AssignmentDTO result = fromJsonString(response.getContentAsString(), AssignmentDTO.class);

        // Primary key should have a non-zero value from the database
        assertNotEquals(0, result.id());

        Assignment a = assignmentRepository.findById(result.id()).orElse(null);
        assertNotNull(a);
        Date expectedDate = new Date(124, 4, 1);
        assertEquals("Final Project", a.getTitle());
        assertEquals(expectedDate, a.getDueDate());

        // Clean up after test. Issue HTTP DELETE request for assignment
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .delete("/assignments/" + result.id()))
                .andReturn()
                .getResponse();

        assertEquals(200, response.getStatus());

        a = assignmentRepository.findById(result.id()).orElse(null);
        assertNull(a);
    }

    @Test
    public void addAssignmentFailsDueDatePastEndDate() throws Exception {
        MockHttpServletResponse response;

        AssignmentDTO assignment = new AssignmentDTO(
                0,
                "Final Project",
                "2024-05-20",
                "cst363",
                1,
                9
        );

        // Issue an HTTP POST request to add the assignment
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/assignments")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(assignment)))
                .andReturn()
                .getResponse();

        // Response should be 404, NOT_FOUND
        assertEquals(404, response.getStatus());

        // Check the expected error message
        String message = response.getErrorMessage();
        assertEquals("DueDate is after course EndDate ", message);
    }

    @Test
    public void addAssignmentFailsBadSection() throws Exception {
        MockHttpServletResponse response;


        //Section 58 isn't valid
        AssignmentDTO assignment = new AssignmentDTO(
                0,
                "Integers: What are they?",
                "2024-05-20",
                "cst363",
                1,
                58
        );

        // The post request, and the response
        response = mvc.perform(
                MockMvcRequestBuilders
                        .post("/assignments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(assignment)))
                .andReturn()
                .getResponse();

        // We should get a NOT_FOUND 404 error
        assertEquals(404, response.getStatus());

        // Checking the message returned
        assertEquals("section not found 58", response.getErrorMessage());
    }

    //Instructor grades an assignment and uploads
    @Test
    public void gradeAssignmentForEnrolled() throws Exception {
        MockHttpServletResponse responseFromGet;
        MockHttpServletResponse responseFromPut;

        String getUrl = "/assignments/2/grades";
        responseFromGet = mvc.perform(
                MockMvcRequestBuilders
                        .get(getUrl))
                .andReturn()
                .getResponse();

        //Check the response code for our get request
        assertEquals(200, responseFromGet.getStatus());

        //Map response to GradeDTO object list
        ObjectMapper objectMapper = new ObjectMapper();
        List<GradeDTO> gradeDTOS = objectMapper.readValue(
                responseFromGet.getContentAsString(),
                TypeFactory.defaultInstance().constructCollectionType(List.class, GradeDTO.class));

        //Set score on returned grades
        List<GradeDTO> returnGrades = new ArrayList<>();
        for(GradeDTO grade: gradeDTOS){
            GradeDTO newGrade = new GradeDTO(
                    grade.gradeId(),
                    grade.studentName(),
                    grade.studentEmail(),
                    grade.assignmentTitle(),
                    grade.courseId(),
                    grade.sectionId(),
                    95
            );
            returnGrades.add(newGrade);

        };
        //Send put request for updated grades
        responseFromPut = mvc.perform(
                MockMvcRequestBuilders
                        .put("/grades")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(returnGrades)))
                .andReturn()
                .getResponse();

        //Check that response from put is 200
        assertEquals(200, responseFromPut.getStatus());

        responseFromGet = mvc.perform(
                MockMvcRequestBuilders
                        .get(getUrl))
                .andReturn()
                .getResponse();

        List<GradeDTO> updatedGrades = objectMapper.readValue(
                responseFromGet.getContentAsString(),
                TypeFactory.defaultInstance().constructCollectionType(List.class, GradeDTO.class));

        //Check that the grades in the database match our updated list
        assertEquals(returnGrades, updatedGrades);

        //Check that there was an actual update to the grades
        assertNotEquals(gradeDTOS, updatedGrades);

        //Set grades back to original state
        responseFromPut = mvc.perform(
                MockMvcRequestBuilders
                        .put("/grades")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(gradeDTOS)))
                .andReturn()
                .getResponse();
        //Check that put worked
        assertEquals(200, responseFromPut.getStatus());

        responseFromGet = mvc.perform(
                MockMvcRequestBuilders
                        .get(getUrl))
                .andReturn()
                .getResponse();

        List<GradeDTO> resetGrades = objectMapper.readValue(
                responseFromGet.getContentAsString(),
                TypeFactory.defaultInstance().constructCollectionType(List.class, GradeDTO.class));

        //Check that grades now match original state
        assertEquals(gradeDTOS, resetGrades);
    }

    @Test
    public void gradeAssignmentButAssignmentIdInvalid() throws Exception {
        MockHttpServletResponse response;

        //create assignment with invalid assignmentId
        AssignmentDTO assignment = new AssignmentDTO(
                10,
                "Final Project",
                "2024-05-20",
                "cst363",
                1,
                9
        );

        // Issue an HTTP POST request to add the assignment
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .get("/assignments/"+assignment.id()+"/grades")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(assignment)))
                .andReturn()
                .getResponse();

        // Response should be 404, NOT_FOUND
        assertEquals(404, response.getStatus());

        // Check the expected error message
        String message = response.getErrorMessage();
        assertEquals("assignment not found "+assignment.id(), message);
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T fromJsonString(String str, Class<T> valueType) {
        try {
            return new ObjectMapper().readValue(str, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
