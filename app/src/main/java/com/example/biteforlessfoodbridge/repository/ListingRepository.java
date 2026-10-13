package com.example.biteforlessfoodbridge.repository;

import com.example.biteforlessfoodbridge.model.FoodListing;

import java.util.List;

/** Single source of truth contract for listings (Room impl lands in Phase 6). */
public interface ListingRepository {
    List<FoodListing> getAll();
    FoodListing getById(String id);
    List<FoodListing> search(String query);
    void save(FoodListing listing);
    void delete(String id);
}
