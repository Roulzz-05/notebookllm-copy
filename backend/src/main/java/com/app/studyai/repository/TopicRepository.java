package com.app.studyai.repository;

import com.app.studyai.model.Topic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    @EntityGraph(attributePaths = {"children", "children.children"})
    @Query("SELECT t FROM Topic t WHERE t.document.id = :documentId AND t.parent IS NULL")
    List<Topic> findByDocumentIdAndParentIsNull(@Param("documentId") Long documentId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByDocumentId(Long documentId);
}
