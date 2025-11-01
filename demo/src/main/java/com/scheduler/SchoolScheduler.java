package com.scheduler;
// SchoolScheduler.java

// SchoolSchedulerFullConfig.java
import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;

import java.util.*;
import java.util.stream.Collectors;

public class SchoolScheduler {
  public static void main(String[] args) {
    Loader.loadNativeLibraries();
    // ---- Data from user ----
    String[] teachers = {"Ms. Smith", "Mr. Jones", "Dr. Brown", "Ms. Davis", "Dr. Lee"};
    String[] courses = {"Math 101", "Math 102", "Physics 101", "Chemistry 101", "English 101",
                        "AP Calculus", "AP Physics", "Biology 101"};
    String[] timeSlots = {
      "Mon 8-9","Mon 9-10","Mon 10-11","Mon 11-12","Mon 1-2","Mon 2-3",
      "Tue 8-9","Tue 9-10","Tue 10-11","Tue 11-12","Tue 1-2","Tue 2-3",
      "Wed 8-9","Wed 9-10","Wed 10-11","Wed 11-12","Wed 1-2","Wed 2-3"
    };
    String[] rooms = {
      "Building A - Room 101","Building A - Room 102","Building A - Lab 201",
      "Building B - Room 301","Building B - Lab 302","Building C - Room 401"
    };

    Map<String, Map<String,Object>> slotInfo = new HashMap<>();
    // fill slotInfo (only necessary fields used)
    for (String s : timeSlots) {
      String[] parts = s.split(" ");
      String day = parts[0];
      String hours = parts[1];
      int hour = Integer.parseInt(hours.split("-")[0]);
      Map<String,Object> meta = new HashMap<>();
      meta.put("day", day);
      meta.put("hour", hour);
      meta.put("is_lunch", false);
      slotInfo.put(s, meta);
    }
    // room_buildings
    Map<String,String> roomBuildings = new HashMap<>();
    roomBuildings.put("Building A - Room 101","A");
    roomBuildings.put("Building A - Room 102","A");
    roomBuildings.put("Building A - Lab 201","A");
    roomBuildings.put("Building B - Room 301","B");
    roomBuildings.put("Building B - Lab 302","B");
    roomBuildings.put("Building C - Room 401","C");

    Map<String,String> roomTypes = new HashMap<>();
    roomTypes.put("Building A - Room 101","standard");
    roomTypes.put("Building A - Room 102","standard");
    roomTypes.put("Building A - Lab 201","science_lab");
    roomTypes.put("Building B - Room 301","standard");
    roomTypes.put("Building B - Lab 302","science_lab");
    roomTypes.put("Building C - Room 401","standard");

    Map<String,String> courseRoomRequirements = new HashMap<>();
    courseRoomRequirements.put("Math 101","standard");
    courseRoomRequirements.put("Math 102","standard");
    courseRoomRequirements.put("Physics 101","science_lab");
    courseRoomRequirements.put("Chemistry 101","science_lab");
    courseRoomRequirements.put("English 101","standard");
    courseRoomRequirements.put("AP Calculus","standard");
    courseRoomRequirements.put("AP Physics","science_lab");
    courseRoomRequirements.put("Biology 101","science_lab");

    Set<String> apCourses = new HashSet<>(Arrays.asList("AP Calculus","AP Physics"));

    Map<String, List<String>> teacherQualifications = new HashMap<>();
    teacherQualifications.put("Ms. Smith", Arrays.asList("Math 101","Math 102","AP Calculus"));
    teacherQualifications.put("Mr. Jones", Arrays.asList("Math 101","Physics 101","AP Physics"));
    teacherQualifications.put("Dr. Brown", Arrays.asList("Physics 101","Chemistry 101"));
    teacherQualifications.put("Ms. Davis", Arrays.asList("English 101"));
    teacherQualifications.put("Dr. Lee", Arrays.asList("Biology 101","Chemistry 101"));

    Map<String, List<String>> apCertified = new HashMap<>();
    apCertified.put("Ms. Smith", Arrays.asList("AP Calculus"));
    apCertified.put("Mr. Jones", Arrays.asList("AP Physics"));
    apCertified.put("Dr. Brown", Collections.emptyList());
    apCertified.put("Ms. Davis", Collections.emptyList());
    apCertified.put("Dr. Lee", Collections.emptyList());

    // Teacher availability: Ms. Smith: Mon/Tue/Wed; Mr. Jones Mon/Tue; Dr. Brown Mon/Wed; Ms Davis all; Dr. Lee Tue/Wed
    Map<String, List<String>> teacherAvailability = new HashMap<>();
    teacherAvailability.put("Ms. Smith", Arrays.stream(timeSlots).filter(s-> {
      String day = (String)slotInfo.get(s).get("day"); return day.equals("Mon")||day.equals("Tue")||day.equals("Wed");
    }).collect(Collectors.toList()));
    teacherAvailability.put("Mr. Jones", Arrays.stream(timeSlots).filter(s-> {
      String day = (String)slotInfo.get(s).get("day"); return day.equals("Mon")||day.equals("Tue");
    }).collect(Collectors.toList()));
    teacherAvailability.put("Dr. Brown", Arrays.stream(timeSlots).filter(s-> {
      String day = (String)slotInfo.get(s).get("day"); return day.equals("Mon")||day.equals("Wed");
    }).collect(Collectors.toList()));
    teacherAvailability.put("Ms. Davis", Arrays.stream(timeSlots).filter(s-> {
      String day = (String)slotInfo.get(s).get("day"); return day.equals("Mon")||day.equals("Tue")||day.equals("Wed");
    }).collect(Collectors.toList()));
    teacherAvailability.put("Dr. Lee", Arrays.stream(timeSlots).filter(s-> {
      String day = (String)slotInfo.get(s).get("day"); return day.equals("Tue")||day.equals("Wed");
    }).collect(Collectors.toList()));

    // Room availability: all rooms are available for all timeSlots
    Map<String, List<String>> roomAvailability = new HashMap<>();
    for (String r : rooms) roomAvailability.put(r, Arrays.asList(timeSlots));

    Map<String,Integer> requiredPrepPeriods = new HashMap<>();
    requiredPrepPeriods.put("Ms. Smith", 1);
    requiredPrepPeriods.put("Mr. Jones", 1);
    requiredPrepPeriods.put("Dr. Brown", 1);
    requiredPrepPeriods.put("Ms. Davis", 1);
    requiredPrepPeriods.put("Dr. Lee", 1);

    int maxConsecutiveTeaching = 3;
    int maxClassesPerDay = 5;

    // ---- Model ----
    CpModel model = new CpModel();

    int C = courses.length;
    int S = timeSlots.length;
    int R = rooms.length;
    int P = teachers.length;

    // index maps
    Map<String,Integer> courseIndex = new HashMap<>();
    for (int i=0;i<C;i++) courseIndex.put(courses[i], i);
    Map<String,Integer> slotIndex = new HashMap<>();
    for (int i=0;i<S;i++) slotIndex.put(timeSlots[i], i);
    Map<String,Integer> roomIndex = new HashMap<>();
    for (int i=0;i<R;i++) roomIndex.put(rooms[i], i);
    Map<String,Integer> teacherIndex = new HashMap<>();
    for (int i=0;i<P;i++) teacherIndex.put(teachers[i], i);

    // 4D assign[c][s][r][p] boolean
    IntVar[][][][] assign = new IntVar[C][S][R][P];
    for (int c=0;c<C;c++) {
      String course = courses[c];
      for (int s=0;s<S;s++) {
        String slot = timeSlots[s];
        for (int r=0;r<R;r++) {
          String room = rooms[r];
          for (int p=0;p<P;p++) {
            String teacher = teachers[p];
            String name = String.format("c%d_s%d_r%d_p%d", c,s,r,p);
            assign[c][s][r][p] = model.newBoolVar(name);

            // disallow if teacher not qualified for course
            List<String> qual = teacherQualifications.getOrDefault(teacher, Collections.emptyList());
            if (!qual.contains(course)) {
              model.addEquality(assign[c][s][r][p], 0);
              continue;
            }
            // disallow if course is AP but teacher lacks AP cert for that course
            if (apCourses.contains(course)) {
              List<String> apList = apCertified.getOrDefault(teacher, Collections.emptyList());
              if (!apList.contains(course)) {
                model.addEquality(assign[c][s][r][p], 0);
                continue;
              }
            }
            // disallow if room doesn't match course requirement
            String req = courseRoomRequirements.getOrDefault(course,"standard");
            String rtype = roomTypes.getOrDefault(room,"standard");
            if (!rtype.equals(req)) {
              model.addEquality(assign[c][s][r][p], 0);
              continue;
            }
            // disallow if teacher not available at slot
            List<String> tAvail = teacherAvailability.getOrDefault(teacher, Collections.emptyList());
            if (!tAvail.contains(slot)) {
              model.addEquality(assign[c][s][r][p], 0);
              continue;
            }
            // disallow if room not available at slot
            List<String> rAvail = roomAvailability.getOrDefault(room, Collections.emptyList());
            if (!rAvail.contains(slot)) {
              model.addEquality(assign[c][s][r][p], 0);
              continue;
            }
            // otherwise allowed
          }
        }
      }
    }

    // Each course must be assigned exactly once (one slot, one room, one teacher)
    for (int c=0;c<C;c++) {
      List<IntVar> terms = new ArrayList<>();
      for (int s=0;s<S;s++) for (int r=0;r<R;r++) for (int p=0;p<P;p++) terms.add(assign[c][s][r][p]);
      model.addEquality(LinearExpr.sum(terms.toArray(new IntVar[0])), 1);
    }

    // One class per room per timeslot
    for (int s=0;s<S;s++) {
      for (int r=0;r<R;r++) {
        List<IntVar> terms = new ArrayList<>();
        for (int c=0;c<C;c++) for (int p=0;p<P;p++) terms.add(assign[c][s][r][p]);
        model.addLessOrEqual(LinearExpr.sum(terms.toArray(new IntVar[0])), 1);
      }
    }

    // Teacher cannot teach more than one class at same timeslot
    for (int s=0;s<S;s++) {
      for (int p=0;p<P;p++) {
        List<IntVar> terms = new ArrayList<>();
        for (int c=0;c<C;c++) for (int r=0;r<R;r++) terms.add(assign[c][s][r][p]);
        model.addLessOrEqual(LinearExpr.sum(terms.toArray(new IntVar[0])), 1);
      }
    }

    // Max classes per day and required prep periods
    // Build day->slot indices mapping
    Map<String, List<Integer>> daySlots = new HashMap<>();
    for (int s=0;s<S;s++) {
      String day = (String)slotInfo.get(timeSlots[s]).get("day");
      daySlots.computeIfAbsent(day, k->new ArrayList<>()).add(s);
    }
    int slotsPerDay = daySlots.values().stream().mapToInt(List::size).max().orElse(0);

    for (int p=0;p<P;p++) {
      String teacher = teachers[p];
      int prepReq = requiredPrepPeriods.getOrDefault(teacher, 0);
      for (Map.Entry<String, List<Integer>> e : daySlots.entrySet()) {
        List<Integer> ds = e.getValue();
        List<IntVar> terms = new ArrayList<>();
        for (int s : ds) for (int c=0;c<C;c++) for (int r=0;r<R;r++) terms.add(assign[c][s][r][p]);
        // teacher must have at least prepReq free slots => teaching <= slotsInDay - prepReq
        int allowed = ds.size() - prepReq;
        allowed = Math.min(allowed, maxClassesPerDay); // also cap by maxClassesPerDay
        model.addLessOrEqual(LinearExpr.sum(terms.toArray(new IntVar[0])), allowed);
      }
    }

    // Max consecutive teaching: sliding window per day
    for (int p=0;p<P;p++) {
      String teacher = teachers[p];
      for (Map.Entry<String, List<Integer>> e : daySlots.entrySet()) {
        List<Integer> slotsForDay = new ArrayList<>(e.getValue());
        // sort by hour to ensure order (slotInfo keyed has hour)
        slotsForDay.sort(Comparator.comparingInt(i -> (int)slotInfo.get(timeSlots[i]).get("hour")));
        int windowSize = maxConsecutiveTeaching + 1;
        for (int i=0; i + windowSize <= slotsForDay.size(); i++) {
          List<IntVar> windowVars = new ArrayList<>();
          for (int j=0;j<windowSize;j++) {
            int s = slotsForDay.get(i+j);
            for (int c=0;c<C;c++) for (int r=0;r<R;r++) windowVars.add(assign[c][s][r][p]);
          }
          model.addLessOrEqual(LinearExpr.sum(windowVars.toArray(new IntVar[0])), maxConsecutiveTeaching);
        }
      }
    }

    // Solve
    CpSolver solver = new CpSolver();
    solver.getParameters().setMaxTimeInSeconds(20.0);
    CpSolverStatus status = solver.solve(model);

    if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
      System.out.println("Schedule found:");
      for (int c=0;c<C;c++) {
        for (int s=0;s<S;s++) {
          for (int r=0;r<R;r++) {
            for (int p=0;p<P;p++) {
              if (solver.booleanValue((BoolVar)assign[c][s][r][p])) {
                System.out.printf("%-12s -> %-11s | %-22s | Teacher: %-10s%n",
                                  courses[c], timeSlots[s], rooms[r], teachers[p]);
              }
            }
          }
        }
      }
      // summary: teacher load
      System.out.println("\nTeacher load summary (total assigned):");
      for (int p=0;p<P;p++) {
        int total=0;
        for (int c=0;c<C;c++) for (int s=0;s<S;s++) for (int r=0;r<R;r++)
          if (solver.booleanValue((BoolVar)assign[c][s][r][p])) total++;
        System.out.printf("%-10s : %d%n", teachers[p], total);
      }
    } else {
      System.out.println("No feasible schedule found with the current constraints.");
    }
  }
}
