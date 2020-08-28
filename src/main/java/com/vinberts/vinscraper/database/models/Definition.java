package com.vinberts.vinscraper.database.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 */
@Entity
@NoArgsConstructor
@Data
@Table(name = "definitions")
public class Definition implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(nullable = false, length = 800)
    private String word;

    @Column(nullable = false, length = 23000)
    private String meaning;

    @Column(nullable = false, length = 14000)
    private String example;

    @Column(name = "date_added", nullable = false, columnDefinition = "timestamp without time zone NOT NULL DEFAULT now()")
    private LocalDateTime dateAdded;

    @Column(nullable = false, length = 800)
    private String username;

    @Column(name = "up_votes")
    private Long upVotes;

    @Column(name = "down_votes")
    private Long downVotes;

}
