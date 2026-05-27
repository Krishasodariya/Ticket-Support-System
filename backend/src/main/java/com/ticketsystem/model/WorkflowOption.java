package com.ticketsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_options", uniqueConstraints = @UniqueConstraint(columnNames = {"type", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // STATUS or PRIORITY

    @Column(nullable = false)
    private String name;

    @Column
    private String label;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
