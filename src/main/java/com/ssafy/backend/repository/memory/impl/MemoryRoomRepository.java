//package com.ssafy.backend.repository.memory.impl;
//
//import com.ssafy.backend.memory.Room;
//import com.ssafy.backend.repository.memory.api.RoomRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//@Repository
//public class MemoryRoomRepository implements RoomRepository {
//
//    private static final int MAX_ROOMS = 1000; // 방 ID 범위: 0 ~ 999
//    private final ConcurrentHashMap<Long, Room> rooms = new ConcurrentHashMap<>(); // 방 저장소
//    private final ConcurrentLinkedQueue<Long> availableRoomIds = new ConcurrentLinkedQueue<>(); // 방번호 풀
//
//    public MemoryRoomRepository() {
//        // 방 번호 풀 초기화 (0~999)
//        for (long i = 0; i < MAX_ROOMS; i++) {
//            availableRoomIds.offer(i);
//        }
//    }
//
//    @Override
//    public long getNextRoomId() {
//        Long roomNumber = availableRoomIds.poll();
//        if (roomNumber != null) {
//            return roomNumber;
//        }
//        // 풀이 비었으면 예외 발생 (모든 방이 사용 중)
//        throw new IllegalStateException("No available rooms.");
//    }
//
//    @Override
//    public Room save(Room room) {
//        rooms.put(room.getRoomId(), room);
//        return room;
//    }
//
//    @Override
//    public Optional<Room> findById(long roomId) {
//        return Optional.ofNullable(rooms.get(roomId));
//    }
//
//    @Override
//    public List<Room> findAll() {
//        return new ArrayList<>(rooms.values());
//    }
//
//    @Override
//    public void delete(long roomId) {
//        Room removedRoom = rooms.remove(roomId);
//        if (removedRoom != null && removedRoom.isEmpty()) {
//            // 방이 비어있을 때만 번호 풀에 반환
//            availableRoomIds.offer(roomId);
//        }
//        // 방이 비어있지 않으면 해당 번호는 그냥 "소실"됨
//        // 1000개 중 몇 개 소실되는 것은 큰 문제가 아님
//    }
//
//    @Override
//    public void clearStore() {
//        rooms.clear();
//        availableRoomIds.clear();
//        for (long i = 0; i < MAX_ROOMS; i++) {
//            availableRoomIds.offer(i);
//        }
//    }
//
//    // 주기적으로 빈 방들 정리하는 스케줄러 (선택사항)
////    @Scheduled(fixedRate = 300000) // 5분마다
////    public void cleanupEmptyRooms() {
////        rooms.entrySet().removeIf(entry -> {
////            Room room = entry.getValue();
////            if (room.isEmpty()) {
////                availableRoomIds.offer(entry.getKey());
////                return true;
////            }
////            return false;
////        });
////    }
//}