package com.byteforce.web;

import java.util.List;

public record Task(
    int id,
    String title,
    String status,
    boolean isCompleted,
    String createdAt,
    boolean isDraft,
    boolean isDeleted,
    List<SubTask> subtasks) {
  public int subtaskCount() {
    return subtasks.size();
  }
}
