package moodlit_api.moodlit.dto;


import lombok.*;
import java.util.UUID;

public class AuthResponses {

    @Data
    @AllArgsConstructor
    public static class Auth {
        private String token;
        private UserInfo user;
    }

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String fullName;
        private String email;
        private String authProvider;
    }

    @Data
    @AllArgsConstructor
    public static class Message {
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class Error {
        private String error;
    }
}