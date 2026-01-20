# MODULE KNOWLEDGE BASE

Generated: 2026-01-20

Commit: e716b3e657e0e3b054b70a4920e882446579b8e0

Branch: kafka-native

## OVERVIEW
Adds Micronaut Control Panel UI integration to the test resources server for monitoring resolvers, property resolutions, and Docker health.

## STRUCTURE
```
./test-resources-control-panel/
├── src/main/java/io/micronaut/testresources/controlpanel/
│   ├── ControlPanelPropertyResolutionListener.java
│   ├── DockerHealth.java
│   ├── DockerHealthControlPanel.java
│   ├── Status.java
│   ├── TestResourcesContainer.java
│   ├── TestResourcesControlPanel.java
│   ├── TestResourcesControlPanelBody.java
│   ├── TestResourcesPanelRegistration.java
├── src/main/resources/views/
│   ├── docker/
│   │   ├── body.hbs
│   │   ├── detail.hbs
│   ├── test-resources/
│   │   ├── body.hbs
│   │   ├── detail.hbs
├── build.gradle
```

## WHERE TO LOOK
- Panel widgets: TestResourcesControlPanel.java (per-resolver panels), DockerHealthControlPanel.java (Docker status)
- Configurations: ControlPanelPropertyResolutionListener.java (resolution event listener), build.gradle (deps: micronaut-control-panel-ui, testcontainers, test-resources-server)
- Views: src/main/resources/views/*/*.hbs (Handlebars templates for panel body and details)
- Registration: TestResourcesPanelRegistration.java (dynamic panel bean registration)

No custom controllers; leverages Micronaut Control Panel framework for UI rendering and routes.

## CODE MAP
Small, focused Java module (~8 classes) extending Micronaut Control Panel abstractions. Integrates with test resources resolvers via listeners and loaders.

## CONVENTIONS
- Endpoints: /control-panel (localhost-only access by default)
- Auth: None specified; inherits from server config (enable via micronaut.control-panel.enabled=true)
- Naming: Panel IDs like 'test-resources-jdbc-mysql'; uses resolver IDs for uniqueness
- Views: Separate body.hbs (summary) and detail.hbs (expanded) per panel type

## ANTI-PATTERNS (THIS MODULE)
- Tight coupling to server internals: Do not directly access TestResourcesService; use PropertyResolutionListener interface
- Static panel definitions: Prefer dynamic registration over hardcoded beans to support variable resolvers

## UNIQUE STYLES
- Dynamic creation of panels at startup based on discovered TestResourcesResolvers
- Real-time Docker status via TestContainers API queries
- Capture of resolution events (success/errors) for display without core modifications

## COMMANDS
```bash
./gradlew :micronaut-test-resources-control-panel:build   # Build module
./gradlew :micronaut-test-resources-server:run            # Run server with control panel enabled
```

## NOTES
- Module depends on test-resources-server; enables UI at /control-panel for inspecting resolvers and Docker.
- Displays resolved properties, errors, container lists, and health status.
- No dedicated tests; relies on server integration testing.
