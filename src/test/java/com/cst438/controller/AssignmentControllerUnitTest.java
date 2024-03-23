package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
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
                "cst499",
                1,
                1
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
