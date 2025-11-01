package com.scheduler;

import com.scheduler.SchedulerModel.SolutionResult;

import java.util.Optional;

public class Main {
  public static void main(String[] args) {
    Config cfg = new Config();
    SchedulerModel model = new SchedulerModel(cfg);
    Optional<SolutionResult> maybe = model.solve();
    if (maybe.isPresent()) {
      PrettyPrinter.print(maybe.get());
    } else {
      System.out.println("No feasible schedule found with the current constraints.");
    }
  }
}
