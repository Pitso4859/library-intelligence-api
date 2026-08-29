package com.pitso.repository;

/** Lightweight projection used by the analytics endpoint. */
public interface AuthorCountProjection {
    String getAuthor();
    long getBookCount();
}
