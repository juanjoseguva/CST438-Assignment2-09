package com.cst438.domain;

import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface CourseRepository extends CrudRepository<Course, String> {
	

    Course findById(String courseId);

    List<Course> findAllByOrderByCourseIdAsc();
}
