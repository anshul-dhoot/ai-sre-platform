# Minimum End-to-End AI-SRE Demo — v1

## 1. Purpose

Build the smallest production-shaped AI-SRE platform that can detect a realistic application failure, collect relevant evidence, create an incident, and use an AI agent to investigate and explain the likely cause.

The platform is intentionally laptop-friendly and will evolve incrementally.

## 2. v1 Success Criteria

A complete v1 demonstration must show:

1. Two application services running locally.
2. An event flowing through Kafka.
3. Structured application logs being generated.
4. Logs being collected and queried through the observability stack.
5. A deterministic failure being detected.
6. An incident being created and persisted.
7. The AI-SRE agent receiving the incident and investigating available evidence.
8. The agent producing an evidence-backed investigation report.

The first failure scenario is Payment Service degradation.

## 3. v1 Application Flow

```text
Order Service
     |
     | OrderCreated
     v
   Kafka
     |
     v
Payment Service
     |
     v
Structured Logs
     |
     v
Loki / Grafana
     |
     v
Detection / Alert
     |
     v
Incident
     |
     v
AI-SRE Agent
     |
     v
Investigation Report
```

## 4. Application Services

### Order Service

Responsibilities:

- Accept a simulated order.
- Create an `OrderCreated` event.
- Publish the event to Kafka.
- Produce structured operational logs.

### Payment Service

Responsibilities:

- Consume `OrderCreated` events.
- Simulate payment processing.
- Produce structured success, warning and error logs.
- Support an intentionally injectable degradation/failure for testing.

Only these two services are required for v1.

## 5. Kafka

Kafka provides asynchronous communication between Order Service and Payment Service.

The initial event is:

`OrderCreated`

The event contract should be simple but structured so that future event types can be introduced without redesigning the platform.

Kafka is part of the application/data flow, not the observability mechanism.

## 6. Telemetry

v1 starts with structured application logs.

Logs should:

- be machine-readable;
- contain timestamp and service identity;
- have an appropriate severity;
- represent meaningful business or operational activity;
- include correlation information where applicable;
- avoid sensitive information;
- provide useful evidence for investigation.

Metrics and distributed traces are intentionally deferred.

The telemetry/evidence design should allow additional evidence types to be introduced later.

## 7. Observability

v1 uses established observability tooling rather than implementing a custom logging platform.

Initial stack:

- OpenTelemetry where useful for instrumentation/collection.
- Loki for log storage and querying.
- Grafana for visualization and investigation.

The platform should treat logs as one evidence source rather than making logs the permanent foundation of all investigation.

Future evidence sources may include metrics, traces, Kafka signals, Airflow failures, Spark failures and data-quality signals.

## 8. Detection

v1 uses deterministic detection.

Examples:

- payment failure rate exceeds a defined threshold;
- payment latency exceeds a defined threshold;
- a critical payment failure condition occurs.

Detection should rely on existing monitoring/alerting capabilities where practical.

The custom platform should not attempt to recreate a general-purpose monitoring engine.

Machine-learning-based anomaly detection is out of scope for v1.

## 9. Incident

A significant detected condition becomes an incident.

An incident contains enough information to begin investigation, including:

- incident ID;
- severity;
- detection time;
- affected service;
- triggering condition;
- observed symptoms;
- relevant evidence references;
- current status.

Incident state should be persisted in PostgreSQL.

## 10. AI-SRE Agent

The AI-SRE agent investigates incidents rather than continuously monitoring every application event.

For v1 it should be able to:

- retrieve incident information;
- retrieve relevant log evidence;
- inspect the incident timeline;
- form competing hypotheses;
- compare hypotheses against available evidence;
- explain the reasoning;
- identify the most likely cause;
- state uncertainty/confidence;
- produce an investigation report.

The agent is advisory.

It must not autonomously:

- deploy code;
- restart services;
- modify infrastructure;
- delete data;
- change production configuration.

## 11. First Failure Scenario

Payment Service is deliberately degraded.

Expected sequence:

```text
Payment processing slows/fails
        |
        v
Payment logs show degradation
        |
        v
Detection condition is met
        |
        v
Incident created
        |
        v
AI-SRE Agent investigates
        |
        v
Agent correlates evidence
        |
        v
Evidence-backed diagnosis
```

The initial implementation should make the failure deterministic and reproducible.

## 12. Runtime

Docker Compose will be used for the local environment.

The architecture must remain practical for an 8 GB RAM / 256 GB Windows laptop.

Resource-heavy technologies should not be introduced unless they provide a clear learning or platform benefit.

## 13. Explicitly Out of Scope for v1

- Kubernetes
- Apache Spark
- streaming data processing beyond the basic Kafka flow
- machine-learning anomaly detection
- autonomous remediation
- large-scale data lake
- vector database
- service mesh
- complex API gateway
- full enterprise observability suite
- large number of microservices

These may be introduced later only when a real platform or learning requirement justifies them.

## 14. Evolution

The platform will evolve through successive learning phases:

```text
v1
AI-SRE + basic logs + Kafka
        |
        v
More realistic microservices
        |
        v
Airflow + batch data processing
        |
        v
ETL / ELT + data-quality failures
        |
        v
Spark + distributed processing
        |
        v
Streaming + richer telemetry
        |
        v
AI-SRE v2
```

Each phase should introduce a real engineering problem rather than adding technology for its own sake.