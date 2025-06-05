package com.vehicleShared.managers;

import com.vehicleShared.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class DbManager {
    private static final Logger log = LoggerFactory.getLogger(DbManager.class);
    private Connection db;

    public boolean isDbConnected() {
        try {
            return db != null && !db.isClosed() && db.isValid(2);
        } catch (SQLException e) {
            log.error("ошибка проверки подключения: {}", e.getMessage());
            return false;
        }
    }

    public boolean initDb(String url, String user, String password) {
        try {
            log.info("попытка подключения к {} с пользователем {}", url, user);
            db = DriverManager.getConnection(url, user, password);
            db.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            log.error("ошибка подключения: {}, SQL state: {}", e.getMessage(), e.getSQLState());
            return false;
        }
    }

    public void closeDb() {
        if (isDbConnected()) {
            try {
                db.close();
                log.info("база закрыта");
            } catch (SQLException e) {
                log.error("ошибка закрытия базы: {}", e.getMessage());
            }
        }
    }

    public synchronized List<Vehicle> loadFromDb(String userId, boolean loadAll) throws SQLException {
        if (!isDbConnected()) throw new SQLException("база не подключена");
        List<Vehicle> vehicles = new ArrayList<>();
        String sql = loadAll ? "select * from s466080.vehicles" : "select * from s466080.vehicles where user_id = ?";
        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            if (!loadAll) stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
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
            }
        }
        log.info("загружено {} записей для userId={}", vehicles.size(), loadAll ? "все" : userId);
        return vehicles;
    }

    public synchronized boolean authenticateUser(String login, String password) throws SQLException {
        if (!isDbConnected()) throw new SQLException("база не подключена");
        try (PreparedStatement stmt = db.prepareStatement("select password from s466080.users where login = ?")) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    String hashedInput = md5(password);
                    return storedPassword.equals(hashedInput);
                }
            }
        }
        return false;
    }

    public synchronized boolean registerUser(String login, String password) throws SQLException {
        if (!isDbConnected()) throw new SQLException("база не подключена");
        try (PreparedStatement stmt = db.prepareStatement("select password from s466080.users where login = ?")) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return false;
            }
        }
        try (PreparedStatement stmt = db.prepareStatement("insert into s466080.users (login, password) values (?, ?)")) {
            stmt.setString(1, login);
            stmt.setString(2, md5(password));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) return false;
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

    public boolean addVehicle(Vehicle vehicle, String userId) throws SQLException {
        if (!isDbConnected()) throw new SQLException("база не подключена");
        String sql = "insert into s466080.vehicles (name, coordinates_x, coordinates_y, creation_date, engine_power, vehicle_type, fuel_type, user_id) values (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, vehicle.getName());
            stmt.setFloat(2, vehicle.getCoordinates().getX());
            stmt.setInt(3, vehicle.getCoordinates().getY());
            stmt.setTimestamp(4, Timestamp.from(vehicle.getCreationDate().toInstant()));
            stmt.setFloat(5, vehicle.getPower());
            stmt.setString(6, vehicle.getType().name());
            stmt.setString(7, vehicle.getFuelType().name());
            stmt.setString(8, userId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    vehicle.setId(rs.getLong(1)); // устанавливаем id
                    return true;
                }
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
}