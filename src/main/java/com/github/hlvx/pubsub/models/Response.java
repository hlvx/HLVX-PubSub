package com.github.hlvx.pubsub.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {
    private Type type;
    private String nonce;
    private Error error;

    public Response(Type type, String nonce) {
        this.type = type;
        this.nonce = nonce;
    }

    @JsonProperty("error")
    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @JsonProperty("type")
    public Type getType() {
        return type;
    }

    @JsonProperty("nonce")
    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {
        MESSAGE,
        RESPONSE
    }

    public enum Error {
        ERR_BADMESSAGE,
        ERR_BADAUTH,
        ERR_SERVER,
        ERR_BADTOPIC
    }
}
