package org.example.recommendhouse.dto;

import lombok.Data;

@Data
public class PolicyNewsResponse {
    private Response response;

    @Data
    public static class Response {
        private Body body;
    }

    @Data
    public static class Body {
        private Items items;
    }

    @Data
    public static class Items {
        private PolicyNews[] item;
    }
}