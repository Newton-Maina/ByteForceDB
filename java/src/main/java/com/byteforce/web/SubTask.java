package com.byteforce.web;

public record SubTask(
    int sid, int parentId, String subtitle, String substatus, boolean isSubCompleted) {}
