# 🏫 School Scheduler using Google OR-Tools (Java)

This project demonstrates a **constraint-based school scheduling system** built in **Java** using **Google OR-Tools (CP-SAT solver)**.

It automatically assigns courses to **teachers**, **rooms**, and **time slots** while enforcing real-world constraints such as teacher qualifications, room types, availability, prep periods, and more.

---

## ✨ Features

- ✅ Assigns courses → teacher → room → time slot automatically  
- 🧑‍🏫 Respects teacher qualifications and AP certifications  
- 🧪 Enforces room type requirements (standard vs. science lab)  
- 📅 Handles teacher and room availability  
- 🕐 Enforces prep periods and limits daily workload  
- 🚫 Prevents overlapping or consecutive teaching beyond limits  
- ⚖️ Finds any feasible schedule satisfying all constraints  

---

## 🧰 Tech Stack

| Component | Description |
|------------|-------------|
| **Language** | Java 11+ |
| **Solver** | [Google OR-Tools (CP-SAT)](https://developers.google.com/optimization) |
| **Build Tool** | Maven |

---

## 📦 Project Structure


---

## ⚙️ Prerequisites

- Java 11 or higher  
- Maven 3.6+  
- Internet connection to download OR-Tools via Maven

---

## 🚀 Setup & Run

1. **Clone this repository**
```bash
   git clone https://github.com/alxmancilla/SchedulerMakerOR.git
   cd SchedulerMakerOR
 ```

2. **Compile and run**
```bash
mvn compile exec:java -Dexec.mainClass=SchoolScheduler
```
