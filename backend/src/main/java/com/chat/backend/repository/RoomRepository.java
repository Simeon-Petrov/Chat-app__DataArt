package com.chat.backend.repository;

import com.chat.backend.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByType(String type);
    Optional<Room> findByName(String name);
    boolean existsByName(String name);
}