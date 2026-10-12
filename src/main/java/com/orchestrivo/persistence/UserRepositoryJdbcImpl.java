package com.orchestrivo.persistence;

import com.orchestrivo.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepositoryJdbcImpl implements UserRepository {

    public UserRepositoryJdbcImpl() {
        ensureSchema();
    }

    private void ensureSchema() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            boolean hasFullName = false;
            try (ResultSet rs = meta.getColumns(null, null, "users", "full_name")) {
                if (rs.next()) {
                    hasFullName = true;
                }
            }
            if (!hasFullName) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN full_name VARCHAR(255) NULL AFTER email");
                } catch (SQLException ignored) {}
            }
        } catch (Exception ignored) {}
    }

    @Override
    public User save(User user) {
        String sql;
        boolean isUpdate = user.getId() > 0;
        
        if (isUpdate) {
            sql = "UPDATE users SET email = ?, full_name = ?, password_hash = ?, role = ?, is_active = ? WHERE id = ?";
        } else {
            sql = "INSERT INTO users (email, full_name, password_hash, role, is_active) VALUES (?, ?, ?, ?, ?)";
        }

        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole());
            pstmt.setBoolean(5, user.isActive());
            
            if (isUpdate) {
                pstmt.setLong(6, user.getId());
                pstmt.executeUpdate();
            } else {
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
                    }
                }
            }
            
            // Reload to get generated timestamps
            return findById(user.getId()).orElse(user);
            
        } catch (SQLException e) {
            throw new RuntimeException("Error saving user", e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by email", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all users", e);
        }
        return users;
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DataSourceManager.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user by id", e);
        }
    }
    
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        try {
            user.setFullName(rs.getString("full_name"));
        } catch (SQLException ignored) {}
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
}
