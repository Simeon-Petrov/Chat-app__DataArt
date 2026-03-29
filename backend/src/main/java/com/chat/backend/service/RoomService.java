package com.chat.backend.service;

import com.chat.backend.model.*;
import com.chat.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomBanRepository roomBanRepository;
    private final UserRepository userRepository;

    public Room createRoom(String name, String description, String type, User owner) {
        if (roomRepository.existsByName(name)) {
            throw new IllegalArgumentException("Room name already taken");
        }
        Room room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setType(type);
        room.setOwner(owner);
        roomRepository.save(room);

        RoomMember member = new RoomMember();
        member.setRoom(room);
        member.setUser(owner);
        member.setRole("OWNER");
        roomMemberRepository.save(member);

        return room;
    }

    public List<Room> getPublicRooms() {
        return roomRepository.findByType("PUBLIC");
    }

    @Transactional
    public void joinRoom(Long roomId, User user) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (roomBanRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            throw new IllegalArgumentException("You are banned from this room");
        }
        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            throw new IllegalArgumentException("Already a member");
        }

        RoomMember member = new RoomMember();
        member.setRoom(room);
        member.setUser(user);
        member.setRole("MEMBER");
        roomMemberRepository.save(member);
    }

    @Transactional
    public void leaveRoom(Long roomId, User user) {
        RoomMember member = roomMemberRepository
                .findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Not a member"));

        if (member.getRole().equals("OWNER")) {
            throw new IllegalArgumentException("Owner cannot leave the room");
        }
        roomMemberRepository.deleteByRoomIdAndUserId(roomId, user.getId());
    }

    @Transactional
    public void kickAndBan(Long roomId, Long targetUserId, User admin) {
        RoomMember adminMember = roomMemberRepository
                .findByRoomIdAndUserId(roomId, admin.getId())
                .orElseThrow(() -> new IllegalArgumentException("Not a member"));

        if (!adminMember.getRole().equals("ADMIN") && !adminMember.getRole().equals("OWNER")) {
            throw new IllegalArgumentException("No permission");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        roomMemberRepository.deleteByRoomIdAndUserId(roomId, targetUserId);

        RoomBan ban = new RoomBan();
        ban.setRoom(room);
        ban.setUser(target);
        ban.setBannedBy(admin);
        roomBanRepository.save(ban);
    }

    @Transactional
    public void deleteRoom(Long roomId, User user) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only owner can delete the room");
        }
        roomRepository.delete(room);
    }

    public List<RoomMember> getRoomMembers(Long roomId) {
        return roomMemberRepository.findByRoomId(roomId);
    }

    @Transactional
    public void inviteUser(Long roomId, Long targetUserId, User inviter) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, inviter.getId())) {
            throw new IllegalArgumentException("You are not a member");
        }
        if (roomBanRepository.existsByRoomIdAndUserId(roomId, targetUserId)) {
            throw new IllegalArgumentException("User is banned");
        }
        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, targetUserId)) {
            throw new IllegalArgumentException("Already a member");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        RoomMember member = new RoomMember();
        member.setRoom(room);
        member.setUser(target);
        member.setRole("MEMBER");
        roomMemberRepository.save(member);
    }

    @Transactional
    public Room getOrCreateDirectRoom(User user1, User user2) {
        String roomName = "dm_" + Math.min(user1.getId(), user2.getId())
                          + "_" + Math.max(user1.getId(), user2.getId());

        return roomRepository.findByName(roomName).orElseGet(() -> {
            Room room = new Room();
            room.setName(roomName);
            room.setDescription("Direct message");
            room.setType("PRIVATE");
            room.setOwner(user1);
            roomRepository.save(room);

            RoomMember m1 = new RoomMember();
            m1.setRoom(room); m1.setUser(user1); m1.setRole("OWNER");
            roomMemberRepository.save(m1);

            RoomMember m2 = new RoomMember();
            m2.setRoom(room); m2.setUser(user2); m2.setRole("MEMBER");
            roomMemberRepository.save(m2);

            return room;
        });
    }
}