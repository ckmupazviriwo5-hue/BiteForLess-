package com.example.biteforlessfoodbridge.model;

/** Local vendor (demo data). */
public class Vendor {
    private final String id;
    private String name;
    private String location;
    private VendorStatus status;

    public Vendor(String id, String name, String location, VendorStatus status) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.status = status;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public VendorStatus getStatus() { return status; }
    public void setStatus(VendorStatus status) { this.status = status; }

    public boolean isActive() { return status == VendorStatus.APPROVED; }
}
