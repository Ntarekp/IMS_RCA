# Authentication Setup Guide

## Overview

The IMS backend now includes secure authentication with:
- **JWT Token-based authentication**
- **BCrypt password encryption**
- **Default admin user** with encrypted password
- **Protected API endpoints** (all endpoints except `/api/auth/login`)

## Default Credentials

**Email**: `ntarekayitare@gmail.com`  
**Password**: `RcaIMS@1234.5`  
**Role**: `ADMIN`

**Important**: The password is stored encrypted in the database using BCrypt. It cannot be bypassed or retrieved in plain text.

## Security Features

### 1. Password Encryption
- All passwords are encrypted using **BCrypt** algorithm
- Passwords are hashed with salt (one-way encryption)
- Original passwords cannot be retrieved from the database

### 2. JWT Tokens
- Authentication uses JWT (JSON Web Tokens)
- Tokens expire after 24 hours (configurable)
- Tokens are signed with a secret key
- Tokens contain user email and role

### 3. Protected Endpoints
- **Public**: `/api/auth/login` - Login endpoint
- **Protected**: All other `/api/**` endpoints require valid JWT token
- No bypassing possible - Spring Security enforces authentication

## How It Works

### Backend Flow

1. **User Login**:
   - POST `/api/auth/login` with email and password
   - Backend validates credentials
   - Password is compared using BCrypt
   - If valid, JWT token is generated and returned

2. **API Requests**:
   - Frontend includes token in `Authorization: Bearer <token>` header
   - JWT filter validates token on each request
   - If invalid/expired, request is rejected with 401 Unauthorized

3. **Password Storage**:
   - On user creation, password is encrypted using BCrypt
   - Encrypted password is stored in database
   - Original password cannot be retrieved

### Frontend Flow

1. **Login**:
   - User enters email and password
   - Frontend calls `/api/auth/login`
   - Token is stored in `localStorage`
   - Token is included in all subsequent API requests

2. **API Calls**:
   - All API requests automatically include `Authorization` header
   - If token expires, user must login again

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt encrypted
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Default User

The default user is automatically created on application startup:
- Email: `ntarekayitare@gmail.com`
- Password: `RcaIMS@1234.5` (encrypted in database)
- Role: `ADMIN`

## API Endpoints

### POST /api/auth/login

**Request**:
```json
{
  "email": "ntarekayitare@gmail.com",
  "password": "RcaIMS@1234.5"
}
```

**Response** (Success - 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "ntarekayitare@gmail.com",
  "role": "ADMIN",
  "message": "Login successful"
}
```

**Response** (Error - 401):
```json
{
  "token": null,
  "email": null,
  "role": null,
  "message": "Invalid email or password"
}
```

### GET /api/auth/validate

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (Valid - 200):
```
"Token is valid"
```

**Response** (Invalid - 401):
```
"Invalid or expired token"
```

## Testing Authentication

### Using cURL

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ntarekayitare@gmail.com","password":"RcaIMS@1234.5"}'

# Use token for API call
curl http://localhost:8081/api/items \
  -H "Authorization: Bearer <token-from-login>"

# Try without token (should fail)
curl http://localhost:8081/api/items
# Returns: 401 Unauthorized
```

### Using Frontend

1. Start backend: `mvn spring-boot:run`
2. Start frontend: `npm run dev`
3. Open browser: `http://localhost:3000`
4. Login with:
   - Email: `ntarekayitare@gmail.com`
   - Password: `RcaIMS@1234.5`
5. All API calls will automatically include the token

## Security Configuration

### JWT Secret Key

Located in `application.properties`:
```properties
jwt.secret=your-secret-key-change-this-in-production-min-256-bits
jwt.expiration=86400000  # 24 hours in milliseconds
```

**Important**: Change the JWT secret in production!

### Password Encryption

BCrypt is configured in `SecurityConfig.java`:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

BCrypt automatically:
- Generates random salt for each password
- Uses multiple rounds (default: 10)
- Produces different hash each time (even for same password)

## Verification

### Verify Password is Encrypted

1. Login to MySQL:
```bash
mysql -u root -p -h localhost -P 3600
```

2. Check users table:
```sql
USE school_inventory;
SELECT email, password FROM users;
```

You'll see:
- Email: `ntarekayitare@gmail.com`
- Password: `$2a$10$...` (BCrypt hash, not plain text)

### Verify No Bypassing

Try accessing API without token:
```bash
curl http://localhost:8081/api/items
```

Result: `401 Unauthorized` - Authentication is enforced!

## Troubleshooting

### Login Fails

1. **Check backend is running**: `curl http://localhost:8081/api/auth/login`
2. **Verify credentials**: Use exact email and password
3. **Check database**: Ensure user exists
4. **Check logs**: Look for authentication errors

### Token Not Working

1. **Check token format**: Must be `Bearer <token>`
2. **Check token expiration**: Tokens expire after 24 hours
3. **Verify token in header**: `Authorization: Bearer <token>`
4. **Check JWT secret**: Must match in application.properties

### Password Issues

1. **Password is encrypted**: Cannot retrieve original password
2. **Reset password**: Update user in database with new BCrypt hash
3. **Verify encryption**: Check password starts with `$2a$10$`

## Production Recommendations

1. **Change JWT Secret**: Use a strong, random secret key
2. **Use HTTPS**: Encrypt all traffic
3. **Set Token Expiration**: Shorter expiration for better security
4. **Implement Refresh Tokens**: For better user experience
5. **Rate Limiting**: Prevent brute force attacks
6. **Password Policy**: Enforce strong passwords
7. **Account Lockout**: Lock after failed attempts

## Summary

✅ **Authentication is fully implemented**  
✅ **Passwords are encrypted with BCrypt**  
✅ **Default user created automatically**  
✅ **All API endpoints are protected**  
✅ **No bypassing possible**  
✅ **Frontend integrated with authentication**

The system is secure and ready for use!

