package com.cst438.domain;

import jakarta.persistence.*;

@Entity
public class Grade {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="grade_id")
    private int gradeId;

    // TODO complete this class
    
    // add relationship between grade and assignment entities
    @ManyToOne
    @JoinColumn(name="assignment_id")
    private Assignment assignment;
    // add relationship between grade and enrollment entities
    @ManyToOne
    @JoinColumn(name="enrollment_id")
    private Enrollment enrollment;
    // add additional attribute for score
    @Column(name="score")
    private Integer score;

    // add getter/setter methods
    public int getGradeId() {return gradeId; }
    public void setGradeId(int gradeId) {this.gradeId = gradeId; }
    public Integer getScore() {return score; }
    public void setScore (Integer score) {this.score = score; }
    public Assignment getAssignment() {return assignment; }
    public void setAssignment(Assignment assignment) {this.assignment = assignment; }
    public Enrollment getEnrollment() {return enrollment; }
    public void setEnrollment(Enrollment enrollment) {this.enrollment = enrollment; }

}