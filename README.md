# VolunteerHub

A comprehensive full-stack application for managing volunteers, coordinating volunteer activities, and connecting volunteers with organizations. Built with React on the frontend and Spring Boot on the backend.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Project Architecture](#project-architecture)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

VolunteerHub is a platform designed to streamline volunteer management and coordination. It provides a seamless experience for both volunteers seeking opportunities and organizations looking to recruit volunteer support. The application features user authentication, role-based access (Volunteer or Organizer), and profile management to facilitate connections within the volunteer community.

**Key Users:**
- **Volunteers:** Individuals looking to contribute their time and skills
- **Organizations:** Groups and institutions seeking volunteer support

---

## Features

- ✅ **User Registration** - Create accounts with role selection (Volunteer or Organizer)
- ✅ **User Authentication** - Secure login with email and password
- ✅ **Profile Management** - View and manage user profile information
- ✅ **Role-Based Access** - Different user types for volunteers and organizations
- ✅ **Responsive UI** - React-based frontend with intuitive design
- ✅ **RESTful API** - Spring Boot backend with clean API architecture
- ✅ **Cross-Origin Support** - CORS enabled for seamless frontend-backend communication

---

## Technology Stack

### Frontend
- **Framework:** React 19.2.4
- **DOM Rendering:** React DOM 19.2.4
- **Build Tool:** React Scripts 5.0.1
- **Testing:** Jest, React Testing Library
- **Styling:** CSS

### Backend
- **Framework:** Spring Boot
- **ORM:** Spring Data JPA with Jakarta Persistence (JPA)
- **Database:** MySQL
- **REST APIs:** Spring Boot REST with JSON
- **Cross-Origin:** CORS enabled

### Communication
- **Protocol:** HTTP/REST
- **Format:** JSON
- **Frontend Server Port:** 3000
- **Backend Server Port:** 8080
- **Database Port:** 3306

---

## Project Structure

```
VolunteerHub/
├── backend/                               # Spring Boot Backend
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/volunteerhub/
│           │       ├── VolunteerHubApplication.java    # Main Spring Boot App
│           │       ├── controller/
│           │       │   └── AuthController.java          # REST endpoints for auth
│           │       ├── model/
│           │       │   └── User.java                    # User entity/model
│           │       ├── repository/
│           │       │   └── UserRepository.java          # Database access layer
│           │       └── service/
│           │           └── UserService.java             # Business logic layer
│           └── resources/
│               └── application.properties               # Configuration
│
├── frontend/                              # React Frontend
│   ├── public/
│   │   ├── index.html                     # Main HTML file
│   │   ├── manifest.json                  # PWA manifest
│   │   └── robots.txt                     # SEO robots file
│   ├── src/
│   │   ├── App.js                         # Main app component
│   │   ├── App.css                        # App styling
│   │   ├── Login.js                       # Login component
│   │   ├── Register.js                    # Registration component
│   │   ├── Profile.js                     # User profile component
│   │   ├── index.js                       # React entry point
│   │   ├── index.css                      # Global styles
│   │   └── setupTests.js                  # Test configuration
│   ├── package.json                       # Dependencies
│   └── README.md                          # Frontend-specific docs
│
└── README.md                              # This file
```

---

## Prerequisites

Before running VolunteerHub, ensure you have the following installed:

### Backend Requirements
- **Java Development Kit (JDK):** Version 11 or higher
- **Maven:** For building and managing Java dependencies
- **MySQL Server:** Version 5.7 or higher
- **MySQL Client:** For database management

### Frontend Requirements
- **Node.js:** Version 14.0 or higher
- **npm:** Version 6.0 or higher (comes with Node.js)

### Verification
```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Check Node.js and npm versions
node --version
npm --version
```

---

## Installation

### 1. Clone the Repository

```bash
cd c:\Users\pooji\VolunteerHub
```

### 2. Backend Setup (Spring Boot + MySQL)

#### Step 1: Create MySQL Database
```sql
-- Open MySQL client and run:
CREATE DATABASE volunteerhub;
USE volunteerhub;
```

#### Step 2: Configure Database Connection
Edit `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/volunteerhub
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.port=8080
```

#### Step 3: Build Backend
```bash
cd backend
mvn clean install
```

### 3. Frontend Setup (React)

#### Step 1: Install Dependencies
```bash
cd frontend
npm install
```

#### Step 2: Configure API Endpoint (if needed)
The frontend is configured to communicate with the backend at `http://localhost:8080`.
Update this in component files if your backend runs on a different port.

---

## Running the Application

### Option 1: Run Both Services Simultaneously

**Terminal 1 - Start Backend:**
```bash
cd backend
mvn spring-boot:run
# Backend starts on http://localhost:8080
```

**Terminal 2 - Start Frontend:**
```bash
cd frontend
npm start
# Frontend starts on http://localhost:3000
```

Open your browser and navigate to `http://localhost:3000`

### Option 2: Run Backend as JAR

```bash
cd backend
mvn clean package
java -jar target/volunteerhub-0.0.1-SNAPSHOT.jar
```

Then start the frontend in a separate terminal:
```bash
cd frontend
npm start
```

---

## API Endpoints

All endpoints are hosted at `http://localhost:8080`

### Authentication Endpoints

#### Register a New User
- **URL:** `/register`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "role": "Volunteer"
  }
  ```
- **Response (Success):**
  ```json
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "role": "Volunteer"
  }
  ```

#### Login User
- **URL:** `/login`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "email": "john@example.com",
    "password": "password123"
  }
  ```
