package com.vehicleShared.managers;

import com.vehicleShared.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class DbManager {
    private static final Logger log = LoggerFactory.getLogger(DbManager.class);
    private Connection db;

    public boolean initDb(String url, String user, String password) {
        try {
            DbManager.log.info("попытка подключения к " + url + " с пользователем " + user);
            db = DriverManager.getConnection(url, user, password);
            System.out.println("подключение успешно");
            return true;
        } catch (SQLException e) {
            DbManager.log.info("ошибка подключения к бд: " + e.getMessage());
            DbManager.log.info("SQL state: " + e.getSQLState());
            return false;
        }
    }

    public void closeDb() {
        if (db != null) {
            try {
                db.close();
            } catch (SQLException e) {
                System.err.println("ошибка закрытия базы: " + e.getMessage());
            }
        }
    }

    public synchronized List<Vehicle> loadFromDb() throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        System.out.println("начинаем загрузку из базы в " + System.currentTimeMillis());
        List<Vehicle> vehicles = new ArrayList<>();
        System.out.println("создаем statement");
        Statement stmt = db.createStatement();
        System.out.println("выполняем запрос select * from s466080.vehicles");
        ResultSet rs = stmt.executeQuery("select * from s466080.vehicles");
        System.out.println("запрос выполнен в " + System.currentTimeMillis());
        while (rs.next()) {
            System.out.println("обрабатываем строку id=" + rs.getLong("id"));
            Vehicle vehicle = new Vehicle(
                    rs.getLong("id"),
                    new Coordinates(rs.getFloat("coordinates_x"), rs.getInt("coordinates_y")),
                    rs.getString("name"),
                    rs.getFloat("engine_power"),
                    VehicleType.valueOf(rs.getString("vehicle_type")),
                    FuelType.valueOf(rs.getString("fuel_type"))
            );
            Timestamp ts = rs.getTimestamp("creation_date");
            if (ts != null) {
                vehicle.setCreationDate(ts.toLocalDateTime().atZone(ZonedDateTime.now().getZone()));
            }
            vehicles.add(vehicle);
        }
        System.out.println("загружено " + vehicles.size() + " записей в " + System.currentTimeMillis());
        rs.close();
        stmt.close();
        return vehicles;
    }

    public synchronized boolean addVehicle(Vehicle vehicle, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        if (vehicle.getName() == null || vehicle.getName().isEmpty() ||
                vehicle.getCoordinates() == null || vehicle.getPower() <= 0 ||
                vehicle.getType() == null || vehicle.getFuelType() == null ||
                userId == null) {
            throw new SQLException("некорректные данные машины или пользователь");
        }
        PreparedStatement stmt = db.prepareStatement(
                "insert into s466080.vehicles (name, coordinates_x, coordinates_y, creation_date, engine_power, vehicle_type, fuel_type, user_id) values (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        );
        stmt.setString(1, vehicle.getName());
        stmt.setFloat(2, vehicle.getCoordinates().getX());
        stmt.setInt(3, vehicle.getCoordinates().getY());
        stmt.setTimestamp(4, Timestamp.valueOf(vehicle.getCreationDate().toLocalDateTime()));
        stmt.setFloat(5, vehicle.getPower());
        stmt.setString(6, vehicle.getType().name());
        stmt.setString(7, vehicle.getFuelType().name());
        stmt.setString(8, userId);
        int rows = stmt.executeUpdate();
        if (rows > 0) {
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                vehicle.setId(rs.getLong(1));
                return true;
            }
        }
        return false;
    }

    public synchronized boolean updateVehicle(long id, Vehicle vehicle, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        if (vehicle.getName() == null || vehicle.getName().isEmpty() ||
                vehicle.getCoordinates() == null || vehicle.getPower() <= 0 ||
                vehicle.getType() == null || vehicle.getFuelType() == null ||
                userId == null) {
            throw new SQLException("некорректные данные машины или пользователь");
        }
        PreparedStatement stmt = db.prepareStatement(
                "update s466080.vehicles set name = ?, coordinates_x = ?, coordinates_y = ?, creation_date = ?, engine_power = ?, vehicle_type = ?, fuel_type = ? where id = ? and user_id = ?"
        );
        stmt.setString(1, vehicle.getName());
        stmt.setFloat(2, vehicle.getCoordinates().getX());
        stmt.setInt(3, vehicle.getCoordinates().getY());
        stmt.setTimestamp(4, Timestamp.valueOf(vehicle.getCreationDate().toLocalDateTime()));
        stmt.setFloat(5, vehicle.getPower());
        stmt.setString(6, vehicle.getType().name());
        stmt.setString(7, vehicle.getFuelType().name());
        stmt.setLong(8, id);
        stmt.setString(9, userId);
        int rows = stmt.executeUpdate();
        if (rows > 0) {
            vehicle.setId(id);
            return true;
        }
        return false;
    }

    public synchronized boolean removeVehicle(long id, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        if (userId == null) throw new SQLException("пользователь не указан");
        PreparedStatement stmt = db.prepareStatement("delete from s466080.vehicles where id = ? and user_id = ?");
        stmt.setLong(1, id);
        stmt.setString(2, userId);
        int rows = stmt.executeUpdate();
        return rows > 0;
    }

    public synchronized boolean canModify(long id, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        if (userId == null) return false;
        PreparedStatement stmt = db.prepareStatement("select user_id from s466080.vehicles where id = ?");
        stmt.setLong(1, id);
        ResultSet rs = stmt.executeQuery();
        return rs.next() && rs.getString("user_id").equals(userId);
    }

    public synchronized boolean authenticateUser(String login, String password) throws SQLException {
        if (db == null) return false;
        System.out.println("аутентификация для " + login + ", пароль: " + password);
        PreparedStatement stmt = db.prepareStatement("select password from s466080.users where login = ?");
        stmt.setString(1, login);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String storedPassword = rs.getString("password");
            String hashedInput = md5(password);
            System.out.println("хеш в базе: " + storedPassword + ", введенный хеш: " + hashedInput);
            boolean result = storedPassword.equals(hashedInput);
            System.out.println("результат аутентификации: " + result);
            rs.close();
            stmt.close();
            return result;
        }
        System.out.println("пользователь " + login + " не найден");
        rs.close();
        stmt.close();
        return false;
    }

    public synchronized boolean registerUser(String login, String password) throws SQLException {
        if (db == null) return false;
        System.out.println("проверка пользователя " + login);
        PreparedStatement stmt = db.prepareStatement("select password from s466080.users where login = ?");
        stmt.setString(1, login);
        System.out.println("выполняем select для " + login);
        ResultSet rs = stmt.executeQuery();
        System.out.println("select выполнен");
        if (rs.next()) {
            System.out.println("пользователь " + login + " уже существует");
            rs.close();
            stmt.close();
            return false;
        }
        rs.close();
        stmt.close();
        System.out.println("выполняем insert для " + login);
        stmt = db.prepareStatement("insert into s466080.users (login, password) values (?, ?)");
        stmt.setString(1, login);
        stmt.setString(2, md5(password));
        System.out.println("выполняем update");
        try {
            stmt.executeUpdate();
            System.out.println("insert успешен для " + login);
            stmt.close();
            return true;
        } catch (SQLException e) {
            System.out.println("ошибка insert: " + e.getMessage());
            if (e.getSQLState().equals("23505")) {
                stmt.close();
                return false;
            }
            throw e;
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}