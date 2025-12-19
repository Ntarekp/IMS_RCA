# Login Troubleshooting Guide

## Issue: "Invalid email or password"

If you're getting this error when trying to login, follow these steps:

### Step 1: Verify Backend is Running

1. Check if backend is running on port 8081:
   ```bash
   curl http://localhost:8081/api/auth/login
   ```
   Should return a response (even if it's an error about missing body).

2. Check backend logs for:
   - "Default user created successfully!" message
   - Any authentication errors

### Step 2: Verify User Exists in Database

**Option A: Using MySQL Command Line**
```bash
mysql -u root -p -h localhost -P 3600
```

Then run:
```sql
USE school_inventory;
SELECT id, email, role, enabled FROM users;
```

You should see:
- Email: `ntarekayitare@gmail.com`
- Role: `ADMIN`
- Enabled: `1` (true)

**Option B: Check Backend Logs**
Look for:
- "Default user created successfully!" on startup
- Or "Default user already exists: ntarekayitare@gmail.com"

### Step 3: Verify Credentials

**Correct Credentials:**
- Email: `ntarekayitare@gmail.com` (no spaces, lowercase)
- Password: `RcaIMS@1234.5` (exact, including the dot at the end)

**Common Mistakes:**
- ❌ Extra spaces: ` ntarekayitare@gmail.com ` (with spaces)
- ❌ Wrong case: `Ntarekayitare@gmail.com` (should be lowercase)
- ❌ Missing dot: `RcaIMS@1234.5` → `RcaIMS@12345`
- ❌ Extra comma: `RcaIMS@1234.5,` (no comma)

### Step 4: Reset User Password (If Needed)

If the user exists but password doesn't work, you can reset it:

**Option A: Delete and Recreate (Easiest)**
```sql
DELETE FROM users WHERE email = 'ntarekayitare@gmail.com';
```
Then restart the backend - it will recreate the user automatically.

**Option B: Update Password Hash**
```sql
-- This requires generating a BCrypt hash, which is complex
-- Better to delete and recreate (Option A)
```

### Step 5: Check Backend Logs for Details

The updated code now logs:
- "Authentication failed: User not found" - User doesn't exist
- "Authentication failed: Invalid password" - Password mismatch
- "Authentication failed: User account is disabled" - Account disabled
- "Authentication successful" - Login worked

### Step 6: Test with cURL

Test the login API directly:

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"ntarekayitare@gmail.com\",\"password\":\"RcaIMS@1234.5\"}"
```

**Expected Success Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "ntarekayitare@gmail.com",
  "role": "ADMIN",
  "message": "Login successful"
}
```

**Expected Error Response:**
```json
{
  "token": null,
  "email": null,
  "role": null,
  "message": "Invalid email or password"
}
```

### Step 7: Frontend Issues

If backend works but frontend doesn't:

1. **Check Browser Console** (F12):
   - Look for network errors
   - Check if API call is being made
   - Verify response from backend

2. **Check API URL**:
   - Verify `api/config.ts` has: `BASE_URL: 'http://localhost:8081'`
   - Ensure backend is running on port 8081

3. **Clear Browser Cache**:
   - Clear localStorage: `localStorage.clear()`
   - Refresh page
   - Try login again

## Quick Fix: Recreate Default User

If nothing works, delete and recreate:

1. **Stop backend**

2. **Delete user from database:**
   ```sql
   DELETE FROM users WHERE email = 'ntarekayitare@gmail.com';
   ```

3. **Restart backend** - User will be recreated automatically

4. **Try login again**

## Still Not Working?

1. **Check Database Connection:**
   - Verify MySQL is running
   - Check `application.properties` has correct database credentials
   - Ensure database `school_inventory` exists

2. **Check Spring Security:**
   - Verify `SecurityConfig.java` allows `/api/auth/**` endpoints
   - Check CORS configuration

3. **Check Logs:**
   - Look for any exceptions during startup
   - Check for authentication errors in logs

4. **Verify Password Encoding:**
   - Check if `PasswordEncoder` bean is created
   - Verify BCrypt is being used

## Contact Support

If none of these steps work, provide:
- Backend logs (especially startup logs)
- Database user table contents (without password)
- Error message from frontend
- Browser console errors

