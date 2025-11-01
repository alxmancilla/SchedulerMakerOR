package com.scheduler;

import com.scheduler.SchedulerModel.Assignment;
import com.scheduler.SchedulerModel.SolutionResult;

import java.util.*;

public class PrettyPrinter {
  public static void print(SolutionResult res) {
    if (res == null || res.assignments.isEmpty()) {
      System.out.println("No assignments to print.");
      return;
    }
    System.out.println("Schedule found:");
    res.assignments.sort(Comparator.comparing(a -> a.slot));
    for (Assignment a : res.assignments) {
      System.out.printf("%-12s -> %-11s | %-25s | Teacher: %-12s%n",
        a.course, a.slot, a.room, a.teacher);
    }

    System.out.println("\nTeacher load summary:");
    Map<String, Integer> load = new HashMap<>();
    for (Assignment a : res.assignments) load.merge(a.teacher, 1, Integer::sum);
    for (Map.Entry<String, Integer> e : load.entrySet()) {
      System.out.printf("%-12s : %d%n", e.getKey(), e.getValue());
    }
  }
}

