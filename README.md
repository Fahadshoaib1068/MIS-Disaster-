# Smart Disaster Response MIS

Enterprise-level Smart Disaster Response Management Information System (MIS) developed for efficient disaster coordination, secure transaction handling, and real-time resource management.

## Project Overview

This system is designed to assist government agencies and emergency response teams during disasters such as floods, earthquakes, fires, and medical emergencies. The platform supports multiple stakeholders through a centralized management system with secure database operations and role-based access control.

The system integrates:
- Frontend interface
- Java backend
- SQL database
- Database automation
- Reporting and analytics

---

### Team Members
1. Fahad Shoaib
2. Wania Bakhat
3. Fahima Sohail

## Key Features

### Emergency Reporting
- Real-time disaster report submission
- Incident severity tracking
- Status monitoring and prioritization

### Rescue Team Management
- Rescue team assignment and tracking
- Availability management
- Historical activity records

### Resource Management
- Warehouse inventory management
- Resource allocation workflows
- Low stock alerts
- Dispatch and consumption tracking

### Hospital Coordination
- Bed availability monitoring
- Patient assignment system
- Hospital load balancing

### Financial Management
- Donation tracking
- Expense management
- Procurement records
- Financial audit trails

### Security & RBAC
- Secure authentication system
- Encrypted password storage
- Role-Based Access Control (RBAC)
- Restricted role-specific operations

### Database Features
- ACID-compliant transaction handling
- Triggers for automation
- Views for abstraction and security
- Indexing and performance optimization
- Audit logging and monitoring

### MIS Reporting & Analytics
- Disaster statistics dashboards
- Resource utilization reports
- Financial summaries
- Response analytics

---

## Technologies Used

### Backend
- Java
- Servlets
- JDBC
- Maven

### Frontend
- HTML
- CSS
- JavaScript
- JSP

### Database
- MySQL

---

## System Modules

The application contains:

- Authentication & RBAC
- Emergency Reporting
- Rescue Team Management
- Resource Allocation
- Hospital Coordination
- Financial Management
- Approval Workflows
- Audit & Monitoring
- MIS Dashboards & Analytics

---

## Performance Features

The system includes:

- Query optimization using indexes
- Trigger-based automation
- ACID transaction handling
- Audit logging
- Role-specific views
- Concurrent operation support

---

## Project Structure

```text
Smart-Disaster-Response-MIS/
│
├── database/
│   ├── DB_PROG table.sql
│   ├── Transaction_Handling.sql
│   ├── Triggers_Implementation.sql
│   ├── Indexing_Query_Performance.sql
│   └── views.sql
│
├── src/
│   ├── main/java/com/disaster/
│   ├── main/resources/
│   └── main/webapp/
│
├── pom.xml
├── .gitignore
└── README.md

## Setup Instructions

### 1. Clone Repository

```bash
git clone https://github.com/Fahadshoaib1068/MIS-Disaster-.git
```

---

### 2. Open Project

Open the project in any Java IDE:

- IntelliJ IDEA
- Eclipse
- VS Code

---

### 3. Configure Database

1. Install MySQL Server
2. Create a new database:

```sql
CREATE DATABASE disaster_mis;
```

3. Open the `/database` folder
4. Execute the SQL files in MySQL Workbench or any SQL client:

```text
DB_PROG table.sql
Transaction_Handling.sql
Triggers_Implementation.sql
views.sql
Indexing_Query_Performance.sql
```

---

### 4. Configure Database Connection

Update database credentials inside:

```text
src/main/resources/db.properties
```

Example:

```properties
db.url=jdbc:mysql://localhost:3306/disaster_mis
db.username=root
db.password=your_password
```

---

### 5. Run Application

#### Using Maven

```bash
mvn clean install
```

#### Deploy on Apache Tomcat

1. Configure Apache Tomcat Server
2. Deploy the generated WAR file
3. Start Tomcat Server
4. Open browser:

```text
http://localhost:8080/DB_backend_2/
```

---

