# Complete Setup Guide - IMS Project

This guide will help you set up and run the complete Inventory Management System with both backend and frontend integrated.

## 📋 Prerequisites

Before starting, ensure you have:

- **Java 21** or higher
- **Maven 3.6+** (or use Maven Wrapper)
- **MySQL 8.0+** (running on port 3600)
- **Node.js 18+** (recommended: 20+)
- **npm** or **yarn**

## 🗄️ Step 1: Database Setup

### 1.1 Install MySQL

If MySQL is not installed:
- **Windows**: Download from [MySQL Downloads](https://dev.mysql.com/downloads/mysql/)
- **Mac**: `brew install mysql`
- **Linux**: `sudo apt-get install mysql-server`

### 1.2 Start MySQL Server

```bash
# Windows (as Administrator)
net start MySQL80

# Mac/Linux
sudo systemctl start mysql
# or
sudo service mysql start
```

### 1.3 Verify MySQL is Running

```bash
mysql --version
```

### 1.4 Configure MySQL (if needed)

Ensure MySQL is configured to accept connections on port **3600**:
- Check MySQL configuration file (`my.cnf` or `my.ini`)
- Default port is 3306, but this project uses 3600

**Option 1**: Change MySQL port to 3600 in MySQL config
**Option 2**: Update backend `application.properties` to use port 3306

## 🔧 Step 2: Backend Setup

### 2.1 Navigate to Backend Directory

```bash
cd D:\Projects\ims
```

### 2.2 Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3600/school_inventory?createDatabaseIfNotExists=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

**Important**: Replace `your_mysql_password` with your actual MySQL root password.

### 2.3 Build Backend

```bash
# Using Maven Wrapper (Windows)
.\mvnw.cmd clean install

# Using Maven Wrapper (Mac/Linux)
./mvnw clean install

# Or using Maven directly
mvn clean install
```

### 2.4 Run Backend

```bash
# Using Maven Wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Using Maven Wrapper (Mac/Linux)
./mvnw spring-boot:run

# Or using Maven directly
mvn spring-boot:run
```

### 2.5 Verify Backend is Running

Open browser or use curl:
```bash
curl http://localhost:8081/api/items
```

You should see an empty array `[]` or a list of items if any exist.

**Backend is now running on**: `http://localhost:8081`

## 🎨 Step 3: Frontend Setup

### 3.1 Navigate to Frontend Directory

Open a **new terminal** (keep backend running):

```bash
cd D:\Projects\ims-frontend\Rca-stock-management
```

### 3.2 Install Dependencies

```bash
npm install
```

This will install all required packages (React, TypeScript, Vite, etc.)

### 3.3 Verify API Configuration

Check `api/config.ts` - it should have:
```typescript
BASE_URL: 'http://localhost:8081'
```

If your backend runs on a different port, update this.

### 3.4 Start Frontend Development Server

```bash
npm run dev
```

### 3.5 Verify Frontend is Running

Open browser: **http://localhost:3000**

You should see the login screen.

**Frontend is now running on**: `http://localhost:3000`

## ✅ Step 4: Verify Integration

### 4.1 Test Backend API

In a new terminal, test the backend:

```bash
# Get all items (should return empty array initially)
curl http://localhost:8081/api/items

# Create a test item
curl -X POST http://localhost:8081/api/items \
  -H "Content-Type: application/json" \
  -d '{"name":"Rice","unit":"Sacks","minimumStock":10,"description":"Test item"}'

# Get items again (should show the new item)
curl http://localhost:8081/api/items
```

### 4.2 Test Frontend

1. Open browser: `http://localhost:3000`
2. Click "Login" (mock authentication)
3. Navigate to "Stock Management"
4. Click "Add Item"
5. Fill in the form and submit
6. Verify the item appears in the list

### 4.3 Test Complete Flow

1. **Add an Item**:
   - Go to Stock Management
   - Click "Add Item"
   - Fill: Name="Rice", Unit="Sacks", Min Threshold=10
   - Submit

2. **Record Stock IN**:
   - The item should appear in the list
   - You can add initial quantity when creating, or record transactions separately

3. **View Dashboard**:
   - Go to Dashboard
   - See statistics and charts
   - View recent inventory

4. **View Transactions**:
   - Go to Transactions
   - See transaction history

5. **View Reports**:
   - Go to Reports
   - See balance report
   - Check low stock alerts

## 🐛 Troubleshooting

### Backend Won't Start

**Error**: "Port 8081 already in use"
- **Solution**: 
  - Change port in `application.properties`: `server.port=8082`
  - Update frontend `api/config.ts` to match
  - Or stop the process using port 8081

**Error**: "Cannot connect to database"
- **Solution**:
  - Verify MySQL is running: `mysql --version`
  - Check database credentials in `application.properties`
  - Ensure MySQL is on port 3600 (or update config)
  - Test connection: `mysql -u root -p -h localhost -P 3600`

**Error**: "Database doesn't exist"
- **Solution**: The `createDatabaseIfNotExists=true` should create it automatically
- Or create manually: `CREATE DATABASE school_inventory;`

### Frontend Won't Start

**Error**: "Port 3000 already in use"
- **Solution**: 
  - Change port in `vite.config.ts`
  - Or stop the process using port 3000

**Error**: "Cannot find module"
- **Solution**: 
  - Run `npm install` again
  - Delete `node_modules` and `package-lock.json`, then `npm install`

**Error**: "API connection failed"
- **Solution**:
  - Verify backend is running: `curl http://localhost:8081/api/items`
  - Check `api/config.ts` has correct BASE_URL
  - Check browser console for CORS errors
  - Verify CORS is enabled in backend

### CORS Errors

**Error**: "CORS policy blocked"
- **Solution**:
  - Backend CORS is already configured
  - Verify `CorsConfig.java` allows all origins (for development)
  - Check browser console for specific error
  - Ensure backend is running

### Data Not Loading

**Error**: Items list is empty or shows error
- **Solution**:
  - Check backend is running
  - Open browser DevTools → Network tab
  - Check API calls are successful
  - Verify database has data
  - Check browser console for errors

## 📝 Quick Start Commands

### Start Everything (Two Terminals)

**Terminal 1 - Backend:**
```bash
cd D:\Projects\ims
.\mvnw.cmd spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd D:\Projects\ims-frontend\Rca-stock-management
npm run dev
```

### Stop Everything

- **Backend**: Press `Ctrl+C` in backend terminal
- **Frontend**: Press `Ctrl+C` in frontend terminal
- **MySQL**: Keep running (or stop if needed)

## 🎯 Next Steps

1. **Add Sample Data**:
   - Use the frontend to add items
   - Record some transactions
   - Generate reports

2. **Explore Features**:
   - Dashboard analytics
   - Stock management
   - Transaction history
   - Reports and alerts

3. **Customize**:
   - Update styling
   - Add new features
   - Configure for your needs

## 📚 Additional Resources

- **Backend README**: `D:\Projects\ims\README.md`
- **Frontend README**: `D:\Projects\ims-frontend\Rca-stock-management\README.md`
- **Integration Guide**: `D:\Projects\ims-frontend\Rca-stock-management\INTEGRATION.md`

## ✅ Checklist

- [ ] MySQL installed and running
- [ ] Database configured in `application.properties`
- [ ] Backend builds successfully
- [ ] Backend runs on `http://localhost:8081`
- [ ] Frontend dependencies installed
- [ ] Frontend runs on `http://localhost:3000`
- [ ] Can create items from frontend
- [ ] Can view items in frontend
- [ ] Transactions work
- [ ] Reports display correctly

## 🎉 Success!

If all steps are completed, you should have:
- ✅ Backend API running on port 8081
- ✅ Frontend application running on port 3000
- ✅ Full integration between frontend and backend
- ✅ Database connected and working
- ✅ Complete inventory management system operational

Enjoy using your Inventory Management System!

