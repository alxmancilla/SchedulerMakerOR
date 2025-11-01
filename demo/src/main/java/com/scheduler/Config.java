package com.scheduler;

import java.util.*;
import java.util.stream.Collectors;

public final class Config {
  public final String[] teachers = {
    "Ms. Smith", "Mr. Jones", "Dr. Brown", "Ms. Davis", "Dr. Lee"
  };

  public final String[] courses = {
    "Math 101", "Math 102", "Physics 101", "Chemistry 101",
    "English 101", "AP Calculus", "AP Physics", "Biology 101"
  };

  public final String[] timeSlots = {
    "Mon 8-9","Mon 9-10","Mon 10-11","Mon 11-12","Mon 1-2","Mon 2-3",
    "Tue 8-9","Tue 9-10","Tue 10-11","Tue 11-12","Tue 1-2","Tue 2-3",
    "Wed 8-9","Wed 9-10","Wed 10-11","Wed 11-12","Wed 1-2","Wed 2-3"
  };

  public final String[] rooms = {
    "Building A - Room 101","Building A - Room 102","Building A - Lab 201",
    "Building B - Room 301","Building B - Lab 302","Building C - Room 401"
  };

  public final Map<String, Map<String, Object>> slotInfo = new HashMap<>();
  public final Map<String, String> roomBuildings = new HashMap<>();
  public final Map<String, String> roomTypes = new HashMap<>();
  public final Map<String, String> courseRoomRequirements = new HashMap<>();
  public final Set<String> apCourses = new HashSet<>();
  public final Map<String, List<String>> teacherQualifications = new HashMap<>();
  public final Map<String, List<String>> apCertified = new HashMap<>();
  public final Map<String, List<String>> teacherAvailability = new HashMap<>();
  public final Map<String, List<String>> roomAvailability = new HashMap<>();
  public final Map<String, Integer> requiredPrepPeriods = new HashMap<>();
  public final int maxConsecutiveTeaching = 3;
  public final int maxClassesPerDay = 5;

  // index maps for convenience
  public final Map<String, Integer> courseIndex = new HashMap<>();
  public final Map<String, Integer> slotIndex = new HashMap<>();
  public final Map<String, Integer> roomIndex = new HashMap<>();
  public final Map<String, Integer> teacherIndex = new HashMap<>();

  public Config() {
    // slotInfo
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

    // room buildings
    roomBuildings.put("Building A - Room 101","A");
    roomBuildings.put("Building A - Room 102","A");
    roomBuildings.put("Building A - Lab 201","A");
    roomBuildings.put("Building B - Room 301","B");
    roomBuildings.put("Building B - Lab 302","B");
    roomBuildings.put("Building C - Room 401","C");

    // room types
    roomTypes.put("Building A - Room 101","standard");
    roomTypes.put("Building A - Room 102","standard");
    roomTypes.put("Building A - Lab 201","science_lab");
    roomTypes.put("Building B - Room 301","standard");
    roomTypes.put("Building B - Lab 302","science_lab");
    roomTypes.put("Building C - Room 401","standard");

    // course room requirements
    courseRoomRequirements.put("Math 101","standard");
    courseRoomRequirements.put("Math 102","standard");
    courseRoomRequirements.put("Physics 101","science_lab");
    courseRoomRequirements.put("Chemistry 101","science_lab");
    courseRoomRequirements.put("English 101","standard");
    courseRoomRequirements.put("AP Calculus","standard");
    courseRoomRequirements.put("AP Physics","science_lab");
    courseRoomRequirements.put("Biology 101","science_lab");

    // AP courses
    apCourses.addAll(Arrays.asList("AP Calculus","AP Physics"));

    // teacher qualifications
    teacherQualifications.put("Ms. Smith", Arrays.asList("Math 101","Math 102","AP Calculus"));
    teacherQualifications.put("Mr. Jones", Arrays.asList("Math 101","Physics 101","AP Physics"));
    teacherQualifications.put("Dr. Brown", Arrays.asList("Physics 101","Chemistry 101"));
    teacherQualifications.put("Ms. Davis", Arrays.asList("English 101"));
    teacherQualifications.put("Dr. Lee", Arrays.asList("Biology 101","Chemistry 101"));

    // ap certified
    apCertified.put("Ms. Smith", Arrays.asList("AP Calculus"));
    apCertified.put("Mr. Jones", Arrays.asList("AP Physics"));
    apCertified.put("Dr. Brown", Collections.emptyList());
    apCertified.put("Ms. Davis", Collections.emptyList());
    apCertified.put("Dr. Lee", Collections.emptyList());

    // teacher availability
    teacherAvailability.put("Ms. Smith", Arrays.stream(timeSlots)
      .filter(s -> {
        String day = (String) slotInfo.get(s).get("day");
        return day.equals("Mon")||day.equals("Tue")||day.equals("Wed");
      }).collect(Collectors.toList()));

    teacherAvailability.put("Mr. Jones", Arrays.stream(timeSlots)
      .filter(s -> {
        String day = (String) slotInfo.get(s).get("day");
        return day.equals("Mon")||day.equals("Tue");
      }).collect(Collectors.toList()));

    teacherAvailability.put("Dr. Brown", Arrays.stream(timeSlots)
      .filter(s -> {
        String day = (String) slotInfo.get(s).get("day");
        return day.equals("Mon")||day.equals("Wed");
      }).collect(Collectors.toList()));

    teacherAvailability.put("Ms. Davis", Arrays.stream(timeSlots)
      .filter(s -> {
        String day = (String) slotInfo.get(s).get("day");
        return day.equals("Mon")||day.equals("Tue")||day.equals("Wed");
      }).collect(Collectors.toList()));

    teacherAvailability.put("Dr. Lee", Arrays.stream(timeSlots)
      .filter(s -> {
        String day = (String) slotInfo.get(s).get("day");
        return day.equals("Tue")||day.equals("Wed");
      }).collect(Collectors.toList()));

    // room availability: all rooms available all slots
    for (String r : rooms) {
      roomAvailability.put(r, Arrays.asList(timeSlots));
    }

    // required prep periods per day
    requiredPrepPeriods.put("Ms. Smith", 1);
    requiredPrepPeriods.put("Mr. Jones", 1);
    requiredPrepPeriods.put("Dr. Brown", 1);
    requiredPrepPeriods.put("Ms. Davis", 1);
    requiredPrepPeriods.put("Dr. Lee", 1);

    // indexes
    for (int i = 0; i < courses.length; i++) courseIndex.put(courses[i], i);
    for (int i = 0; i < timeSlots.length; i++) slotIndex.put(timeSlots[i], i);
    for (int i = 0; i < rooms.length; i++) roomIndex.put(rooms[i], i);
    for (int i = 0; i < teachers.length; i++) teacherIndex.put(teachers[i], i);
  }
}
