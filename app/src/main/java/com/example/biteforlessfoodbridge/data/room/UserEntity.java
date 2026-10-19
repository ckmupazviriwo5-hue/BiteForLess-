package com.example.biteforlessfoodbridge.data.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String email;
    private String passwordHash;
    private String salt;
    private String name;
    private String role; // CUSTOMER, VENDOR, ADMIN
    private String phone;
    private String businessName;
    private String businessCategory;
    private String whatsappNumber;
    private String location;
    private long createdAt;

    public UserEntity(@NonNull String id, String email, String passwordHash, String salt,
                      String name, String role, String phone, String businessName,
                      String businessCategory, String whatsappNumber, String location, long createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.businessName = businessName;
        this.businessCategory = businessCategory;
        this.whatsappNumber = whatsappNumber;
        this.location = location;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
