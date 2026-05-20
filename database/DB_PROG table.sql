-- MEMBERS (24I-3040, 24I-3116, 24I-3148)
CREATE DATABASE disaster_mis;
GO 
USE disaster_mis;
GO
create table Warehouse(
WarehouseID int primary key identity(1,1),
WarehouseName varchar(20) not null,
Capacity int not null,
ContactNumber varchar(20),
StreetNo varchar(10),
City varchar(20)
);

create table Users(
UserID int primary key identity(1,1),
UserName varchar(50) not null,
Email varchar(100) not null unique,
PasswordHash varchar(255) not null,
CreatedAt datetime default getdate(),
PhoneNumber varchar(20),
IsActive bit default 1
);

create table Administrator(
UserID int primary key,
FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

create table Field_Officer(
UserID int primary key,
StreetNo varchar(20),
City varchar(20),
FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

create table Emergency_Operator(
UserID int primary key,
FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

create table Warehouse_Manager(
UserID int primary key,
AssignedWarehouseID int,
FOREIGN KEY (UserID) REFERENCES Users(UserID),
FOREIGN KEY (AssignedWarehouseID) REFERENCES Warehouse(WarehouseID)
);

create table Finance_Officer(
UserID int primary key,
Department varchar(50),
FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

create table Disaster_Type(
TypeID int primary key identity(1,1),
TypeName varchar(20) not null check (TypeName in ('Earthquake','Flood','Wildfire','Other'))
);

create table Disaster_Event(
EventID int primary key identity(1,1),
EventName varchar(100) not null,
AffectedRegion varchar(100) not null,
StartDate datetime not null,
EndDate datetime,
SeverityLevel int not null check (SeverityLevel between 1 and 5),
Status varchar(20) not null default 'Closed' check (Status in ('Active', 'Closed')),
BudgetAllocated decimal(18, 2) not null default 0.00
);

create table DisasterEventType(
TypeID int,
EventID int,
PRIMARY KEY (TypeID, EventID),
FOREIGN KEY (TypeID) REFERENCES Disaster_Type(TypeID),
FOREIGN KEY (EventID) REFERENCES Disaster_Event(EventID)
);

create table Emergency_Report (
ReportID int identity(1,1) primary key,
latitude float,
longitude float,
Address varchar(200),
SeverityLevel int not null check (SeverityLevel between 1 and 5),
Reporttime datetime default getdate(),
Status varchar(20) not null default 'Pending' check (Status in ('Pending', 'Assigned', 'Resolved')),
Description varchar(200),
Citizeninfo varchar(100),
Eventid int not null,
foreign key (eventid) references Disaster_Event(EventID)
);

create table Rescue_Team (
TeamID int identity(1,1) primary key,
Teamname varchar(100),
TeamType varchar(50) check (TeamType in ('Medical','Fire','Rescue')),
CurrentLat float,
CurrentLong float,
AvailabilityStatus varchar(50) check (AvailabilityStatus in('Available','Assigned','Busy','Completed')),
Capacity int
);

create table Team_Assignment (
AssignmentID int identity(1,1) primary key,
Teamid int,
Reportid int,
Authorizedby int,
Assignedat datetime default getdate(),
Completedat datetime,
Status varchar(50) check (Status in ('Active','Completed')),
foreign key (Teamid) references Rescue_Team(TeamID),
foreign key (Reportid) references Emergency_Report(ReportID),
foreign key (Authorizedby) references Field_Officer(UserID)
);


create table Resources(
ResourceID int primary key identity(1,1),
ResourceName varchar(20) not null,
ResourceType varchar(20) not null check (ResourceType in ('Food', 'Water', 'Medicine', 'Shelter')),
Unit varchar(15) not null check ( Unit in ('kg', 'liters', 'Units')),
ThresholdLevel int
);

create table Inventory (
Warehouseid int,
Resourceid int,
QuantityAvailable int,
QuantityDispatched int,
QuantityConsumed int,
LastUpdated datetime default getdate(),
primary key (Warehouseid, resourceid),
foreign key (Warehouseid) references Warehouse(warehouseID),
foreign key (resourceid) references Resources(resourceID)
);

create table Resource_Allocation (
Allocationid int identity(1,1) primary key,
Resourceid int,
Warehouseid int,
Eventid int,
Requestedby int,
Requestedqty int,
Approvedqty int,
Allocationdate datetime default getdate(),
Status varchar(50) not null default 'Pending' check (Status in ('Pending','Approved','Rejected','Dispatched')),
Purpose varchar(200),
foreign key (Resourceid) references Resources(ResourceID),
foreign key (Warehouseid) references Warehouse(WarehouseID),
foreign key (Eventid) references Disaster_Event(EventID),
foreign key (Requestedby) references Users(UserID)
);

create table Hospital (
HospitalID int primary key identity(1,1),
Name varchar(100),
StreetNo varchar(20),
City varchar(20),
Totalbeds int,
Availablebeds int,
Contactnumber varchar(20)
);

create table Patient (
PatientID int identity(1,1) primary key,
Name varchar(100),
Age int,
Gender varchar(10),
Condition varchar(50) not null check(Condition in ('Stable','Critical')),
Admissiontime datetime,
Dischargetime datetime,
Hospitalid int,
Reportid int,
foreign key (hospitalid) references Hospital(HospitalID),
foreign key (reportid) references Emergency_Report(ReportID)
);

create table Donor (
DonorID int identity(1,1) primary key,
Donorname varchar(100),
Donortype varchar(50) not null check(Donortype in ('Individual','Organization')),
TotalDonated decimal(18,2) default 0.00,
Contactinfo varchar(100)
);

create table Financial_Transaction (
Transactionid int identity(1,1) primary key,
Transactiontype varchar(50) not null check(Transactiontype in ('Donation','Expense','Procurement')),
Amount float,
Transactiondate datetime default getdate(),
Description varchar(200),
Referencenumber varchar(50),
Eventid int,
Userid int,
Donorid int,
foreign key (Eventid) references Disaster_Event(EventID),
foreign key (Userid) references Users(UserID),
foreign key (Donorid) references Donor(DonorID)
);

create table approval_request (
Approvalid int identity(1,1) primary key,
Requesttype varchar(50) check (Requesttype in ('ResourceAllocation','RescueDeployment','Financial')),
Requestedat datetime default getdate(),
Status varchar(50) not null default 'Pending' check (Status in ('Pending','Approved','Rejected')),
Decisionat datetime,
Comments varchar(200),
Referenceid int,
Requestedby int,
Approvedby int,
foreign key (requestedby) references Users(UserID),
foreign key (approvedby) references Users(UserID)
);

create table Audit_Log (
LogID int identity(1,1) primary key,
TableAffected varchar(50),
Action varchar(20),
Oldvalue varchar(max),
Newvalue varchar(max),
Timestamp datetime default getdate(),
Performedby int,
foreign key (performedby) references Users(UserID)
);

create table Alert (
Alertid int identity(1,1) primary key,
Alerttype varchar(50) check (Alerttype in ('LowStock','Escalation','Assignment')),
Message varchar(255),
Createdat datetime default getdate(),
Isread bit default 0,
Recipientuserid int,
foreign key (recipientuserid) references Users(UserID)
);

-- ── 1. Warehouses ─────────────────────────────────────────────
INSERT INTO Warehouse (WarehouseName, Capacity, ContactNumber, StreetNo, City) VALUES
('Lahore WH-1', 5000, '042-1111111', 'ST-01', 'Lahore'),
('Karachi WH-1', 8000, '021-2222222', 'ST-12', 'Karachi'),
('Islamabad WH', 3000, '051-3333333', 'ST-05', 'Islamabad'),
('Peshawar WH', 4000, '091-4444444', 'ST-08', 'Peshawar'),
('Quetta WH', 3500, '081-5555555', 'ST-03', 'Quetta');
 
-- ── 2. Users ──────────────────────────────────────────────────
 
INSERT INTO Users (UserName, Email, PasswordHash, PhoneNumber, IsActive) VALUES
('Ali Raza', 'ali.raza@mis.gov.pk', 'Admin@123', '0300-1111111', 1), -- 1: Admin
('Sara Khan', 'sara.khan@mis.gov.pk', 'Admin@456', '0301-2222222', 1), -- 2: Admin
('Usman Tariq', 'usman.tariq@mis.gov.pk', 'Operator@123', '0302-3333333', 1), -- 3: Emergency Operator
('Anam Butt', 'anam.butt@mis.gov.pk', 'Operator@456', '0303-4444444', 1), -- 4: Emergency Operator
('Bilal Ahmed', 'bilal.ahmed@mis.gov.pk', 'Officer@123', '0304-5555555', 1), -- 5: Field Officer
('Hina Malik', 'hina.malik@mis.gov.pk', 'Officer@456', '0305-6666666', 1), -- 6: Field Officer
('Fahad Qureshi', 'fahad.q@mis.gov.pk', 'Warehouse@123', '0306-7777777', 1), -- 7: Warehouse Manager
('Zara Iqbal', 'zara.iqbal@mis.gov.pk', 'Warehouse@456', '0307-8888888', 1), -- 8: Warehouse Manager
('Kamran Siddiqui','kamran.s@mis.gov.pk', 'Finance@123', '0308-9999999', 1), -- 9: Finance Officer
('Nadia Hussain', 'nadia.h@mis.gov.pk', 'Finance@456', '0309-0000000', 1); -- 10: Finance Officer
 
-- ── 3. Role Sub-Tables ────────────────────────────────────────
INSERT INTO Administrator (UserID) VALUES (1), (2);
 
INSERT INTO Emergency_Operator (UserID) VALUES (3), (4);
 
INSERT INTO Field_Officer (UserID, StreetNo, City) VALUES
(5, 'ST-22', 'Lahore'),
(6, 'ST-09', 'Karachi');
 
INSERT INTO Warehouse_Manager (UserID, AssignedWarehouseID) VALUES
(7, 1),
(8, 2);
 
INSERT INTO Finance_Officer (UserID, Department) VALUES
(9, 'Federal Finance'),
(10, 'Provincial Finance');
 
-- ── 4. Disaster Types ─────────────────────────────────────────
INSERT INTO Disaster_Type (TypeName) VALUES
('Earthquake'),
('Flood'),
('Wildfire'),
('Other');
 
-- ── 5. Disaster Events ────────────────────────────────────────
INSERT INTO Disaster_Event (EventName, AffectedRegion, StartDate, EndDate, SeverityLevel, Status, BudgetAllocated) VALUES
('Lahore Mega Flood 2025', 'Punjab', '2025-07-10', NULL, 5, 'Active', 50000000.00),
('Balochistan Earthquake 2025', 'Balochistan', '2025-08-02', '2025-09-15', 4, 'Closed', 30000000.00),
('KPK Wildfire Season', 'Khyber Pakhtunkhwa', '2025-06-01', NULL, 3, 'Active', 15000000.00),
('Karachi Urban Flash Flood', 'Sindh', '2025-08-20', '2025-09-01', 4, 'Closed', 25000000.00),
('Sindh Riverine Flood', 'Sindh', '2025-09-10', NULL, 5, 'Active', 40000000.00);
 
-- ── 6. Disaster Event Types (junction) ───────────────────────
INSERT INTO DisasterEventType (TypeID, EventID) VALUES
(2, 1), -- Flood → Lahore Mega Flood
(1, 2), -- Earthquake → Balochistan
(3, 3), -- Wildfire → KPK
(2, 4), -- Flood → Karachi Flash Flood
(2, 5); -- Flood → Sindh Riverine
 
-- ── 7. Emergency Reports ─────────────────────────────────────
INSERT INTO Emergency_Report (Latitude, Longitude, Address, SeverityLevel, Status, Description, Citizeninfo, Eventid) VALUES
(31.5204, 74.3587, 'Model Town, Lahore', 5, 'Pending', 'Entire colony flooded, families trapped on rooftops', 'Ahmed Nawaz, 0311-1234567', 1),
(31.4697, 74.4200, 'Johar Town, Lahore', 4, 'Assigned', 'Roads submerged, hospital inaccessible', 'Fatima Bibi, 0312-2345678', 1),
(30.1798, 66.9750, 'Quetta City Center', 4, 'Assigned', 'Building collapsed, 20 people trapped', 'Rashid Khan, 0313-3456789', 2),
(30.2200, 67.0100, 'Pishin District', 3, 'Resolved', 'Minor tremors, structural damage reported', 'Gulnaz Akhtar, 0314-4567890', 2),
(34.0151, 71.5249, 'Peshawar Hills', 3, 'Pending', 'Forest fire spreading toward village', 'Hamid Shah, 0315-5678901', 3),
(24.8607, 67.0011, 'Gulshan-e-Iqbal, Karachi', 4, 'Resolved', 'Storm drains overflowed, ground floor flooded', 'Amina Siddiqui, 0316-6789012', 4),
(27.5000, 68.0000, 'Sukkur, Sindh', 5, 'Pending', 'River breached, multiple villages submerged', 'Zubair Lashari, 0317-7890123', 5),
(27.7000, 68.8500, 'Larkana, Sindh', 5, 'Assigned', 'Mass evacuation required, livestock lost', 'Parveen Chandio, 0318-8901234', 5);
 
-- ── 8. Rescue Teams ───────────────────────────────────────────
INSERT INTO Rescue_Team (Teamname, TeamType, CurrentLat, CurrentLong, AvailabilityStatus, Capacity) VALUES
('Alpha Medical Unit', 'Medical', 31.5100, 74.3500, 'Assigned', 20),
('Beta Fire Squad', 'Fire', 30.1900, 66.9800, 'Busy', 15),
('Gamma Rescue Brigade', 'Rescue', 34.0200, 71.5300, 'Available', 30),
('Delta Medical Team', 'Medical', 24.8700, 67.0100, 'Available', 25),
('Echo Rescue Unit', 'Rescue', 27.5100, 68.0100, 'Busy', 35),
('Zeta Fire Response', 'Fire', 31.4600, 74.4100, 'Available', 20),
('Eta Medical Response', 'Medical', 27.7100, 68.8600, 'Assigned', 18),
('Theta Rescue Force', 'Rescue', 30.2100, 67.0200, 'Completed', 25);
 
-- ── 9. Team Assignments ──────────────────────────────────────
INSERT INTO Team_Assignment (Teamid, Reportid, Authorizedby, Assignedat, Completedat, Status) VALUES
(1, 2, 5, '2025-07-11 08:00', NULL, 'Active'),
(2, 3, 5, '2025-08-03 09:00', NULL, 'Active'),
(8, 4, 6, '2025-08-04 10:00', '2025-08-05 16:00','Completed'),
(5, 8, 6, '2025-09-11 07:00', NULL, 'Active'),
(7, 8, 5, '2025-09-11 07:30', NULL, 'Active');
 
-- ── 10. Resources ────────────────────────────────────────────
INSERT INTO Resources (ResourceName, ResourceType, Unit, ThresholdLevel) VALUES
('Rice Bags', 'Food', 'kg', 500),
('Mineral Water', 'Water', 'liters',1000),
('First Aid Kits', 'Medicine', 'Units', 100),
('Tents', 'Shelter', 'Units', 50),
('Oral Rehydration', 'Medicine', 'Units', 200),
('Blankets', 'Shelter', 'Units', 100),
('Cooking Oil', 'Food', 'kg', 300),
('Purification Tab', 'Water', 'Units', 200);
 
-- ── 11. Inventory ────────────────────────────────────────────
INSERT INTO Inventory (Warehouseid, Resourceid, QuantityAvailable, QuantityDispatched, QuantityConsumed) VALUES
-- Lahore WH-1
(1, 1, 2000, 500, 300),
(1, 2, 5000, 1000, 800),
(1, 3, 300, 50, 30),
(1, 4, 150, 40, 20),
-- Karachi WH-1
(2, 1, 3000, 200, 150),
(2, 2, 8000, 500, 400),
(2, 5, 500, 100, 80),
(2, 6, 400, 80, 60),
-- Islamabad WH
(3, 3, 200, 20, 15),
(3, 4, 80, 10, 5),
(3, 7, 600, 100, 90),
(3, 8, 300, 50, 40),
-- Peshawar WH
(4, 1, 1500, 300, 200),
(4, 6, 200, 30, 25),
-- Quetta WH
(5, 1, 1000, 400, 350),
(5, 3, 90, 20, 18), -- Below threshold!
(5, 2, 2000, 800, 700);
 
-- ── 12. Resource Allocations ─────────────────────────────────
INSERT INTO Resource_Allocation (Resourceid, Warehouseid, Eventid, Requestedby, Requestedqty, Approvedqty, Status, Purpose) VALUES
(1, 1, 1, 5, 1000, 800, 'Approved', 'Food distribution for flood victims in Model Town'),
(2, 1, 1, 5, 2000, 1500, 'Dispatched', 'Drinking water for Lahore flood camps'),
(3, 5, 2, 6, 100, 80, 'Approved', 'First aid for earthquake casualties in Quetta'),
(4, 3, 3, 5, 50, NULL, 'Pending', 'Emergency shelter for wildfire evacuees in KPK'),
(6, 2, 5, 6, 200, NULL, 'Pending', 'Blankets for flood victims in Sindh'),
(5, 2, 4, 5, 100, 100, 'Dispatched', 'ORS for Karachi flash flood victims');
 
-- ── 13. Hospitals ────────────────────────────────────────────
INSERT INTO Hospital (Name, StreetNo, City, Totalbeds, Availablebeds, Contactnumber) VALUES
('Services Hospital', 'Mall Road', 'Lahore', 500, 120, '042-9201234'),
('Jinnah Hospital', 'Fatima Jinnah','Lahore', 400, 80, '042-9201235'),
('Civil Hospital Karachi', 'Karachi Old', 'Karachi', 800, 200, '021-9215740'),
('PMDC Quetta', 'Brewery Road','Quetta', 300, 90, '081-9201236'),
('Lady Reading Hospital', 'GT Road', 'Peshawar', 600, 150, '091-9219401'),
('Ghulam Muhammad Hospital', 'Station Road','Sukkur', 200, 40, '071-9310205');
 
-- ── 14. Patients ─────────────────────────────────────────────
INSERT INTO Patient (Name, Age, Gender, Condition, Admissiontime, Dischargetime, Hospitalid, Reportid) VALUES
('Tariq Mahmood', 45, 'Male', 'Critical', '2025-07-11 10:00', NULL, 1, 1),
('Rukhsana Bibi', 32, 'Female', 'Stable', '2025-07-12 08:00', NULL, 1, 2),
('Ghulam Nabi', 60, 'Male', 'Critical', '2025-08-03 14:00', NULL, 4, 3),
('Asma Begum', 28, 'Female', 'Stable', '2025-08-04 11:00', '2025-08-07 09:00',4, 4),
('Saif Ullah', 19, 'Male', 'Stable', '2025-08-21 09:00', '2025-08-25 10:00',3, 6),
('Bibi Hajra', 55, 'Female', 'Critical', '2025-09-11 06:00', NULL, 6, 7),
('Murad Ali', 38, 'Male', 'Stable', '2025-09-11 08:00', NULL, 6, 8);
 
-- ── 15. Donors ───────────────────────────────────────────────
INSERT INTO Donor (Donorname, Donortype, TotalDonated, Contactinfo) VALUES
('Arif Habib Foundation', 'Organization', 5000000.00, 'arif.habib@ahf.pk'),
('Al-Khidmat Foundation', 'Organization', 3000000.00, 'donations@alkhidmat.pk'),
('Muhammad Wasim', 'Individual', 50000.00, '0321-1234567'),
('Engro Corporation', 'Organization', 8000000.00, 'csr@engro.com'),
('Ayesha Siddiqui', 'Individual', 25000.00, '0322-2345678'),
('Pakistan Red Crescent', 'Organization', 10000000.00,'info@prcs.pk');
 
-- ── 16. Financial Transactions ───────────────────────────────
INSERT INTO Financial_Transaction (Transactiontype, Amount, Description, Referencenumber, Eventid, Userid, Donorid) VALUES
('Donation', 5000000.00, 'Arif Habib Foundation donation for Lahore flood', 'TXN-001', 1, 9, 1),
('Donation', 3000000.00, 'Al-Khidmat donation for Balochistan earthquake', 'TXN-002', 2, 9, 2),
('Expense', 800000.00, 'Food procurement – 800 rice bags for Lahore camps', 'TXN-003', 1, 9, NULL),
('Procurement', 1200000.00, 'Water tankers hired for Sindh flood relief', 'TXN-004', 5, 10, NULL),
('Donation', 50000.00, 'Individual donation by Muhammad Wasim', 'TXN-005', 1, 10, 3),
('Expense', 500000.00, 'Medical supplies for earthquake victims', 'TXN-006', 2, 9, NULL),
('Donation', 8000000.00, 'Engro Corporation – Sindh flood relief fund', 'TXN-007', 5, 10, 4),
('Expense', 300000.00, 'Transport and logistics for rescue teams', 'TXN-008', 3, 9, NULL),
('Donation', 10000000.00,'Pakistan Red Crescent emergency allocation', 'TXN-009', 5, 10, 6),
('Procurement', 450000.00, 'Tent procurement for KPK wildfire evacuees', 'TXN-010', 3, 9, NULL);
 
-- ── 17. Approval Requests ────────────────────────────────────
INSERT INTO Approval_Request (Requesttype, Status, Decisionat, Comments, Referenceid, Requestedby, Approvedby) VALUES
('ResourceAllocation', 'Approved', '2025-07-11 12:00', 'Approved – urgent flood need', 1, 5, 1),
('ResourceAllocation', 'Approved', '2025-07-12 09:00', 'Approved – critical water shortage', 2, 5, 1),
('ResourceAllocation', 'Approved', '2025-08-04 10:00', 'Approved – earthquake medical need', 3, 6, 2),
('ResourceAllocation', 'Pending', NULL, NULL, 4, 5, NULL),
('ResourceAllocation', 'Pending', NULL, NULL, 5, 6, NULL),
('RescueDeployment', 'Approved', '2025-07-11 07:30', 'Alpha Medical Unit deployed to Lahore', 1, 5, 1),
('RescueDeployment', 'Approved', '2025-08-03 08:45', 'Beta Fire deployed to Quetta', 2, 6, 2),
('Financial', 'Approved', '2025-07-13 11:00', 'Food procurement expense approved', 3, 9, 1),
('Financial', 'Pending', NULL, NULL, 4, 10, NULL);
 
-- ── 18. Audit Logs ───────────────────────────────────────────
INSERT INTO Audit_Log (TableAffected, Action, Oldvalue, Newvalue, Performedby) VALUES
('Emergency_Report', 'UPDATE', 'Status=Pending', 'Status=Assigned', 3),
('Rescue_Team', 'UPDATE', 'AvailabilityStatus=Available', 'AvailabilityStatus=Assigned', 5),
('Inventory', 'UPDATE', 'QuantityAvailable=2000', 'QuantityAvailable=1200', 7),
('Resource_Allocation','INSERT', NULL, 'AllocationID=1 Created', 5),
('Approval_Request', 'UPDATE', 'Status=Pending', 'Status=Approved', 1),
('Financial_Transaction','INSERT', NULL, 'TransactionID=1 Created', 9),
('Patient', 'INSERT', NULL, 'PatientID=1 Admitted', 3),
('Hospital', 'UPDATE', 'AvailableBeds=121', 'AvailableBeds=120', 3);
 
-- ── 19. Alerts ───────────────────────────────────────────────
INSERT INTO Alert (Alerttype, Message, Isread, Recipientuserid) VALUES
('LowStock', 'Quetta WH: First Aid Kits below threshold (90 < 100)', 0, 7),
('LowStock', 'Quetta WH: First Aid Kits critically low – reorder now', 0, 1),
('Escalation', 'Report #7 – Sukkur Sindh: Severity 5, no team assigned', 0, 3),
('Assignment', 'Team Alpha Medical Unit assigned to Report #2', 1, 5),
('Assignment', 'Team Beta Fire Squad assigned to Report #3', 1, 6),
('Escalation', 'Patient Tariq Mahmood in Critical condition at Services Hospital', 0, 3),
('LowStock', 'Islamabad WH: Tents near threshold (80 units remaining)', 0, 8);

-- Q1 (All Emergency Reports)
SELECT * 
FROM Emergency_Report;

--Q2 (High Severity Reports)
SELECT ReportID, Address, SeverityLevel, Status
FROM Emergency_Report
WHERE SeverityLevel >= 4
ORDER BY SeverityLevel DESC;

--Q3 (Available Rescue Teams)
SELECT TeamID, Teamname, TeamType
FROM Rescue_Team
WHERE AvailabilityStatus = 'Available';

--Q4 (Total Stock by Warehouse)
SELECT w.WarehouseName, SUM(i.QuantityAvailable) AS TotalStock
FROM Warehouse w
JOIN Inventory i ON w.WarehouseID = i.WarehouseID
GROUP BY w.WarehouseName;

--Q5 (Low Stock Resources)
SELECT r.ResourceName, i.QuantityAvailable, r.ThresholdLevel
FROM Resources r
JOIN Inventory i ON r.ResourceID = i.ResourceID
WHERE i.QuantityAvailable < r.ThresholdLevel;

--Q6 (Active Rescue Teams)
SELECT Teamname
FROM Rescue_Team
WHERE TeamID IN (
    SELECT TeamID 
    FROM Team_Assignment 
    WHERE Status = 'Active'
);

--Q7 (Critical Patients)
SELECT Name, Age, HospitalID
FROM Patient
WHERE Condition = 'Critical';

--Q8 (Total Donations by Donor)
SELECT d.DonorName, SUM(ft.Amount) AS TotalDonated
FROM Donor d
JOIN Financial_Transaction ft ON d.DonorID = ft.DonorID
WHERE ft.TransactionType = 'Donation'
GROUP BY d.DonorName;

--Q9 (Recent Audit Logs)
SELECT TableAffected, Action, Timestamp
FROM Audit_Log
ORDER BY Timestamp DESC;

--Q10 (Unread Alerts)
SELECT Message, AlertType
FROM Alert
WHERE IsRead = 0;

--Q11 (Active Disaster Events with Expenses)
SELECT  de.EventID,de.EventName, de.AffectedRegion, de.BudgetAllocated, SUM(ft.Amount) AS TotalSpent
FROM Disaster_Event de
LEFT JOIN Financial_Transaction ft ON de.EventID = ft.Eventid
WHERE de.Status = 'Active'
GROUP BY de.EventID, de.EventName, de.AffectedRegion, de.BudgetAllocated;

--Q12 (Resource Allocation Status)
SELECT ra.Allocationid, r.ResourceName, w.WarehouseName, ra.Requestedqty, ra.Status
FROM Resource_Allocation ra
JOIN Resources r ON ra.Resourceid = r.ResourceID
JOIN Warehouse w ON ra.Warehouseid = w.WarehouseID
ORDER BY ra.Status;

--Q13 (Rescue Team Assignments)
SELECT rt.TeamID,rt.Teamname,COUNT(ta.AssignmentID) AS TotalAssignments
FROM Rescue_Team rt
LEFT JOIN Team_Assignment ta  ON rt.TeamID = ta.Teamid
GROUP BY rt.TeamID, rt.Teamname;

--Q14 (Financial Transactions for Active Events)
SELECT de.EventID,de.EventName,de.AffectedRegion,de.BudgetAllocated,SUM(ft.Amount) AS TotalTransactions,COUNT(ft.Transactionid) AS TotalTransactionsCount
FROM Disaster_Event de
LEFT JOIN Financial_Transaction ft ON de.EventID = ft.Eventid
WHERE de.Status = 'Active'
GROUP BY de.EventID, de.EventName, de.AffectedRegion, de.BudgetAllocated
HAVING SUM(ft.Amount) IS NOT NULL
ORDER BY TotalTransactions DESC;

--Q15 (Rescue team performance and wrokload)
SELECT rt.TeamID,rt.Teamname,rt.TeamType, 
COUNT(ta.AssignmentID) AS TotalAssignments,
    (
        SELECT COUNT(*) 
        FROM Team_Assignment ta2 
        WHERE ta2.Teamid = rt.TeamID 
        AND ta2.Status = 'Completed'
    ) AS CompletedMissions
FROM Rescue_Team rt
LEFT JOIN Team_Assignment ta ON rt.TeamID = ta.Teamid
GROUP BY rt.TeamID, rt.Teamname, rt.TeamType
ORDER BY TotalAssignments DESC;
