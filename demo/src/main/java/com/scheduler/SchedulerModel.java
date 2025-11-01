package com.scheduler;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.*;

public class SchedulerModel {
  private final Config cfg;

  public SchedulerModel(Config cfg) {
    this.cfg = cfg;
  }

  public Optional<SolutionResult> solve() {
    Loader.loadNativeLibraries();
    CpModel model = new CpModel();

    int C = cfg.courses.length;
    int S = cfg.timeSlots.length;
    int R = cfg.rooms.length;
    int P = cfg.teachers.length;

    // BoolVar assign[c][s][r][p]
    BoolVar[][][][] assign = new BoolVar[C][S][R][P];

    for (int c = 0; c < C; c++) {
      String course = cfg.courses[c];
      for (int s = 0; s < S; s++) {
        String slot = cfg.timeSlots[s];
        for (int r = 0; r < R; r++) {
          String room = cfg.rooms[r];
          for (int p = 0; p < P; p++) {
            String teacher = cfg.teachers[p];
            String name = String.format("c%d_s%d_r%d_p%d", c, s, r, p);
            BoolVar v = model.newBoolVar(name);
            assign[c][s][r][p] = v;

            // Disallow if teacher not qualified
            List<String> qual = cfg.teacherQualifications.getOrDefault(teacher, Collections.emptyList());
            if (!qual.contains(course)) {
              model.addEquality(v, 0);
              continue;
            }
            // Disallow AP if not certified
            if (cfg.apCourses.contains(course)) {
              List<String> apList = cfg.apCertified.getOrDefault(teacher, Collections.emptyList());
              if (!apList.contains(course)) {
                model.addEquality(v, 0);
                continue;
              }
            }
            // Room type mismatch
            String req = cfg.courseRoomRequirements.getOrDefault(course, "standard");
            String rtype = cfg.roomTypes.getOrDefault(room, "standard");
            if (!rtype.equals(req)) {
              model.addEquality(v, 0);
              continue;
            }
            // Teacher availability
            List<String> tAvail = cfg.teacherAvailability.getOrDefault(teacher, Collections.emptyList());
            if (!tAvail.contains(slot)) {
              model.addEquality(v, 0);
              continue;
            }
            // Room availability
            List<String> rAvail = cfg.roomAvailability.getOrDefault(room, Collections.emptyList());
            if (!rAvail.contains(slot)) {
              model.addEquality(v, 0);
              continue;
            }
          }
        }
      }
    }

    // Each course exactly once
    for (int c = 0; c < C; c++) {
      List<BoolVar> terms = new ArrayList<>();
      for (int s = 0; s < S; s++)
        for (int r = 0; r < R; r++)
          for (int p = 0; p < P; p++)
            terms.add(assign[c][s][r][p]);
      model.addEquality(LinearExpr.sum(terms.toArray(new BoolVar[0])), 1);
    }

    // One class per room per timeslot
    for (int s = 0; s < S; s++) {
      for (int r = 0; r < R; r++) {
        List<BoolVar> terms = new ArrayList<>();
        for (int c = 0; c < C; c++)
          for (int p = 0; p < P; p++)
            terms.add(assign[c][s][r][p]);
        model.addLessOrEqual(LinearExpr.sum(terms.toArray(new BoolVar[0])), 1);
      }
    }

    // Teacher cannot teach more than one class at same timeslot
    for (int s = 0; s < S; s++) {
      for (int p = 0; p < P; p++) {
        List<BoolVar> terms = new ArrayList<>();
        for (int c = 0; c < C; c++)
          for (int r = 0; r < R; r++)
            terms.add(assign[c][s][r][p]);
        model.addLessOrEqual(LinearExpr.sum(terms.toArray(new BoolVar[0])), 1);
      }
    }

    // Day slot mapping
    Map<String, List<Integer>> daySlots = new HashMap<>();
    for (int s = 0; s < S; s++) {
      String day = (String)cfg.slotInfo.get(cfg.timeSlots[s]).get("day");
      daySlots.computeIfAbsent(day, k -> new ArrayList<>()).add(s);
    }

    // Max classes per day and prep periods
    for (int p = 0; p < P; p++) {
      String teacher = cfg.teachers[p];
      int prepReq = cfg.requiredPrepPeriods.getOrDefault(teacher, 0);
      for (Map.Entry<String, List<Integer>> e : daySlots.entrySet()) {
        List<Integer> ds = e.getValue();
        List<BoolVar> terms = new ArrayList<>();
        for (int s : ds)
          for (int c = 0; c < C; c++)
            for (int r = 0; r < R; r++)
              terms.add(assign[c][s][r][p]);
        int allowed = Math.min(ds.size() - prepReq, cfg.maxClassesPerDay);
        if (allowed < 0) allowed = 0;
        model.addLessOrEqual(LinearExpr.sum(terms.toArray(new BoolVar[0])), allowed);
      }
    }

    // Max consecutive teaching via sliding window per day
    for (int p = 0; p < P; p++) {
      String teacher = cfg.teachers[p];
      for (Map.Entry<String, List<Integer>> e : daySlots.entrySet()) {
        List<Integer> slotsForDay = new ArrayList<>(e.getValue());
        slotsForDay.sort(Comparator.comparingInt(i -> (int)cfg.slotInfo.get(cfg.timeSlots[i]).get("hour")));
        int windowSize = cfg.maxConsecutiveTeaching + 1;
        for (int i = 0; i + windowSize <= slotsForDay.size(); i++) {
          List<BoolVar> windowVars = new ArrayList<>();
          for (int j = 0; j < windowSize; j++) {
            int s = slotsForDay.get(i + j);
            for (int c = 0; c < C; c++)
              for (int r = 0; r < R; r++)
                windowVars.add(assign[c][s][r][p]);
          }
          model.addLessOrEqual(LinearExpr.sum(windowVars.toArray(new BoolVar[0])), cfg.maxConsecutiveTeaching);
        }
      }
    }

    // Solve (feasible)
    CpSolver solver = new CpSolver();
    solver.getParameters().setMaxTimeInSeconds(20.0);

    CpSolverStatus status = solver.solve(model);

    if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
      // build result
      SolutionResult res = new SolutionResult();
      for (int c = 0; c < C; c++) {
        for (int s = 0; s < S; s++) {
          for (int r = 0; r < R; r++) {
            for (int p = 0; p < P; p++) {
              if (solver.booleanValue(assign[c][s][r][p])) {
                res.assignments.add(new Assignment(cfg.courses[c], cfg.timeSlots[s], cfg.rooms[r], cfg.teachers[p]));
              }
            }
          }
        }
      }
      return Optional.of(res);
    } else {
      return Optional.empty();
    }
  }

  // Simple DTOs
  public static class SolutionResult {
    public final List<Assignment> assignments = new ArrayList<>();
  }

  public static class Assignment {
    public final String course;
    public final String slot;
    public final String room;
    public final String teacher;

    public Assignment(String course, String slot, String room, String teacher) {
      this.course = course;
      this.slot = slot;
      this.room = room;
      this.teacher = teacher;
    }
  }
}
