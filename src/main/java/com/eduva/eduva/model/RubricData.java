package com.eduva.eduva.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rubrics")
public class RubricData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssignmentData assignment;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // Getters and Setters
}
