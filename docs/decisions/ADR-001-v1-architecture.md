# ADR-001 — Minimum AI-SRE Architecture v1

## Status

Accepted

## Context

The AI-SRE Platform is a practical learning and portfolio project intended to develop hands-on understanding of:

- AI agents;
- microservices;
- distributed systems;
- Kafka;
- data engineering;
- Big Data;
- observability;
- Site Reliability Engineering (SRE);
- production engineering.

The project must remain runnable on a laptop with limited resources.

The architecture should therefore start small and evolve based on real requirements rather than attempting to reproduce a complete enterprise technology stack from the beginning.

## Decision

The first end-to-end implementation will contain:

- two Spring Boot application services;
- Apache Kafka;
- structured application logs;
- OpenTelemetry where useful;
- Loki;
- Grafana;
- deterministic detection/alerting;
- PostgreSQL for incident persistence;
- an AI-SRE investigation agent;
- Docker Compose for local execution.

The first complete scenario will be Payment Service degradation.

## Why this architecture

### Spring Boot

Provides a realistic microservice environment and aligns with the project's Java/Spring background.

### Kafka

Introduces asynchronous communication, event-driven architecture, consumer behaviour and later data-engineering possibilities.

### Structured logs

Provide the simplest useful initial telemetry source while establishing good operational logging practices.

### Loki and Grafana

Provide an established way to store, query and visualize logs without building a custom observability platform.

### Deterministic detection

Provides predictable and explainable incident detection for v1.

Machine-learning-based detection is intentionally postponed until deterministic detection and observability are understood.

### PostgreSQL

Provides simple persistent storage for incident state without introducing an additional specialized database.

### AI-SRE Agent

This is the primary custom intelligence component.

It investigates incidents using evidence instead of replacing established monitoring systems.

### Docker Compose

Provides a practical local deployment model while keeping the initial environment manageable.

## Alternatives deliberately rejected for v1

### Kubernetes

Rejected because orchestration is not required to demonstrate the first AI-SRE workflow and would add significant operational complexity.

### Apache Spark

Rejected from v1 because the initial failure scenario does not require distributed data processing. Spark will be introduced when the platform evolves into genuine Big Data/data-processing scenarios.

### Machine-learning anomaly detection

Rejected initially because deterministic detection is easier to understand, reproduce and validate.

### Large observability suites

Rejected because the learning objective is understanding observability and building the AI-SRE layer, not learning multiple commercial observability products simultaneously.

### Custom log/monitoring engine

Rejected because mature monitoring and observability tools already solve these problems. The project should focus custom engineering effort on incident correlation and AI investigation.

### Autonomous remediation

Rejected because v1 focuses on detection and investigation. Remediation should initially remain human-approved and advisory.

## Consequences

### Positive

- Small enough to run locally.
- Provides a complete end-to-end engineering story.
- Establishes realistic boundaries between application, messaging, observability, detection, incident management and AI investigation.
- Creates a foundation for later Big Data and distributed-systems learning.
- Avoids unnecessary technology complexity.

### Negative

- v1 will not represent the full complexity of a large production platform.
- Logs alone cannot provide every type of operational insight.
- Detection will initially be less sophisticated than machine-learning-based anomaly detection.
- Some production concerns will intentionally be deferred.

These limitations are deliberate and will be addressed as the platform evolves.