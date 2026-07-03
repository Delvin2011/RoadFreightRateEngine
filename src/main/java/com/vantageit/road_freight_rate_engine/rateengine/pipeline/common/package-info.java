/**
 * Pure logic shared across multiple stages of the rate engine's 8-stage computation pipeline
 * (e.g. chargeable weight, reused by Stage 1 validation, Stage 5 vehicle selection and Stage 6
 * base freight). No persistence, no HTTP concerns — just the shared calculations themselves.
 */
package com.vantageit.road_freight_rate_engine.rateengine.pipeline.common;
