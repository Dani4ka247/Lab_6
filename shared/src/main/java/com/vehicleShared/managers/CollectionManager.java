package com.vehicleShared.managers;

import com.vehicleShared.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class CollectionManager extends ConcurrentHashMap<Long, Vehicle> {
    public final LocalDateTime initializationDate;
    private static Connection db;

    public CollectionManager() {
        this.initializationDate = LocalDateTime.now();
    }

    public boolean initDb(String url, String user, String password) {
        try {
            db = DriverManager.getConnection(url, user, password);
            return true;
        } catch (SQLException e) {
            System.err.println("ошибка подключения к бд: " + e.getMessage());
            return false;
        }
    }

    public boolean authenticateUser(String login, String password) throws SQLException {
        if (db == null) return false;
        PreparedStatement stmt = db.prepareStatement("select password from s466080.users where login = ?");
        stmt.setString(1, login);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String storedPassword = rs.getString("password");
            return storedPassword.equals(md5(password)); // исправлено
        }
        return false;
    }

    public boolean registerUser(String login, String password) throws SQLException {
        if (db == null) return false;
        PreparedStatement stmt = db.prepareStatement("insert into s466080.users (login, password) values (?, ?)");
        stmt.setString(1, login);
        stmt.setString(2, md5(password));
        try {
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public void loadFromDb() throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        Statement stmt = db.createStatement();
        ResultSet rs = stmt.executeQuery("select * from s466080.vehicles");
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
            put(rs.getLong("id"), vehicle); // исправлено
        }
    }

    public String md5(String input) {
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

    public String getSortedVehiclesByPower() {
        return entrySet()
                .stream()
                .sorted((e1, e2) -> Float.compare(e1.getValue().getPower(), e2.getValue().getPower()))
                .map(entry -> entry.getKey() + " : " + entry.getValue().toString())
                .collect(Collectors.joining("\n"));
    }

    public String getVehiclesByMinPower(float minimumPower) {
        return entrySet()
                .stream()
                .filter(entry -> entry.getValue().getPower() >= minimumPower)
                .map(entry -> entry.getKey() + " : " + entry.getValue().toString())
                .collect(Collectors.joining("\n"));
    }

    public boolean addVehicle(Vehicle vehicle, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        PreparedStatement stmt = db.prepareStatement(
                "insert into s466080.vehicles (name, coordinates_x, coordinates_y, creation_date, engine_power, vehicle_type, fuel_type, user_id) values (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        );
        stmt.setString(1, vehicle.getName());
        stmt.setFloat(2, vehicle.getCoordinates().getX());
        stmt.setFloat(3, vehicle.getCoordinates().getY());
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
                put(vehicle.getId(), vehicle);
                return true;
            }
        }
        return false;
    }

    public boolean updateVehicle(long id, Vehicle vehicle, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        PreparedStatement stmt = db.prepareStatement(
                "update s466080.vehicles set name = ?, coordinates_x = ?, coordinates_y = ?, creation_date = ?, engine_power = ?, vehicle_type = ?, fuel_type = ? where id = ? and user_id = ?"
        );
        stmt.setString(1, vehicle.getName());
        stmt.setFloat(2, vehicle.getCoordinates().getX());
        stmt.setFloat(3, vehicle.getCoordinates().getY());
        stmt.setTimestamp(4, Timestamp.valueOf(vehicle.getCreationDate().toLocalDateTime()));
        stmt.setFloat(5, vehicle.getPower());
        stmt.setString(6, vehicle.getType().name());
        stmt.setString(7, vehicle.getFuelType().name());
        stmt.setLong(8, id);
        stmt.setString(9, userId);
        int rows = stmt.executeUpdate();
        if (rows > 0) {
            vehicle.setId(id);
            put(id, vehicle);
            return true;
        }
        return false;
    }

    public boolean removeVehicle(long id, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        PreparedStatement stmt = db.prepareStatement("delete from s466080.vehicles where id = ? and user_id = ?");
        stmt.setLong(1, id);
        stmt.setString(2, userId);
        int rows = stmt.executeUpdate();
        if (rows > 0) {
            remove(id);
            return true;
        }
        return false;
    }

    public boolean canModify(long id, String userId) throws SQLException {
        if (db == null) throw new SQLException("база не подключена");
        PreparedStatement stmt = db.prepareStatement("select user_id from s466080.vehicles where id = ?");
        stmt.setLong(1, id);
        ResultSet rs = stmt.executeQuery();
        return rs.next() && rs.getString("user_id").equals(userId);
    }

    public static Vehicle requestVehicleInformation(Scanner scanner, long id) {
        String vehicleName = InputValidator.getValidInput(scanner, s -> s, "введите название машины: ", "неужели так сложно название ввести?!");
        Coordinates coordinates = InputValidator.getValidInput(scanner, Coordinates::parser, "введите координаты машины в формате x,y: ", "тебе надо ввести два числа через запятую(x<982,y<67). например 22.8,7");
        Float enginePower = InputValidator.getValidInput(scanner, Float::parseFloat, "введите мощность двигателя машины: ", "введи число");
        VehicleType vehicleType = InputValidator.getValidInput(scanner, s -> VehicleType.values()[Integer.parseInt(s.trim()) - 1], "выберите тип машины {1:CAR, 2:BOAT, 3:HOVERBOARD}: ", "тебе стоит ввести номер нужного значения");
        FuelType fuelType = InputValidator.getValidInput(scanner, s -> FuelType.values()[Integer.parseInt(s.trim()) - 1], "выберите тип топлива {1:GASOLINE, 2:KEROSENE, 3:ELECTRICITY, 4:MANPOWER, 5:NUCLEAR}: ", "тебе стоит ввести номер нужного значения");
        return new Vehicle(id, coordinates, vehicleName, enginePower, vehicleType, fuelType);
    }
}