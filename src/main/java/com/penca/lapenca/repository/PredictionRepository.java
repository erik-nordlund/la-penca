package com.penca.lapenca.repository;

import com.penca.lapenca.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
}