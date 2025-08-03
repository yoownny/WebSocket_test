//package com.ssafy.backend.repository.memory.api;
//
//import com.ssafy.backend.memory.Room;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface RoomRepository {
//    /**
//     * 사용 가능한 다음 방 ID를 가져옵니다.
//     * @return 새로운 방 ID
//     * @throws IllegalStateException 사용 가능한 방이 없을 경우
//     */
//    long getNextRoomId();
//
//    Room save(Room room);
//
//    Optional<Room> findById(long roomId);
//
//    List<Room> findAll();
//
//    void delete(long roomId);
//
//    void clearStore();
//}