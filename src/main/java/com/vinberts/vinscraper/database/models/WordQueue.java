package com.vinberts.vinscraper.database.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 *
 */
@Entity
@NoArgsConstructor
@Data
@Table(name = "words_queue")
public class WordQueue {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", nullable = false)
    private UUID uuid;

    @Column(nullable = false, length = 800)
    private String word;

    @Column(length = 3000)
    private String url;

    @Column(columnDefinition = "boolean")
    private Boolean processed;

    @Column(columnDefinition = "boolean", name = "has_error")
    private Boolean hasError;

    @Column(columnDefinition = "boolean", name = "being_processed")
    private Boolean beingProcessed;

    @Column(columnDefinition = "boolean", name = "re_run")
    private Boolean reRun;

    @Column(name = "date_added", nullable = false, columnDefinition = "timestamp without time zone NOT NULL DEFAULT now()")
    private LocalDateTime dateAdded;
}