- **Response (Success):**
  ```json
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "Volunteer"
  }
  ```
- **Response (Failure):**
  ```json
  {
    "error": "Invalid email or password"
  }
  ```

---

## Project Architecture

### Frontend Architecture

```
App.js (Main Container - State Management)
├── Register.js (User Registration)
│   └── Form: name, email, password, role
│   └── Action: POST /register
├── Login.js (User Authentication)
│   └── Form: email, password
│   └── Action: POST /login
└── Profile.js (User Profile View)
    └── Display: name, email, role
```

**State Flow:**
1. User lands on app → sees Register/Login options
2. Registers → POST to backend → success → can login
3. Logins → POST to backend → validates credentials → success → stores user state
4. App.js checks user state → renders Profile component
5. Profile displays authenticated user's information

### Backend Architecture (Layered MVC)

```
AuthController
└── Handles HTTP requests
    └── @PostMapping("/register")
    └── @PostMapping("/login")
        ↓
UserService
└── Business Logic Layer
    └── register(User) → validates & saves
    └── login(email, password) → authenticates user
        ↓
UserRepository
└── Data Access Layer
    └── findByEmail(email) → queries database
    └── save(User) → persists data
        ↓
User Entity
└── Model Layer
    └── Mapped to MySQL 'users' table
        ↓
MySQL Database
```

---

## Future Enhancements

### Phase 2: Core Features
- [ ] Volunteer opportunity listings
- [ ] Event creation and management
- [ ] Volunteer sign-up for events
- [ ] Email notifications
- [ ] User profile picture/avatar
- [ ] Organization profiles

### Phase 3: Advanced Features
- [ ] Real-time chat between volunteers and organizations
- [ ] Volunteer hours tracking
- [ ] Ratings and reviews system
- [ ] Search and filtering capabilities
- [ ] Calendar view for events
- [ ] Mobile app (React Native or Flutter)

### Phase 4: Security & Performance
- [ ] Password hashing (bcrypt/Spring Security)
- [ ] JWT token authentication
- [ ] Role-based access control (RBAC)
- [ ] Input validation and sanitization
- [ ] API rate limiting
- [ ] Caching layer (Redis)
- [ ] Database indexing optimization

### Phase 5: DevOps
- [ ] Docker containerization
- [ ] CI/CD pipeline (GitHub Actions/Jenkins)
- [ ] Cloud deployment (AWS/Azure/GCP)
- [ ] Automated testing suite
- [ ] API documentation (Swagger/OpenAPI)

---

## Contributing

We welcome contributions to VolunteerHub! To contribute:

1. **Fork the repository**
   ```bash
   git clone <your-fork-url>
   cd VolunteerHub
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**
   - Follow existing code style and conventions
   - Test your changes thoroughly
   - Write clear commit messages

4. **Commit and push**
   ```bash
   git add .
   git commit -m "Add meaningful commit message"
   git push origin feature/your-feature-name
   ```

5. **Submit a pull request**
   - Provide a clear description of changes
   - Reference any related issues

### Code Style Guidelines
- **Java Backend:** Follow Spring Boot conventions, use camelCase for methods/variables
- **React Frontend:** Use functional components, meaningful variable names, proper JSX formatting
- **Comments:** Add comments for complex logic
- **Testing:** Include unit tests for new features

---

## Troubleshooting

### Issue: Backend won't start
- **Solution:** Verify MySQL is running and credentials in `application.properties` are correct
- **Solution:** Check Java version is 11 or higher

### Issue: Frontend can't connect to backend
- **Solution:** Ensure backend is running on port 8080
- **Solution:** Check CORS settings in backend
- **Solution:** Verify frontend API URL matches backend address

### Issue: Database connection error
- **Solution:** Create the `volunteerhub` database
- **Solution:** Update MySQL username and password in configuration

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Contact & Support

For questions or support, please reach out to the development team or create an issue in the repository.

---

**Happy Volunteering! 🤝**

*VolunteerHub - Connecting Hearts, Building Communities*
